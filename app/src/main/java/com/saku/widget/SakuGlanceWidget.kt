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
import com.saku.data.CardModel
import com.saku.data.ReviewEase
import com.saku.data.SakuPreferences

class SakuGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = SakuPreferences(context)
        val activeCard = prefs.getActiveCard()
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
        val textColorSubtle = fixedColor(Color(0xFFB0B0B0))
        val textColorMuted = fixedColor(Color(0xFF888888))
        val cardBgColor = fixedColor(Color(0xFF121212))
        val kanjiBoxBg = fixedColor(Color(0xFF1E1E1E))
        val showAnswerBg = fixedColor(Color(0xFF262626))

        val kanjiSize = if (isVeryCompact) 48.dp else if (isCompactHeight) 58.dp else 66.dp
        val kanjiFontSize = if (isVeryCompact) 26.sp else if (isCompactHeight) 32.sp else 38.sp
        val buttonHeight = if (isVeryCompact) 34.dp else if (isCompactHeight) 40.dp else 46.dp
        val buttonFontSize = if (isVeryCompact) 11.sp else 13.sp

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(cardBgColor)
                .cornerRadius(18.dp)
                .padding(if (isCompactHeight) 10.dp else 14.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Content: Kanji on Left, Details on Right (Clickable to Toggle Answer)
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .clickable(actionRunCallback<SakuToggleAnswerAction>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Kanji Box
                    Box(
                        modifier = GlanceModifier
                            .size(kanjiSize)
                            .background(kanjiBoxBg)
                            .cornerRadius(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = card.kanji,
                            style = TextStyle(
                                color = textColorWhite,
                                fontSize = kanjiFontSize,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(12.dp))

                    if (!isAnswerRevealed) {
                        // FRONT STATE: Show Kanji with Example/Context Sentence (No reading or English yet)
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EXAMPLE / CONTEXT",
                                style = TextStyle(
                                    color = textColorMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(3.dp))
                            val frontExample = card.exampleSentence.ifEmpty {
                                card.example.substringBefore("•").trim().ifEmpty { card.kanji }
                            }
                            Text(
                                text = frontExample,
                                style = TextStyle(
                                    color = textColorWhite,
                                    fontSize = if (isCompactHeight) 14.sp else 16.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = if (isCompactHeight) 2 else 3
                            )
                        }
                    } else {
                        // BACK STATE: Show Reading (Kana + Romaji), Meaning, and Full Example with Translation
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Kana + Romaji
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = card.kana,
                                    style = TextStyle(
                                        color = textColorWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (card.romaji.isNotEmpty()) {
                                    Spacer(modifier = GlanceModifier.width(6.dp))
                                    Text(
                                        text = card.romaji,
                                        style = TextStyle(
                                            color = textColorSubtle,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = GlanceModifier.height(2.dp))

                            // English Definition
                            Text(
                                text = card.meaning,
                                style = TextStyle(
                                    color = textColorWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = if (isCompactHeight) 1 else 2
                            )

                            // Full Example Sentence with English Translation
                            if (card.example.isNotEmpty() && !isVeryCompact) {
                                Spacer(modifier = GlanceModifier.height(3.dp))
                                Text(
                                    text = card.example,
                                    style = TextStyle(
                                        color = textColorMuted,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = if (isCompactHeight) 1 else 2
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Bottom Action Bar: "Show Answer" (Front) OR Again / Hard / Good / Easy (Back)
                if (!isAnswerRevealed) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                            .background(showAnswerBg)
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
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(buttonHeight),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EaseButton(
                            label = "Again",
                            color = fixedColor(Color(0xFFFF6B6B)),
                            fontSize = buttonFontSize,
                            ease = ReviewEase.AGAIN
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        EaseButton(
                            label = "Hard",
                            color = fixedColor(Color(0xFFFFA94D)),
                            fontSize = buttonFontSize,
                            ease = ReviewEase.HARD
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        EaseButton(
                            label = "Good",
                            color = fixedColor(Color(0xFF51CF66)),
                            fontSize = buttonFontSize,
                            ease = ReviewEase.GOOD
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        EaseButton(
                            label = "Easy",
                            color = fixedColor(Color(0xFF74C0FC)),
                            fontSize = buttonFontSize,
                            ease = ReviewEase.EASY
                        )
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
                .padding(horizontal = 4.dp, vertical = 6.dp)
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
