package com.saku.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern Liquid Glass styling replicating the exact glassmorphism design:
 * - Ambient soft drop shadow (0 6px 6px rgba(0,0,0,0.2), 0 0 20px rgba(0,0,0,0.1))
 * - Translucent multi-stop glass tint gradient
 * - Specular rim lighting (bright top-left highlight fading along edges)
 * - Dual inset specular reflections (inset 2px 2px white + inset -1px -1px)
 * - Top gloss reflection sheen across upper curve
 * - Bouncy spring physics (cubic-bezier / Spring.DampingRatioMediumBouncy)
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(26.dp),
    cornerRadius: Dp = 26.dp,
    tintColor: Color = Color.White.copy(alpha = 0.14f),
    darkBaseAlpha: Float = 0.65f,
    specularAlpha: Float = 0.50f,
    borderAlpha: Float = 0.45f,
    shadowElevation: Dp = 8.dp
): Modifier = this
    .shadow(
        elevation = shadowElevation,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.35f),
        spotColor = Color.Black.copy(alpha = 0.50f)
    )
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                tintColor.copy(alpha = (tintColor.alpha * 1.5f).coerceAtMost(0.35f)),
                Color(0xFF1E222D).copy(alpha = darkBaseAlpha),
                Color(0xFF13161E).copy(alpha = (darkBaseAlpha * 1.15f).coerceAtMost(0.92f))
            )
        )
    )
    .border(
        width = 1.2.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = specularAlpha),
                Color.White.copy(alpha = specularAlpha * 0.3f),
                Color.White.copy(alpha = 0.04f),
                Color.White.copy(alpha = specularAlpha * 0.7f)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        ),
        shape = shape
    )
    .drawWithContent {
        drawContent()

        val cr = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())

        // 1. Dual Inset Specular Highlight (replicates inset 2px 2px 1px 0 rgba(255,255,255,0.5))
        drawRoundRect(
            brush = Brush.linearGradient(
                0.0f to Color.White.copy(alpha = specularAlpha * 0.75f),
                0.35f to Color.White.copy(alpha = specularAlpha * 0.20f),
                0.70f to Color.Transparent,
                1.0f to Color.White.copy(alpha = specularAlpha * 0.45f),
                start = Offset(2f, 2f),
                end = Offset(size.width - 2f, size.height - 2f)
            ),
            topLeft = Offset(1.5f, 1.5f),
            size = Size(size.width - 3f, size.height - 3f),
            cornerRadius = cr,
            style = Stroke(width = 1.5f)
        )

        // 2. Curved top gloss reflection sheen (light catching the upper curved lens)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height * 0.38f
            ),
            topLeft = Offset.Zero,
            size = Size(size.width, size.height * 0.38f),
            cornerRadius = cr
        )
    }

/**
 * Standalone Liquid Glass Container.
 */
@Composable
fun LiquidGlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    cornerRadius: Dp = 26.dp,
    tintColor: Color = Color.White.copy(alpha = 0.14f),
    darkBaseAlpha: Float = 0.65f,
    specularAlpha: Float = 0.50f,
    borderAlpha: Float = 0.45f,
    shadowElevation: Dp = 8.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(
            shape = shape,
            cornerRadius = cornerRadius,
            tintColor = tintColor,
            darkBaseAlpha = darkBaseAlpha,
            specularAlpha = specularAlpha,
            borderAlpha = borderAlpha,
            shadowElevation = shadowElevation
        ),
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * Interactive Liquid Glass Button with spring physics scaling.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    cornerRadius: Dp = 20.dp,
    tintColor: Color = Color.White.copy(alpha = 0.16f),
    darkBaseAlpha: Float = 0.60f,
    specularAlpha: Float = 0.55f,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "LiquidGlassButtonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1.0f else 0.45f
            }
            .liquidGlass(
                shape = shape,
                cornerRadius = cornerRadius,
                tintColor = tintColor,
                darkBaseAlpha = darkBaseAlpha,
                specularAlpha = specularAlpha,
                shadowElevation = if (isPressed) 2.dp else 6.dp
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
