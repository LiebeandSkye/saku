package com.saku.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Single static / twinkling star data model.
 */
data class Star(
    val xNorm: Float,
    val yNorm: Float,
    val radiusDp: Float,
    val baseAlpha: Float,
    val twinklePhase: Float,
    val twinkleSpeed: Float
)

/**
 * Active shooting star state model.
 */
data class ShootingStarState(
    val startX: Float,
    val startY: Float,
    val angleRad: Float,
    val lengthPx: Float,
    val travelDistancePx: Float,
    val progress: Float, // 0f to 1f
    val alpha: Float
)

/**
 * Generates a stable list of pseudo-random stars.
 */
fun generateStarfield(count: Int = 65, seed: Long = 42L): List<Star> {
    val rng = Random(seed)
    return List(count) {
        Star(
            xNorm = rng.nextFloat(),
            yNorm = rng.nextFloat(),
            radiusDp = 1.0f + rng.nextFloat() * 1.8f,
            baseAlpha = 0.25f + rng.nextFloat() * 0.65f,
            twinklePhase = rng.nextFloat() * (2f * PI.toFloat()),
            twinkleSpeed = 1.2f + rng.nextFloat() * 2.2f
        )
    }
}

/**
 * Low-overhead, hardware-accelerated Canvas for drawing starfields and shooting stars.
 */
@Composable
fun ShootingStarsCanvas(
    stars: List<Star>,
    shootingStars: List<ShootingStarState>,
    theme: AppTheme,
    animTimeSeconds: Float,
    modifier: Modifier = Modifier
) {
    // Theme-driven colors
    val (starColor, streakColor) = when (theme) {
        AppTheme.LIGHT -> Pair(
            Color(0xFF22262E), // Crisp dark charcoal / black in light mode
            Color(0xFF16181D)
        )
        AppTheme.DIM -> Pair(
            Color(0xFFFFFFFF), // Brilliant white
            Color(0xFFFFFFFF)
        )
        AppTheme.DARK -> Pair(
            Color(0xFFFFFFFF),
            Color(0xFFFFFFFF)
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Draw scattered twinkling stars
        for (star in stars) {
            val cx = star.xNorm * w
            val cy = star.yNorm * h
            val twinkle = 0.65f + 0.35f * sin(animTimeSeconds * star.twinkleSpeed + star.twinklePhase)
            val computedAlpha = (star.baseAlpha * twinkle).coerceIn(0.08f, 0.95f)

            drawCircle(
                color = starColor.copy(alpha = computedAlpha),
                radius = star.radiusDp.dp.toPx(),
                center = Offset(cx, cy)
            )
        }

        // 2. Draw shooting stars with fading gradient tails
        for (meteor in shootingStars) {
            if (meteor.progress <= 0f || meteor.progress >= 1f || meteor.alpha <= 0f) continue

            // Head position along travel vector
            val currentDistance = meteor.travelDistancePx * meteor.progress
            val headX = meteor.startX + cos(meteor.angleRad) * currentDistance
            val headY = meteor.startY + sin(meteor.angleRad) * currentDistance

            // Tail position behind head
            val tailX = headX - cos(meteor.angleRad) * meteor.lengthPx
            val tailY = headY - sin(meteor.angleRad) * meteor.lengthPx

            // Fade in at start, fade out at end
            val lifecycleAlpha = when {
                meteor.progress < 0.15f -> meteor.progress / 0.15f
                meteor.progress > 0.80f -> (1f - meteor.progress) / 0.20f
                else -> 1f
            }
            val netAlpha = (meteor.alpha * lifecycleAlpha).coerceIn(0f, 1f)

            if (netAlpha > 0.01f) {
                // Radiant gradient tail (fades to transparent at the tail)
                val tailBrush = Brush.linearGradient(
                    colors = listOf(
                        streakColor.copy(alpha = netAlpha * 0.95f),
                        streakColor.copy(alpha = netAlpha * 0.40f),
                        streakColor.copy(alpha = 0f)
                    ),
                    start = Offset(headX, headY),
                    end = Offset(tailX, tailY)
                )

                drawLine(
                    brush = tailBrush,
                    start = Offset(headX, headY),
                    end = Offset(tailX, tailY),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Glowing head core
                drawCircle(
                    color = streakColor.copy(alpha = netAlpha),
                    radius = 2.8.dp.toPx(),
                    center = Offset(headX, headY)
                )

                // Soft outer aura on head for dark/dim mode
                if (theme != AppTheme.LIGHT) {
                    drawCircle(
                        color = streakColor.copy(alpha = netAlpha * 0.35f),
                        radius = 5.5.dp.toPx(),
                        center = Offset(headX, headY)
                    )
                }
            }
        }
    }
}

/**
 * Persistent, battery-friendly ambient shooting stars background for the Cards tab.
 * Streaks a fast shooting star across the screen every 6–8 seconds.
 * Automatically halts frame ticks when [isActive] is false (e.g., user is on another tab).
 */
@Composable
fun CardsShootingStarsBackground(
    theme: AppTheme,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val stars = remember { generateStarfield(count = 55, seed = 777L) }
    var animTime by remember { mutableFloatStateOf(0f) }
    var activeMeteor by remember { mutableStateOf<ShootingStarState?>(null) }

    // Smooth continuous twinkle time when active
    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        var startNanos = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (startNanos == 0L) startNanos = frameTimeNanos
                animTime = (frameTimeNanos - startNanos) / 1_000_000_000f
            }
        }
    }

    // Occasional shooting star scheduler: triggers every 6 to 8 seconds
    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        while (isActive) {
            // Ambient quiet delay between 6.0s and 8.0s
            val delayMs = 6000L + Random.nextLong(2000L)
            delay(delayMs)
            if (!isActive) break

            // Configure a new shooting star starting off-screen top-right streaking diagonally down-left
            val startX = 600f + Random.nextFloat() * 400f
            val startY = -50f + Random.nextFloat() * 300f
            // Angle around -35 degrees (flying down and left)
            val angle = (145f + Random.nextFloat() * 15f) * (PI.toFloat() / 180f)
            val length = 320f + Random.nextFloat() * 140f
            val distance = 1100f + Random.nextFloat() * 300f

            val durationMs = 650 // Fast and snappy
            val startTime = System.currentTimeMillis()

            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)

                activeMeteor = ShootingStarState(
                    startX = startX,
                    startY = startY,
                    angleRad = angle,
                    lengthPx = length,
                    travelDistancePx = distance,
                    progress = progress,
                    alpha = 0.85f
                )

                if (progress >= 1f) break
                withFrameNanos { }
            }
            activeMeteor = null
        }
    }

    val activeList = remember(activeMeteor) {
        if (activeMeteor != null) listOf(activeMeteor!!) else emptyList()
    }

    Box(modifier = modifier) {
        ShootingStarsCanvas(
            stars = stars,
            shootingStars = activeList,
            theme = theme,
            animTimeSeconds = animTime,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Snappy Cold-Start Intro Animation (~1.1s duration).
 * Features cosmic starfield, rapid high-speed shooting star bursts, and an illuminated Saku title/logo.
 * Includes instant tap-to-skip so the user never has to wait.
 */
@Composable
fun SakuCosmicIntro(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stars = remember { generateStarfield(count = 70, seed = 12345L) }
    var animTime by remember { mutableFloatStateOf(0f) }
    var shootingStarsList by remember { mutableStateOf<List<ShootingStarState>>(emptyList()) }

    // Overall intro container alpha and brand scale
    val introAlpha = remember { Animatable(1f) }
    val brandScale = remember { Animatable(0.88f) }
    val brandAlpha = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    fun triggerDismiss() {
        if (!isDismissing) {
            isDismissing = true
            onDismiss()
        }
    }

    // High precision frame ticker for the intro sequence
    LaunchedEffect(Unit) {
        val durationMs = 1100L // Fast 1.1s intro

        // Animate brand entrance
        brandAlpha.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing))
        brandScale.animateTo(1.0f, animationSpec = tween(400, easing = FastOutSlowInEasing))

        // Wait until intro time reaches dismiss window
        delay(durationMs - 250)

        // Fade out smoothly into the app
        introAlpha.animateTo(0f, animationSpec = tween(220, easing = LinearEasing))
        triggerDismiss()
    }

    // Rapid shooting star spawner: launches 2-3 fast meteors in rapid succession
    LaunchedEffect(Unit) {
        val meteors = mutableListOf<ShootingStarState>()
        val startTime = System.currentTimeMillis()

        // Meteor specs (startX, startY, durationMs)
        val meteorConfigs = listOf(
            Triple(850f, 60f, 480),   // High and fast
            Triple(950f, 350f, 520),  // Mid-screen streak
            Triple(700f, 650f, 460)   // Low streak
        )

        while (isActive && !isDismissing) {
            val now = System.currentTimeMillis() - startTime
            animTime = now / 1000f

            meteors.clear()
            for (i in meteorConfigs.indices) {
                val (startX, startY, dur) = meteorConfigs[i]
                val meteorStartDelay = i * 220L // Staggered rapid succession
                val elapsed = now - meteorStartDelay
                if (elapsed in 0..dur) {
                    val progress = (elapsed.toFloat() / dur).coerceIn(0f, 1f)
                    meteors.add(
                        ShootingStarState(
                            startX = startX,
                            startY = startY,
                            angleRad = 142f * (PI.toFloat() / 180f),
                            lengthPx = 340f,
                            travelDistancePx = 1300f,
                            progress = progress,
                            alpha = 0.95f
                        )
                    )
                }
            }

            shootingStarsList = meteors.toList()
            withFrameNanos { }
        }
    }

    // Full screen overlay with instant tap-to-skip
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = introAlpha.value }
            .background(Color(0xFF07080C)) // Deep cosmic space
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                triggerDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        // Starfield & fast shooting stars
        ShootingStarsCanvas(
            stars = stars,
            shootingStars = shootingStarsList,
            theme = AppTheme.DARK,
            animTimeSeconds = animTime,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle cosmic nebula glow in the center
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SakuColors.VibrantMatcha.copy(alpha = 0.16f),
                            Color(0xFF1E3A2B).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Center Brand Emblem: Japanese title + SAKU
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = brandScale.value
                    scaleY = brandScale.value
                    alpha = brandAlpha.value
                }
        ) {
            Text(
                text = "アンキ",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "SAKU",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SakuColors.VibrantMatchaLight,
                letterSpacing = 5.sp
            )
        }
    }
}
