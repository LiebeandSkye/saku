package com.saku.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * An animated rotating status label that cycles smoothly through a list of phrases
 * with a Claude-style vertical slide and fade transition.
 */
@Composable
fun RotatingStatusText(
    phrases: List<String>,
    isGenerating: Boolean,
    modifier: Modifier = Modifier,
    intervalMs: Long = 1200L,
    color: Color = SakuColors.OnSage,
    style: TextStyle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = color
    ),
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1
) {
    if (phrases.isEmpty()) return

    var currentIndex by remember(phrases) { mutableIntStateOf(0) }

    LaunchedEffect(isGenerating, phrases) {
        if (!isGenerating) {
            currentIndex = 0
            return@LaunchedEffect
        }
        currentIndex = 0
        while (isActive) {
            delay(intervalMs)
            currentIndex = (currentIndex + 1) % phrases.size
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                (slideInVertically(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) { height -> height / 2 } + fadeIn(
                    animationSpec = tween(durationMillis = 280)
                )) togetherWith (slideOutVertically(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) { height -> -height / 2 } + fadeOut(
                    animationSpec = tween(durationMillis = 280)
                ))
            },
            label = "RotatingStatusTextTransition"
        ) { targetIndex ->
            val phrase = phrases.getOrElse(targetIndex) { phrases.first() }
            Text(
                text = phrase,
                style = style,
                maxLines = maxLines,
                overflow = overflow
            )
        }
    }
}
