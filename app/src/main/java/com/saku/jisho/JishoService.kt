package com.saku.jisho

import com.saku.data.JishoJapanese
import com.saku.data.JishoLink
import com.saku.data.JishoSense
import com.saku.data.JishoWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class JishoService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    // Simple LRU cache for recent queries to save network calls
    private val cache = object : LinkedHashMap<String, List<JishoWord>>(30, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<JishoWord>>?): Boolean {
            return size > 50
        }
    }

    suspend fun searchWords(query: String): Result<List<JishoWord>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        synchronized(cache) {
            cache[trimmed.lowercase()]?.let { cached ->
                return@withContext Result.success(cached)
            }
        }

        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://jisho.org/api/v1/search/words?keyword=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Saku-Android/1.0 (Japanese Learning App; contact: saku-app@github.com)")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Jisho API error: HTTP ${response.code} ${response.message}")
                )
            }

            val bodyString = response.body?.string()
                ?: return@withContext Result.failure(IOException("Empty response from Jisho"))

            val jsonObject = JSONObject(bodyString)
            val dataArray = jsonObject.optJSONArray("data") ?: JSONArray()
            val wordsList = mutableListOf<JishoWord>()

            for (i in 0 until dataArray.length()) {
                val itemObj = dataArray.optJSONObject(i) ?: continue

                val slug = itemObj.optString("slug", "")
                val isCommon = itemObj.optBoolean("is_common", false)
                val tags = parseStringList(itemObj.optJSONArray("tags"))
                val jlpt = parseStringList(itemObj.optJSONArray("jlpt"))

                // Japanese writings
                val japaneseArray = itemObj.optJSONArray("japanese") ?: JSONArray()
                val japaneseList = mutableListOf<JishoJapanese>()
                for (j in 0 until japaneseArray.length()) {
                    val jObj = japaneseArray.optJSONObject(j) ?: continue
                    val word = if (jObj.has("word") && !jObj.isNull("word")) jObj.optString("word") else null
                    val reading = if (jObj.has("reading") && !jObj.isNull("reading")) jObj.optString("reading") else null
                    japaneseList.add(JishoJapanese(word = word, reading = reading))
                }

                // Senses
                val sensesArray = itemObj.optJSONArray("senses") ?: JSONArray()
                val sensesList = mutableListOf<JishoSense>()
                for (s in 0 until sensesArray.length()) {
                    val sObj = sensesArray.optJSONObject(s) ?: continue

                    val englishDefs = parseStringList(sObj.optJSONArray("english_definitions"))
                    val partsOfSpeech = parseStringList(sObj.optJSONArray("parts_of_speech"))
                    val senseTags = parseStringList(sObj.optJSONArray("tags"))
                    val restrictions = parseStringList(sObj.optJSONArray("restrictions"))
                    val seeAlso = parseStringList(sObj.optJSONArray("see_also"))
                    val antonyms = parseStringList(sObj.optJSONArray("antonyms"))
                    val info = parseStringList(sObj.optJSONArray("info"))

                    val linksArray = sObj.optJSONArray("links") ?: JSONArray()
                    val linksList = mutableListOf<JishoLink>()
                    for (l in 0 until linksArray.length()) {
                        val lObj = linksArray.optJSONObject(l) ?: continue
                        val text = lObj.optString("text", "")
                        val linkUrl = lObj.optString("url", "")
                        if (text.isNotBlank() || linkUrl.isNotBlank()) {
                            linksList.add(JishoLink(text = text, url = linkUrl))
                        }
                    }

                    sensesList.add(
                        JishoSense(
                            englishDefinitions = englishDefs,
                            partsOfSpeech = partsOfSpeech,
                            links = linksList,
                            tags = senseTags,
                            restrictions = restrictions,
                            seeAlso = seeAlso,
                            antonyms = antonyms,
                            info = info
                        )
                    )
                }

                wordsList.add(
                    JishoWord(
                        slug = slug,
                        isCommon = isCommon,
                        tags = tags,
                        jlpt = jlpt,
                        japanese = japaneseList,
                        senses = sensesList
                    )
                )
            }

            synchronized(cache) {
                cache[trimmed.lowercase()] = wordsList
            }

            Result.success(wordsList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val s = array.optString(i)
            if (!s.isNullOrBlank()) {
                list.add(s)
            }
        }
        return list
    }
}
