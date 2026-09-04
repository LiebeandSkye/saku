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
}
