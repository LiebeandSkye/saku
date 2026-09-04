package com.saku.data

import java.util.UUID

data class AnkiVocabularyItem(
    val kanji: String,
    val reading: String = "",
    val meaning: String = "",
    val isSuspended: Boolean = false
) {
    val displayWord: String
        get() = kanji.ifBlank { reading }
}

data class ReadingVocabularySummary(
    val studiedCount: Int = 0,
    val suspendedCount: Int = 0,
    val words: List<AnkiVocabularyItem> = emptyList()
) {
    val totalCount: Int get() = studiedCount + suspendedCount
}

data class GeneratedStory(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val jlptLevel: String,
    val createdAt: Long = System.currentTimeMillis(),
    val targetWords: List<String> = emptyList()
)
