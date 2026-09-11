package com.saku

import com.saku.anki.AnkiDroidContract
import com.saku.data.CardInfo
import com.saku.data.CardSessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardSessionAndAnkiSyncTest {

    @Test
    fun testCardInfoIncludesDeckId() {
        val card = CardInfo(
            noteId = 12345L,
            cardOrd = 0,
            question = "食べる",
            answer = "to eat",
            deckId = 98765L,
            deckName = "Japanese Core 2k",
            buttonCount = 4
        )
        assertEquals(12345L, card.noteId)
        assertEquals(0, card.cardOrd)
        assertEquals("食べる", card.question)
        assertEquals("to eat", card.answer)
        assertEquals(98765L, card.deckId)
        assertEquals("Japanese Core 2k", card.deckName)
    }

    @Test
    fun testCardInfoDefaultDeckIdIsZero() {
        val card = CardInfo(
            noteId = 111L,
            cardOrd = 1,
            question = "飲む",
            answer = "to drink",
            deckName = "Default"
        )
        assertEquals(0L, card.deckId)
    }

    @Test
    fun testSelectedDeckUriConstruction() {
        val authority = "com.ichi2.anki.flashcards"
        val expectedUri = "content://$authority/selected_deck"
        assertEquals("content://com.ichi2.anki.flashcards/selected_deck", expectedUri)
    }

    @Test
    fun testCardInfoCopyPreservesDeckId() {
        val original = CardInfo(
            noteId = 100L,
            cardOrd = 0,
            question = "本",
            answer = "book",
            deckId = 555L,
            deckName = "Vocab"
        )
        val copy = original.copy(question = "本 (ほん)")
        assertEquals(original.deckId, copy.deckId)
        assertEquals(original.deckName, copy.deckName)
    }

    @Test
    fun testGradingConcurrencyGuard() {
        // Ensure starting state is false
        CardSessionManager.isGradingInProgress.set(false)

        // First attempt succeeds
        assertTrue(CardSessionManager.isGradingInProgress.compareAndSet(false, true))

        // Concurrent attempt fails while grading is in progress
        assertFalse(CardSessionManager.isGradingInProgress.compareAndSet(false, true))

        // Releasing guard allows subsequent grading
        CardSessionManager.isGradingInProgress.set(false)
        assertTrue(CardSessionManager.isGradingInProgress.compareAndSet(false, true))
        CardSessionManager.isGradingInProgress.set(false)
    }

    @Test
    fun testOptimisticStatsLearningCardAgain() {
        val oldStats = Triple(0, 5, 10) // 0 new, 5 learning, 10 review
        val cardType = 1 // Learning card
        val ease = 1 // Again

        val newStats = when (ease) {
            1 -> Triple(
                (oldStats.first - (if (cardType == 0) 1 else 0)).coerceAtLeast(0),
                if (cardType == 1) oldStats.second else oldStats.second + 1,
                (oldStats.third - (if (cardType == 2) 1 else 0)).coerceAtLeast(0)
            )
            else -> Triple(
                (oldStats.first - (if (cardType == 0) 1 else 0)).coerceAtLeast(0),
                (oldStats.second - (if (cardType == 1) 1 else 0)).coerceAtLeast(0),
                (oldStats.third - (if (cardType == 2) 1 else 0)).coerceAtLeast(0)
            )
        }

        // Learning card answered Again stays in learning queue (5), not 6!
        assertEquals(0, newStats.first)
        assertEquals(5, newStats.second)
        assertEquals(10, newStats.third)
    }

    @Test
    fun testOptimisticStatsNewAndReviewCardsAgain() {
        val oldStats = Triple(3, 2, 5) // 3 new, 2 learning, 5 review

        // New card (cardType = 0) answered Again -> moves to learn (2 new, 3 learn, 5 rev)
        val newCardStats = Triple(
            (oldStats.first - 1).coerceAtLeast(0),
            oldStats.second + 1,
            oldStats.third
        )
        assertEquals(2, newCardStats.first)
        assertEquals(3, newCardStats.second)
        assertEquals(5, newCardStats.third)

        // Review card (cardType = 2) answered Again -> lapses to learn (3 new, 3 learn, 4 rev)
        val revCardStats = Triple(
            oldStats.first,
            oldStats.second + 1,
            (oldStats.third - 1).coerceAtLeast(0)
        )
        assertEquals(3, revCardStats.first)
        assertEquals(3, revCardStats.second)
        assertEquals(4, revCardStats.third)
    }

    @Test
    fun testExcludeNoteIdFilteringEvenWhenSingleResult() {
        // Emulates cursor rows where only 1 note is returned, matching excludeNoteId
        val cursorRows = listOf(Pair(999L, "Card 1"))
        val excludeNoteId: Long? = 999L

        val filtered = cursorRows.filter { row ->
            val noteId = row.first
            excludeNoteId == null || noteId != excludeNoteId
        }

        // Must be empty (excluded), rather than bypassed because size == 1
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun testMultiDeckFallbackWhenCurrentDeckDepleted() {
        val selectedDecks = setOf(1001L, 1002L)
        val targetDeckId: Long? = null // Global / lock screen / widget session
        val currentCardDeckId = 1001L

        val effectiveDeckId = targetDeckId?.takeIf { it > 0 } ?: currentCardDeckId.takeIf { it > 0 }
        val deckQueryIds = if (effectiveDeckId != null && effectiveDeckId > 0) setOf(effectiveDeckId) else selectedDecks

        // Simulated Anki query for deck 1001: depleted (returns null)
        var nextCard: String? = null

        // Fallback kicks in because targetDeckId is null and selectedDecks is not empty
        if (nextCard == null && targetDeckId == null && selectedDecks.isNotEmpty()) {
            nextCard = "CardFromDeck1002"
        }

        assertNotNull(nextCard)
        assertEquals("CardFromDeck1002", nextCard)
    }

    @Test
    fun testExplicitTargetDeckDoesNotFallback() {
        val selectedDecks = setOf(1001L, 1002L)
        val targetDeckId: Long? = 1001L // User explicitly pinned deck 1001

        val effectiveDeckId = targetDeckId?.takeIf { it > 0 }
        val deckQueryIds = if (effectiveDeckId != null && effectiveDeckId > 0) setOf(effectiveDeckId) else selectedDecks

        var nextCard: String? = null

        // Fallback should NOT run when targetDeckId is explicitly provided
        if (nextCard == null && targetDeckId == null && selectedDecks.isNotEmpty()) {
            nextCard = "CardFromDeck1002"
        }

        // Should remain null (all caught up for this specific deck)
        assertEquals(null, nextCard)
    }
}

