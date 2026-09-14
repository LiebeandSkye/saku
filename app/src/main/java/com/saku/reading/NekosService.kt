package com.saku.reading

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object NekosService {
    private const val TAG = "NekosService"
    private const val SAFABOORU_API = "https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1"
    private const val NEKOS_FALLBACK_API = "https://api.nekosapi.com/v4/images/random?rating=safe&without_tags=exposed_girl_breasts,dick,pussy,nsfw&limit=1"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Maps story title, theme, and topic keywords to high-quality anime scenery tags.
     * Prioritizes background environments and atmospheric landscapes over character portraits.
     */
    fun determineTag(title: String, theme: String?, topic: String?): String {
        val combined = "${title.lowercase()} ${theme?.lowercase().orEmpty()} ${topic?.lowercase().orEmpty()}"

        return when {
            // Food / Cafe / Restaurant / Dining
            combined.contains("food") || combined.contains("ramen") || combined.contains("sushi") ||
                combined.contains("cook") || combined.contains("eat") || combined.contains("meal") ||
                combined.contains("restaurant") || combined.contains("cafe") || combined.contains("tea") ||
                combined.contains("料理") || combined.contains("ご飯") || combined.contains("ラーメン") ||
                combined.contains("寿司") || combined.contains("食べ") || combined.contains("食事") ||
                combined.contains("カフェ") || combined.contains("喫茶") -> "cafe+scenery"

            // School / Classroom / Study
            combined.contains("school") || combined.contains("classroom") || combined.contains("uniform") ||
                combined.contains("student") || combined.contains("class") || combined.contains("study") ||
                combined.contains("学校") || combined.contains("教室") || combined.contains("制服") ||
                combined.contains("学生") || combined.contains("勉強") || combined.contains("高校") ||
                combined.contains("中学") -> "classroom+scenery"

            // Shrine / Temple / Traditional Japan
            combined.contains("shrine") || combined.contains("temple") || combined.contains("kyoto") ||
                combined.contains("folklore") || combined.contains("legend") || combined.contains("shinto") ||
                combined.contains("神社") || combined.contains("寺") || combined.contains("京都") ||
                combined.contains("祭り") || combined.contains("鳥居") -> "shrine+scenery"

            // Sakura / Flowers / Cherry blossoms / Spring
            combined.contains("flower") || combined.contains("sakura") || combined.contains("cherry") ||
                combined.contains("blossom") || combined.contains("spring") || combined.contains("garden") ||
                combined.contains("花") || combined.contains("桜") || combined.contains("春") ||
                combined.contains("庭") -> "cherry_blossoms+scenery"

            // Night / Evening / Stars / City lights
            combined.contains("night") || combined.contains("evening") || combined.contains("star") ||
                combined.contains("moon") || combined.contains("city") || combined.contains("tokyo") ||
                combined.contains("夜") || combined.contains("星") || combined.contains("月") ||
                combined.contains("夕") || combined.contains("都市") || combined.contains("東京") -> "night+scenery"

            // Train / Station / Travel / Transit
            combined.contains("train") || combined.contains("station") || combined.contains("subway") ||
                combined.contains("trip") || combined.contains("shinkansen") || combined.contains("電車") ||
                combined.contains("駅") || combined.contains("新幹線") || combined.contains("切符") -> "train+scenery"

            // Sea / Ocean / Beach / Summer
            combined.contains("summer") || combined.contains("beach") || combined.contains("sea") ||
                combined.contains("ocean") || combined.contains("swim") || combined.contains("water") ||
                combined.contains("夏") || combined.contains("海") || combined.contains("泳") ||
                combined.contains("水") -> "sea+scenery"

            // Winter / Snow
            combined.contains("winter") || combined.contains("snow") || combined.contains("cold") ||
                combined.contains("冬") || combined.contains("雪") -> "snow+scenery"

            // Nature / Park / Mountain / Forest / Walk
            combined.contains("nature") || combined.contains("park") || combined.contains("forest") ||
                combined.contains("mountain") || combined.contains("hike") || combined.contains("walk") ||
                combined.contains("公園") || combined.contains("山") || combined.contains("森") ||
                combined.contains("散歩") || combined.contains("自然") -> "nature+scenery"

            // Room / Home / Daily life
            combined.contains("room") || combined.contains("home") || combined.contains("house") ||
                combined.contains("daily") || combined.contains("morning") || combined.contains("部屋") ||
                combined.contains("家") || combined.contains("日常") || combined.contains("朝") -> "room+scenery"

            else -> "scenery"
        }
    }

    /**
     * Fetches a safe anime scenery / landscape background image suitable for the story.
     * Tries the scenic topic query first, then falls back to general atmospheric anime scenery.
     */
    suspend fun fetchImageUrl(title: String, theme: String?, topic: String?): String? = withContext(Dispatchers.IO) {
        val tag = determineTag(title, theme, topic)

        // 1. Fetch scenic anime background matching the tag
        val sceneryUrl = querySafebooruScenery(tag)
        if (!sceneryUrl.isNullOrBlank()) {
            return@withContext sceneryUrl
        }

        // 2. Fallback to general anime scenery
        val fallbackScenery = querySafebooruScenery("scenery")
        if (!fallbackScenery.isNullOrBlank()) {
            return@withContext fallbackScenery
        }

        // 3. Last-resort fallback to safe Nekos API
        queryNekosApiFallback()
    }

    private fun querySafebooruScenery(tag: String): String? {
        val fullTag = "$tag+rating:general"
        val url = "$SAFABOORU_API&tags=$fullTag&limit=10"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Saku-Android/2.7.0")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Safebooru returned HTTP ${response.code}")
                    return null
                }
                val bodyStr = response.body?.string() ?: return null
                parseSafebooruJson(bodyStr)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching scenery from Safebooru: ${e.message}")
            null
        }
    }

    private fun parseSafebooruJson(jsonStr: String): String? {
        return try {
            val trimmed = jsonStr.trim()
            if (!trimmed.startsWith("[")) return null
            val array = JSONArray(trimmed)
            if (array.length() == 0) return null

            // Pick a random image from the matching results for variety
            val randomIndex = Random.nextInt(array.length())
            val item = array.getJSONObject(randomIndex)

            // Prefer sample_url for fast, lightweight loading; fallback to file_url
            val sampleUrl = item.optString("sample_url", "")
            val fileUrl = item.optString("file_url", "")

            val rawUrl = if (sampleUrl.isNotBlank()) sampleUrl else fileUrl
            when {
                rawUrl.startsWith("//") -> "https:$rawUrl"
                rawUrl.startsWith("http") -> rawUrl
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Safebooru JSON: ${e.message}")
            null
        }
    }

    private fun queryNekosApiFallback(): String? {
        val request = Request.Builder()
            .url(NEKOS_FALLBACK_API)
            .header("User-Agent", "Saku-Android/2.7.0")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyStr = response.body?.string() ?: return null
                val trimmed = bodyStr.trim()
                val array = if (trimmed.startsWith("{")) {
                    val obj = JSONObject(trimmed)
                    obj.optJSONArray("value") ?: obj.optJSONArray("items") ?: return null
                } else if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    return null
                }
                if (array.length() > 0) {
                    array.getJSONObject(0).optString("url", "").ifBlank { null }
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in Nekos fallback: ${e.message}")
            null
        }
    }
}
