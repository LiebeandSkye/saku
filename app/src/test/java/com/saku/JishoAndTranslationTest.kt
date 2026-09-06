package com.saku

import com.saku.data.JishoJapanese
import com.saku.data.JishoSense
import com.saku.data.JishoWord
import com.saku.translation.TranslationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JishoAndTranslationTest {

    @Test
    fun testJishoWordComputedProperties() {
        val word = JishoWord(
            slug = "食べる",
            isCommon = true,
            jlpt = listOf("jlpt-n5"),
            tags = listOf("wanikani6"),
            japanese = listOf(
                JishoJapanese(word = "食べる", reading = "たべる"),
                JishoJapanese(word = "喰べる", reading = "たべる")
            ),
            senses = listOf(
                JishoSense(
                    englishDefinitions = listOf("to eat"),
                    partsOfSpeech = listOf("Ichidan verb", "Transitive verb")
                )
            )
        )

        assertEquals("食べる", word.primaryWord)
        assertEquals("たべる", word.primaryReading)
        assertEquals("taberu", word.romaji)
        assertEquals("N5", word.jlptBadge)
        assertTrue(word.isCommon)
        assertEquals(1, word.otherForms.size)
        assertEquals("喰べる (たべる)", word.otherForms[0])
    }

    @Test
    fun testJishoWordKanaOnly() {
        val word = JishoWord(
            slug = "ありがとう",
            isCommon = true,
            jlpt = listOf("jlpt-n5"),
            japanese = listOf(
                JishoJapanese(word = null, reading = "ありがとう")
            ),
            senses = listOf(
                JishoSense(englishDefinitions = listOf("thank you"))
            )
        )

        assertEquals("ありがとう", word.primaryWord)
        assertEquals("ありがとう", word.primaryReading)
        assertEquals("arigatou", word.romaji)
        assertEquals("N5", word.jlptBadge)
    }

    @Test
    fun testTranslationResult() {
        val result = TranslationResult(
            sourceText = "猫が好きです",
            translatedText = "I like cats",
            romaji = "Neko ga sukidesu"
        )

        assertEquals("猫が好きです", result.sourceText)
        assertEquals("I like cats", result.translatedText)
        assertEquals("Neko ga sukidesu", result.romaji)
    }
}
