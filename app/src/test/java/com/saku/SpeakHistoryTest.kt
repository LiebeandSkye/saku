package com.saku

import com.saku.data.SavedSpeakSession
import com.saku.data.SpeakHistoryManager
import com.saku.speak.ChatMessage
import com.saku.util.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpeakHistoryTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var historyManager: SpeakHistoryManager

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        historyManager = SpeakHistoryManager(fakePrefs)
    }

    @Test
    fun testGenerateTitleFromFirstUserMessage() {
        val messages = listOf(
            ChatMessage(text = "Hello from AI", isUser = false),
            ChatMessage(text = "今日はいい天気ですね！", isUser = true),
            ChatMessage(text = "そうですね！", isUser = false)
        )
        val title = SavedSpeakSession.generateTitle(messages)
        assertEquals("今日はいい天気ですね！", title)
    }

    @Test
    fun testGenerateTitleLongTruncation() {
        val longText = "これは非常に長いメッセージの例です。タイトルとして適切に短縮されるべきです。"
        val messages = listOf(
            ChatMessage(text = longText, isUser = true)
        )
        val title = SavedSpeakSession.generateTitle(messages)
        assertTrue(title.endsWith("..."))
        assertTrue(title.length <= 35)
    }

    @Test
    fun testSaveAndRetrieveSession() {
        val session = SavedSpeakSession(
            id = "session-1",
            title = "買い物について",
            messages = listOf(
                ChatMessage(text = "りんごをください", isUser = true),
                ChatMessage(text = "かしこまりました！", isUser = false)
            ),
            createdAt = 1000L,
            updatedAt = 1000L,
            isPinned = false
        )

        historyManager.saveSession(session)
        val sessions = historyManager.getSessions()

        assertEquals(1, sessions.size)
        val retrieved = sessions.first()
        assertEquals("session-1", retrieved.id)
        assertEquals("買い物について", retrieved.title)
        assertEquals(2, retrieved.messages.size)
        assertEquals("りんごをください", retrieved.messages[0].text)
        assertTrue(retrieved.messages[0].isUser)
        assertEquals("かしこまりました！", retrieved.messages[1].text)
        assertFalse(retrieved.messages[1].isUser)
        assertFalse(retrieved.isPinned)
    }

    @Test
    fun testSortingPinnedFirstThenUpdatedAt() {
        val s1 = SavedSpeakSession(
            id = "s1",
            title = "Session 1",
            messages = emptyList(),
            updatedAt = 1000L,
            isPinned = false
        )
        val s2 = SavedSpeakSession(
            id = "s2",
            title = "Session 2",
            messages = emptyList(),
            updatedAt = 3000L,
            isPinned = false
        )
        val s3 = SavedSpeakSession(
            id = "s3",
            title = "Session 3 Pinned",
            messages = emptyList(),
            updatedAt = 500L,
            isPinned = true
        )

        historyManager.saveSession(s1)
        historyManager.saveSession(s2)
        historyManager.saveSession(s3)

        val sorted = historyManager.getSessions()
        assertEquals(3, sorted.size)
        assertEquals("s3", sorted[0].id)
        assertTrue(sorted[0].isPinned)
        assertEquals("s2", sorted[1].id)
        assertEquals("s1", sorted[2].id)
    }

    @Test
    fun testTogglePin() {
        val s1 = SavedSpeakSession(
            id = "s1",
            title = "Test",
            messages = emptyList(),
            isPinned = false
        )
        historyManager.saveSession(s1)
        assertFalse(historyManager.getSessions().first().isPinned)

        val pinnedState = historyManager.togglePin("s1")
        assertTrue(pinnedState)
        assertTrue(historyManager.getSessions().first().isPinned)

        val unpinnedState = historyManager.togglePin("s1")
        assertFalse(unpinnedState)
        assertFalse(historyManager.getSessions().first().isPinned)
    }

    @Test
    fun testDeleteSession() {
        val s1 = SavedSpeakSession(id = "s1", title = "1", messages = emptyList())
        val s2 = SavedSpeakSession(id = "s2", title = "2", messages = emptyList())
        historyManager.saveSession(s1)
        historyManager.saveSession(s2)

        assertEquals(2, historyManager.getSessions().size)
        historyManager.deleteSession("s1")

        val remaining = historyManager.getSessions()
        assertEquals(1, remaining.size)
        assertEquals("s2", remaining.first().id)
    }

    @Test
    fun testClearAll() {
        val s1 = SavedSpeakSession(id = "s1", title = "1", messages = emptyList())
        historyManager.saveSession(s1)
        assertEquals(1, historyManager.getSessions().size)

        historyManager.clearAll()
        assertTrue(historyManager.getSessions().isEmpty())
    }

    @Test
    fun testLimitTo50Sessions() {
        for (i in 1..60) {
            val session = SavedSpeakSession(
                id = "session-$i",
                title = "Session $i",
                messages = emptyList(),
                updatedAt = i.toLong()
            )
            historyManager.saveSession(session)
        }

        val sessions = historyManager.getSessions()
        assertEquals(50, sessions.size)
        // Most recent should be retained
        assertEquals("session-60", sessions.first().id)
    }

    @Test
    fun testSaveActiveSessionOnNewChat() {
        val chatMessages = listOf(
            ChatMessage(text = "こんにちは！お元気ですか？", isUser = true),
            ChatMessage(text = "こんにちは！元気ですよ。あなたは？", isUser = false)
        )
        val sessionId = "session-active-1"
        val title = SavedSpeakSession.generateTitle(chatMessages)
        val session = SavedSpeakSession(
            id = sessionId,
            title = title,
            messages = chatMessages,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Save session as done in startNewChat
        historyManager.saveSession(session)

        val retrieved = historyManager.getSessions()
        assertEquals(1, retrieved.size)
        assertEquals("session-active-1", retrieved[0].id)
        assertEquals("こんにちは！お元気ですか？", retrieved[0].title)
        assertEquals(2, retrieved[0].messages.size)
    }
}
