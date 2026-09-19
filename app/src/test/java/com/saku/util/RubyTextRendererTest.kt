package com.saku.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RubyTextRendererTest {

    @Test
    fun testParseRubyTokens_bracketNotation() {
        val input = "私[わたし]は日本語[にほんご]を勉強[べんきょう]します"
        val tokens = RubyTextRenderer.parseRubyTokens(input)
        assertTrue(tokens.isNotEmpty())

        val kanjiTokens = tokens.filter { it.ruby != null }
        assertEquals(3, kanjiTokens.size)
        assertEquals("私", kanjiTokens[0].base)
        assertEquals("わたし", kanjiTokens[0].ruby)
        assertEquals("日本語", kanjiTokens[1].base)
        assertEquals("にほんご", kanjiTokens[1].ruby)
        assertEquals("勉強", kanjiTokens[2].base)
        assertEquals("べんきょう", kanjiTokens[2].ruby)
    }

    @Test
    fun testParseRubyTokens_plainText() {
        val input = "こんにちは世界"
        val tokens = RubyTextRenderer.parseRubyTokens(input)
        assertEquals(1, tokens.size)
        assertEquals("こんにちは世界", tokens[0].base)
        assertEquals(null, tokens[0].ruby)
    }

    @Test
    fun testParseRubyTokens_empty() {
        val tokens = RubyTextRenderer.parseRubyTokens("")
        assertTrue(tokens.isEmpty())
    }

    @Test
    fun testBuildRubyVocab_prefixesAndSuffixes() {
        assertEquals("食[た]べる", RubyTextRenderer.buildRubyVocab("食べる", "たべる"))
        assertEquals("泳[およ]ぐ", RubyTextRenderer.buildRubyVocab("泳ぐ", "およぐ"))
        assertEquals("お 茶[ちゃ]", RubyTextRenderer.buildRubyVocab("お茶", "おちゃ"))
        assertEquals("ご 飯[はん]", RubyTextRenderer.buildRubyVocab("ご飯", "ごはん"))
        assertEquals("お 酒[さけ]", RubyTextRenderer.buildRubyVocab("お酒", "おさけ"))
        assertEquals("思[おも]い 出[だ]す", RubyTextRenderer.buildRubyVocab("思い出す", "おもいだす"))
    }

    @Test
    fun testParseRubyTokens_prefixDeduplication() {
        // When input has honorific kana directly preceding kanji without space
        val tokens = RubyTextRenderer.parseRubyTokens("お茶[おちゃ]")
        assertEquals(2, tokens.size)
        assertEquals("お", tokens[0].base)
        assertEquals(null, tokens[0].ruby)
        assertEquals("茶", tokens[1].base)
        assertEquals("ちゃ", tokens[1].ruby)
    }

    @Test
    fun testParseRubyTokens_htmlWithRpAndAttributes() {
        val input = "<ruby class=\"furigana\">漢字<rp>(</rp><rt>かんじ</rt><rp>)</rp></ruby>"
        val tokens = RubyTextRenderer.parseRubyTokens(input)
        assertEquals(1, tokens.size)
        assertEquals("漢字", tokens[0].base)
        assertEquals("かんじ", tokens[0].ruby)
    }
}
