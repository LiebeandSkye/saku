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
        private const val KEY_CURRENT_FURIGANA = "current_furigana"
        private const val KEY_CURRENT_ROMAJI = "current_romaji"
        private const val KEY_CURRENT_MEANING = "current_meaning"
        private const val KEY_CURRENT_EXAMPLE = "current_example"
        private const val KEY_CURRENT_EXAMPLE_SENTENCE = "current_example_sentence"
        private const val KEY_CURRENT_EXAMPLE_FURIGANA = "current_example_furigana"
        private const val KEY_CURRENT_EXAMPLE_TRANS = "current_example_trans"
        private const val KEY_CURRENT_EXAMPLE_FURIGANA_LINE = "current_example_furigana_line"
        private const val KEY_CURRENT_EXAMPLE_SENTENCE_LINE = "current_example_sentence_line"
        private const val KEY_CURRENT_CARD_ID = "current_card_id"
        private const val KEY_CURRENT_NOTE_ID = "current_note_id"
        private const val KEY_CURRENT_CARD_ORD = "current_card_ord"
        private const val KEY_COUNT_NEW = "count_new"
        private const val KEY_COUNT_LEARN = "count_learn"
        private const val KEY_COUNT_REVIEW = "count_review"
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
            .putString(KEY_CURRENT_FURIGANA, card.furigana)
            .putString(KEY_CURRENT_ROMAJI, card.romaji)
            .putString(KEY_CURRENT_MEANING, card.meaning)
            .putString(KEY_CURRENT_EXAMPLE, card.example)
            .putString(KEY_CURRENT_EXAMPLE_SENTENCE, card.exampleSentence)
            .putString(KEY_CURRENT_EXAMPLE_FURIGANA, card.exampleFurigana)
            .putString(KEY_CURRENT_EXAMPLE_TRANS, card.exampleTranslation)
            .putString(KEY_CURRENT_EXAMPLE_FURIGANA_LINE, card.exampleFuriganaLine)
            .putString(KEY_CURRENT_EXAMPLE_SENTENCE_LINE, card.exampleSentenceLine)
            .putInt(KEY_COUNT_NEW, card.newCount)
            .putInt(KEY_COUNT_LEARN, card.learnCount)
            .putInt(KEY_COUNT_REVIEW, card.reviewCount)
            .putBoolean(KEY_IS_ANSWER_REVEALED, false)
            .apply()
    }

    fun getActiveCard(): CardModel {
        return CardModel(
            cardId = prefs.getLong(KEY_CURRENT_CARD_ID, -1L),
            noteId = prefs.getLong(KEY_CURRENT_NOTE_ID, -1L),
            deckId = selectedDeckId,
            cardOrd = prefs.getInt(KEY_CURRENT_CARD_ORD, 0),
            kanji = prefs.getString(KEY_CURRENT_KANJI, "九") ?: "九",
            kana = prefs.getString(KEY_CURRENT_KANA, "きゅう") ?: "きゅう",
            furigana = prefs.getString(KEY_CURRENT_FURIGANA, "きゅう") ?: "きゅう",
            romaji = prefs.getString(KEY_CURRENT_ROMAJI, "kyuu") ?: "kyuu",
            meaning = prefs.getString(KEY_CURRENT_MEANING, "nine") ?: "nine",
            example = prefs.getString(KEY_CURRENT_EXAMPLE, "野球は九人で1チームです。 • In baseball there are nine people on one team.") ?: "野球は九人で1チームです。 • In baseball there are nine people on one team.",
            exampleSentence = prefs.getString(KEY_CURRENT_EXAMPLE_SENTENCE, "野球は九人で1チームです。") ?: "野球は九人で1チームです。",
            exampleFurigana = prefs.getString(KEY_CURRENT_EXAMPLE_FURIGANA, "野球[やきゅう]は 九人[きゅうにん]で 1チームです。") ?: "野球[やきゅう]は 九人[きゅうにん]で 1チームです。",
            exampleTranslation = prefs.getString(KEY_CURRENT_EXAMPLE_TRANS, "In baseball there are nine people on one team.") ?: "In baseball there are nine people on one team.",
            exampleFuriganaLine = prefs.getString(KEY_CURRENT_EXAMPLE_FURIGANA_LINE, "や きゅう   きゅうにん") ?: "や きゅう   きゅうにん",
            exampleSentenceLine = prefs.getString(KEY_CURRENT_EXAMPLE_SENTENCE_LINE, "野球は九人で1チームです。") ?: "野球は九人で1チームです。",
            newCount = prefs.getInt(KEY_COUNT_NEW, 15),
            learnCount = prefs.getInt(KEY_COUNT_LEARN, 17),
            reviewCount = prefs.getInt(KEY_COUNT_REVIEW, 21)
        )
    }
}

