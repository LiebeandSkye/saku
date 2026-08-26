package com.saku.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider as createColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = SakuPreferences(context)
        val activeCard = prefs.getActiveCard()

        provideContent {
            GlanceTheme {
                SakuWidgetContent(card = activeCard)
            }
        }
    }

    private fun fixedColor(color: Color): ColorProvider {
        return createColorProvider(day = color, night = color)
    }

    @Composable
    fun SakuWidgetContent(card: CardModel) {
        val textColorWhite = fixedColor(Color.White)
        val textColorSubtle = fixedColor(Color(0xFFAAAAAA))
        val textColorMuted = fixedColor(Color(0xFF777777))
        val cardBgColor = fixedColor(Color(0xFF121212))
        val kanjiBoxBg = fixedColor(Color(0xFF1E1E1E))

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(cardBgColor)
                .cornerRadius(16.dp)
                .padding(14.dp)
                .clickable(actionRunCallback<SakuNextCardAction>())
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Content: Kanji on Left, Details on Right
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Kanji Box
                    Box(
                        modifier = GlanceModifier
                            .size(62.dp)
                            .background(kanjiBoxBg)
                            .cornerRadius(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = card.kanji,
                            style = TextStyle(
                                color = textColorWhite,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(14.dp))

                    // Reading, Meaning, Example
                    Column(
                        modifier = GlanceModifier.defaultWeight()
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
                            maxLines = 1
                        )

                        if (card.example.isNotEmpty()) {
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = card.example,
                                style = TextStyle(
                                    color = textColorMuted,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(10.dp))

                // Bottom Minimal Action Buttons (Again, Hard, Good, Easy)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EaseButton(
                        label = "Again",
                        color = fixedColor(Color(0xFFFF6B6B)),
                        ease = ReviewEase.AGAIN
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    EaseButton(
                        label = "Hard",
                        color = fixedColor(Color(0xFFFFA94D)),
                        ease = ReviewEase.HARD
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    EaseButton(
                        label = "Good",
                        color = fixedColor(Color(0xFF51CF66)),
                        ease = ReviewEase.GOOD
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    EaseButton(
                        label = "Easy",
                        color = fixedColor(Color(0xFF74C0FC)),
                        ease = ReviewEase.EASY
                    )
                }
            }
        }
    }

    @Composable
    private fun EaseButton(label: String, color: ColorProvider, ease: ReviewEase) {
        val buttonBg = fixedColor(Color(0xFF222222))
        Box(
            modifier = GlanceModifier
                .background(buttonBg)
                .cornerRadius(6.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
