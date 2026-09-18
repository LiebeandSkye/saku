package com.saku.data

import android.content.Context
import android.content.SharedPreferences
import com.saku.speak.ChatMessage
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedSpeakSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
) {
    companion object {
        fun generateTitle(messages: List<ChatMessage>): String {
            val firstUserMsg = messages.firstOrNull { it.isUser }?.text?.trim()
            val firstMsg = messages.firstOrNull()?.text?.trim()
            val text = firstUserMsg ?: firstMsg
            return if (!text.isNullOrBlank()) {
                val singleLine = text.replace("\n", " ").trim()
                if (singleLine.length > 32) "${singleLine.take(32)}..." else singleLine
            } else {
                "会話 (Conversation)"
            }
        }
    }
}

class SpeakHistoryManager(
    private val context: Context?,
    private val prefs: SharedPreferences
) {

    constructor(context: Context) : this(
        context,
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    constructor(prefs: SharedPreferences) : this(
        null,
        prefs
    )

    fun getSessions(): List<SavedSpeakSession> {
        val jsonStr = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        val list = mutableListOf<SavedSpeakSession>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val messagesJson = obj.optJSONArray("messages")
                val messagesList = mutableListOf<ChatMessage>()
                if (messagesJson != null) {
                    for (j in 0 until messagesJson.length()) {
                        val mObj = messagesJson.getJSONObject(j)
                        messagesList.add(
                            ChatMessage(
                                id = mObj.optString("id", UUID.randomUUID().toString()),
                                text = mObj.optString("text", ""),
                                isUser = mObj.optBoolean("isUser", true),
                                timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                }
                list.add(
                    SavedSpeakSession(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "会話"),
                        messages = messagesList,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedWith(
            compareByDescending<SavedSpeakSession> { it.isPinned }
                .thenByDescending { it.updatedAt }
        )
    }

    fun saveSession(session: SavedSpeakSession) {
        val existing = getSessions().filterNot { it.id == session.id }.toMutableList()
        existing.add(0, session) // Newest or updated first

        val sorted = existing.sortedWith(
            compareByDescending<SavedSpeakSession> { it.isPinned }
                .thenByDescending { it.updatedAt }
        )
        // Limit to 50 saved sessions to keep storage lightweight
        val trimmed = if (sorted.size > 50) sorted.take(50) else sorted
        saveList(trimmed)
    }

    fun togglePin(id: String): Boolean {
        val sessions = getSessions().toMutableList()
        val index = sessions.indexOfFirst { it.id == id }
        if (index == -1) return false
        val current = sessions[index]
        val updated = current.copy(isPinned = !current.isPinned)
        sessions[index] = updated
        val sorted = sessions.sortedWith(
            compareByDescending<SavedSpeakSession> { it.isPinned }
                .thenByDescending { it.updatedAt }
        )
        saveList(sorted)
        return updated.isPinned
    }

    fun deleteSession(id: String) {
        val updated = getSessions().filterNot { it.id == id }
        saveList(updated)
    }

    fun clearAll() {
        prefs.edit().remove(KEY_SESSIONS).apply()
    }

    private fun saveList(list: List<SavedSpeakSession>) {
        try {
            val array = JSONArray()
            for (session in list) {
                val obj = JSONObject().apply {
                    put("id", session.id)
                    put("title", session.title)
                    put("createdAt", session.createdAt)
                    put("updatedAt", session.updatedAt)
                    put("isPinned", session.isPinned)

                    val messagesArr = JSONArray()
                    session.messages.forEach { msg ->
                        val mObj = JSONObject().apply {
                            put("id", msg.id)
                            put("text", msg.text)
                            put("isUser", msg.isUser)
                            put("timestamp", msg.timestamp)
                        }
                        messagesArr.put(mObj)
                    }
                    put("messages", messagesArr)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_SESSIONS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val PREFS_NAME = "saku_speak_history"
        private const val KEY_SESSIONS = "saved_speak_sessions"
    }
}
