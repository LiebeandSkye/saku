package com.saku.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider as createColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.saku.anki.AnkiDroidClient
import com.saku.anki.JapaneseFieldParser
import com.saku.data.CardModel
import com.saku.data.ReviewEase
import com.saku.data.SakuPreferences

class SakuGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = SakuPreferences(context)
        val ankiClient = AnkiDroidClient(context)
        var activeCard = prefs.getActiveCard()

        if (activeCard.cardId <= 0 && ankiClient.isAnkiDroidInstalled() && ankiClient.isPermissionGranted()) {
            try {
                val dueCards = ankiClient.getDueCards(prefs.selectedDeckId, limit = 10)
                val realCard = dueCards.firstOrNull { it.cardId > 0 }
                if (realCard != null) {
                    activeCard = realCard
                    prefs.saveActiveCard(activeCard)
                }
            } catch (e: Exception) {
                // Log and continue with saved card
            }
        }

        val isAnswerRevealed = prefs.isAnswerRevealed

        provideContent {
            GlanceTheme {
                SakuWidgetContent(card = activeCard, isAnswerRevealed = isAnswerRevealed)
            }
        }
    }

    private fun fixedColor(color: Color): ColorProvider {
        return createColorProvider(day = color, night = color)
    }

    @Composable
    fun SakuWidgetContent(card: CardModel, isAnswerRevealed: Boolean) {
        val size = LocalSize.current
        val isCompactHeight = size.height < 145.dp
        val isVeryCompact = size.height < 110.dp

        val textColorWhite = fixedColor(Color.White)
        val textColorSubtle = fixedColor(Color(0xFFCCCCCC))
        val textColorMuted = fixedColor(Color(0xFF888888))
        val textColorFurigana = fixedColor(Color(0xFF6CA0DC))
        val cardBgColor = fixedColor(Color(0xFF161616))
        val buttonBgColor = fixedColor(Color(0xFF262626))

        val countNewColor = fixedColor(Color(0xFF5C8AFF))
        val countLearnColor = fixedColor(Color(0xFFE06C75))
        val countReviewColor = fixedColor(Color(0xFF98C379))

        val kanjiFontSize = if (isVeryCompact) 26.sp else if (isCompactHeight) 32.sp else 38.sp
        val buttonHeight = if (isVeryCompact) 32.dp else if (isCompactHeight) 38.dp else 44.dp
        val buttonFontSize = if (isVeryCompact) 11.sp else 13.sp

        val newCountStr = if (card.newCount > 0) card.newCount.toString() else "15"
        val learnCountStr = if (card.learnCount > 0) card.learnCount.toString() else "17"
        val reviewCountStr = if (card.reviewCount > 0) card.reviewCount.toString() else "21"

        val frontSentence = card.exampleSentence.ifEmpty {
            card.example.substringBefore("•").trim().ifEmpty { card.kanji }
        }
        val furiganaWord = card.furigana.ifEmpty { card.kana }
        val sentenceTrans = card.exampleTranslation.ifEmpty {
            val after = card.example.substringAfter("•", "").trim()
            after.ifEmpty { card.meaning }
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(cardBgColor)
                .cornerRadius(18.dp)
                .padding(if (isCompactHeight) 10.dp else 14.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Status Bar: Review Counts (15, 17, 21)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = newCountStr,
                        style = TextStyle(
                            color = countNewColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = learnCountStr,
                        style = TextStyle(
                            color = countLearnColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = reviewCountStr,
                        style = TextStyle(
                            color = countReviewColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Middle Content Area (Clickable to toggle)
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .clickable(actionRunCallback<SakuToggleAnswerAction>()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isAnswerRevealed) {
                        // FRONT STATE (Question: Big Kanji + Example Sentence)
                        Text(
                            text = card.kanji,
                            style = TextStyle(
                                color = textColorWhite,
                                fontSize = kanjiFontSize,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = frontSentence,
                            style = TextStyle(
                                color = textColorWhite,
                                fontSize = if (isCompactHeight) 13.sp else 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = if (isCompactHeight) 2 else 3
                        )
                    } else {
                        // BACK STATE (Answer: Furigana + Kanji + Meaning + Aligned Sentence Furigana + Translation)
                        if (furiganaWord.isNotEmpty()) {
                            Text(
                                text = furiganaWord,
                                style = TextStyle(
                                    color = textColorSubtle,
                                    fontSize = if (isVeryCompact) 11.sp else 13.sp
                                )
                            )
                        }
                        Text(
                            text = card.kanji,
                            style = TextStyle(
                                color = textColorWhite,
                                fontSize = if (isVeryCompact) 24.sp else if (isCompactHeight) 28.sp else 34.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                        )
                        Text(
                            text = card.meaning,
                            style = TextStyle(
                                color = textColorWhite,
                                fontSize = if (isVeryCompact) 13.sp else 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )

                        val sentenceSource = card.exampleFurigana.ifEmpty { card.exampleSentence }
                        val segments = JapaneseFieldParser.parseFuriganaSegments(sentenceSource, targetWord = card.kanji)

                        if (segments.isNotEmpty() && !isVeryCompact) {
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                segments.forEach { seg ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (seg.reading.isNotEmpty()) {
                                            Text(
                                                text = seg.reading,
                                                style = TextStyle(
                                                    color = if (seg.isTarget) textColorFurigana else textColorSubtle,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        } else {
                                            Text(
                                                text = " ",
                                                style = TextStyle(fontSize = 9.sp)
                                            )
                                        }
                                        Text(
                                            text = seg.text,
                                            style = TextStyle(
                                                color = if (seg.isTarget) textColorFurigana else textColorWhite,
                                                fontSize = if (isCompactHeight) 12.sp else 14.sp,
                                                fontWeight = if (seg.isTarget) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }

                            if (sentenceTrans.isNotEmpty() && !isCompactHeight) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = sentenceTrans,
                                    style = TextStyle(
                                        color = textColorMuted,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Bottom Action Bar
                if (!isAnswerRevealed) {
                    // Front: Full-width "Show Answer" button
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                            .background(buttonBgColor)
                            .cornerRadius(10.dp)
                            .clickable(actionRunCallback<SakuToggleAnswerAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Show Answer",
                            style = TextStyle(
                                color = textColorWhite,
                                fontSize = if (isVeryCompact) 12.sp else 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    // Back: 3 Buttons (Again, Hard, Open Anki)
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(buttonHeight),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Again Button
                        EaseButton(
                            label = "Again",
                            color = fixedColor(Color(0xFFFF6B6B)),
                            fontSize = buttonFontSize,
                            ease = ReviewEase.AGAIN
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))

                        // Hard Button
                        EaseButton(
                            label = "Hard",
                            color = fixedColor(Color(0xFFFFA94D)),
                            fontSize = buttonFontSize,
                            ease = ReviewEase.HARD
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))

                        // Open Anki Button
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight()
                                .background(buttonBgColor)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .clickable(actionRunCallback<SakuOpenAnkiAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Open Anki",
                                style = TextStyle(
                                    color = fixedColor(Color(0xFF74C0FC)),
                                    fontSize = buttonFontSize,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RowScope.EaseButton(label: String, color: ColorProvider, fontSize: androidx.compose.ui.unit.TextUnit, ease: ReviewEase) {
        val buttonBg = fixedColor(Color(0xFF242424))
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .background(buttonBg)
                .cornerRadius(8.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .clickable(
                    actionRunCallback<SakuGradeCardAction>(
                        actionParametersOf(SakuGradeCardAction.EASE_PARAM to ease.value)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = color,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

