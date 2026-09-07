package com.saku.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(
    private val prefs: SharedPreferences
) {

    constructor(context: Context) : this(
        context.getSharedPreferences(
            "saku_prefs",
            Context.MODE_PRIVATE
        )
    )

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    var selectedDeckIds: Set<String>
        get() = prefs.getStringSet(KEY_SELECTED_DECKS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_SELECTED_DECKS, value).apply()

    var updateIntervalMinutes: Int
        get() = prefs.getInt(KEY_UPDATE_INTERVAL, 30)
        set(value) = prefs.edit().putInt(KEY_UPDATE_INTERVAL, value).apply()

    var snoozeDurationMinutes: Int
        get() = prefs.getInt(KEY_SNOOZE_DURATION, 60)
        set(value) = prefs.edit().putInt(KEY_SNOOZE_DURATION, value).apply()

    var snoozeUntil: Long
        get() = prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_SNOOZE_UNTIL, value).apply()

    var backgroundType: String
        get() = prefs.getString(KEY_BACKGROUND_TYPE, "anki_lock") ?: "anki_lock"
        set(value) = prefs.edit().putString(KEY_BACKGROUND_TYPE, value).apply()

    var customImageUri: String?
        get() = prefs.getString(KEY_CUSTOM_IMAGE_URI, null)
        set(value) = prefs.edit().putString(KEY_CUSTOM_IMAGE_URI, value).apply()

    var savedImageUris: Set<String>
        get() = prefs.getStringSet(KEY_SAVED_IMAGE_URIS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_SAVED_IMAGE_URIS, value).apply()

    var blurRadius: Int
        get() = prefs.getInt(KEY_BLUR_RADIUS, 25)
        set(value) = prefs.edit().putInt(KEY_BLUR_RADIUS, value).apply()

    var dimOpacity: Float
        get() = prefs.getFloat(KEY_DIM_OPACITY, 0.30f)
        set(value) = prefs.edit().putFloat(KEY_DIM_OPACITY, value).apply()

    var artworkOpacity: Float
        get() = prefs.getFloat(KEY_ARTWORK_OPACITY, 0.90f)
        set(value) = prefs.edit().putFloat(KEY_ARTWORK_OPACITY, value).apply()

    var classicRevealedAction: String
        get() = prefs.getString(KEY_CLASSIC_REVEALED_ACTION, "suspend") ?: "suspend"
        set(value) = prefs.edit().putString(KEY_CLASSIC_REVEALED_ACTION, value).apply()

    var geminiApiKey: String?
        get() = prefs.getString(KEY_GEMINI_API_KEY, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value?.trim()).apply()

    var readingJlptLevel: String
        get() = prefs.getString(KEY_READING_JLPT_LEVEL, "N5") ?: "N5"
        set(value) = prefs.edit().putString(KEY_READING_JLPT_LEVEL, value).apply()

    var geminiModel: String
        get() {
            val saved = prefs.getString(KEY_GEMINI_MODEL, null)
            return if (saved.isNullOrBlank() || saved.startsWith("gemini-1.") || saved.startsWith("gemini-2.")) {
                DEFAULT_GEMINI_MODEL
            } else {
                saved
            }
        }
        set(value) = prefs.edit().putString(KEY_GEMINI_MODEL, value.trim()).apply()

    var hasAcceptedInternetDisclosure: Boolean
        get() = prefs.getBoolean(KEY_INTERNET_DISCLOSURE, false)
        set(value) = prefs.edit().putBoolean(KEY_INTERNET_DISCLOSURE, value).apply()

    var highlightVocabularyWords: Boolean
        get() = prefs.getBoolean(KEY_HIGHLIGHT_VOCABULARY_WORDS, true)
        set(value) = prefs.edit().putBoolean(KEY_HIGHLIGHT_VOCABULARY_WORDS, value).apply()

    var lastReadStoryId: String?
        get() = prefs.getString(KEY_LAST_READ_STORY_ID, null)
        set(value) = prefs.edit().putString(KEY_LAST_READ_STORY_ID, value).apply()

    var readingBackgroundImageUri: String?
        get() = prefs.getString(KEY_READING_BACKGROUND_IMAGE_URI, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_READING_BACKGROUND_IMAGE_URI, value?.trim()).apply()

    var elevenLabsApiKey: String?
        get() = prefs.getString(KEY_ELEVENLABS_API_KEY, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_ELEVENLABS_API_KEY, value?.trim()).apply()

    var elevenLabsVoiceId: String
        get() {
            val saved = prefs.getString(KEY_ELEVENLABS_VOICE_ID, null)
            return if (saved.isNullOrBlank()) DEFAULT_ELEVENLABS_VOICE_ID else extractVoiceId(saved)
        }
        set(value) = prefs.edit().putString(KEY_ELEVENLABS_VOICE_ID, extractVoiceId(value).trim()).apply()

    var elevenLabsVoiceName: String?
        get() = prefs.getString(KEY_ELEVENLABS_VOICE_NAME, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_ELEVENLABS_VOICE_NAME, value?.trim()).apply()

    var appTheme: String
        get() = prefs.getString(KEY_APP_THEME, "dim") ?: "dim"
        set(value) = prefs.edit().putString(KEY_APP_THEME, value).apply()

    val isSnoozed: Boolean
        get() = System.currentTimeMillis() < snoozeUntil

    fun addSavedImageUri(uriStr: String) {
        val set = savedImageUris.toMutableSet()
        set.add(uriStr)
        savedImageUris = set
    }

    fun removeSavedImageUri(uriStr: String) {
        val set = savedImageUris.toMutableSet()
        set.remove(uriStr)
        savedImageUris = set
        if (customImageUri == uriStr) {
            customImageUri = set.firstOrNull()
            if (customImageUri == null) {
                backgroundType = "transparent"
            }
        }
    }

    fun getSelectedDeckIdsAsLongs(): Set<Long> {
        return selectedDeckIds.mapNotNull { it.toLongOrNull() }.toSet()
    }

    companion object {
        const val DEFAULT_GEMINI_MODEL = "gemini-3.5-flash-lite"

        val AVAILABLE_GEMINI_MODELS = listOf(
            GeminiModelOption("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite", "Ultra-fast / Low Latency"),
            GeminiModelOption("gemini-3.8-flash", "Gemini 3.8 Flash", "Latest & Most Intelligent"),
            GeminiModelOption("gemini-3.7-flash", "Gemini 3.7 Flash", "Fast & Multimodal"),
            GeminiModelOption("gemini-3.6-flash", "Gemini 3.6 Flash", "Stable Flash"),
            GeminiModelOption("gemini-3.5-flash", "Gemini 3.5 Flash", "Balanced")
        )

        fun getModelDisplayName(modelId: String): String {
            return AVAILABLE_GEMINI_MODELS.firstOrNull { it.id.equals(modelId, ignoreCase = true) }?.name ?: modelId
        }

        fun getShortModelLabel(modelId: String): String {
            return when (modelId.lowercase()) {
                "gemini-3.5-flash-lite" -> "3.5 Lite (Fastest)"
                "gemini-3.8-flash" -> "3.8 Flash"
                "gemini-3.7-flash" -> "3.7 Flash"
                "gemini-3.6-flash" -> "3.6 Flash"
                "gemini-3.5-flash" -> "3.5 Flash"
                else -> modelId.removePrefix("gemini-")
            }
        }

        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_SELECTED_DECKS = "selected_decks"
        private const val KEY_UPDATE_INTERVAL = "update_interval"
        private const val KEY_SNOOZE_DURATION = "snooze_duration"
        private const val KEY_SNOOZE_UNTIL = "snooze_until"
        private const val KEY_BACKGROUND_TYPE = "background_type"
        private const val KEY_CUSTOM_IMAGE_URI = "custom_image_uri"
        private const val KEY_SAVED_IMAGE_URIS = "saved_image_uris"
        private const val KEY_BLUR_RADIUS = "blur_radius"
        private const val KEY_DIM_OPACITY = "dim_opacity"
        private const val KEY_ARTWORK_OPACITY = "artwork_opacity"
        private const val KEY_CLASSIC_REVEALED_ACTION = "classic_revealed_action"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_READING_JLPT_LEVEL = "reading_jlpt_level"
        private const val KEY_GEMINI_MODEL = "gemini_model"
        private const val KEY_INTERNET_DISCLOSURE = "internet_disclosure_accepted"
        private const val KEY_HIGHLIGHT_VOCABULARY_WORDS = "highlight_vocabulary_words"
        const val DEFAULT_ELEVENLABS_VOICE_ID = "JTlYtJrcTzPC71hMLOxo"
        const val DEFAULT_ELEVENLABS_VOICE_URL = "https://elevenlabs.io/voices/JTlYtJrcTzPC71hMLOxo"
        const val DEFAULT_ELEVENLABS_MODEL_ID = "eleven_multilingual_v2"

        fun extractVoiceId(input: String): String {
            val trimmed = input.trim()
            if (trimmed.isBlank()) return DEFAULT_ELEVENLABS_VOICE_ID
            // Match pattern like /voices/{voice_id}
            val voicePattern = Regex("""/voices/([a-zA-Z0-9_-]+)""")
            val match = voicePattern.find(trimmed)
            if (match != null) {
                return match.groupValues[1]
            }
            // If full URL, take last segment without query/fragment
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                val cleanUrl = trimmed.substringBefore('?').substringBefore('#').trimEnd('/')
                val lastSegment = cleanUrl.substringAfterLast('/')
                if (lastSegment.isNotBlank()) {
                    return lastSegment
                }
            }
            return trimmed
        }

        private const val KEY_ELEVENLABS_API_KEY = "elevenlabs_api_key"
        private const val KEY_ELEVENLABS_VOICE_ID = "elevenlabs_voice_id"
        private const val KEY_ELEVENLABS_VOICE_NAME = "elevenlabs_voice_name"
        private const val KEY_LAST_READ_STORY_ID = "last_read_story_id"
        private const val KEY_READING_BACKGROUND_IMAGE_URI = "reading_background_image_uri"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_RECENT_JISHO_SEARCHES = "recent_jisho_searches"
    }

    var recentJishoSearches: List<String>
        get() {
            val raw = prefs.getString(KEY_RECENT_JISHO_SEARCHES, "") ?: ""
            if (raw.isBlank()) return emptyList()
            return raw.split(";;;").filter { it.isNotBlank() }
        }
        set(value) {
            val joined = value.take(15).joinToString(";;;")
            prefs.edit().putString(KEY_RECENT_JISHO_SEARCHES, joined).apply()
        }

    fun addRecentJishoSearch(term: String) {
        val trimmed = term.trim()
        if (trimmed.isBlank()) return
        val current = recentJishoSearches.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        recentJishoSearches = current.take(15)
    }

    fun clearRecentJishoSearches() {
        recentJishoSearches = emptyList()
    }
}

data class GeminiModelOption(
    val id: String,
    val name: String,
    val tag: String
)
