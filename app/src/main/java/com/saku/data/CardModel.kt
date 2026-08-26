package com.saku.data

data class CardModel(
    val cardId: Long,
    val noteId: Long,
    val deckId: Long,
    val cardOrd: Int = 0,           // Ordinal of card template (0, 1, ...)
    val kanji: String,              // e.g. "日" or "日本語"
    val kana: String,               // e.g. "ひ" or "にほんご"
    val romaji: String = "",        // e.g. "hi" or "nihongo"
    val meaning: String,            // e.g. "sun, day"
    val example: String = "",       // e.g. "日本 • Japan" or "私は学生です。 • I am a student."
    val exampleSentence: String = "", // e.g. "私は学生です。"
    val exampleTranslation: String = "", // e.g. "I am a student."
    val intervalDays: Int = 0,
    val isDue: Boolean = true
)

data class AnkiDeck(
    val id: Long,
    val name: String,
    val dueCardCount: Int = 0
)

enum class ReviewEase(val value: Int, val label: String) {
    AGAIN(1, "Again"),
    HARD(2, "Hard"),
    GOOD(3, "Good"),
    EASY(4, "Easy")
}
