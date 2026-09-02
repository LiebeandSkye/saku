package com.saku.data

data class DeckInfo(
    val id: Long,
    val name: String,
    val newCount: Int,
    val learnCount: Int,
    val reviewCount: Int
) {
    val totalDue: Int get() = newCount + learnCount + reviewCount
}

data class CardInfo(
    val noteId: Long,
    val cardOrd: Int,
    val question: String,
    val answer: String,
    val deckName: String,
    val buttonCount: Int = 4,
    val nextReviewTimes: String = "",
    val kanji: String = "",
    val kanjiFurigana: String = "",
    val kanjiMeaning: String = "",
    val sentence: String = "",
    val sentenceFurigana: String = "",
    val sentenceMeaning: String = "",
    val imageFileName: String = "",
    val cardType: Int = 0 // 0 = New, 1 = Learn, 2 = Review
)

data class CardModel(
    val cardId: Long = 0L,
    val noteId: Long = 0L,
    val deckId: Long = 0L,
    val cardOrd: Int = 0,
    val kanji: String = "",
    val kana: String = "",
    val furigana: String = "",
    val romaji: String = "",
    val meaning: String = "",
    val example: String = "",
    val exampleSentence: String = "",
    val exampleFurigana: String = "",
    val exampleTranslation: String = "",
    val exampleFuriganaLine: String = "",
    val exampleSentenceLine: String = "",
    val newCount: Int = 0,
    val learnCount: Int = 0,
    val reviewCount: Int = 0,
    val intervalDays: Int = 0,
    val isDue: Boolean = true,
    val cardType: Int = 0,
    val imageFileName: String = ""
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
