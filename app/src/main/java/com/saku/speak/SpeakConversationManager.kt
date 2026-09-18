package com.saku.speak

import androidx.compose.runtime.mutableStateListOf

/**
 * Singleton to preserve Speak tab conversation messages across tab navigation in HorizontalPager.
 */
object SpeakConversationManager {
    val messages = mutableStateListOf<ChatMessage>()

    fun clear() {
        messages.clear()
    }
}
