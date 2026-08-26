package com.saku.anki

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import java.util.regex.Pattern

data class FuriganaSegment(
    val text: String,
    val reading: String = "",
    val isTarget: Boolean = false
)

data class ParsedJapaneseCard(
    val kanji: String,
    val kana: String,
    val furigana: String = "",
    val romaji: String,
    val meaning: String,
    val example: String,
    val exampleSentence: String = "",
    val exampleFurigana: String = "",
    val exampleTranslation: String = "",
    val exampleFuriganaLine: String = "",
    val exampleSentenceLine: String = ""
)

object JapaneseFieldParser {

    private val STYLE_TAG_PATTERN = Pattern.compile("(?i)<style[^>]*>.*?</style>", Pattern.DOTALL)
    private val SCRIPT_TAG_PATTERN = Pattern.compile("(?i)<script[^>]*>.*?</script>", Pattern.DOTALL)
    private val HTML_TAG_PATTERN = Pattern.compile("<[^>]*>")
    private val SOUND_TAG_PATTERN = Pattern.compile("\\[sound:[^]]+]")
    private val RUBY_TAG_PATTERN = Pattern.compile("(?i)<ruby[^>]*>(?:<rb[^>]*>)?(.*?)(?:</rb>)?(?:<rp[^>]*>.*?</rp>)?<rt[^>]*>(.*?)</rt>(?:<rp[^>]*>.*?</rp>)?</ruby>")
    private val FURIGANA_BRACKET_PATTERN = Pattern.compile("([^ \\[\\]]+)\\[([^\\]]+)\\]")

    // Japanese Unicode ranges
    private val KANJI_PATTERN = Pattern.compile("[\\u4E00-\\u9FAF\\u3400-\\u4DBF]")
    private val KANA_PATTERN = Pattern.compile("[\\u3040-\\u309F\\u30A0-\\u30FF]")

    /**
     * Strips HTML, style/script blocks, and Anki sound tags from text.
     */
    fun cleanHtml(input: String): String {
        if (input.isEmpty()) return ""
        var clean = STYLE_TAG_PATTERN.matcher(input).replaceAll("")
        clean = SCRIPT_TAG_PATTERN.matcher(clean).replaceAll("")
        clean = SOUND_TAG_PATTERN.matcher(clean).replaceAll("")
        // Replace breaks and block tags with spaces
        clean = clean.replace(Regex("(?i)<br\\s*/?>"), " ")
            .replace(Regex("(?i)</?p>"), " ")
            .replace(Regex("(?i)</?div>"), " ")
        clean = HTML_TAG_PATTERN.matcher(clean).replaceAll("")
        return decodeHtmlEntities(clean).trim().replace(Regex("\\s+"), " ")
    }

    private fun decodeHtmlEntities(input: String): String {
        return input.replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&minus;", "-")
            .replace("&#8217;", "'")
            .replace("&#8216;", "'")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
    }

    /**
     * Parses ruby tags `<ruby>漢<rt>かん</rt></ruby>` into kanji and kana strings.
     */
    fun extractFromRuby(rawHtml: String): Pair<String, String>? {
        if (!rawHtml.contains("<ruby", ignoreCase = true)) return null

        val kanjiSb = StringBuilder()
        val kanaSb = StringBuilder()
        val matcher = RUBY_TAG_PATTERN.matcher(rawHtml)
        var hasRuby = false
        var lastEnd = 0

        while (matcher.find()) {
            hasRuby = true
            val precedingText = cleanHtml(rawHtml.substring(lastEnd, matcher.start()))
            kanjiSb.append(precedingText)
            kanaSb.append(precedingText)

            val baseText = cleanHtml(matcher.group(1) ?: "")
            val readingText = cleanHtml(matcher.group(2) ?: "")

            kanjiSb.append(baseText)
            kanaSb.append(readingText)
            lastEnd = matcher.end()
        }

        if (!hasRuby) return null

        val remainingText = cleanHtml(rawHtml.substring(lastEnd))
        kanjiSb.append(remainingText)
        kanaSb.append(remainingText)

        return Pair(kanjiSb.toString().trim(), kanaSb.toString().trim())
    }

    /**
     * Extracts pure Kanji and clean reading from Anki Furigana bracket formats like "日本[にほん]" or " 私[わたし]".
     */
    fun extractKanjiAndKana(rawExpression: String): Pair<String, String> {
        // 1. Try ruby tags first if present
        extractFromRuby(rawExpression)?.let { (k, r) ->
            if (k.isNotEmpty() && r.isNotEmpty()) return Pair(k, r)
        }

        val cleaned = cleanHtml(rawExpression)
        val matcher = FURIGANA_BRACKET_PATTERN.matcher(cleaned)
        val kanjiSb = StringBuilder()
        val kanaSb = StringBuilder()
        var lastEnd = 0
        var foundBrackets = false

        while (matcher.find()) {
            foundBrackets = true
            val precedingText = cleaned.substring(lastEnd, matcher.start())
            kanjiSb.append(precedingText)
            kanaSb.append(precedingText)

            val kanjiPart = matcher.group(1) ?: ""
            val readingPart = matcher.group(2) ?: ""

            kanjiSb.append(kanjiPart)
            kanaSb.append(readingPart)
            lastEnd = matcher.end()
        }

        if (foundBrackets) {
            val remainingText = cleaned.substring(lastEnd)
            kanjiSb.append(remainingText)
            kanaSb.append(remainingText)

            val finalKanji = kanjiSb.toString().trim()
            val finalKana = kanaSb.toString().trim()
            return Pair(
                if (finalKanji.isNotEmpty()) finalKanji else cleaned,
                if (finalKana.isNotEmpty()) finalKana else cleaned
            )
        }

        return Pair(cleaned, cleaned)
    }

    /**
     * Parses a Japanese sentence (with bracket furigana or ruby tags) into individual FuriganaSegments.
     */
    fun parseFuriganaSegments(rawSentence: String, targetWord: String = ""): List<FuriganaSegment> {
        val segments = mutableListOf<FuriganaSegment>()
        if (rawSentence.isBlank()) return segments

        // 1. Check for Ruby tags
        if (rawSentence.contains("<ruby", ignoreCase = true)) {
            val matcher = RUBY_TAG_PATTERN.matcher(rawSentence)
            var lastEnd = 0
            while (matcher.find()) {
                val pre = cleanHtml(rawSentence.substring(lastEnd, matcher.start()))
                if (pre.isNotEmpty()) {
                    addPlainSegmentsWithTarget(segments, pre, targetWord)
                }
                val base = cleanHtml(matcher.group(1) ?: "")
                val reading = cleanHtml(matcher.group(2) ?: "")
                val isTarget = isTargetMatch(base, targetWord)
                if (base.isNotEmpty()) {
                    segments.add(FuriganaSegment(text = base, reading = reading, isTarget = isTarget))
                }
                lastEnd = matcher.end()
            }
            val post = cleanHtml(rawSentence.substring(lastEnd))
            if (post.isNotEmpty()) {
                addPlainSegmentsWithTarget(segments, post, targetWord)
            }
            return segments
        }

        // 2. Parse Bracket format: 野球[やきゅう]は 九人[きゅうにん]で 1チームです。
        // Check if there is target highlighting in HTML e.g. <span class="highlight">...</span> or <b>...</b>
        val processed = rawSentence
            .replace(Regex("(?i)<(?:span|b|strong)[^>]*class=[\"']?[^\"'>]*highlight[^\"'>]*[\"']?[^>]*>(.*?)</(?:span|b|strong)>"), " @@TARGET_START@@$1@@TARGET_END@@ ")
            .replace(Regex("(?i)<(?:b|strong)>(.*?)</(?:b|strong)>"), " @@TARGET_START@@$1@@TARGET_END@@ ")

        val cleaned = cleanHtml(processed)
        val matcher = FURIGANA_BRACKET_PATTERN.matcher(cleaned)
        var lastEnd = 0
        var foundAny = false

        while (matcher.find()) {
            foundAny = true
            val preceding = cleaned.substring(lastEnd, matcher.start())
            if (preceding.isNotEmpty()) {
                addPlainSegmentsWithTarget(segments, preceding, targetWord)
            }

            var kanjiPart = matcher.group(1) ?: ""
            val readingPart = matcher.group(2) ?: ""

            var isExplicitTarget = false
            if (kanjiPart.contains("@@TARGET_START@@")) {
                isExplicitTarget = true
                kanjiPart = kanjiPart.replace("@@TARGET_START@@", "").replace("@@TARGET_END@@", "")
            }

            val isTarget = isExplicitTarget || isTargetMatch(kanjiPart, targetWord)
            segments.add(FuriganaSegment(text = kanjiPart.trim(), reading = readingPart.trim(), isTarget = isTarget))
            lastEnd = matcher.end()
        }

        if (foundAny) {
            val remaining = cleaned.substring(lastEnd)
            if (remaining.isNotEmpty()) {
                addPlainSegmentsWithTarget(segments, remaining, targetWord)
            }
            return segments
        }

        // 3. Plain sentence fallback
        addPlainSegmentsWithTarget(segments, cleaned, targetWord)
        return segments
    }

    private fun addPlainSegmentsWithTarget(
        segments: MutableList<FuriganaSegment>,
        text: String,
        targetWord: String
    ) {
        val clean = text.replace("@@TARGET_START@@", "").replace("@@TARGET_END@@", "").trim()
        if (clean.isEmpty()) return

        val cleanTarget = targetWord.trim()
        if (cleanTarget.isNotEmpty() && clean.contains(cleanTarget)) {
            val parts = clean.split(cleanTarget)
            for (i in parts.indices) {
                if (parts[i].isNotEmpty()) {
                    segments.add(FuriganaSegment(text = parts[i], reading = "", isTarget = false))
                }
                if (i < parts.size - 1) {
                    segments.add(FuriganaSegment(text = cleanTarget, reading = "", isTarget = true))
                }
            }
        } else {
            segments.add(FuriganaSegment(text = clean, reading = "", isTarget = false))
        }
    }

    private fun isTargetMatch(text: String, targetWord: String): Boolean {
        if (targetWord.isBlank() || text.isBlank()) return false
        val cleanT = targetWord.trim()
        val cleanText = text.trim()
        return cleanText == cleanT || cleanText.contains(cleanT) || cleanT.contains(cleanText)
    }

    /**
     * Formats Furigana segments into a 2-line string for Notification RemoteViews where:
     * Line 1: Spaced Furigana reading aligned over each kanji
     * Line 2: Japanese sentence with proportional spacing
     */
    fun alignFuriganaTwoLines(segments: List<FuriganaSegment>): Pair<String, String> {
        if (segments.isEmpty()) return Pair("", "")

        val furiganaSb = StringBuilder()
        val sentenceSb = StringBuilder()

        for (seg in segments) {
            val text = seg.text
            val reading = seg.reading

            if (reading.isNotEmpty()) {
                val textLen = text.length

                // Format spaced furigana over kanji
                val spacedReading = reading.toCharArray().joinToString(" ")
                furiganaSb.append(spacedReading).append(" ")
                sentenceSb.append(text).append(" ".repeat(maxOf(1, spacedReading.length - textLen)))
            } else {
                sentenceSb.append(text)
                // Append spaces of equal length on furigana line
                furiganaSb.append(" ".repeat(text.length))
            }
        }

        return Pair(furiganaSb.toString().trimEnd(), sentenceSb.toString().trimEnd())
    }

    /**
     * Checks if a card is an instruction/welcome note (e.g. Kaishi 1.5k Welcome Card)
     * rather than an actual vocabulary flashcard.
     */
    fun isInstructionOrInvalidCard(kanji: String, kana: String = "", meaning: String = ""): Boolean {
        // If neither kanji nor kana has Japanese characters, it's not a Japanese card
        if (!hasJapaneseChars(kanji) && !hasJapaneseChars(kana)) {
            return true
        }

        val combined = "$kanji $kana $meaning".lowercase()
        val instructionKeywords = listOf(
            "welcome to kaishi",
            "welcome to",
            "read me",
            "readme",
            "instructions",
            "instruction",
            "how to use",
            "deck settings",
            "guide",
            "kaishi 1.5k",
            "kaishi 2.3k",
            "kaishi 2k"
        )

        for (kw in instructionKeywords) {
            if (combined.contains(kw) && (!hasJapaneseChars(kanji) || kanji.length > 20)) {
                return true
            }
        }

        return false
    }

    /**
     * Maps note fields from any deck (Kaishi 1.5k/2k, Core 2k/6k, Tango, Basic, Yomichan)
     * to a structured Japanese card model with rich Furigana support.
     */
    fun mapFieldsToJapaneseCard(
        fieldNames: List<String>,
        fieldValues: List<String>,
        fallbackQuestion: String = "",
        fallbackAnswer: String = ""
    ): ParsedJapaneseCard {
        val fieldsByName = mutableMapOf<String, String>()
        for (i in fieldNames.indices) {
            val name = fieldNames[i].trim().lowercase()
            val value = fieldValues.getOrNull(i) ?: ""
            fieldsByName[name] = value
        }

        // 1. Extract Target Word / Kanji
        val kanjiKeywords = listOf(
            "vocabularykanji", "vocabkanji", "vocabword", "targetword", "word",
            "expression", "kanji", "front", "japanese", "vocabulary", "vocab", "japaneseword"
        )
        val rawKanji = findFieldByKeywords(fieldsByName, kanjiKeywords, excludeKeywords = listOf("sentence", "sent", "example"))

        // 2. Extract Kana / Reading
        val kanaKeywords = listOf(
            "vocabularykana", "vocabkana", "targetwordkana", "vocabreading", "reading",
            "kana", "yomikata", "pronunciation", "hiragana", "katakana", "readingkana"
        )
        val rawKana = findFieldByKeywords(fieldsByName, kanaKeywords, excludeKeywords = listOf("sentence", "sent", "example"))

        // 3. Extract Vocabulary Furigana (e.g. Vocabulary-Furigana or Furigana)
        val furiganaKeywords = listOf(
            "vocabularyfurigana", "vocabfurigana", "targetwordfurigana", "furigana", "furiganaplain"
        )
        val rawFurigana = findFieldByKeywords(fieldsByName, furiganaKeywords, excludeKeywords = listOf("sentence", "sent", "example"))

        // 4. Extract Meaning / English
        val meaningKeywords = listOf(
            "vocabularyenglish", "vocabenglish", "targetwordmeaning", "vocabmeaning", "meaning",
            "english", "glossary", "definition", "translation", "primarymeaning", "back", "englishmeaning", "shortdefinition"
        )
        val rawMeaning = findFieldByKeywords(fieldsByName, meaningKeywords, excludeKeywords = listOf("sentence", "sent", "example"))

        // 5. Extract Example Sentence (Kanji / Expression)
        val exampleKeywords = listOf(
            "sentenceexpression", "sentencekanji", "examplesentence", "sentence", "example",
            "context", "japanesesentence", "sentkanji", "examplejapanese", "sentexpression", "sample"
        )
        val rawExample = findFieldByKeywords(fieldsByName, exampleKeywords, excludeKeywords = listOf("audio", "image", "picture", "english", "meaning", "translation"))

        // 6. Extract Sentence Furigana (e.g. Sentence-Furigana or Sentence-Kana)
        val sentenceFuriganaKeywords = listOf(
            "sentencefurigana", "sentfurigana", "examplefurigana", "sentencekana", "sentkana"
        )
        val rawSentenceFurigana = findFieldByKeywords(fieldsByName, sentenceFuriganaKeywords, excludeKeywords = listOf("audio", "image", "picture"))

        // 7. Extract Example Translation
        val exampleTransKeywords = listOf(
            "sentenceenglish", "sentencemeaning", "sentencetranslation", "exampletranslation",
            "exampleenglish", "senteng", "sentmeaning", "examplemeaning", "senttrans"
        )
        val rawExampleTranslation = findFieldByKeywords(fieldsByName, exampleTransKeywords)

        // Fallback: If positional guessing is needed
        val cleanValues = fieldValues.map { cleanHtml(it) }.filter { it.isNotEmpty() && !it.startsWith("[sound:") }
        val inferredKanji = if (rawKanji.isEmpty()) {
            cleanValues.firstOrNull { hasJapaneseChars(it) && it.length < 20 } ?: cleanValues.getOrNull(0) ?: ""
        } else rawKanji

        // Process furigana / ruby on Kanji
        val (extractedKanji, extractedKanaFromExpr) = extractKanjiAndKana(inferredKanji)

        val inferredKana = if (rawKana.isNotEmpty()) {
            val (pureKana, _) = extractKanjiAndKana(rawKana)
            pureKana
        } else if (extractedKanaFromExpr.isNotEmpty() && extractedKanaFromExpr != extractedKanji) {
            extractedKanaFromExpr
        } else {
            cleanValues.firstOrNull { isPureKana(it) && it != inferredKanji } ?: extractedKanaFromExpr
        }

        val inferredMeaning = if (rawMeaning.isEmpty()) {
            cleanValues.firstOrNull { !hasJapaneseChars(it) && it != inferredKanji && it != inferredKana }
                ?: cleanValues.getOrNull(2) ?: ""
        } else rawMeaning

        val inferredExample = if (rawExample.isEmpty()) {
            cleanValues.firstOrNull { hasJapaneseChars(it) && it != inferredKanji && it != inferredKana && it.length > 4 } ?: ""
        } else rawExample

        // Parse fallback Question & Answer if provided (for cards queried from rendered HTML)
        var fallbackParsedKanji = ""
        var fallbackParsedKana = ""
        var fallbackParsedMeaning = ""
        var fallbackParsedExample = ""

        if (fallbackQuestion.isNotEmpty() || fallbackAnswer.isNotEmpty()) {
            val (qKanji, qKana) = extractKanjiAndKana(fallbackQuestion)
            fallbackParsedKanji = qKanji
            fallbackParsedKana = qKana

            val answerClean = fallbackAnswer
                .replace(Regex("(?i)<hr\\s*(id=[\"']?answer[\"']?)?\\s*/?>"), " @@SPLIT@@ ")
            val answerParts = answerClean.split("@@SPLIT@@").map { cleanHtml(it) }
            val answerBack = answerParts.lastOrNull { it.isNotEmpty() } ?: cleanHtml(fallbackAnswer)

            if (!hasJapaneseChars(answerBack)) {
                fallbackParsedMeaning = answerBack
            } else {
                val lines = answerBack.split("\n", "  ").map { it.trim() }.filter { it.isNotEmpty() }
                for (line in lines) {
                    if (!hasJapaneseChars(line) && fallbackParsedMeaning.isEmpty()) {
                        fallbackParsedMeaning = line
                    } else if (hasJapaneseChars(line) && line.length > 5 && fallbackParsedExample.isEmpty()) {
                        fallbackParsedExample = line
                    }
                }
                if (fallbackParsedMeaning.isEmpty()) fallbackParsedMeaning = answerBack
            }
        }

        val finalKanji = cleanHtml(extractedKanji).ifEmpty { fallbackParsedKanji.ifEmpty { "日" } }
        val finalKana = cleanHtml(inferredKana).ifEmpty { fallbackParsedKana.ifEmpty { "ひ" } }

        val finalMeaning = cleanHtml(inferredMeaning).ifEmpty { fallbackParsedMeaning.ifEmpty { "sun, day" } }
        val finalExample = cleanHtml(inferredExample).ifEmpty { fallbackParsedExample }
        val finalExampleTrans = cleanHtml(rawExampleTranslation)
        val (cleanExampleSentence, _) = extractKanjiAndKana(finalExample)
        val fullExample = if (finalExample.isNotEmpty() && finalExampleTrans.isNotEmpty()) {
            "$finalExample • $finalExampleTrans"
        } else {
            finalExample.ifEmpty { finalExampleTrans }
        }

        val finalRomaji = kanaToRomaji(finalKana)

        // Process Sentence Furigana
        val sentenceFuriganaSource = rawSentenceFurigana.ifEmpty { rawExample }
        val segments = parseFuriganaSegments(sentenceFuriganaSource, targetWord = finalKanji)
        val (fLine, sLine) = alignFuriganaTwoLines(segments)

        val pureFurigana = if (rawFurigana.isNotEmpty()) {
            val (_, extractedReading) = extractKanjiAndKana(rawFurigana)
            extractedReading
        } else {
            finalKana
        }

        return ParsedJapaneseCard(
            kanji = finalKanji,
            kana = finalKana,
            furigana = pureFurigana,
            romaji = finalRomaji,
            meaning = finalMeaning,
            example = fullExample,
            exampleSentence = cleanExampleSentence,
            exampleFurigana = sentenceFuriganaSource,
            exampleTranslation = finalExampleTrans,
            exampleFuriganaLine = fLine,
            exampleSentenceLine = sLine.ifEmpty { cleanExampleSentence }
        )
    }

    private fun normalizeKey(key: String): String {
        return key.lowercase().replace(Regex("[^a-z0-9]"), "")
    }

    private fun findFieldByKeywords(
        fieldsByName: Map<String, String>,
        keywords: List<String>,
        excludeKeywords: List<String> = emptyList()
    ): String {
        val normalizedMap = mutableMapOf<String, String>()
        for ((k, v) in fieldsByName) {
            val normKey = normalizeKey(k)
            val isExcluded = excludeKeywords.any { normKey.contains(normalizeKey(it)) }
            if (!isExcluded) {
                normalizedMap[normKey] = v
            }
        }

        // 1. Exact match on normalized keys
        for (keyword in keywords) {
            val normKw = normalizeKey(keyword)
            normalizedMap[normKw]?.let {
                if (it.isNotEmpty()) return it
            }
        }

        // 2. Starts-with or Contains match on normalized keys
        for (keyword in keywords) {
            val normKw = normalizeKey(keyword)
            for ((normKey, value) in normalizedMap) {
                if (normKey.contains(normKw) && value.isNotEmpty()) {
                    return value
                }
            }
        }
        return ""
    }

    fun hasJapaneseChars(text: String): Boolean {
        return KANJI_PATTERN.matcher(text).find() || KANA_PATTERN.matcher(text).find()
    }

    fun isPureKana(text: String): Boolean {
        val stripped = text.replace(Regex("[\\s・、。！？!?]"), "")
        if (stripped.isEmpty()) return false
        return KANA_PATTERN.matcher(stripped).find() && !KANJI_PATTERN.matcher(stripped).find() && !text.contains(Regex("[a-zA-Z]"))
    }

    /**
     * Converts Hiragana and Katakana to Romaji (Hepburn approximation).
     */
    fun kanaToRomaji(kanaInput: String): String {
        if (kanaInput.isEmpty()) return ""
        val hiragana = katakanaToHiragana(kanaInput)

        val digraphs = mapOf(
            "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
            "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho", "しぇ" to "she",
            "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho", "ちぇ" to "che",
            "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
            "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
            "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
            "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
            "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
            "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo", "じぇ" to "je",
            "ぢゃ" to "ja", "ぢゅ" to "ju", "ぢょ" to "jo",
            "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
            "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
            "ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe", "ふぉ" to "fo",
            "てぃ" to "ti", "でぃ" to "di", "どぅ" to "du",
            "つぁ" to "tsa", "つぃ" to "tsi", "つぇ" to "tse", "つぉ" to "tso",
            "ゔぁ" to "va", "ゔぃ" to "vi", "ゔぇ" to "ve", "ゔぉ" to "vo"
        )

        val monophthongs = mapOf(
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
            "だ" to "da", "ぢ" to "ji", "づ" to "zu", "de" to "de", "ど" to "do",
            "ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "ぼ" to "bo",
            "ぱ" to "pa", "ぴ" to "pi", "ぷ" to "pu", "ぺ" to "pe", "ぽ" to "po",
            "ゔ" to "vu", "ぁ" to "a", "ぃ" to "i", "ぅ" to "u", "ぇ" to "e", "ぉ" to "o"
        )

        val vowels = setOf('a', 'i', 'u', 'e', 'o')
        val sb = StringBuilder()
        var i = 0
        while (i < hiragana.length) {
            if (hiragana[i] == 'ー') {
                val lastVowel = sb.lastOrNull { it in vowels }
                if (lastVowel != null) {
                    sb.append(lastVowel)
                }
                i++
                continue
            }

            if (hiragana[i] == 'っ' || hiragana[i] == 'ッ') {
                if (i + 1 < hiragana.length) {
                    val nextPair = if (i + 2 < hiragana.length) hiragana.substring(i + 1, i + 3) else ""
                    val nextRomaji = digraphs[nextPair] ?: monophthongs[hiragana[i + 1].toString()] ?: ""
                    if (nextRomaji.isNotEmpty()) {
                        val firstConsonant = if (nextRomaji.startsWith("ch")) 't' else nextRomaji[0]
                        sb.append(firstConsonant)
                        i++
                        continue
                    }
                }
            }

            if (i + 1 < hiragana.length) {
                val pair = hiragana.substring(i, i + 2)
                if (digraphs.containsKey(pair)) {
                    sb.append(digraphs[pair])
                    i += 2
                    continue
                }
            }

            val single = hiragana[i].toString()
            if (monophthongs.containsKey(single)) {
                sb.append(monophthongs[single])
            } else {
                sb.append(single)
            }
            i++
        }
        return sb.toString().trim()
    }

    private fun katakanaToHiragana(input: String): String {
        val sb = StringBuilder()
        for (c in input) {
            if (c in '\u30A1'..'\u30F6') {
                sb.append((c.code - 0x60).toChar())
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }
}

object FuriganaBitmapRenderer {

    fun renderFuriganaSentenceBitmap(
        context: Context,
        segments: List<FuriganaSegment>,
        kanjiTextSizeSp: Float = 16f,
        furiganaTextSizeSp: Float = 10f,
        defaultTextColor: Int = Color.WHITE,
        targetTextColor: Int = Color.parseColor("#6CA0DC"),
        furiganaDefaultColor: Int = Color.parseColor("#CCCCCC"),
        furiganaTargetColor: Int = Color.parseColor("#6CA0DC")
    ): Bitmap? {
        if (segments.isEmpty()) return null

        val displayMetrics = context.resources.displayMetrics
        val kanjiPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, kanjiTextSizeSp, displayMetrics)
        val furiganaPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, furiganaTextSizeSp, displayMetrics)

        val kanjiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = kanjiPx
            typeface = Typeface.DEFAULT_BOLD
        }

        val furiganaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = furiganaPx
            typeface = Typeface.DEFAULT
        }

        class MeasuredSegment(
            val seg: FuriganaSegment,
            val kanjiWidth: Float,
            val furiganaWidth: Float,
            val totalWidth: Float
        )

        val measured = segments.map { seg ->
            val kw = kanjiPaint.measureText(seg.text)
            val fw = if (seg.reading.isNotEmpty()) furiganaPaint.measureText(seg.reading) else 0f
            val tw = maxOf(kw, fw)
            MeasuredSegment(seg, kw, fw, tw)
        }

        val totalContentWidth = measured.sumOf { it.totalWidth.toDouble() }.toFloat()
        if (totalContentWidth <= 0f) return null

        val furiganaMetrics = furiganaPaint.fontMetrics
        val kanjiMetrics = kanjiPaint.fontMetrics

        val furiganaHeight = furiganaMetrics.descent - furiganaMetrics.ascent
        val kanjiHeight = kanjiMetrics.descent - kanjiMetrics.ascent
        val spacingBetween = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, displayMetrics)

        val totalHeight = (furiganaHeight + spacingBetween + kanjiHeight).toInt() + 6
        val totalWidth = totalContentWidth.toInt() + 12

        val bitmap = Bitmap.createBitmap(maxOf(totalWidth, 1), maxOf(totalHeight, 1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        var currentX = 6f
        val furiganaBaseline = 2f - furiganaMetrics.ascent
        val kanjiBaseline = furiganaBaseline + furiganaMetrics.descent + spacingBetween - kanjiMetrics.ascent

        for (m in measured) {
            val isTarget = m.seg.isTarget
            val text = m.seg.text
            val reading = m.seg.reading

            if (reading.isNotEmpty()) {
                furiganaPaint.color = if (isTarget) furiganaTargetColor else furiganaDefaultColor
                val furiganaX = currentX + (m.totalWidth - m.furiganaWidth) / 2f
                canvas.drawText(reading, furiganaX, furiganaBaseline, furiganaPaint)
            }

            kanjiPaint.color = if (isTarget) targetTextColor else defaultTextColor
            val kanjiX = currentX + (m.totalWidth - m.kanjiWidth) / 2f
            canvas.drawText(text, kanjiX, kanjiBaseline, kanjiPaint)

            currentX += m.totalWidth
        }

        return bitmap
    }
}

