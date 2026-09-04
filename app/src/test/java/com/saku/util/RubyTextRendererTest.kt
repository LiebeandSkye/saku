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
}
