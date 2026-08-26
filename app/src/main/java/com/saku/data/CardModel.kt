package com.saku.data

data class CardModel(
    val cardId: Long,
    val noteId: Long,
    val deckId: Long,
    val cardOrd: Int = 0,               // Ordinal of card template (0, 1, ...)
    val kanji: String,                  // e.g. "九" or "日本語"
    val kana: String,                   // e.g. "きゅう" or "にほんご"
    val furigana: String = "",          // e.g. "きゅう" or "九[きゅう]"
    val romaji: String = "",            // e.g. "kyuu" or "nihongo"
    val meaning: String,                // e.g. "nine" or "sun, day"
    val example: String = "",           // e.g. "野球は九人で1チームです。 • In baseball there are nine people on one team."
    val exampleSentence: String = "",   // e.g. "野球は九人で1チームです。"
    val exampleFurigana: String = "",   // e.g. "野球[やきゅう]は 九人[きゅうにん]で 1チームです。"
    val exampleTranslation: String = "",// e.g. "In baseball there are nine people on one team."
    val exampleFuriganaLine: String = "", // e.g. "や きゅう   きゅうにん"
    val exampleSentenceLine: String = "", // e.g. "野球は九人で1チームです。"
    val newCount: Int = 0,              // Anki deck new card count (e.g. 15)
    val learnCount: Int = 0,            // Anki deck learn card count (e.g. 17)
    val reviewCount: Int = 0,           // Anki deck review card count (e.g. 21)
    val intervalDays: Int = 0,
    val isDue: Boolean = true
)

data class AnkiDeck(
    val id: Long,
    val name: String,
    val dueCardCount: Int = 0,
    val newCount: Int = 0,
    val learnCount: Int = 0,
    val reviewCount: Int = 0
)

enum class ReviewEase(val value: Int, val label: String) {
    AGAIN(1, "Again"),
    HARD(2, "Hard"),
    GOOD(3, "Good"),
    EASY(4, "Easy")
}

