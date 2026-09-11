package com.saku.reading

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saku.data.AnkiVocabularyItem
import com.saku.data.GeneratedStory
import com.saku.data.PreferencesManager
import com.saku.data.ReadingHistoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class GenerationStatus {
    object Idle : GenerationStatus()
    data class Generating(val jlptLevel: String, val theme: String?, val topic: String?) : GenerationStatus()
    data class Success(val story: GeneratedStory) : GenerationStatus()
    data class Error(val message: String) : GenerationStatus()
}

/**
 * Application-scoped manager for AI story generation.
 * Decouples Gemini story generation from Compose UI lifecycle so requests
 * continue uninterrupted when switching between tabs (Cards, Reading, Jisho)
 * or briefly leaving the app.
 */
object StoryGenerationManager {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    var storyService: GeminiStoryService = GeminiStoryService()

    var status by mutableStateOf<GenerationStatus>(GenerationStatus.Idle)
        private set

    val isGenerating: Boolean
        get() = status is GenerationStatus.Generating

    var latestStory by mutableStateOf<GeneratedStory?>(null)
        private set

    var currentJlptLevel by mutableStateOf("N5")
        private set

    fun startGeneration(
        context: Context,
        apiKey: String,
        jlptLevel: String,
        vocabularyList: List<AnkiVocabularyItem>,
        preferredModel: String = PreferencesManager.DEFAULT_GEMINI_MODEL,
        theme: String? = null,
        topic: String? = null,
        onSuccess: ((GeneratedStory) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ): Boolean {
        if (isGenerating) return false

        val appContext = context.applicationContext
        currentJlptLevel = jlptLevel
        status = GenerationStatus.Generating(jlptLevel, theme, topic)

        applicationScope.launch {
            val result = storyService.generateStory(
                apiKey = apiKey,
                jlptLevel = jlptLevel,
                vocabularyList = vocabularyList,
                preferredModel = preferredModel,
                theme = theme,
                topic = topic
            )

            result.onSuccess { story ->
                latestStory = story

                // Automatically save into reading history on background IO dispatcher
                withContext(Dispatchers.IO) {
                    val historyManager = ReadingHistoryManager(appContext)
                    historyManager.saveStory(story)
                }

                val prefs = PreferencesManager(appContext)
                prefs.lastReadStoryId = story.id

                status = GenerationStatus.Success(story)
                onSuccess?.invoke(story)
            }.onFailure { error ->
                val msg = error.message ?: "Failed to generate story"
                status = GenerationStatus.Error(msg)
                onError?.invoke(msg)
            }
        }
        return true
    }

    fun dismissStatus() {
        status = GenerationStatus.Idle
    }

    fun reset() {
        status = GenerationStatus.Idle
        latestStory = null
    }
}
