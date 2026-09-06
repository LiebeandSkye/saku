package com.saku.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class AppTheme(val id: String, val label: String) {
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    DIM("dim", "Dim");

    companion object {
        fun fromId(id: String): AppTheme = values().find { it.id.equals(id, ignoreCase = true) } ?: DIM
    }
}

object SakuColors {
    // Vibrant Green Accent - energetic, punchy Matcha Jade requested for high contrast & clarity
    val VibrantMatcha = Color(0xFF52C47C)
    val VibrantMatchaLight = Color(0xFF6EE7A0)
    val VibrantMatchaContainer = Color(0xFF1B3824)
    val VibrantMatchaBorder = Color(0xFF2E633D)
    val OnVibrantMatcha = Color(0xFF091F11)

    // Dynamic Reactive Colors (observe state in Compose)
    var currentTheme by mutableStateOf(AppTheme.DIM)
        private set

    // Backgrounds
    var Background by mutableStateOf(Color(0xFF15171C))
        private set
    var BackgroundSecondary by mutableStateOf(Color(0xFF1A1D23))
        private set

    // Surfaces & Bento Cards
    var Surface by mutableStateOf(Color(0xFF1F222A))
        private set
    var SurfaceElevated by mutableStateOf(Color(0xFF262A34))
        private set
    var SurfaceVariant by mutableStateOf(Color(0xFF2C313D))
        private set

    // Borders & Hairlines
    var Border by mutableStateOf(Color(0xFF2F3440))
        private set
    var BorderSubtle by mutableStateOf(Color(0xFF262A35))
        private set
    var BorderHighlight by mutableStateOf(Color(0xFF414858))
        private set
    var BorderFocus by mutableStateOf(Color(0xFF414858))
        private set

    // Primary Accent - Calming Sage Green
    var SagePrimary by mutableStateOf(Color(0xFF7E9F85))
        private set
    var SageLight by mutableStateOf(Color(0xFF9AB8A0))
        private set
    var SageContainer by mutableStateOf(Color(0xFF233127))
        private set
    var SageContainerBorder by mutableStateOf(Color(0xFF374D3D))
        private set
    var OnSage by mutableStateOf(Color(0xFF0F1B12))
        private set

    // Semantic Accents
    var AccentRose by mutableStateOf(Color(0xFFCF7B88))
        private set
    var AccentRoseContainer by mutableStateOf(Color(0xFF332024))
        private set
    var AccentAmber by mutableStateOf(Color(0xFFD9A668))
        private set
    var AccentAmberContainer by mutableStateOf(Color(0xFF33281D))
        private set
    var AccentSage by mutableStateOf(Color(0xFF7E9F85))
        private set
    var AccentSlateBlue by mutableStateOf(Color(0xFF7B9EC7))
        private set
    var AccentSlateBlueContainer by mutableStateOf(Color(0xFF202A36))
        private set
    var AccentLavender by mutableStateOf(Color(0xFFA594BA))
        private set
    var AccentLavenderContainer by mutableStateOf(Color(0xFF2B2536))
        private set

    // Typography
    var TextPrimary by mutableStateOf(Color(0xFFE8EAF0))
        private set
    var TextSecondary by mutableStateOf(Color(0xFF9AA1AD))
        private set
    var TextMuted by mutableStateOf(Color(0xFF6E7482))
        private set
    var TextTertiary by mutableStateOf(Color(0xFF6E7482))
        private set
    var TextDisabled by mutableStateOf(Color(0xFF4D5360))
        private set

    fun applyTheme(theme: AppTheme) {
        currentTheme = theme
        when (theme) {
            AppTheme.LIGHT -> {
                Background = Color(0xFFF6F8FA)
                BackgroundSecondary = Color(0xFFEEF2F6)
                Surface = Color(0xFFFFFFFF)
                SurfaceElevated = Color(0xFFF1F4F9)
                SurfaceVariant = Color(0xFFE3E8EF)
                Border = Color(0xFFD8DEE4)
                BorderSubtle = Color(0xFFE4E9EE)
                BorderHighlight = Color(0xFFB4BFCC)
                BorderFocus = Color(0xFF2E7D32)
                SagePrimary = Color(0xFF2E7D32)
                SageLight = Color(0xFF43A047)
                SageContainer = Color(0xFFE8F5E9)
                SageContainerBorder = Color(0xFFC8E6C9)
                OnSage = Color(0xFFFFFFFF)
                AccentRose = Color(0xFFD32F2F)
                AccentRoseContainer = Color(0xFFFFEBEE)
                AccentAmber = Color(0xFFE65100)
                AccentAmberContainer = Color(0xFFFFF3E0)
                AccentSage = Color(0xFF2E7D32)
                AccentSlateBlue = Color(0xFF1976D2)
                AccentSlateBlueContainer = Color(0xFFE3F2FD)
                AccentLavender = Color(0xFF7B1FA2)
                AccentLavenderContainer = Color(0xFFF3E5F5)
                TextPrimary = Color(0xFF1A1D23)
                TextSecondary = Color(0xFF57606A)
                TextMuted = Color(0xFF8C95A0)
                TextTertiary = Color(0xFF8C95A0)
                TextDisabled = Color(0xFFB1BAC4)
            }
            AppTheme.DARK -> {
                // Pitch dark / OLED Obsidian black
                Background = Color(0xFF090B0E)
                BackgroundSecondary = Color(0xFF0F1217)
                Surface = Color(0xFF13171F)
                SurfaceElevated = Color(0xFF1B202B)
                SurfaceVariant = Color(0xFF232A38)
                Border = Color(0xFF262D3D)
                BorderSubtle = Color(0xFF1D2330)
                BorderHighlight = Color(0xFF384359)
                BorderFocus = Color(0xFF52C47C)
                SagePrimary = VibrantMatcha
                SageLight = VibrantMatchaLight
                SageContainer = VibrantMatchaContainer
                SageContainerBorder = VibrantMatchaBorder
                OnSage = OnVibrantMatcha
                AccentRose = Color(0xFFE06C75)
                AccentRoseContainer = Color(0xFF2D161A)
                AccentAmber = Color(0xFFE5C07B)
                AccentAmberContainer = Color(0xFF2B2213)
                AccentSage = VibrantMatcha
                AccentSlateBlue = Color(0xFF61AFEF)
                AccentSlateBlueContainer = Color(0xFF152233)
                AccentLavender = Color(0xFFC678DD)
                AccentLavenderContainer = Color(0xFF281733)
                TextPrimary = Color(0xFFF0F3F8)
                TextSecondary = Color(0xFFA2AAB8)
                TextMuted = Color(0xFF656D7E)
                TextTertiary = Color(0xFF656D7E)
                TextDisabled = Color(0xFF424957)
            }
            AppTheme.DIM -> {
                // Saku signature soothing soft slate
                Background = Color(0xFF15171C)
                BackgroundSecondary = Color(0xFF1A1D23)
                Surface = Color(0xFF1F222A)
                SurfaceElevated = Color(0xFF262A34)
                SurfaceVariant = Color(0xFF2C313D)
                Border = Color(0xFF2F3440)
                BorderSubtle = Color(0xFF262A35)
                BorderHighlight = Color(0xFF414858)
                BorderFocus = Color(0xFF414858)
                SagePrimary = Color(0xFF7E9F85)
                SageLight = Color(0xFF9AB8A0)
                SageContainer = Color(0xFF233127)
                SageContainerBorder = Color(0xFF374D3D)
                OnSage = Color(0xFF0F1B12)
                AccentRose = Color(0xFFCF7B88)
                AccentRoseContainer = Color(0xFF332024)
                AccentAmber = Color(0xFFD9A668)
                AccentAmberContainer = Color(0xFF33281D)
                AccentSage = Color(0xFF7E9F85)
                AccentSlateBlue = Color(0xFF7B9EC7)
                AccentSlateBlueContainer = Color(0xFF202A36)
                AccentLavender = Color(0xFFA594BA)
                AccentLavenderContainer = Color(0xFF2B2536)
                TextPrimary = Color(0xFFE8EAF0)
                TextSecondary = Color(0xFF9AA1AD)
                TextMuted = Color(0xFF6E7482)
                TextTertiary = Color(0xFF6E7482)
                TextDisabled = Color(0xFF4D5360)
            }
        }
    }
}

@Composable
fun SakuTheme(
    theme: AppTheme = AppTheme.DIM,
    content: @Composable () -> Unit
) {
    LaunchedEffect(theme) {
        SakuColors.applyTheme(theme)
    }

    val colorScheme = if (theme == AppTheme.LIGHT) {
        lightColorScheme(
            primary = SakuColors.SagePrimary,
            onPrimary = SakuColors.OnSage,
            primaryContainer = SakuColors.SageContainer,
            onPrimaryContainer = SakuColors.SagePrimary,
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
    } else {
        darkColorScheme(
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
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
