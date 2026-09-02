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
        var text = input.replace(Regex("<ruby>([^<]+)<rt>([^<]+)</rt></ruby>"), "$1[$2]")
        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
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

            val ruby = normalized.substring(bracketOpen + 1, bracketClose).trim()
            val beforeBracket = normalized.substring(cursor, bracketOpen)

            val lastSpaceIdx = beforeBracket.lastIndexOf(' ')
            val baseStart: Int
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
            }

            val base = beforeBracket.substring(baseStart).trim()
            if (base.isNotEmpty() && ruby.isNotEmpty()) {
                tokens.add(RubyToken(base = base, ruby = ruby, isTarget = false))
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

        var commonSuffixLen = 0
        while (commonSuffixLen < cleanVocab.length &&
            commonSuffixLen < cleanFuri.length &&
            cleanVocab[cleanVocab.length - 1 - commonSuffixLen] == cleanFuri[cleanFuri.length - 1 - commonSuffixLen]
        ) {
            commonSuffixLen++
        }

        if (commonSuffixLen > 0) {
            val kanjiPart = cleanVocab.substring(0, cleanVocab.length - commonSuffixLen)
            val kanaPart = cleanFuri.substring(0, cleanFuri.length - commonSuffixLen)
            val suffix = cleanVocab.substring(cleanVocab.length - commonSuffixLen)
            if (kanjiPart.isNotEmpty() && kanaPart.isNotEmpty()) {
                return "$kanjiPart[$kanaPart]$suffix"
            }
        }

        return "$cleanVocab[$cleanFuri]"
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

        val measuredTokens = tokens.map { token ->
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
            (context.resources.displayMetrics.widthPixels * 0.85f).toInt()
        }

        val lines = mutableListOf<MutableList<MeasuredToken>>()
        var currentLine = mutableListOf<MeasuredToken>()
        var currentLineWidth = 0f

        for (mToken in measuredTokens) {
            if (currentLineWidth + mToken.totalWidth > effectiveMaxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = mutableListOf(mToken)
                currentLineWidth = mToken.totalWidth
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
        val bitmapHeight = max(1, (lines.size * totalLineHeight).toInt())

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        var currentY = 0f
        for (line in lines) {
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
