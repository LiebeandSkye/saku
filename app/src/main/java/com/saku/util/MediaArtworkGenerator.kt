package com.saku.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.saku.R
import com.saku.data.CardInfo
import com.saku.data.PreferencesManager
import java.io.File
import kotlin.math.max
import kotlin.math.min

object MediaArtworkGenerator {

    private const val ARTWORK_SIZE = 800

    fun generateArtwork(
        context: Context,
        card: CardInfo?,
        stats: Triple<Int, Int, Int>,
        isRevealed: Boolean,
        imageBitmap: Bitmap?,
        showBottomControls: Boolean = true,
        targetWidth: Int = ARTWORK_SIZE,
        targetHeight: Int = ARTWORK_SIZE,
        fontScaleMultiplier: Float = 1.0f
    ): Bitmap {
        val prefs = PreferencesManager(context)
        val width = if (targetWidth > 0) targetWidth else ARTWORK_SIZE
        val height = if (targetHeight > 0) targetHeight else ARTWORK_SIZE
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw full widget background picture (blurred & dimmed if set, or clean transparent/dark)
        drawBackground(context, prefs, canvas, width, height)

        val baseDensity = (width.toFloat() / 360f).coerceIn(0.9f, 4.0f)
        val fontScale = fontScaleMultiplier.coerceIn(0.7f, 2.5f)
        val sp = baseDensity * fontScale

        val topInset = max(34f * baseDensity, height * 0.08f)
        val statsY = topInset + 10f * baseDensity
        val sideInset = max(20f * baseDensity, width * 0.06f)

        val deckName = card?.deckName?.ifEmpty { "Saku" } ?: "All Caught Up"
        val deckTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 11.5f * sp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
        }

        canvas.drawText(
            deckName,
            width - sideInset,
            statsY,
            deckTextPaint
        )

        val cardType = card?.cardType ?: 0
        val newC = stats.first
        val learnC = stats.second
        val revC = stats.third

        val newText = "$newC"
        val learnText = "$learnC"
        val revText = "$revC"
        val dotText = " · "

        val newPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8AB4F8")
            textSize = 11.5f * sp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
        }
        val dotPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 10.5f * sp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
        }
        val learnPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F28B82")
            textSize = 11.5f * sp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
        }
        val revPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#81C995")
            textSize = 11.5f * sp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
        }

        val wNew = newPaint.measureText(newText)
        val wDot1 = dotPaint.measureText(dotText)
        val wLearn = learnPaint.measureText(learnText)
        val wDot2 = dotPaint.measureText(dotText)
        val wRev = revPaint.measureText(revText)

        var curStatsX = sideInset

        canvas.drawText(newText, curStatsX, statsY, newPaint)
        if (cardType == 0 && card != null) {
            val underlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8AB4F8")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(curStatsX, statsY + 4f * baseDensity, curStatsX + wNew, statsY + 6.5f * baseDensity),
                2f,
                2f,
                underlinePaint
            )
        }
        curStatsX += wNew

        canvas.drawText(dotText, curStatsX, statsY, dotPaint)
        curStatsX += wDot1

        canvas.drawText(learnText, curStatsX, statsY, learnPaint)
        if (cardType == 1 && card != null) {
            val underlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F28B82")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(curStatsX, statsY + 4f * baseDensity, curStatsX + wLearn, statsY + 6.5f * baseDensity),
                2f,
                2f,
                underlinePaint
            )
        }
        curStatsX += wLearn

        canvas.drawText(dotText, curStatsX, statsY, dotPaint)
        curStatsX += wDot2

        canvas.drawText(revText, curStatsX, statsY, revPaint)
        if (cardType == 2 && card != null) {
            val underlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#81C995")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(curStatsX, statsY + 4f * baseDensity, curStatsX + wRev, statsY + 6.5f * baseDensity),
                2f,
                2f,
                underlinePaint
            )
        }

        if (card == null) {
            drawCongratulations(canvas, width, height, showBottomControls, sp, baseDensity)
            return bitmap
        }

        val kanjiText = card.kanji.ifEmpty { card.question }
        val kanjiFurigana = card.kanjiFurigana
        val kanjiMeaning = card.kanjiMeaning.ifEmpty { card.answer }
        val cleanMeaning = kanjiMeaning.replace(Regex("<[^>]*>"), "").trim()
        val rawSentence = card.sentence.replace(Regex("<[^>]*>"), "").trim()
        val sentenceFurigana = card.sentenceFurigana
        val sentenceMeaning = card.sentenceMeaning
        val cleanSentenceMeaning = sentenceMeaning.replace(Regex("<[^>]*>"), "").trim()

        val vocabRubyText = if (isRevealed && kanjiFurigana.isNotBlank()) {
            RubyTextRenderer.buildRubyVocab(kanjiText, kanjiFurigana)
        } else {
            kanjiText
        }

        val vocabRubyBmp = RubyTextRenderer.renderRubyBitmapPx(
            context = context,
            rawText = vocabRubyText,
            baseTextSizePx = if (kanjiText.length > 5) 22f * sp else 28f * sp,
            rubyTextSizePx = if (isRevealed) 11f * sp else 0f,
            baseTextColor = Color.WHITE,
            rubyTextColor = Color.parseColor("#7EB6FF"),
            highlightColor = Color.WHITE,
            maxWidthPx = (width * 0.88f).toInt(),
            isCentered = true,
            isBold = true
        )
        val vocabH = vocabRubyBmp?.height?.toFloat() ?: (28f * sp)

        val meaningLayout = if (isRevealed && cleanMeaning.isNotBlank()) {
            val meaningPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F1F5F9")
                textSize = (if (cleanMeaning.length > 30) 11.5f else 13.5f) * sp
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
            }
            StaticLayout.Builder.obtain(
                cleanMeaning,
                0,
                cleanMeaning.length,
                meaningPaint,
                (width * 0.84f).toInt()
            )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(2)
            .build()
        } else {
            null
        }
        val meaningH = meaningLayout?.height?.toFloat() ?: 0f

        val dividerGapTop = 8f * baseDensity
        val dividerGapBottom = 10f * baseDensity
        val dividerH = dividerGapTop + 2f + dividerGapBottom

        val sentenceRubyBmp = if (isRevealed && sentenceFurigana.isNotBlank()) {
            RubyTextRenderer.renderRubyBitmapPx(
                context = context,
                rawText = sentenceFurigana,
                highlightWord = kanjiText,
                baseTextSizePx = 14.5f * sp,
                rubyTextSizePx = 7f * sp,
                baseTextColor = Color.WHITE,
                rubyTextColor = Color.parseColor("#90CAF9"),
                highlightColor = Color.parseColor("#8AB4F8"),
                maxWidthPx = (width * 0.86f).toInt(),
                isCentered = true,
                isBold = false
            )
        } else {
            null
        }

        val sentenceLayout = if (sentenceRubyBmp == null && rawSentence.isNotBlank()) {
            val sentencePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 14.5f * sp
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
            }
            StaticLayout.Builder.obtain(
                rawSentence,
                0,
                rawSentence.length,
                sentencePaint,
                (width * 0.84f).toInt()
            )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(3)
            .build()
        } else {
            null
        }
        val sentenceH = sentenceRubyBmp?.height?.toFloat()
            ?: sentenceLayout?.height?.toFloat()
            ?: 0f

        val sentMeaningLayout = if (isRevealed && cleanSentenceMeaning.isNotBlank()) {
            val sentMeaningPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                textSize = 11f * sp
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
            }
            StaticLayout.Builder.obtain(
                cleanSentenceMeaning,
                0,
                cleanSentenceMeaning.length,
                sentMeaningPaint,
                (width * 0.84f).toInt()
            )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(3)
            .build()
        } else {
            null
        }
        val sentMeaningH = sentMeaningLayout?.height?.toFloat() ?: 0f

        val maxImgWidth = (width * 0.38f).toInt()
        val maxImgHeight = min(height * 0.20f, 100f * baseDensity)
        val imgDrawWidth: Float
        val imgDrawHeight: Float
        if (imageBitmap != null) {
            val imgAspect = imageBitmap.width.toFloat() / max(1, imageBitmap.height).toFloat()
            if (imgAspect > 1f) {
                imgDrawWidth = min(maxImgWidth.toFloat(), imageBitmap.width.toFloat())
                imgDrawHeight = imgDrawWidth / imgAspect
            } else {
                imgDrawHeight = min(maxImgHeight, imageBitmap.height.toFloat())
                imgDrawWidth = imgDrawHeight * imgAspect
            }
        } else {
            imgDrawWidth = 0f
            imgDrawHeight = 0f
        }

        val contentTop = statsY + 14f * baseDensity
        // When showBottomControls is false (used by widget), leave room for the bottom floating buttons
        val contentBottom = if (showBottomControls) (height - 30f * baseDensity) else (height - 52f * baseDensity)
        val availableH = max(60f * baseDensity, contentBottom - contentTop)

        var totalContentH = vocabH
        if (meaningH > 0f) totalContentH += 6f * baseDensity + meaningH
        totalContentH += dividerH
        totalContentH += sentenceH
        if (sentMeaningH > 0f) totalContentH += 6f * baseDensity + sentMeaningH
        if (imgDrawHeight > 0f) totalContentH += 8f * baseDensity + imgDrawHeight

        val shouldDrawSentMeaning = sentMeaningH > 0f && (totalContentH <= availableH || availableH > 180f * baseDensity)
        val shouldDrawImage = imageBitmap != null && imgDrawHeight > 0f && (totalContentH <= availableH || availableH > 220f * baseDensity)

        var effectiveContentH = vocabH
        if (meaningH > 0f) effectiveContentH += 6f * baseDensity + meaningH
        effectiveContentH += dividerH
        effectiveContentH += sentenceH
        if (shouldDrawSentMeaning) effectiveContentH += 6f * baseDensity + sentMeaningH
        if (shouldDrawImage) effectiveContentH += 8f * baseDensity + imgDrawHeight

        val startY = contentTop + max(0f, (availableH - effectiveContentH) / 2f)
        var curY = startY

        if (vocabRubyBmp != null) {
            val vocabX = (width - vocabRubyBmp.width) / 2f
            canvas.drawBitmap(vocabRubyBmp, vocabX, curY, null)
            curY += vocabH
        }

        if (meaningLayout != null) {
            curY += 6f * baseDensity
            canvas.save()
            canvas.translate(width * 0.08f, curY)
            meaningLayout.draw(canvas)
            canvas.restore()
            curY += meaningH
        }

        curY += dividerGapTop
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4DFFFFFF")
            strokeWidth = 2f
        }
        canvas.drawLine(
            width * 0.15f,
            curY,
            width * 0.85f,
            curY,
            dividerPaint
        )
        curY += 2f + dividerGapBottom

        if (sentenceRubyBmp != null) {
            val sentenceX = (width - sentenceRubyBmp.width) / 2f
            canvas.drawBitmap(sentenceRubyBmp, sentenceX, curY, null)
            curY += sentenceH
        } else if (sentenceLayout != null) {
            canvas.save()
            canvas.translate(width * 0.08f, curY)
            sentenceLayout.draw(canvas)
            canvas.restore()
            curY += sentenceH
        }

        if (shouldDrawSentMeaning) {
            curY += 6f * baseDensity
            canvas.save()
            canvas.translate(width * 0.08f, curY)
            sentMeaningLayout?.draw(canvas)
            canvas.restore()
            curY += sentMeaningH
        }

        if (shouldDrawImage && imageBitmap != null) {
            curY += 8f * baseDensity
            val imgLeft = (width - imgDrawWidth) / 2f
            val dstRect = RectF(imgLeft, curY, imgLeft + imgDrawWidth, curY + imgDrawHeight)
            val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.save()
            val clipPath = android.graphics.Path().apply {
                addRoundRect(dstRect, 10f * baseDensity, 10f * baseDensity, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(imageBitmap, null, dstRect, imgPaint)
            canvas.restore()
        }

        if (showBottomControls) {
            val hintText = if (!isRevealed) "|◀ Again   •   ▶ Reveal   •   ▶| Good" else "|◀ Again   •   ❚❚ Hide   •   ▶| Good"
            val hintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = 9.5f * sp
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
            }
            canvas.drawText(
                hintText,
                width / 2f,
                height - 12f * baseDensity,
                hintPaint
            )
        }

        return bitmap
    }

    private fun drawCongratulations(
        canvas: Canvas,
        width: Int,
        height: Int,
        showBottomControls: Boolean = true,
        sp: Float = 2.2f,
        baseDensity: Float = 2.2f
    ) {
        val congratsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f * sp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
        }
        canvas.drawText(
            "お疲れ様でした！",
            width / 2f,
            height * 0.35f,
            congratsPaint
        )

        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            textSize = 13f * sp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
        }
        canvas.drawText(
            "All reviews complete for today! 🎉",
            width / 2f,
            height * 0.44f,
            subPaint
        )

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4DFFFFFF")
            strokeWidth = 2f
        }
        canvas.drawLine(
            width * 0.15f,
            height * 0.50f,
            width * 0.85f,
            height * 0.50f,
            dividerPaint
        )

        val cheerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 12f * sp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
        }
        canvas.drawText(
            "また明日頑張りましょう！",
            width / 2f,
            height * 0.58f,
            cheerPaint
        )

        val tomorrowPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 10.5f * sp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
        }
        canvas.drawText(
            "Come back tomorrow for your next cards.",
            width / 2f,
            height * 0.65f,
            tomorrowPaint
        )

        if (showBottomControls) {
            val hintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8AB4F8")
                textSize = 9.5f * sp
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(4f * baseDensity, 0f, 1.5f * baseDensity, Color.parseColor("#99000000"))
            }
            canvas.drawText(
                "• Tap Open Anki to Study Ahead •",
                width / 2f,
                height - 12f * baseDensity,
                hintPaint
            )
        }
    }

    private fun drawBackground(context: Context, prefs: PreferencesManager, canvas: Canvas, width: Int, height: Int) {
        val bgType = prefs.backgroundType
        val radius = prefs.blurRadius.coerceAtLeast(0)
        val dimAlpha = (prefs.dimOpacity * 255).toInt().coerceIn(0, 255)
        val artworkAlpha = (prefs.artworkOpacity * 255).toInt().coerceIn(0, 255)
        val dstRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        val bitmapToDraw: Bitmap? = when (bgType) {
            "anki_lock", "default" -> {
                try {
                    val original = BitmapFactory.decodeResource(context.resources, R.drawable.anki_lock)
                    if (original != null && radius > 0) {
                        val blurred = ImageBlurUtil.fastBlur(original, 0.25f, radius)
                        if (blurred != original) original.recycle()
                        blurred
                    } else original
                } catch (e: Throwable) {
                    null
                }
            }
            "custom" -> {
                val uriStr = prefs.customImageUri
                if (!uriStr.isNullOrBlank()) {
                    loadCustomBackgroundBitmap(context, uriStr, width, height, radius)
                } else null
            }
            "dark_blur", "sunset" -> {
                try {
                    val preset = ImageBlurUtil.createPresetBackground(bgType, width, height)
                    if (radius > 0) {
                        val blurred = ImageBlurUtil.fastBlur(preset, 0.25f, radius)
                        if (blurred != preset) {
                            preset.recycle()
                        }
                        blurred
                    } else {
                        preset
                    }
                } catch (t: Throwable) {
                    null
                }
            }
            else -> null // "transparent" / "none" has no background picture
        }

        if (bitmapToDraw != null) {
            drawCroppedBitmapWithOverlay(canvas, bitmapToDraw, artworkAlpha, dimAlpha, dstRect)
            // Recycle temporary generated bitmaps to prevent memory leaks
            if (bgType == "dark_blur" || bgType == "sunset" || bgType == "custom" || bgType == "anki_lock") {
                try {
                    bitmapToDraw.recycle()
                } catch (e: Exception) {}
            }
        } else if (bgType == "dark_blur" || bgType == "sunset") {
            drawPresetGradientDirectly(canvas, bgType, width, height, dimAlpha)
        }
    }

    private fun loadCustomBackgroundBitmap(
        context: Context,
        uriOrPath: String,
        targetWidth: Int,
        targetHeight: Int,
        blurRadius: Int
    ): Bitmap? {
        return try {
            val maxTargetDim = max(targetWidth, targetHeight).coerceAtLeast(300)

            val openStream: () -> java.io.InputStream? = {
                if (uriOrPath.startsWith("/")) {
                    val f = File(uriOrPath)
                    if (f.exists()) f.inputStream() else null
                } else {
                    val uri = Uri.parse(uriOrPath)
                    if (uri.scheme == "file") {
                        val f = File(uri.path ?: "")
                        if (f.exists()) f.inputStream() else null
                    } else {
                        context.contentResolver.openInputStream(uri)
                    }
                }
            }

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: return null

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

            var inSampleSize = 1
            while (boundsOptions.outWidth / (inSampleSize * 2) >= maxTargetDim &&
                boundsOptions.outHeight / (inSampleSize * 2) >= maxTargetDim
            ) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = openStream()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            if (blurRadius > 0) {
                val blurred = ImageBlurUtil.fastBlur(decoded, 0.25f, blurRadius)
                if (blurred != decoded) {
                    decoded.recycle()
                }
                blurred
            } else {
                decoded
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    private fun drawPresetGradientDirectly(canvas: Canvas, type: String, width: Int, height: Int, dimAlpha: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)

        when (type) {
            "dark_blur" -> {
                paint.shader = LinearGradient(
                    0f, 0f, w, h,
                    intArrayOf(
                        Color.parseColor("#1A1F2C"),
                        Color.parseColor("#0F172A"),
                        Color.parseColor("#090D16")
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            "sunset" -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(
                        Color.parseColor("#3B1D54"),
                        Color.parseColor("#2C1635"),
                        Color.parseColor("#150D24")
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            else -> {
                paint.color = Color.parseColor("#0F172A")
            }
        }
        canvas.drawRect(0f, 0f, w, h, paint)

        if (dimAlpha > 0) {
            val dimPaint = Paint().apply {
                color = Color.argb(dimAlpha, 0, 0, 0)
            }
            canvas.drawRect(0f, 0f, w, h, dimPaint)
        }
    }

    private fun drawCroppedBitmapWithOverlay(
        canvas: Canvas,
        bitmap: Bitmap,
        artworkAlpha: Int,
        dimAlpha: Int,
        dstRect: RectF
    ) {
        if (bitmap.isRecycled) return

        val bmpW = bitmap.width.toFloat()
        val bmpH = bitmap.height.toFloat()
        val targetW = dstRect.width()
        val targetH = dstRect.height()

        if (bmpW <= 0f || bmpH <= 0f || targetW <= 0f || targetH <= 0f) return

        val scale = max(targetW / bmpW, targetH / bmpH)
        val scaledW = bmpW * scale
        val scaledH = bmpH * scale
        val cropX = max(0f, (scaledW - targetW) / (2f * scale))
        val cropY = max(0f, (scaledH - targetH) / (2f * scale))
        val cropW = min(bmpW - cropX, targetW / scale)
        val cropH = min(bmpH - cropY, targetH / scale)

        val srcLeft = cropX.toInt().coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
        val srcTop = cropY.toInt().coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
        val srcRight = (cropX + cropW).toInt().coerceIn(srcLeft + 1, bitmap.width)
        val srcBottom = (cropY + cropH).toInt().coerceIn(srcTop + 1, bitmap.height)

        val srcRect = Rect(srcLeft, srcTop, srcRight, srcBottom)
        val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            alpha = artworkAlpha
        }
        canvas.drawBitmap(bitmap, srcRect, dstRect, bmpPaint)

        // Dark dimming overlay to guarantee high contrast and UI legibility
        if (dimAlpha > 0) {
            val dimPaint = Paint().apply {
                color = Color.argb(dimAlpha, 0, 0, 0)
            }
            canvas.drawRect(dstRect, dimPaint)
        }
    }
}
