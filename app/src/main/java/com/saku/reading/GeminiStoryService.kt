package com.saku.reading

import com.saku.data.AnkiVocabularyItem
import com.saku.data.GeneratedStory
import com.saku.data.PreferencesManager
import com.saku.data.StoryQuizQuestion
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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateStory(
        apiKey: String,
        jlptLevel: String,
        vocabularyList: List<AnkiVocabularyItem>,
        preferredModel: String = PreferencesManager.DEFAULT_GEMINI_MODEL
    ): Result<GeneratedStory> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is required"))
        }

        // Select up to 25 target words from available learned flashcards (studied or suspended)
        val studiedPool = vocabularyList.filter { !it.isSuspended }
        val suspendedPool = vocabularyList.filter { it.isSuspended }

        val selectedWords = if (studiedPool.isNotEmpty() && suspendedPool.isNotEmpty()) {
            (studiedPool.shuffled().take(18) + suspendedPool.shuffled().take(10)).distinctBy { it.displayWord }
        } else if (studiedPool.isNotEmpty()) {
            studiedPool.shuffled().take(25).distinctBy { it.displayWord }
        } else {
            suspendedPool.shuffled().take(25).distinctBy { it.displayWord }
        }

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

        // Directly call preferred model with zero fallbacks to ensure maximum speed.
        // If an error occurs, fail immediately and prompt the user to switch models.
        try {
            val story = callGeminiApi(apiKey, preferredModel, prompt, jlptLevel, selectedWords.map { it.displayWord })
            Result.success(story)
        } catch (e: Exception) {
            val errorDetails = e.message ?: "Unknown error"
            val userMsg = "Model '$preferredModel' failed: $errorDetails\nPlease tap the model selector above to switch to another model (e.g. Gemini 3.5 Flash-Lite or Gemini 3.8 Flash)."
            Result.failure(IOException(userMsg, e))
        }
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
                put("temperature", 0.7)
                put("maxOutputTokens", 2048)
                put("responseMimeType", "application/json")
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
                errorJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}: ${response.message}"
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

        return parseStoryResponse(rawText, jlptLevel, targetWords)
    }

    private fun parseStoryResponse(
        rawText: String,
        jlptLevel: String,
        targetWords: List<String>
    ): GeneratedStory {
        val clean = cleanJsonString(rawText)
        return try {
            val json = JSONObject(clean)
            val title = json.optString("title", "日本語の物語 ($jlptLevel)").ifBlank { "日本語の物語 ($jlptLevel)" }
            val storyContent = json.optString("storyJapanese", "").ifBlank { json.optString("content", rawText) }

            val qArray = json.optJSONArray("questions")
            val questions = mutableListOf<StoryQuizQuestion>()
            if (qArray != null) {
                for (i in 0 until qArray.length()) {
                    val qObj = qArray.optJSONObject(i) ?: continue
                    val optsArray = qObj.optJSONArray("options")
                    val opts = mutableListOf<String>()
                    if (optsArray != null) {
                        for (j in 0 until optsArray.length()) {
                            opts.add(optsArray.getString(j))
                        }
                    }
                    if (opts.isNotEmpty()) {
                        questions.add(
                            StoryQuizQuestion(
                                id = qObj.optInt("id", i + 1),
                                questionText = qObj.optString("questionText", ""),
                                options = opts,
                                correctOptionIndex = qObj.optInt("correctOptionIndex", 0),
                                explanation = qObj.optString("explanation", "")
                            )
                        )
                    }
                }
            }

            GeneratedStory(
                id = UUID.randomUUID().toString(),
                title = title,
                content = storyContent,
                jlptLevel = jlptLevel,
                createdAt = System.currentTimeMillis(),
                targetWords = targetWords,
                questions = questions
            )
        } catch (e: Exception) {
            // Fallback for raw text responses
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
            GeneratedStory(
                id = UUID.randomUUID().toString(),
                title = title,
                content = body,
                jlptLevel = jlptLevel,
                createdAt = System.currentTimeMillis(),
                targetWords = targetWords,
                questions = emptyList()
            )
        }
    }

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json", ignoreCase = true)) {
            clean = clean.substring(7)
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3)
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length - 3)
        }
        return clean.trim()
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
            You are a master Japanese teacher and graded-reader author specializing in immersive language learning.
            Write an engaging, coherent Japanese reading passage calibrated strictly to JLPT $jlptLevel.

            [JLPT Level Calibration]
            $levelRules

            [Target Vocabulary from Learner's Flashcards to Naturally Integrate]
            The learner has studied the following target words from their Anki deck. Actively prioritize weaving these specific words into the story so the reader encounters their learned vocabulary in real context:
            $wordPromptList

            [Requirements]
            1. Write a natural, compelling story in Japanese (150-300 words).
            2. Actively prioritize and weave target vocabulary words from the learner's list above into the story wherever natural and appropriate.
            3. Do NOT use romaji. Do NOT use ruby/furigana brackets like [ふりがな]. Standard Japanese characters only.
            4. Create 3 to 4 multiple-choice reading comprehension questions testing understanding of the story and key vocabulary from the target list in context.
               Each question must have exactly 4 options and the 0-indexed correctOptionIndex.
            5. Return ONLY valid JSON with this exact schema (no markdown formatting, no code blocks):
            {
              "title": "Story Title in Japanese",
              "storyJapanese": "Full Japanese story text with natural paragraph breaks.",
              "questions": [
                {
                  "id": 1,
                  "questionText": "Question testing comprehension or vocabulary context",
                  "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
                  "correctOptionIndex": 0,
                  "explanation": "Brief explanation of the answer"
                }
              ]
            }
        """.trimIndent()
    }
}
