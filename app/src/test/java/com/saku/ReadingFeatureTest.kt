package com.saku

import com.saku.data.AnkiVocabularyItem
import com.saku.data.GeneratedStory
import com.saku.data.ReadingVocabularySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingFeatureTest {

    @Test
    fun testAnkiVocabularyItemDisplayWord() {
        val kanjiWord = AnkiVocabularyItem(
            kanji = "学校",
            reading = "がっこう",
            meaning = "school",
            isSuspended = false
        )
        assertEquals("学校", kanjiWord.displayWord)

        val kanaWord = AnkiVocabularyItem(
            kanji = "",
            reading = "ありがとう",
            meaning = "thank you",
            isSuspended = true
        )
        assertEquals("ありがとう", kanaWord.displayWord)
        assertTrue(kanaWord.isSuspended)
    }

    @Test
    fun testReadingVocabularySummaryCounts() {
        val items = listOf(
            AnkiVocabularyItem(kanji = "犬", reading = "いぬ", isSuspended = false),
            AnkiVocabularyItem(kanji = "猫", reading = "ねこ", isSuspended = false),
            AnkiVocabularyItem(kanji = "鳥", reading = "とり", isSuspended = true)
        )
        val summary = ReadingVocabularySummary(
            studiedCount = 2,
            suspendedCount = 1,
            words = items
        )
        assertEquals(2, summary.studiedCount)
        assertEquals(1, summary.suspendedCount)
        assertEquals(3, summary.totalCount)
        assertEquals(3, summary.words.size)
    }

    @Test
    fun testGeneratedStoryStructure() {
        val story = GeneratedStory(
            title = "雨の日の散歩",
            content = "今日は雨が降っています。猫は窓のそばで寝ています。",
            jlptLevel = "N5",
            targetWords = listOf("雨", "猫")
        )

        assertNotNull(story.id)
        assertEquals("雨の日の散歩", story.title)
        assertEquals("N5", story.jlptLevel)
        assertEquals(2, story.targetWords.size)
        assertTrue(story.createdAt > 0)
    }

    @Test
    fun testGeminiModelPresetsAndLabels() {
        assertEquals("gemini-3.5-flash-lite", com.saku.data.PreferencesManager.DEFAULT_GEMINI_MODEL)

        val modelIds = com.saku.data.PreferencesManager.AVAILABLE_GEMINI_MODELS.map { it.id }
        assertTrue(modelIds.contains("gemini-3.8-flash"))
        assertTrue(modelIds.contains("gemini-3.7-flash"))
        assertTrue(modelIds.contains("gemini-3.6-flash"))
        assertTrue(modelIds.contains("gemini-3.5-flash"))
        assertTrue(modelIds.contains("gemini-3.5-flash-lite"))

        assertEquals("Gemini 3.8 Flash", com.saku.data.PreferencesManager.getModelDisplayName("gemini-3.8-flash"))
        assertEquals("Gemini 3.7 Flash", com.saku.data.PreferencesManager.getModelDisplayName("gemini-3.7-flash"))
        assertEquals("custom-model-id", com.saku.data.PreferencesManager.getModelDisplayName("custom-model-id"))

        assertEquals("3.8 Flash", com.saku.data.PreferencesManager.getShortModelLabel("gemini-3.8-flash"))
        assertEquals("3.7 Flash", com.saku.data.PreferencesManager.getShortModelLabel("gemini-3.7-flash"))
        assertEquals("3.6 Flash", com.saku.data.PreferencesManager.getShortModelLabel("gemini-3.6-flash"))
        assertEquals("3.5 Flash", com.saku.data.PreferencesManager.getShortModelLabel("gemini-3.5-flash"))
        assertEquals("3.5 Lite (Fastest)", com.saku.data.PreferencesManager.getShortModelLabel("gemini-3.5-flash-lite"))
    }

    @Test
    fun testVocabularyListPoolFiltering() {
        val studied = (1..30).map { AnkiVocabularyItem(kanji = "学$it", isSuspended = false) }
        val suspended = (1..20).map { AnkiVocabularyItem(kanji = "休$it", isSuspended = true) }
        val combined = studied + suspended

        val studiedOnly = combined.filter { !it.isSuspended }
        val suspendedOnly = combined.filter { it.isSuspended }

        assertEquals(30, studiedOnly.size)
        assertEquals(20, suspendedOnly.size)

        // Verify that sampling from both pools takes words from both
        val sampleStudied = studiedOnly.take(18)
        val sampleSuspended = suspendedOnly.take(10)
        val selected = (sampleStudied + sampleSuspended).distinctBy { it.displayWord }
        assertEquals(28, selected.size)
        assertTrue(selected.any { it.isSuspended })
        assertTrue(selected.any { !it.isSuspended })
    }
}
