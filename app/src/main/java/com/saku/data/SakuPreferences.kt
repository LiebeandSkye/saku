package com.saku.data

import android.content.Context
import android.content.SharedPreferences

class SakuPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("saku_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SELECTED_DECK_ID = "selected_deck_id"
        private const val KEY_SELECTED_DECK_NAME = "selected_deck_name"
        private const val KEY_CURRENT_KANJI = "current_kanji"
        private const val KEY_CURRENT_KANA = "current_kana"
        private const val KEY_CURRENT_ROMAJI = "current_romaji"
        private const val KEY_CURRENT_MEANING = "current_meaning"
        private const val KEY_CURRENT_EXAMPLE = "current_example"
        private const val KEY_CURRENT_EXAMPLE_SENTENCE = "current_example_sentence"
        private const val KEY_CURRENT_EXAMPLE_TRANS = "current_example_trans"
        private const val KEY_CURRENT_CARD_ID = "current_card_id"
        private const val KEY_CURRENT_NOTE_ID = "current_note_id"
        private const val KEY_CURRENT_CARD_ORD = "current_card_ord"
        private const val KEY_LOCKSCREEN_ENABLED = "lockscreen_enabled"
        private const val KEY_MINIMAL_MODE = "minimal_mode"
        private const val KEY_IS_ANSWER_REVEALED = "is_answer_revealed"
    }

    var selectedDeckId: Long
        get() = prefs.getLong(KEY_SELECTED_DECK_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_SELECTED_DECK_ID, value).apply()

    var selectedDeckName: String
        get() = prefs.getString(KEY_SELECTED_DECK_NAME, "All Due Cards") ?: "All Due Cards"
        set(value) = prefs.edit().putString(KEY_SELECTED_DECK_NAME, value).apply()

    var isLockScreenCardEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCKSCREEN_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCKSCREEN_ENABLED, value).apply()

    var isMinimalMode: Boolean
        get() = prefs.getBoolean(KEY_MINIMAL_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_MINIMAL_MODE, value).apply()

    var isAnswerRevealed: Boolean
        get() = prefs.getBoolean(KEY_IS_ANSWER_REVEALED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ANSWER_REVEALED, value).apply()

    fun saveActiveCard(card: CardModel) {
        prefs.edit()
            .putLong(KEY_CURRENT_CARD_ID, card.cardId)
            .putLong(KEY_CURRENT_NOTE_ID, card.noteId)
            .putInt(KEY_CURRENT_CARD_ORD, card.cardOrd)
            .putString(KEY_CURRENT_KANJI, card.kanji)
            .putString(KEY_CURRENT_KANA, card.kana)
            .putString(KEY_CURRENT_ROMAJI, card.romaji)
            .putString(KEY_CURRENT_MEANING, card.meaning)
            .putString(KEY_CURRENT_EXAMPLE, card.example)
            .putString(KEY_CURRENT_EXAMPLE_SENTENCE, card.exampleSentence)
            .putString(KEY_CURRENT_EXAMPLE_TRANS, card.exampleTranslation)
            .putBoolean(KEY_IS_ANSWER_REVEALED, false)
            .apply()
    }

    fun getActiveCard(): CardModel {
        return CardModel(
            cardId = prefs.getLong(KEY_CURRENT_CARD_ID, -1L),
            noteId = prefs.getLong(KEY_CURRENT_NOTE_ID, -1L),
            deckId = selectedDeckId,
            cardOrd = prefs.getInt(KEY_CURRENT_CARD_ORD, 0),
            kanji = prefs.getString(KEY_CURRENT_KANJI, "日") ?: "日",
            kana = prefs.getString(KEY_CURRENT_KANA, "ひ") ?: "ひ",
            romaji = prefs.getString(KEY_CURRENT_ROMAJI, "hi") ?: "hi",
            meaning = prefs.getString(KEY_CURRENT_MEANING, "sun") ?: "sun",
            example = prefs.getString(KEY_CURRENT_EXAMPLE, "日本 • Japan") ?: "日本 • Japan",
            exampleSentence = prefs.getString(KEY_CURRENT_EXAMPLE_SENTENCE, "日本") ?: "日本",
            exampleTranslation = prefs.getString(KEY_CURRENT_EXAMPLE_TRANS, "Japan") ?: "Japan"
        )
    }
}
