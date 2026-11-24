package com.obsidian.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsidian.music.ui.theme.ObsidianMotion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interactive Liquid Waveform Scrubber:
 * - Audio-reactive multi-bar waveform that gently ripples while playing and settles when paused
 * - Dragging shows a magnifying liquid glass droplet thumb + floating live timestamp pill
 * - Subtle tactile haptic ticks as you scrub across bars
 */
@Composable
fun LiquidWaveformScrubber(
    trackSeed: String,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    accentColor: Color,
    onSeekToMs: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(1) }
    var lastHapticStep by remember { mutableIntStateOf(-1) }

    val playbackFraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val activeFraction = if (isDragging) dragFraction else playbackFraction
    val displayMs = (activeFraction * durationMs).toLong()

    // Deterministic organic bar amplitudes per track
    val barCount = 46
    val baseAmplitudes = remember(trackSeed) {
        val hash = trackSeed.hashCode()
        List(barCount) { i ->
            val v1 = sin((i * 0.42f) + (hash % 17)) * 0.32f
            val v2 = sin((i * 1.15f) + (hash % 31)) * 0.22f
            val envelope = sin((i.toFloat() / (barCount - 1)) * PI.toFloat()) * 0.35f + 0.45f
            (0.32f + abs(v1 + v2)) * envelope
        }
    }

    val infinite = rememberInfiniteTransition(label = "waveformRipple")
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val playAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.45f,
        animationSpec = ObsidianMotion.LiquidMorphSpring,
        label = "playAmplitude"
    )

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.45f else 1.0f,
        animationSpec = ObsidianMotion.GelBounceSpring,
        label = "thumbScale"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Floating timestamp tooltip while scrubbing
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
        ) {
            val pillOffsetX = ((activeFraction * widthPx) - with(density) { 26.dp.toPx() })
                .coerceIn(0f, (widthPx - with(density) { 56.dp.toPx() }).coerceAtLeast(0f))

            androidx.compose.animation.AnimatedVisibility(
                visible = isDragging,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
                modifier = Modifier.offset { IntOffset(pillOffsetX.roundToInt(), 0) }
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.50f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatDurationMs(displayMs),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Waveform Canvas + Touch Scrubber
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSeekToMs((frac * durationMs).toLong())
                    }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            dragFraction = frac
                            val step = (frac * 24).toInt()
                            if (step != lastHapticStep) {
                                lastHapticStep = step
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeekToMs((dragFraction * durationMs).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val centerY = h / 2f
            val step = w / barCount
            val barWidth = (step * 0.56f).coerceIn(2.5f, 8f)
            val thumbX = activeFraction * w

            val activeBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.92f),
                    accentColor.copy(alpha = 0.95f),
                    Color.White
                ),
                startX = 0f,
                endX = thumbX.coerceAtLeast(1f)
            )

            for (i in 0 until barCount) {
                val barCenterX = i * step + step / 2f
                val normalizedX = barCenterX / w
                val isPlayed = normalizedX <= activeFraction

                // Subtle fluid wave motion while playing
                val ripple = if (isPlaying && isPlayed) {
                    1f + 0.16f * sin(wavePhase + i * 0.45f)
                } else {
                    1f
                }

                // Local magnification near the thumb while dragging
                val distToThumb = abs(barCenterX - thumbX) / (w * 0.10f)
                val magnify = if (isDragging && distToThumb < 1f) {
                    1f + (1f - distToThumb) * 0.35f
                } else 1f

                val rawHeight = baseAmplitudes[i] * h * 0.88f * playAmplitude * ripple * magnify
                val barHeight = rawHeight.coerceIn(5.dp.toPx(), h * 0.94f)
                val top = centerY - barHeight / 2f

                if (isPlayed) {
                    drawRoundRect(
                        brush = activeBrush,
                        topLeft = Offset(barCenterX - barWidth / 2f, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                } else {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.22f),
                        topLeft = Offset(barCenterX - barWidth / 2f, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            // Liquid Glass Droplet Thumb
            val radius = 7.dp.toPx() * thumbScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    center = Offset(thumbX, centerY),
                    radius = radius * 2.2f
                ),
                radius = radius * 2.2f,
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = radius,
                center = Offset(thumbX, centerY)
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDurationMs(displayMs),
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "-${formatDurationMs((durationMs - displayMs).coerceAtLeast(0L))}",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Compact 3-bar live animated equalizer indicator for currently playing rows and mini-player.
 */
@Composable
fun LiveEqualizerBars(
    isPlaying: Boolean,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "miniEq")
    val b1 by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "b1"
    )
    val b2 by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "b2"
    )
    val b3 by infinite.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(tween(360), RepeatMode.Reverse),
        label = "b3"
    )

    Canvas(modifier = modifier) {
        val barW = size.width / 5f
        val heights = if (isPlaying) listOf(b1, b2, b3) else listOf(0.28f, 0.28f, 0.28f)
        heights.forEachIndexed { idx, frac ->
            val bh = (size.height * frac).coerceAtLeast(barW)
            val x = idx * (barW * 1.9f)
            drawRoundRect(
                color = tint,
                topLeft = Offset(x, size.height - bh),
                size = Size(barW, bh),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}

fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}
