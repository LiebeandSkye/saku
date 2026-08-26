package com.saku.anki

import java.util.regex.Pattern

object JapaneseFieldParser {

    private val HTML_TAG_PATTERN = Pattern.compile("<[^>]*>")
    private val SOUND_TAG_PATTERN = Pattern.compile("\\[sound:[^]]+]")
    private val FURIGANA_BRACKET_PATTERN = Pattern.compile(" ?([^ \\[\\]]+)\\[([^\\]]+)\\]")

    /**
     * Strips HTML and Anki sound tags from text.
     */
    fun cleanHtml(input: String): String {
        var clean = SOUND_TAG_PATTERN.matcher(input).replaceAll("")
        clean = HTML_TAG_PATTERN.matcher(clean).replaceAll("")
        return clean.replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .trim()
    }

    /**
     * Extracts pure Kanji and clean reading from Anki Furigana bracket formats like "日本[にほん]".
     */
    fun extractKanjiAndKana(rawExpression: String): Pair<String, String> {
        val cleaned = cleanHtml(rawExpression)
        val matcher = FURIGANA_BRACKET_PATTERN.matcher(cleaned)
        val kanjiSb = StringBuilder()
        val kanaSb = StringBuilder()
        var lastEnd = 0

        while (matcher.find()) {
            kanjiSb.append(cleaned.substring(lastEnd, matcher.start()))
            kanaSb.append(cleaned.substring(lastEnd, matcher.start()))

            val kanjiPart = matcher.group(1) ?: ""
            val readingPart = matcher.group(2) ?: ""

            kanjiSb.append(kanjiPart)
            kanaSb.append(readingPart)
            lastEnd = matcher.end()
        }
        kanjiSb.append(cleaned.substring(lastEnd))
        kanaSb.append(cleaned.substring(lastEnd))

        val finalKanji = kanjiSb.toString().trim()
        val finalKana = kanaSb.toString().trim()

        return Pair(
            if (finalKanji.isNotEmpty()) finalKanji else cleaned,
            if (finalKana.isNotEmpty()) finalKana else cleaned
        )
    }

    /**
     * Generates a basic romaji approximation for kana if not provided in the deck.
     */
    fun kanaToRomaji(kana: String): String {
        val map = mapOf(
            "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
            "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
            "さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
            "た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
            "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
            "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
            "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
            "や" to "ya", "ゆ" to "yu", "よ" to "yo",
            "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
            "わ" to "wa", "を" to "o", "ん" to "n",
            "が" to "ga", "ぎ" to "gi", "ぐ" to "gu", "げ" to "ge", "ご" to "go",
            "ざ" to "za", "じ" to "ji", "ず" to "zu", "ぜ" to "ze", "ぞ" to "zo",
            "だ" to "da", "ぢ" to "ji", "づ" to "zu", "で" to "de", "ど" to "do",
            "ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "ぼ" to "bo",
            "ぱ" to "pa", "ぴ" to "pi", "ぷ" to "pu", "ぺ" to "pe", "ぽ" to "po"
        )
        val sb = StringBuilder()
        var i = 0
        while (i < kana.length) {
            val char = kana[i].toString()
            if (map.containsKey(char)) {
                sb.append(map[char])
            } else {
                sb.append(char)
            }
            i++
        }
        return sb.toString().trim()
    }
}
