package com.saku.data

import com.saku.anki.JapaneseFieldParser

data class JishoJapanese(
    val word: String? = null,
    val reading: String? = null
)

data class JishoLink(
    val text: String,
    val url: String
)

data class JishoSense(
    val englishDefinitions: List<String> = emptyList(),
    val partsOfSpeech: List<String> = emptyList(),
    val links: List<JishoLink> = emptyList(),
    val tags: List<String> = emptyList(),
    val restrictions: List<String> = emptyList(),
    val seeAlso: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val info: List<String> = emptyList()
)

data class JishoWord(
    val slug: String,
    val isCommon: Boolean = false,
    val tags: List<String> = emptyList(),
    val jlpt: List<String> = emptyList(),
    val japanese: List<JishoJapanese> = emptyList(),
    val senses: List<JishoSense> = emptyList()
) {
    val primaryWord: String
        get() = japanese.firstOrNull()?.word ?: slug

    val primaryReading: String
        get() = japanese.firstOrNull()?.reading ?: ""

    val romaji: String
        get() = if (primaryReading.isNotBlank()) {
            JapaneseFieldParser.kanaToRomaji(primaryReading)
        } else if (primaryWord.isNotBlank() && primaryWord != slug) {
            JapaneseFieldParser.kanaToRomaji(primaryWord)
        } else {
            ""
        }

    val jlptBadge: String?
        get() {
            val level = jlpt.firstOrNull() ?: return null
            val digits = level.filter { it.isDigit() }
            return if (digits.isNotBlank()) "N$digits" else level.uppercase()
        }

    val otherForms: List<String>
        get() = japanese.drop(1).mapNotNull { item ->
            when {
                !item.word.isNullOrBlank() && !item.reading.isNullOrBlank() -> "${item.word} (${item.reading})"
                !item.word.isNullOrBlank() -> item.word
                !item.reading.isNullOrBlank() -> item.reading
                else -> null
            }
        }
}
