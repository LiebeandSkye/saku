package com.saku.reading

import com.saku.data.AnkiVocabularyItem
import com.saku.data.GeneratedStory
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

class GeminiStoryService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateStory(
        apiKey: String,
        jlptLevel: String,
        vocabularyList: List<AnkiVocabularyItem>,
        preferredModel: String = "gemini-2.5-flash"
    ): Result<GeneratedStory> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is required"))
        }

        // Select a sample of words (up to 25 studied, up to 10 suspended)
        val studied = vocabularyList.filter { !it.isSuspended }.shuffled().take(25)
        val suspended = vocabularyList.filter { it.isSuspended }.shuffled().take(10)
        val selectedWords = (studied + suspended).distinctBy { it.displayWord }

        val wordPromptList = if (selectedWords.isNotEmpty()) {
            selectedWords.joinToString(", ") { item ->
                val word = item.displayWord
                val reading = if (item.reading.isNotBlank() && item.reading != word) " (${item.reading})" else ""
                val meaning = if (item.meaning.isNotBlank()) " [${item.meaning.take(30)}]" else ""
                "$word$reading$meaning"
            }
        } else {
            "Use natural vocabulary appropriate for JLPT $jlptLevel."
        }

        val prompt = buildJlptStoryPrompt(jlptLevel, wordPromptList)

        // Try preferred model first, then fallback to gemini-1.5-flash
        val modelsToTry = listOf(preferredModel, "gemini-2.5-flash", "gemini-1.5-flash").distinct()

        var lastError: Exception? = null
        for (model in modelsToTry) {
            try {
                val story = callGeminiApi(apiKey, model, prompt, jlptLevel, selectedWords.map { it.displayWord })
                return@withContext Result.success(story)
            } catch (e: Exception) {
                lastError = e
                // If it was an authentication error, don't retry other models
                if (e.message?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
                    e.message?.contains("403", ignoreCase = true) == true
                ) {
                    break
                }
            }
        }

        Result.failure(lastError ?: IOException("Failed to generate story with Gemini API"))
    }

    private fun callGeminiApi(
        apiKey: String,
        model: String,
        prompt: String,
        jlptLevel: String,
        targetWords: List<String>
    ): GeneratedStory {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", prompt)
                        }
                        put(partObj)
                    }
                    put("parts", partsArr)
                }
                put(contentObj)
            }
            put("contents", contentsArr)

            val generationConfig = JSONObject().apply {
                put("temperature", 0.75)
                put("maxOutputTokens", 2048)
            }
            put("generationConfig", generationConfig)
        }

        val requestBody = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errorJson = JSONObject(responseBody)
                errorJson.optJSONObject("error")?.optString("message") ?: response.message
            } catch (e: Exception) {
                "HTTP ${response.code}: ${response.message}"
            }
            throw IOException(errorMsg)
        }

        val responseJson = JSONObject(responseBody)
        val candidates = responseJson.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            throw IOException("No response generated from Gemini")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val contentObj = firstCandidate.optJSONObject("content")
        val partsArr = contentObj?.optJSONArray("parts")
        if (partsArr == null || partsArr.length() == 0) {
            throw IOException("Empty response parts from Gemini")
        }

        val rawText = partsArr.getJSONObject(0).optString("text", "").trim()
        if (rawText.isBlank()) {
            throw IOException("Story text was empty")
        }

        // Parse title and body
        var title = "日本語の物語 ($jlptLevel)"
        var body = rawText

        val lines = rawText.lines()
        if (lines.isNotEmpty()) {
            val firstLine = lines.first().trim()
            if (firstLine.startsWith("タイトル:") || firstLine.startsWith("タイトル：") || firstLine.startsWith("#")) {
                title = firstLine.removePrefix("タイトル:").removePrefix("タイトル：").removePrefix("#").trim()
                body = lines.drop(1).joinToString("\n").trim()
            }
        }

        return GeneratedStory(
            id = UUID.randomUUID().toString(),
            title = title,
            content = body,
            jlptLevel = jlptLevel,
            createdAt = System.currentTimeMillis(),
            targetWords = targetWords
        )
    }

    private fun buildJlptStoryPrompt(jlptLevel: String, wordPromptList: String): String {
        val levelRules = when (jlptLevel) {
            "N5" -> "Strictly JLPT N5: Use elementary ~です/~ます forms, simple sentence conjunctions (そして, でも, だから), and basic particles (は, が, を, に, で, へ, と, も). Keep sentences short, relatable, and clear."
            "N4" -> "Strictly JLPT N4: Use compound sentences, ~て-forms, conditions (~たら, ~なら), comparisons, and basic potential or volitional forms."
            "N3" -> "Strictly JLPT N3: Use natural everyday storytelling with transitional grammar (~ように, ~わけ, ~について, ~に対して) and intermediate sentence flow."
            "N2" -> "Strictly JLPT N2: Use nuanced narrative prose, varied sentence structures, idiomatic expressions, and pre-advanced Japanese vocabulary."
            "N1" -> "Strictly JLPT N1: Use literary, sophisticated Japanese expression, subtle nuances, and rich descriptive vocabulary."
            else -> "Use natural Japanese suitable for JLPT $jlptLevel."
        }

        return """
            [System Context]
            You are a master Japanese teacher and literary author specializing in immersive graded readers for language learners.

            [Task Objective]
            Compose an engaging, coherent Japanese story calibrated precisely to JLPT $jlptLevel reading comprehension.

            [Level Calibration]
            $levelRules

            [Learner Vocabulary to Integrate]
            Naturally integrate as many of these flashcard words from the student's study deck as possible into the narrative context:
            $wordPromptList

            [Negative Constraints]
            - Do NOT include any romaji.
            - Do NOT include any English translations, vocabulary glossaries, footnotes, or commentary.
            - Do NOT include furigana brackets like [ふりがな] or HTML ruby tags. Write clean standard Japanese characters.
            - Output pure Japanese story text only.

            [Output Format]
            Line 1: タイトル: [Japanese Story Title]
            Line 2: (Blank line)
            Line 3+: [Story paragraphs separated by empty lines]
        """.trimIndent()
    }
}
