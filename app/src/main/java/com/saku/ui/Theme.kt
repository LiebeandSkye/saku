package com.saku.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object SakuColors {
    // Backgrounds - gentle, eye-friendly soft slate (no pitch-black OLED glare)
    val Background = Color(0xFF15171C)
    val BackgroundSecondary = Color(0xFF1A1D23)

    // Surfaces & Bento Cards - soothing soft dark obsidian
    val Surface = Color(0xFF1F222A)
    val SurfaceElevated = Color(0xFF262A34)
    val SurfaceVariant = Color(0xFF2C313D)

    // Borders & Hairlines - delicate, low visual noise
    val Border = Color(0xFF2F3440)
    val BorderSubtle = Color(0xFF262A35)
    val BorderHighlight = Color(0xFF414858)
    val BorderFocus = BorderHighlight

    // Primary Accent - Calming Sage Green
    val SagePrimary = Color(0xFF7E9F85)
    val SageLight = Color(0xFF9AB8A0)
    val SageContainer = Color(0xFF233127)
    val SageContainerBorder = Color(0xFF374D3D)
    val OnSage = Color(0xFF0F1B12)

    // Semantic Accents - Soothing Pastels (Zero Neon)
    val AccentRose = Color(0xFFCF7B88)          // Again / Warning
    val AccentRoseContainer = Color(0xFF332024)
    val AccentAmber = Color(0xFFD9A668)         // Hard / Attention
    val AccentAmberContainer = Color(0xFF33281D)
    val AccentSage = Color(0xFF7E9F85)          // Good / Active
    val AccentSlateBlue = Color(0xFF7B9EC7)     // Easy / Informational
    val AccentSlateBlueContainer = Color(0xFF202A36)
    val AccentLavender = Color(0xFFA594BA)      // AI / Reading
    val AccentLavenderContainer = Color(0xFF2B2536)

    // Typography - Soft off-white and neutral grays (prevents eye strain)
    val TextPrimary = Color(0xFFE8EAF0)
    val TextSecondary = Color(0xFF9AA1AD)
    val TextMuted = Color(0xFF6E7482)
    val TextTertiary = TextMuted
    val TextDisabled = Color(0xFF4D5360)
}

val SakuColorScheme = darkColorScheme(
    primary = SakuColors.SagePrimary,
    onPrimary = SakuColors.OnSage,
    primaryContainer = SakuColors.SageContainer,
    onPrimaryContainer = SakuColors.SageLight,
    secondary = SakuColors.AccentSlateBlue,
    onSecondary = Color.White,
    background = SakuColors.Background,
    onBackground = SakuColors.TextPrimary,
    surface = SakuColors.Surface,
    onSurface = SakuColors.TextPrimary,
    surfaceVariant = SakuColors.SurfaceElevated,
    onSurfaceVariant = SakuColors.TextSecondary,
    outline = SakuColors.Border,
    outlineVariant = SakuColors.BorderSubtle
)

@Composable
fun SakuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SakuColorScheme,
        content = content
    )
}
