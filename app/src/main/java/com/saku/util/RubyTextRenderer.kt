package com.saku.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextPaint
import kotlin.math.max
import kotlin.math.min

object RubyTextRenderer {

    data class RubyToken(
        val base: String,
        val ruby: String? = null,
        val isTarget: Boolean = false
    )

    private data class MeasuredToken(
        val token: RubyToken,
        val wBase: Float,
        val wRuby: Float,
        val totalWidth: Float
    )

    private fun isKanji(char: Char): Boolean {
        return char in '\u4E00'..'\u9FAF' || char in '\u3400'..'\u4DBF'
    }

    private fun normalizeRubyString(input: String): String {
        // Support standard HTML ruby, ruby with <rp> parentheses, <rb> base tags, attributes, and case-insensitivity
        val rubyRegex = Regex(
            "<ruby[^>]*>(?:<rb>)?([^<]+?)(?:</rb>)?(?:\\s*<rp>[^<]*</rp>)*\\s*<rt[^>]*>([^<]+?)</rt>(?:\\s*<rp>[^<]*</rp>)*\\s*</ruby>",
            RegexOption.IGNORE_CASE
        )
        var text = input.replace(rubyRegex, "$1[$2]")
        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .replace(Regex("<[^>]*>"), "")
        return text.trim()
    }

    private fun addPlainTokens(list: MutableList<RubyToken>, text: String) {
        val clean = text.replace(" ", "")
        if (clean.isNotEmpty()) {
            list.add(RubyToken(base = clean, ruby = null, isTarget = false))
        }
    }

    fun parseRubyTokens(rawText: String, highlightWord: String = ""): List<RubyToken> {
        val normalized = normalizeRubyString(rawText)
        if (normalized.isBlank()) return emptyList()

        val tokens = mutableListOf<RubyToken>()
        var cursor = 0

        while (cursor < normalized.length) {
            val bracketOpen = normalized.indexOf('[', cursor)
            if (bracketOpen == -1) {
                val remaining = normalized.substring(cursor)
                addPlainTokens(tokens, remaining)
                break
            }

            val bracketClose = normalized.indexOf(']', bracketOpen)
            if (bracketClose == -1) {
                val remaining = normalized.substring(cursor)
                addPlainTokens(tokens, remaining)
                break
            }

            val rubyRaw = normalized.substring(bracketOpen + 1, bracketClose).trim()
            val beforeBracket = normalized.substring(cursor, bracketOpen)

            val lastSpaceIdx = beforeBracket.lastIndexOf(' ')
            val baseStart: Int
            var adjustedRuby = rubyRaw
            if (lastSpaceIdx != -1) {
                val plainPart = beforeBracket.substring(0, lastSpaceIdx)
                addPlainTokens(tokens, plainPart)
                baseStart = lastSpaceIdx + 1
            } else {
                var kIdx = beforeBracket.length - 1
                while (kIdx >= 0 && isKanji(beforeBracket[kIdx])) {
                    kIdx--
                }
                val plainPart = beforeBracket.substring(0, kIdx + 1)
                addPlainTokens(tokens, plainPart)
                baseStart = kIdx + 1

                // If plainPart ends with kana that matches the start of ruby (e.g. お茶[おちゃ] without a space),
                // strip the duplicate kana from ruby so it is not duplicated in rendering
                if (plainPart.isNotEmpty()) {
                    var pIdx = 0
                    while (pIdx < plainPart.length && !isKanji(plainPart[plainPart.length - 1 - pIdx])) {
                        pIdx++
                    }
                    val trailingKana = plainPart.takeLast(pIdx)
                    if (trailingKana.isNotEmpty() && adjustedRuby.startsWith(trailingKana)) {
                        adjustedRuby = adjustedRuby.substring(trailingKana.length)
                    }
                }
            }

            val base = beforeBracket.substring(baseStart).trim()
            if (base.isNotEmpty() && adjustedRuby.isNotEmpty()) {
                tokens.add(RubyToken(base = base, ruby = adjustedRuby, isTarget = false))
            } else if (base.isNotEmpty()) {
                addPlainTokens(tokens, base)
            }

            cursor = bracketClose + 1
        }

        val cleanedTokens = mutableListOf<RubyToken>()
        for (t in tokens) {
            if (t.ruby == null) {
                val text = t.base.replace(" ", "")
                if (text.isNotEmpty()) {
                    cleanedTokens.add(RubyToken(base = text, ruby = null, isTarget = false))
                }
            } else {
                cleanedTokens.add(t)
            }
        }

        val cleanHighlight = highlightWord.trim()
        if (cleanHighlight.isNotEmpty()) {
            val rootKanji = cleanHighlight.filter { isKanji(it) }
            for (i in cleanedTokens.indices) {
                val t = cleanedTokens[i]
                if (t.base == cleanHighlight ||
                    (cleanHighlight.isNotEmpty() && cleanHighlight.contains(t.base) && isKanji(t.base.firstOrNull() ?: ' ')) ||
                    (rootKanji.isNotEmpty() && rootKanji.contains(t.base))) {
                    cleanedTokens[i] = t.copy(isTarget = true)
                }
            }
        }

        return cleanedTokens
    }

    fun buildRubyVocab(vocab: String, furiganaOrKana: String): String {
        val cleanVocab = vocab.trim()
        val cleanFuri = furiganaOrKana.trim()
        if (cleanFuri.isBlank() || cleanVocab.isBlank()) return cleanVocab
        if (cleanFuri.contains("[") && cleanFuri.contains("]")) return cleanFuri
        if (cleanFuri.contains("<ruby>")) return cleanFuri
        if (cleanVocab == cleanFuri) return cleanVocab
        if (cleanVocab.all { !isKanji(it) }) return cleanVocab

        // Strip common prefix kana (e.g. お茶 / おちゃ -> prefix "お", ご飯 / ごはん -> prefix "ご")
        var commonPrefixLen = 0
        while (commonPrefixLen < cleanVocab.length &&
            commonPrefixLen < cleanFuri.length &&
            cleanVocab[commonPrefixLen] == cleanFuri[commonPrefixLen]
        ) {
            commonPrefixLen++
        }

        val vocabAfterPrefix = cleanVocab.substring(commonPrefixLen)
        val furiAfterPrefix = cleanFuri.substring(commonPrefixLen)

        // Strip common suffix kana (e.g. 食べる / たべる -> suffix "る", 思い出す / おもいだす -> suffix "す")
        var commonSuffixLen = 0
        while (commonSuffixLen < vocabAfterPrefix.length &&
            commonSuffixLen < furiAfterPrefix.length &&
            vocabAfterPrefix[vocabAfterPrefix.length - 1 - commonSuffixLen] == furiAfterPrefix[furiAfterPrefix.length - 1 - commonSuffixLen]
        ) {
            commonSuffixLen++
        }

        val prefix = cleanVocab.substring(0, commonPrefixLen)
        val kanjiPart = vocabAfterPrefix.substring(0, vocabAfterPrefix.length - commonSuffixLen)
        val kanaPart = furiAfterPrefix.substring(0, furiAfterPrefix.length - commonSuffixLen)
        val suffix = vocabAfterPrefix.substring(vocabAfterPrefix.length - commonSuffixLen)

        // Check if there is internal okurigana splitting (e.g. 思い出 / おもいだ -> 思[おも]い 出[だ])
        if (kanjiPart.isNotEmpty() && kanaPart.isNotEmpty()) {
            val internalKanaIdx = kanjiPart.indexOfFirst { !isKanji(it) }
            if (internalKanaIdx != -1) {
                val midKanaChar = kanjiPart[internalKanaIdx]
                val midKanaInReading = kanaPart.indexOf(midKanaChar)
                if (midKanaInReading != -1) {
                    val kPart1 = kanjiPart.substring(0, internalKanaIdx)
                    val rPart1 = kanaPart.substring(0, midKanaInReading)
                    val kPart2 = kanjiPart.substring(internalKanaIdx + 1)
                    val rPart2 = kanaPart.substring(midKanaInReading + 1)
                    if (kPart1.isNotEmpty() && rPart1.isNotEmpty() && kPart2.isNotEmpty() && rPart2.isNotEmpty()) {
                        val prefixPart = if (prefix.isNotEmpty()) "$prefix " else ""
                        return "$prefixPart$kPart1[$rPart1]$midKanaChar $kPart2[$rPart2]$suffix"
                    }
                }
            }
            val prefixPart = if (prefix.isNotEmpty()) "$prefix " else ""
            return "$prefixPart$kanjiPart[$kanaPart]$suffix"
        }

        return "$cleanVocab[$cleanFuri]"
    }

    private fun splitPlainBase(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val units = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (Character.isWhitespace(c)) {
                i++
            } else if (c.code in 0x3000..0x9FFF || c.code in 0xFF00..0xFFEF || Character.isSurrogate(c)) {
                if (Character.isHighSurrogate(c) && i + 1 < text.length && Character.isLowSurrogate(text[i + 1])) {
                    units.add(text.substring(i, i + 2))
                    i += 2
                } else {
                    units.add(c.toString())
                    i++
                }
            } else {
                val start = i
                while (i < text.length && !Character.isWhitespace(text[i]) &&
                    !(text[i].code in 0x3000..0x9FFF || text[i].code in 0xFF00..0xFFEF) &&
                    !Character.isSurrogate(text[i])
                ) {
                    i++
                }
                units.add(text.substring(start, i))
            }
        }
        return units
    }

    fun renderRubyBitmapPx(
        context: Context,
        rawText: String,
        highlightWord: String = "",
        baseTextSizePx: Float = 42f,
        rubyTextSizePx: Float = 20f,
        baseTextColor: Int = Color.WHITE,
        rubyTextColor: Int = Color.parseColor("#9AA0A6"),
        highlightColor: Int = Color.parseColor("#8AB4F8"),
        maxWidthPx: Int = 0,
        isCentered: Boolean = true,
        isBold: Boolean = false
    ): Bitmap? {
        val tokens = parseRubyTokens(rawText, highlightWord)
        if (tokens.isEmpty()) return null

        val basePaint = TextPaint().apply {
            isAntiAlias = true
            textSize = baseTextSizePx
            color = baseTextColor
            typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }

        val rubyPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = rubyTextSizePx
            color = rubyTextColor
            typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }

        val targetBasePaint = TextPaint(basePaint).apply {
            color = highlightColor
            isFakeBoldText = true
        }

        val targetRubyPaint = TextPaint(rubyPaint).apply {
            color = highlightColor
            isFakeBoldText = true
        }

        val baseMetrics = basePaint.fontMetrics
        val baseHeight = baseMetrics.descent - baseMetrics.ascent

        val rubyMetrics = rubyPaint.fontMetrics
        val rubyHeight = rubyMetrics.descent - rubyMetrics.ascent

        val hasAnyRuby = tokens.any { !it.ruby.isNullOrBlank() }
        val effectiveRubyHeight = if (hasAnyRuby) rubyHeight else 0f
        val rubyBaseGap = if (hasAnyRuby) 2.5f else 0f
        val lineSpacing = 5f
        val totalLineHeight = effectiveRubyHeight + rubyBaseGap + baseHeight + lineSpacing

        val layoutTokens = mutableListOf<RubyToken>()
        for (token in tokens) {
            if (token.ruby.isNullOrBlank()) {
                val units = splitPlainBase(token.base)
                for (unit in units) {
                    layoutTokens.add(RubyToken(base = unit, ruby = null, isTarget = token.isTarget))
                }
            } else {
                layoutTokens.add(token)
            }
        }

        val measuredTokens = layoutTokens.map { token ->
            val bPaint = if (token.isTarget) targetBasePaint else basePaint
            val rPaint = if (token.isTarget) targetRubyPaint else rubyPaint
            val wBase = bPaint.measureText(token.base)
            val wRuby = if (!token.ruby.isNullOrBlank()) rPaint.measureText(token.ruby) else 0f
            val totalWidth = max(wBase, wRuby)
            MeasuredToken(token, wBase, wRuby, totalWidth)
        }

        val effectiveMaxWidth = if (maxWidthPx > 0) {
            maxWidthPx
        } else {
            val screenW = try {
                context.resources.displayMetrics.widthPixels
            } catch (t: Throwable) {
                1080
            }
            min((screenW * 0.85f).toInt(), 600)
        }

        val lines = mutableListOf<MutableList<MeasuredToken>>()
        var currentLine = mutableListOf<MeasuredToken>()
        var currentLineWidth = 0f

        for (mToken in measuredTokens) {
            if (currentLineWidth + mToken.totalWidth > effectiveMaxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = mutableListOf()
                currentLineWidth = 0f
            }
            if (mToken.totalWidth > effectiveMaxWidth && mToken.token.ruby.isNullOrBlank() && mToken.token.base.length > 1) {
                for (char in mToken.token.base) {
                    val charStr = char.toString()
                    val bPaint = if (mToken.token.isTarget) targetBasePaint else basePaint
                    val wBase = bPaint.measureText(charStr)
                    val charMToken = MeasuredToken(
                        RubyToken(base = charStr, ruby = null, isTarget = mToken.token.isTarget),
                        wBase = wBase,
                        wRuby = 0f,
                        totalWidth = wBase
                    )
                    if (currentLineWidth + charMToken.totalWidth > effectiveMaxWidth && currentLine.isNotEmpty()) {
                        lines.add(currentLine)
                        currentLine = mutableListOf(charMToken)
                        currentLineWidth = charMToken.totalWidth
                    } else {
                        currentLine.add(charMToken)
                        currentLineWidth += charMToken.totalWidth
                    }
                }
            } else {
                currentLine.add(mToken)
                currentLineWidth += mToken.totalWidth
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        val maxLineWidth = lines.maxOfOrNull { line ->
            line.sumOf { it.totalWidth.toDouble() }.toFloat()
        } ?: 0f

        val bitmapWidth = max(1, min(effectiveMaxWidth, maxLineWidth.toInt() + 16))
        val rawHeight = (lines.size * totalLineHeight).toInt()
        val bitmapHeight = max(1, min(rawHeight, 400))

        val bitmap = try {
            Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        } catch (t: Throwable) {
            return null
        }
        val canvas = Canvas(bitmap)

        var currentY = 0f
        for (line in lines) {
            if (currentY + totalLineHeight > bitmapHeight + 10f) break
            val lineWidth = line.sumOf { it.totalWidth.toDouble() }.toFloat()
            var currentX = if (isCentered) {
                max(0f, (bitmapWidth - lineWidth) / 2f)
            } else {
                4f
            }

            val rubyBaseline = currentY - rubyMetrics.ascent
            val baseBaseline = currentY + effectiveRubyHeight + rubyBaseGap - baseMetrics.ascent

            for (mToken in line) {
                val t = mToken.token
                val bPaint = if (t.isTarget) targetBasePaint else basePaint
                val rPaint = if (t.isTarget) targetRubyPaint else rubyPaint

                val baseX = currentX + (mToken.totalWidth - mToken.wBase) / 2f
                canvas.drawText(t.base, baseX, baseBaseline, bPaint)

                if (!t.ruby.isNullOrBlank() && hasAnyRuby) {
                    val rubyX = currentX + (mToken.totalWidth - mToken.wRuby) / 2f
                    canvas.drawText(t.ruby, rubyX, rubyBaseline, rPaint)
                }

                currentX += mToken.totalWidth
            }
            currentY += totalLineHeight
        }

        return bitmap
    }

    fun renderRubyBitmap(
        context: Context,
        rawText: String,
        highlightWord: String = "",
        baseTextSizeSp: Float = 15f,
        rubyTextSizeSp: Float = 8.5f,
        baseTextColor: Int = Color.WHITE,
        rubyTextColor: Int = Color.parseColor("#9AA0A6"),
        highlightColor: Int = Color.parseColor("#8AB4F8"),
        maxWidthPx: Int = 0,
        isCentered: Boolean = true
    ): Bitmap? {
        val density = context.resources.displayMetrics.density
        return renderRubyBitmapPx(
            context = context,
            rawText = rawText,
            highlightWord = highlightWord,
            baseTextSizePx = baseTextSizeSp * density,
            rubyTextSizePx = rubyTextSizeSp * density,
            baseTextColor = baseTextColor,
            rubyTextColor = rubyTextColor,
            highlightColor = highlightColor,
            maxWidthPx = maxWidthPx,
            isCentered = isCentered,
            isBold = false
        )
    }
}
