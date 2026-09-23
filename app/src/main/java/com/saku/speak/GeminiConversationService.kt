package com.saku.speak

import android.util.Base64
import com.saku.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class VoiceTurnResult(
    val userTranscript: String,
    val aiReply: String
)

class GeminiConversationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getCandidateModels(preferredModel: String): List<String> {
        val primary = preferredModel.trim().ifBlank { PreferencesManager.DEFAULT_GEMINI_MODEL }
        return listOf(
            primary,
            "gemini-2.5-flash-lite",
            "gemini-2.5-flash",
            "gemini-2.0-flash"
        ).distinct()
    }

    private fun buildSystemInstruction(customSystemInstruction: String? = null): JSONObject {
        val hasCustomPrompt = !customSystemInstruction.isNullOrBlank()
        val systemPromptText = if (hasCustomPrompt) {
            customSystemInstruction!!.trim() + "\n\n" +
                "Voice Output Note: Your response will be spoken aloud directly to the user via text-to-speech. " +
                "Respond naturally in the exact persona, style, tone, language, and length specified by the system instruction above, " +
                "and output plain spoken text without markdown symbols (*, #, _)."
        } else {
            "You are a friendly Japanese conversational partner named Saku.\n\n" +
                "Role & Persona:\n" +
                "- Speak in natural, everyday conversational Japanese (standard polite-casual blend: です・ます with warm conversational flow).\n" +
                "- Sound like a real, helpful Japanese friend in daily life—not exaggerated, not theatrical, not an anime caricature.\n" +
                "- Use natural conversational interjections (相槌: 「そうなんですね！」「それは楽しみですね」「分かります」).\n\n" +
                "Constraints:\n" +
                "- Strictly 1 short, brisk sentence (15 to 35 characters). Fast responses ensure seamless real-time voice synthesis.\n" +
                "- Never use drawn-out punctuation like 『〜』, multiple exclamation marks 『！！』, or excessive ellipses 『…』.\n" +
                "- Never output markdown, bullet points, romaji, kanji furigana brackets, or translations.\n\n" +
                "Few-Shot Examples:\n" +
                "User: 今日は仕事がとても忙しかったです。\n" +
                "Saku: お疲れ様でした！今夜はゆっくり休んでくださいね。\n\n" +
                "User: 明日は友達と京都へ行きます。\n" +
                "Saku: いいですね！美味しいものをたくさん食べてきてください。\n\n" +
                "User: 日本語の勉強を始めたばかりです。\n" +
                "Saku: 素晴らしいですね！一緒に楽しく練習していきましょう。"
        }

        return JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", systemPromptText)
                })
            })
        }
    }

    suspend fun sendConversationTurn(
        apiKey: String,
        messages: List<ChatMessage>,
        preferredModel: String = PreferencesManager.DEFAULT_GEMINI_MODEL,
        customSystemInstruction: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is required. Please tap 'Setup Key' at the top of the Speak screen."))
        }

        val hasCustomPrompt = !customSystemInstruction.isNullOrBlank()
        val recentMessages = messages.takeLast(10).dropWhile { !it.isUser }
        if (recentMessages.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No user message to send"))
        }

        val sanitizedMessages = mutableListOf<ChatMessage>()
        for (msg in recentMessages) {
            if (sanitizedMessages.isNotEmpty() && sanitizedMessages.last().isUser == msg.isUser) {
                val prev = sanitizedMessages.removeAt(sanitizedMessages.lastIndex)
                sanitizedMessages.add(prev.copy(text = "${prev.text}\n${msg.text}"))
            } else {
                sanitizedMessages.add(msg)
            }
        }

        val contentsArray = JSONArray()
        for (msg in sanitizedMessages) {
            val role = if (msg.isUser) "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", msg.text)
                    })
                })
            })
        }

        val requestBodyJson = JSONObject().apply {
            put("system_instruction", buildSystemInstruction(customSystemInstruction))
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", if (hasCustomPrompt) 0.8 else 0.7)
                put("maxOutputTokens", if (hasCustomPrompt) 220 else 65)
            })
        }

        val payloadStr = requestBodyJson.toString()
        var lastError: Exception = IOException("Failed to connect to Gemini")

        for (model in getCandidateModels(preferredModel)) {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${apiKey.trim()}"
            val request = Request.Builder()
                .url(endpoint)
                .post(payloadStr.toRequestBody(jsonMediaType))
                .build()

            try {
                val (isSuccessful, code, bodyStr) = client.newCall(request).execute().use { resp ->
                    Triple(resp.isSuccessful, resp.code, resp.body?.string() ?: "")
                }

                if (!isSuccessful) {
                    val errorMsg = parseErrorMessage(bodyStr, code)
                    lastError = IOException(errorMsg)
                    if (bodyStr.contains("API_KEY_INVALID", ignoreCase = true) || code == 403) {
                        return@withContext Result.failure(lastError)
                    }
                    continue
                }

                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    lastError = IOException("No response received from Gemini")
                    continue
                }

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val rawText = parts?.optJSONObject(0)?.optString("text", "") ?: ""

                val cleanText = rawText
                    .replace(Regex("[\r\n]+"), " ")
                    .replace(Regex("[*#_`]+"), "")
                    .trim()

                if (cleanText.isNotBlank()) {
                    return@withContext Result.success(cleanText)
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(lastError)
    }

    /**
     * Sends recorded voice audio (WAV/MP4) and conversation history in a single fast multimodal call.
     * Returns both the user's transcribed Japanese speech and Saku's conversational Japanese reply.
     */
    suspend fun sendVoiceTurn(
        apiKey: String,
        audioFile: File,
        priorMessages: List<ChatMessage>,
        preferredModel: String = PreferencesManager.DEFAULT_GEMINI_MODEL,
        customSystemInstruction: String? = null
    ): Result<VoiceTurnResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is required. Please tap 'Setup Key' at the top of the Speak screen."))
        }
        if (!audioFile.exists() || audioFile.length() < 512L) {
            return@withContext Result.failure(IOException("No speech detected"))
        }

        val audioBytes = try {
            audioFile.readBytes()
        } catch (e: Exception) {
            return@withContext Result.failure(IOException("Failed to read recorded audio"))
        }
        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val mimeType = if (audioFile.name.endsWith(".wav", ignoreCase = true)) "audio/wav" else "audio/mp4"
        val hasCustomPrompt = !customSystemInstruction.isNullOrBlank()

        // Include up to last 6 prior messages for conversational context
        val recentMessages = priorMessages.takeLast(6).dropWhile { !it.isUser }
        val sanitizedMessages = mutableListOf<ChatMessage>()
        for (msg in recentMessages) {
            if (sanitizedMessages.isNotEmpty() && sanitizedMessages.last().isUser == msg.isUser) {
                val prev = sanitizedMessages.removeAt(sanitizedMessages.lastIndex)
                sanitizedMessages.add(prev.copy(text = "${prev.text}\n${msg.text}"))
            } else {
                sanitizedMessages.add(msg)
            }
        }
        if (sanitizedMessages.isNotEmpty() && sanitizedMessages.last().isUser) {
            sanitizedMessages.removeAt(sanitizedMessages.lastIndex)
        }

        val contentsArray = JSONArray()
        for (msg in sanitizedMessages) {
            val role = if (msg.isUser) "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", msg.text)
                    })
                })
            })
        }

        // Append the new user voice turn
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("inline_data", JSONObject().apply {
                        put("mime_type", mimeType)
                        put("data", base64Audio)
                    })
                })
                put(JSONObject().apply {
                    put(
                        "text",
                        "Listen carefully to the user's spoken audio above.\n" +
                            "1. Transcribe what the user said into natural Japanese text (kanji/kana). If they spoke English or another language, write what they said in natural Japanese.\n" +
                            "2. Reply in character as Saku.\n" +
                            "If the audio is completely silent or contains no human speech at all, output ONLY:\n" +
                            "USER: SILENCE\n\n" +
                            "Otherwise, output strictly in this exact 2-line format (no markdown, no romaji, no extra lines):\n" +
                            "USER: <what the user said in Japanese>\n" +
                            "SAKU: <Saku's Japanese reply>"
                    )
                })
            })
        })

        val requestBodyJson = JSONObject().apply {
            put("system_instruction", buildSystemInstruction(customSystemInstruction))
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", if (hasCustomPrompt) 0.75 else 0.5)
                put("maxOutputTokens", if (hasCustomPrompt) 240 else 140)
            })
        }

        val payloadStr = requestBodyJson.toString()
        var lastError: Exception = IOException("Could not connect to Gemini")

        for (model in getCandidateModels(preferredModel)) {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${apiKey.trim()}"
            val request = Request.Builder()
                .url(endpoint)
                .post(payloadStr.toRequestBody(jsonMediaType))
                .build()

            try {
                val (isSuccessful, code, bodyStr) = client.newCall(request).execute().use { resp ->
                    Triple(resp.isSuccessful, resp.code, resp.body?.string() ?: "")
                }

                if (!isSuccessful) {
                    val errorMsg = parseErrorMessage(bodyStr, code)
                    lastError = IOException(errorMsg)
                    if (bodyStr.contains("API_KEY_INVALID", ignoreCase = true) || code == 403) {
                        return@withContext Result.failure(lastError)
                    }
                    continue
                }

                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    lastError = IOException("No response from Gemini")
                    continue
                }

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val rawText = parts?.optJSONObject(0)?.optString("text", "")?.trim() ?: ""

                if (rawText.isBlank()) {
                    lastError = IOException("Empty response from Gemini")
                    continue
                }

                val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
                var userTranscript = ""
                var sakuReply = ""

                for (line in lines) {
                    when {
                        line.startsWith("USER:", ignoreCase = true) || line.startsWith("ユーザー:") || line.startsWith("User：") -> {
                            userTranscript = line.substringAfter(":").substringAfter("：").trim()
                        }
                        line.startsWith("SAKU:", ignoreCase = true) || line.startsWith("サク:") || line.startsWith("Saku：") -> {
                            sakuReply = line.substringAfter(":").substringAfter("：").trim()
                        }
                    }
                }

                if (userTranscript.equals("SILENCE", ignoreCase = true) ||
                    userTranscript.equals("NONE", ignoreCase = true) ||
                    rawText.equals("SILENCE", ignoreCase = true)
                ) {
                    return@withContext Result.failure(IOException("No speech detected"))
                }

                if (userTranscript.isBlank() && sakuReply.isBlank()) {
                    if (lines.size >= 2) {
                        userTranscript = lines[0].replace(Regex("[*#_`\"「」『』]+"), "").trim()
                        sakuReply = lines.last().replace(Regex("[*#_`\"「」『』]+"), "").trim()
                    } else {
                        userTranscript = "🎤 (音声入力)"
                        sakuReply = lines[0].replace(Regex("[*#_`\"「」『』]+"), "").trim()
                    }
                } else if (sakuReply.isBlank() && userTranscript.isNotBlank()) {
                    val updatedList = priorMessages + ChatMessage(text = userTranscript, isUser = true)
                    val replyRes = sendConversationTurn(apiKey, updatedList, model, customSystemInstruction)
                    sakuReply = replyRes.getOrElse { "そうなんですね！もう少し詳しく教えてください。" }
                } else if (userTranscript.isBlank() && sakuReply.isNotBlank()) {
                    userTranscript = "🎤 (音声入力)"
                }

                userTranscript = userTranscript.replace(Regex("[*#_`\"「」『』]+"), "").trim()
                sakuReply = sakuReply.replace(Regex("[*#_`\"「」『』]+"), "").trim()

                return@withContext Result.success(
                    VoiceTurnResult(
                        userTranscript = userTranscript,
                        aiReply = sakuReply
                    )
                )
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(lastError)
    }

    private fun parseErrorMessage(body: String, code: Int): String {
        return try {
            val json = JSONObject(body)
            val errorObj = json.optJSONObject("error")
            val message = errorObj?.optString("message") ?: ""
            if (message.isNotBlank()) message else "Gemini API HTTP $code"
        } catch (ignored: Exception) {
            if (body.isNotBlank()) body.take(150) else "Gemini API request failed (HTTP $code)"
        }
    }
}
