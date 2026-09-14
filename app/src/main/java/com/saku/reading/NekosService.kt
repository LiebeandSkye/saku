package com.saku.reading

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object NekosService {
    private const val TAG = "NekosService"
    private const val BASE_URL = "https://api.nekosapi.com/v4/images/random"
    private const val EXCLUDE_TAGS = "exposed_girl_breasts,dick,pussy,nsfw"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Maps story title, theme, and topic keywords to safe, broad Nekos tags.
     * Keeps tag broad as requested to maximize match probability.
     */
    fun determineTag(title: String, theme: String?, topic: String?): String? {
        val combined = "${title.lowercase()} ${theme?.lowercase().orEmpty()} ${topic?.lowercase().orEmpty()}"

        return when {
            // Food / Cooking / Dining
            combined.contains("food") || combined.contains("ramen") || combined.contains("sushi") ||
                combined.contains("cook") || combined.contains("eat") || combined.contains("meal") ||
                combined.contains("restaurant") || combined.contains("料理") || combined.contains("ご飯") ||
                combined.contains("ラーメン") || combined.contains("寿司") || combined.contains("食べ") ||
                combined.contains("食事") -> "food"

            // School / Uniform / Student
            combined.contains("school") || combined.contains("uniform") || combined.contains("student") ||
                combined.contains("class") || combined.contains("study") || combined.contains("学校") ||
                combined.contains("制服") || combined.contains("学生") || combined.contains("勉強") ||
                combined.contains("教室") || combined.contains("高校") || combined.contains("中学") -> "school_uniform"

            // Cat / Animals / Pets
            combined.contains("cat") || combined.contains("neko") || combined.contains("pet") ||
                combined.contains("animal") || combined.contains("猫") || combined.contains("ねこ") ||
                combined.contains("ネコ") -> "catgirl"

            // Flowers / Nature / Sakura
            combined.contains("flower") || combined.contains("sakura") || combined.contains("cherry") ||
                combined.contains("blossom") || combined.contains("garden") || combined.contains("花") ||
                combined.contains("桜") || combined.contains("庭") -> "flowers"

            // Night / Evening / Stars
            combined.contains("night") || combined.contains("evening") || combined.contains("star") ||
                combined.contains("moon") || combined.contains("夜") || combined.contains("星") ||
                combined.contains("月") || combined.contains("夕") -> "night"

            // Outdoors / Park / Scenery / Walk
            combined.contains("park") || combined.contains("outdoor") || combined.contains("walk") ||
                combined.contains("hike") || combined.contains("mountain") || combined.contains("公園") ||
                combined.contains("散歩") || combined.contains("山") || combined.contains("自然") -> "outdoor"

            // Summer / Water / Beach
            combined.contains("summer") || combined.contains("beach") || combined.contains("sea") ||
                combined.contains("swim") || combined.contains("ocean") || combined.contains("夏") ||
                combined.contains("海") || combined.contains("泳") -> "summer"

            // Winter / Snow
            combined.contains("winter") || combined.contains("snow") || combined.contains("cold") ||
                combined.contains("冬") || combined.contains("雪") -> "winter"

            // Gaming / Tech
            combined.contains("game") || combined.contains("gaming") || combined.contains("play") ||
                combined.contains("ゲーム") -> "game"

            // Dress / Celebration / Party
            combined.contains("dress") || combined.contains("party") || combined.contains("festival") ||
                combined.contains("ドレス") || combined.contains("祭り") -> "dress"

            // Maid / Cafe
            combined.contains("maid") || combined.contains("cafe") || combined.contains("カフェ") ||
                combined.contains("喫茶") -> "maid"

            else -> null
        }
    }

    /**
     * Fetches a safe background anime image suitable for the story.
     * Tries the broad tag match first; if empty or fails, falls back to a safe random image.
     */
    suspend fun fetchImageUrl(title: String, theme: String?, topic: String?): String? = withContext(Dispatchers.IO) {
        val tag = determineTag(title, theme, topic)

        // 1. Try with tag if available
        if (tag != null) {
            val taggedUrl = queryNekosApi(tag)
            if (!taggedUrl.isNullOrBlank()) {
                return@withContext taggedUrl
            }
        }

        // 2. Fallback to random safe anime image
        queryNekosApi(null)
    }

    private fun queryNekosApi(tag: String?): String? {
        val urlBuilder = StringBuilder(BASE_URL)
            .append("?rating=safe")
            .append("&without_tags=").append(EXCLUDE_TAGS)
            .append("&limit=1")

        if (!tag.isNullOrBlank()) {
            urlBuilder.append("&tags=").append(tag)
        }

        val request = Request.Builder()
            .url(urlBuilder.toString())
            .header("User-Agent", "Saku-Android/2.7.0")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Nekos API returned HTTP ${response.code}")
                    return null
                }
                val bodyStr = response.body?.string() ?: return null
                parseImageUrlFromJson(bodyStr)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching image from Nekos API: ${e.message}")
            null
        }
    }

    private fun parseImageUrlFromJson(jsonStr: String): String? {
        try {
            // Response format could be {"value": [{"url": "..."}]} or [{"url": "..."}]
            val trimmed = jsonStr.trim()
            val array: JSONArray = if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("value") ?: obj.optJSONArray("items") ?: return null
            } else if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                return null
            }

            if (array.length() > 0) {
                val first = array.getJSONObject(0)
                val url = first.optString("url", "")
                if (url.isNotBlank()) {
                    return url
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Nekos JSON: ${e.message}")
        }
        return null
    }
}
