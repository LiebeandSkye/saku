package com.saku.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saku.reading.GenerationStatus
import kotlinx.coroutines.delay

/**
 * A compact, iOS-style Dynamic Island pill indicator that displays
 * background story generation progress and animated status text at the top of the screen.
 * Seamlessly adapts to Light, Dark, and Dim app themes.
 */
@Composable
fun StoryGenerationPill(
    status: GenerationStatus,
    currentTheme: AppTheme,
    onPillClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = status !is GenerationStatus.Idle

    // Auto-dismiss after success or error with a smooth iOS close transition
    LaunchedEffect(status) {
        when (status) {
            is GenerationStatus.Success -> {
                delay(1600L)
                onDismiss()
            }
            is GenerationStatus.Error -> {
                delay(2600L)
                onDismiss()
            }
            else -> {}
        }
    }

    // Theme-matched styling
    val (backgroundColor, borderColor, textColor, accentColor) = when (currentTheme) {
        AppTheme.LIGHT -> Quad(
            Color.White.copy(alpha = 0.95f),
            SakuColors.BorderHighlight.copy(alpha = 0.45f),
            Color(0xFF1E232A),
            SakuColors.SagePrimary
        )
        AppTheme.DARK -> Quad(
            Color(0xFF0F1116).copy(alpha = 0.96f),
            Color.White.copy(alpha = 0.14f),
            Color(0xFFE8EAF0),
            SakuColors.VibrantMatchaLight
        )
        AppTheme.DIM -> Quad(
            Color(0xFF191C23).copy(alpha = 0.96f),
            SakuColors.BorderHighlight.copy(alpha = 0.35f),
            Color(0xFFE8EAF0),
            SakuColors.SageLight
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it * 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(220)) + scaleIn(
            initialScale = 0.80f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it * 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut(animationSpec = tween(180)) + scaleOut(
            targetScale = 0.80f,
            animationSpec = tween(180)
        ),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 8.dp,
            modifier = Modifier
                .height(32.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPillClick
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                when (status) {
                    is GenerationStatus.Generating -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 1.8.dp,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        val jlpt = status.jlptLevel
                        val phrases = listOf(
                            "Crafting $jlpt story...",
                            "Weaving the plot...",
                            "Polishing details...",
                            "Adding some flair...",
                            "Fine-tuning...",
                            "Almost there..."
                        )
                        RotatingStatusText(
                            phrases = phrases,
                            isGenerating = true,
                            color = textColor,
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = textColor
                            ),
                            intervalMs = 1400L,
                            modifier = Modifier.widthIn(max = 160.dp)
                        )
                    }
                    is GenerationStatus.Success -> {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Story Ready",
                            tint = SakuColors.VibrantMatcha,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Story ready!",
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = textColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is GenerationStatus.Error -> {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Generation Error",
                            tint = SakuColors.AccentRose,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Generation failed",
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = textColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    GenerationStatus.Idle -> {
                        // Invisible placeholder
                        Spacer(modifier = Modifier.size(1.dp))
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
