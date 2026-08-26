package com.saku

import com.saku.anki.JapaneseFieldParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JapaneseFieldParserTest {

    @Test
    fun testKaishi15kFieldMapping() {
        val fieldNames = listOf(
            "Vocabulary-Kanji",
            "Vocabulary-Kana",
            "Vocabulary-English",
            "Vocabulary-Audio",
            "Sentence-Expression",
            "Sentence-Kana",
            "Sentence-English",
            "Sentence-Audio"
        )
        val fieldValues = listOf(
            "私",
            "わたし",
            "I, me",
            "[sound:kaishi_vocab_001.mp3]",
            "私は学生です。",
            "わたしはがくせいです。",
            "I am a student.",
            "[sound:kaishi_sent_001.mp3]"
        )

        val result = JapaneseFieldParser.mapFieldsToJapaneseCard(fieldNames, fieldValues)
        assertEquals("私", result.kanji)
        assertEquals("わたし", result.kana)
        assertEquals("watashi", result.romaji)
        assertEquals("I, me", result.meaning)
        assertEquals("私は学生です。 • I am a student.", result.example)
    }

    @Test
    fun testKaishiWithBracketFurigana() {
        val fieldNames = listOf(
            "Vocabulary-Kanji",
            "Vocabulary-Kana",
            "Vocabulary-English",
            "Sentence-Expression",
            "Sentence-English"
        )
        val fieldValues = listOf(
            "日本語[にほんご]",
            "",
            "Japanese language",
            "日本語[にほんご]を 勉強[べんきょう]します。",
            "I study Japanese."
        )

        val result = JapaneseFieldParser.mapFieldsToJapaneseCard(fieldNames, fieldValues)
        assertEquals("日本語", result.kanji)
        assertEquals("にほんご", result.kana)
        assertEquals("nihongo", result.romaji)
        assertEquals("Japanese language", result.meaning)
    }

    @Test
    fun testRubyTagExtraction() {
        val rawRuby = "<ruby>漢<rt>かん</rt></ruby><ruby>字<rt>じ</rt></ruby>"
        val (kanji, kana) = JapaneseFieldParser.extractKanjiAndKana(rawRuby)
        assertEquals("漢字", kanji)
        assertEquals("かんじ", kana)
    }

    @Test
    fun testHtmlCleaningAndSoundStripping() {
        val htmlWithSound = "<div><span style=\"color:red;\">食べる</span>[sound:eat.mp3]&nbsp;&amp;&nbsp;飲む</div>"
        val cleaned = JapaneseFieldParser.cleanHtml(htmlWithSound)
        assertEquals("食べる & 飲む", cleaned)
    }

    @Test
    fun testKanaToRomajiConversion() {
        assertEquals("sakura", JapaneseFieldParser.kanaToRomaji("さくら"))
        assertEquals("toukyou", JapaneseFieldParser.kanaToRomaji("とうきょう"))
        assertEquals("gakkou", JapaneseFieldParser.kanaToRomaji("がっこう"))
        assertEquals("shinkansen", JapaneseFieldParser.kanaToRomaji("しんかんせん"))
        assertEquals("kaishi", JapaneseFieldParser.kanaToRomaji("かいし"))
        assertEquals("raamen", JapaneseFieldParser.kanaToRomaji("ラーメン"))
    }

    @Test
    fun testPositionalFallbackWhenFieldNamesMissing() {
        val fieldNames = emptyList<String>()
        val fieldValues = listOf(
            "猫[ねこ]",
            "ねこ",
            "cat",
            "猫がいます。"
        )

        val result = JapaneseFieldParser.mapFieldsToJapaneseCard(fieldNames, fieldValues)
        assertEquals("猫", result.kanji)
        assertEquals("ねこ", result.kana)
        assertEquals("neko", result.romaji)
        assertEquals("cat", result.meaning)
        assertTrue(result.example.contains("猫がいます"))
    }

    @Test
    fun testNormalizedFieldNamingWithUnderscoresAndPrefixes() {
        val fieldNames = listOf(
            "Core::Vocab_Kanji",
            "Core::Vocab_Kana",
            "Core::Vocab_Meaning",
            "Core::Sent_Kanji",
            "Core::Sent_Meaning"
        )
        val fieldValues = listOf(
            "車",
            "くるま",
            "car, vehicle",
            "車を運転します。",
            "I drive a car."
        )

        val result = JapaneseFieldParser.mapFieldsToJapaneseCard(fieldNames, fieldValues)
        assertEquals("車", result.kanji)
        assertEquals("くるま", result.kana)
        assertEquals("kuruma", result.romaji)
        assertEquals("car, vehicle", result.meaning)
        assertEquals("車を運転します。 • I drive a car.", result.example)
    }

    @Test
    fun testRenderedHtmlFallbackParsing() {
        val fallbackQ = "<div>勉強[べんきょう]</div>"
        val fallbackA = "<div>勉強</div><hr id=\"answer\"><div>to study</div><div>毎日日本語を勉強します。</div>"

        val result = JapaneseFieldParser.mapFieldsToJapaneseCard(
            fieldNames = emptyList(),
            fieldValues = emptyList(),
            fallbackQuestion = fallbackQ,
            fallbackAnswer = fallbackA
        )
        assertEquals("勉強", result.kanji)
        assertEquals("べんきょう", result.kana)
        assertEquals("benkyou", result.romaji)
        assertTrue(result.meaning.contains("to study"))
        assertTrue(result.example.contains("勉強します"))
    }
}
