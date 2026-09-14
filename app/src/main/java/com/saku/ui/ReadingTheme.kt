package com.saku.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 5 Curated Reading Screen Themes crafted with color theory:
 * - High readability and low ocular strain for long Japanese texts.
 * - Non-vibrant, natural Japanese paper and tea aesthetics.
 * - Options tailored for both bright daylight and pitch-dark night reading.
 */
enum class ReadingTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val kanji: String,
    val isDark: Boolean,
    val containerColor: Color,
    val borderColor: Color,
    val textPrimaryColor: Color,
    val textSecondaryColor: Color,
    val surfaceColor: Color,
    val surfaceBorderColor: Color,
    val wordHighlightBackground: Color,
    val wordHighlightTextColor: Color,
    val jlptBadgeBackground: Color,
    val jlptBadgeBorder: Color,
    val jlptBadgeText: Color,
    val iconTintColor: Color,
    val targetChipBackground: Color,
    val targetChipPresentBackground: Color,
    val targetChipBorder: Color,
    val targetChipPresentBorder: Color,
    val targetChipText: Color,
    val quizQuestionText: Color,
    val quizOptionBackground: Color,
    val quizOptionBorder: Color,
    val quizOptionSelectedBackground: Color,
    val quizOptionSelectedBorder: Color,
    val quizOptionCorrectBackground: Color,
    val quizOptionCorrectBorder: Color,
    val quizOptionWrongBackground: Color,
    val quizOptionWrongBorder: Color,
    val quizExplanationBackground: Color,
    val quizExplanationBorder: Color,
    val quizExplanationText: Color,
    val previewColor: Color
) {
    WARM_PARCHMENT(
        id = "warm_parchment",
        title = "Parchment",
        subtitle = "Warm Cream",
        kanji = "和紙",
        isDark = false,
        containerColor = Color(0xFFF5EEDB),
        borderColor = Color(0xFFE5DDC7),
        textPrimaryColor = Color(0xFF221C14),
        textSecondaryColor = Color(0xFF6C6353),
        surfaceColor = Color(0xFFEBE2CF),
        surfaceBorderColor = Color(0xFFDDD2BC),
        wordHighlightBackground = Color(0xFFF7D5B5),
        wordHighlightTextColor = Color(0xFF221C14),
        jlptBadgeBackground = Color(0xFFE8DECB),
        jlptBadgeBorder = Color(0xFFDDD2BC),
        jlptBadgeText = Color(0xFF5A5243),
        iconTintColor = Color(0xFF6C6453),
        targetChipBackground = Color(0xFFF0E8D7),
        targetChipPresentBackground = Color(0xFFDFD4BE),
        targetChipBorder = Color(0xFFE0D5C0),
        targetChipPresentBorder = Color(0xFFC9BC9F),
        targetChipText = Color(0xFF2C2820),
        quizQuestionText = Color(0xFF2C2820),
        quizOptionBackground = Color(0xFFF7F2E6),
        quizOptionBorder = Color(0xFFDDD2BC),
        quizOptionSelectedBackground = Color(0xFFDFD4BE),
        quizOptionSelectedBorder = Color(0xFF7E9F85),
        quizOptionCorrectBackground = Color(0xFFD8E8D5),
        quizOptionCorrectBorder = Color(0xFF7E9F85),
        quizOptionWrongBackground = Color(0xFFF5D6D9),
        quizOptionWrongBorder = Color(0xFFCF7B88),
        quizExplanationBackground = Color(0xFFF2EADC),
        quizExplanationBorder = Color(0xFFDDD2BC),
        quizExplanationText = Color(0xFF5A5243),
        previewColor = Color(0xFFF5EEDB)
    ),

    MATCHA_MIST(
        id = "matcha_mist",
        title = "Matcha",
        subtitle = "Calming Sage",
        kanji = "抹茶",
        isDark = false,
        containerColor = Color(0xFFEBF2EB),
        borderColor = Color(0xFFD2E0D2),
        textPrimaryColor = Color(0xFF18261C),
        textSecondaryColor = Color(0xFF506855),
        surfaceColor = Color(0xFFDDEADE),
        surfaceBorderColor = Color(0xFFC5D7C6),
        wordHighlightBackground = Color(0xFFCEE7D2),
        wordHighlightTextColor = Color(0xFF18261C),
        jlptBadgeBackground = Color(0xFFD8E6D9),
        jlptBadgeBorder = Color(0xFFC5D7C6),
        jlptBadgeText = Color(0xFF38523D),
        iconTintColor = Color(0xFF4A6850),
        targetChipBackground = Color(0xFFE2EEE3),
        targetChipPresentBackground = Color(0xFFD0E2D2),
        targetChipBorder = Color(0xFFC5D7C6),
        targetChipPresentBorder = Color(0xFFADC4AF),
        targetChipText = Color(0xFF18261C),
        quizQuestionText = Color(0xFF18261C),
        quizOptionBackground = Color(0xFFF2F7F2),
        quizOptionBorder = Color(0xFFC5D7C6),
        quizOptionSelectedBackground = Color(0xFFD0E2D2),
        quizOptionSelectedBorder = Color(0xFF528F5E),
        quizOptionCorrectBackground = Color(0xFFC8E6CC),
        quizOptionCorrectBorder = Color(0xFF4A7D54),
        quizOptionWrongBackground = Color(0xFFF5D6D9),
        quizOptionWrongBorder = Color(0xFFCF7B88),
        quizExplanationBackground = Color(0xFFE2EEE3),
        quizExplanationBorder = Color(0xFFC5D7C6),
        quizExplanationText = Color(0xFF38523D),
        previewColor = Color(0xFFEBF2EB)
    ),

    PAPER_PEARL(
        id = "paper_pearl",
        title = "Pearl",
        subtitle = "E-Ink Mist",
        kanji = "雲紙",
        isDark = false,
        containerColor = Color(0xFFF1F3F5),
        borderColor = Color(0xFFD9DFE5),
        textPrimaryColor = Color(0xFF1E2229),
        textSecondaryColor = Color(0xFF5B6472),
        surfaceColor = Color(0xFFE4E8EE),
        surfaceBorderColor = Color(0xFFD0D7E0),
        wordHighlightBackground = Color(0xFFD7E2F0),
        wordHighlightTextColor = Color(0xFF1E2229),
        jlptBadgeBackground = Color(0xFFDFE4EA),
        jlptBadgeBorder = Color(0xFFD0D7E0),
        jlptBadgeText = Color(0xFF3C4450),
        iconTintColor = Color(0xFF5B6472),
        targetChipBackground = Color(0xFFE9EDF2),
        targetChipPresentBackground = Color(0xFFD7DFE8),
        targetChipBorder = Color(0xFFD0D7E0),
        targetChipPresentBorder = Color(0xFFB8C2CE),
        targetChipText = Color(0xFF1E2229),
        quizQuestionText = Color(0xFF1E2229),
        quizOptionBackground = Color(0xFFF8F9FB),
        quizOptionBorder = Color(0xFFD0D7E0),
        quizOptionSelectedBackground = Color(0xFFD6DFEB),
        quizOptionSelectedBorder = Color(0xFF6B8BAA),
        quizOptionCorrectBackground = Color(0xFFD5E8D8),
        quizOptionCorrectBorder = Color(0xFF5A8E63),
        quizOptionWrongBackground = Color(0xFFF5D6D9),
        quizOptionWrongBorder = Color(0xFFCF7B88),
        quizExplanationBackground = Color(0xFFE7EBF0),
        quizExplanationBorder = Color(0xFFD0D7E0),
        quizExplanationText = Color(0xFF444C58),
        previewColor = Color(0xFFF1F3F5)
    ),

    TWILIGHT_SLATE(
        id = "twilight_slate",
        title = "Twilight",
        subtitle = "Indigo Slate",
        kanji = "藍墨",
        isDark = true,
        containerColor = Color(0xFF1B2028),
        borderColor = Color(0xFF2D3543),
        textPrimaryColor = Color(0xFFE2E6EF),
        textSecondaryColor = Color(0xFF909BB0),
        surfaceColor = Color(0xFF232A35),
        surfaceBorderColor = Color(0xFF353F50),
        wordHighlightBackground = Color(0xFF2A3F35),
        wordHighlightTextColor = Color(0xFF8EE3A9),
        jlptBadgeBackground = Color(0xFF262E3B),
        jlptBadgeBorder = Color(0xFF3B4658),
        jlptBadgeText = Color(0xFFBAC5D6),
        iconTintColor = Color(0xFF909BB0),
        targetChipBackground = Color(0xFF232B36),
        targetChipPresentBackground = Color(0xFF2D3746),
        targetChipBorder = Color(0xFF353F50),
        targetChipPresentBorder = Color(0xFF4B5A70),
        targetChipText = Color(0xFFE2E6EF),
        quizQuestionText = Color(0xFFE2E6EF),
        quizOptionBackground = Color(0xFF1F2530),
        quizOptionBorder = Color(0xFF353F50),
        quizOptionSelectedBackground = Color(0xFF283648),
        quizOptionSelectedBorder = Color(0xFF678BB3),
        quizOptionCorrectBackground = Color(0xFF1F3B29),
        quizOptionCorrectBorder = Color(0xFF52A36B),
        quizOptionWrongBackground = Color(0xFF3E2026),
        quizOptionWrongBorder = Color(0xFFC75A67),
        quizExplanationBackground = Color(0xFF222934),
        quizExplanationBorder = Color(0xFF353F50),
        quizExplanationText = Color(0xFFB0BAC8),
        previewColor = Color(0xFF1B2028)
    ),

    OBSIDIAN_NIGHT(
        id = "obsidian_night",
        title = "Obsidian",
        subtitle = "OLED Night",
        kanji = "漆黒",
        isDark = true,
        containerColor = Color(0xFF0F1115),
        borderColor = Color(0xFF242832),
        textPrimaryColor = Color(0xFFDDE1E8),
        textSecondaryColor = Color(0xFF7D8594),
        surfaceColor = Color(0xFF181C24),
        surfaceBorderColor = Color(0xFF282E3C),
        wordHighlightBackground = Color(0xFF1D3525),
        wordHighlightTextColor = Color(0xFF7FE29F),
        jlptBadgeBackground = Color(0xFF1C202A),
        jlptBadgeBorder = Color(0xFF2D3444),
        jlptBadgeText = Color(0xFFA8B2C4),
        iconTintColor = Color(0xFF7D8594),
        targetChipBackground = Color(0xFF181C24),
        targetChipPresentBackground = Color(0xFF232834),
        targetChipBorder = Color(0xFF282E3C),
        targetChipPresentBorder = Color(0xFF3B4356),
        targetChipText = Color(0xFFDDE1E8),
        quizQuestionText = Color(0xFFDDE1E8),
        quizOptionBackground = Color(0xFF14171E),
        quizOptionBorder = Color(0xFF282E3C),
        quizOptionSelectedBackground = Color(0xFF1D2736),
        quizOptionSelectedBorder = Color(0xFF5A7EA6),
        quizOptionCorrectBackground = Color(0xFF193322),
        quizOptionCorrectBorder = Color(0xFF458F5A),
        quizOptionWrongBackground = Color(0xFF351A1E),
        quizOptionWrongBorder = Color(0xFFB84D5B),
        quizExplanationBackground = Color(0xFF181C23),
        quizExplanationBorder = Color(0xFF282E3C),
        quizExplanationText = Color(0xFFA0A8B6),
        previewColor = Color(0xFF0F1115)
    );

    companion object {
        fun fromId(id: String?): ReadingTheme {
            if (id.isNullOrBlank()) return WARM_PARCHMENT
            return values().find { it.id.equals(id, ignoreCase = true) } ?: WARM_PARCHMENT
        }
    }
}

/**
 * Modern tactile theme selector displaying the 5 curated reading themes:
 * - Color swatch preview with authentic text preview & kanji
 * - High-contrast selection badge & animated border glow
 * - Tactile haptic feedback on change
 */
@Composable
fun ReadingThemePicker(
    selectedTheme: ReadingTheme,
    onThemeSelected: (ReadingTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.AutoStories,
                contentDescription = null,
                tint = SakuColors.SagePrimary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Reading Screen Theme",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = SakuColors.TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = selectedTheme.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SakuColors.SagePrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ReadingTheme.values()) { theme ->
                val isSelected = theme == selectedTheme
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "ReadingThemeScale"
                )

                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) SakuColors.SagePrimary else SakuColors.BorderSubtle,
                    animationSpec = tween(150),
                    label = "ReadingThemeBorder"
                )

                val borderWidth by animateDpAsState(
                    targetValue = if (isSelected) 2.dp else 1.dp,
                    animationSpec = tween(150),
                    label = "ReadingThemeBorderWidth"
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SakuColors.SurfaceElevated,
                    border = BorderStroke(borderWidth, borderColor),
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onThemeSelected(theme)
                            }
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        // Swatch circle showing authentic background and foreground typography preview
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(theme.previewColor)
                                .border(1.5.dp, theme.borderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = theme.kanji,
                                color = theme.textPrimaryColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = theme.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) SakuColors.TextPrimary else SakuColors.TextSecondary,
                            maxLines = 1
                        )

                        Text(
                            text = theme.subtitle,
                            fontSize = 10.sp,
                            color = SakuColors.TextTertiary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
