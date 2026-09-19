package com.saku.speak

import com.saku.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class GeminiConversationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendConversationTurn(
        apiKey: String,
        messages: List<ChatMessage>,
        preferredModel: String = PreferencesManager.DEFAULT_GEMINI_MODEL
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is required. Please set it in Cards -> Settings."))
        }

        val cleanModel = preferredModel.ifBlank { PreferencesManager.DEFAULT_GEMINI_MODEL }

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=${apiKey.trim()}"

        // System prompt: natural, grounded, everyday conversational Japanese with few-shot examples.
        // Avoid theatrical tropes, exaggerated prolonged vowels (〜), and multiple exclamation marks.
        // Keep responses very short and snappy (15 to 35 characters, 1 short sentence) so audio generation is fast.
        val systemInstruction = JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", "You are a friendly Japanese conversational partner named Saku.\n\n" +
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
                            "Saku: 素晴らしいですね！一緒に楽しく練習していきましょう。")
                })
            })
        }

        // Convert the last 10 messages into Gemini's multi-turn contents format.
        // Gemini strictly requires the first turn to be from "user" and turns to alternate.
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

        val generationConfig = JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 65)
        }

        val requestBodyJson = JSONObject().apply {
            put("system_instruction", systemInstruction)
            put("contents", contentsArray)
            put("generationConfig", generationConfig)
        }

        val requestBody = requestBodyJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = parseErrorMessage(bodyStr, response.code)
                    return@withContext Result.failure(IOException(errorMsg))
                }

                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(IOException("No response received from Gemini"))
                }

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val rawText = parts?.optJSONObject(0)?.optString("text", "") ?: ""

                val cleanText = rawText
                    .replace(Regex("[\r\n]+"), " ")
                    .replace(Regex("[*#_`]+"), "")
                    .trim()

                if (cleanText.isBlank()) {
                    Result.failure(IOException("Empty response text from Gemini"))
                } else {
                    Result.success(cleanText)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
