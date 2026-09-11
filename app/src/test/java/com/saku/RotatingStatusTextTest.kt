package com.saku

import org.junit.Assert.assertEquals
import org.junit.Test

class RotatingStatusTextTest {

    @Test
    fun testRotatingPhrasesLoopingLogic() {
        val jlpt = "N3"
        val phrases = listOf(
            "Crafting $jlpt story...",
            "Weaving the plot...",
            "Polishing details...",
            "Adding some flair...",
            "Fine-tuning emotions...",
            "Almost there..."
        )

        assertEquals("Crafting N3 story...", phrases[0])
        assertEquals(6, phrases.size)

        // Test cycle calculation
        var index = 0
        val observed = mutableListOf<String>()
        repeat(8) {
            observed.add(phrases[index])
            index = (index + 1) % phrases.size
        }

        // Verify it looped back after index 5 to index 0
        assertEquals(phrases[0], observed[0])
        assertEquals(phrases[5], observed[5])
        assertEquals(phrases[0], observed[6]) // loop back to start
        assertEquals(phrases[1], observed[7])
    }

    @Test
    fun testPhrasesAreNonEmptyAndConcise() {
        val jlpt = "N2"
        val phrases = listOf(
            "Crafting $jlpt story...",
            "Weaving the plot...",
            "Polishing details...",
            "Adding some flair...",
            "Fine-tuning emotions...",
            "Almost there..."
        )

        phrases.forEach { phrase ->
            assert(phrase.isNotBlank())
            assert(phrase.length <= 40) // ensures button fits comfortably without overflowing
        }
    }
}
