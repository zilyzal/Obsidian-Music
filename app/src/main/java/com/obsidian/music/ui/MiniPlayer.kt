package com.obsidian.music.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.obsidian.music.data.Track
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Floating liquid-glass mini player. Tap to open the full player; swipe sideways to skip:
 * the contents slide and fade inside the glass, which stays put and swells slightly.
 */
@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    accent: Color,
    positionProvider: () -> Long,
    durationMs: Long,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    artModifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(30.dp)
    val press = rememberLiquidPress()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val swipe = remember { Animatable(0f) }
    val threshold = with(LocalDensity.current) { 84.dp.toPx() }
    val travel = with(LocalDensity.current) { 220.dp.toPx() }

    Box(
        modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .height(70.dp)
            .liquidClickable(press, onClick = onOpen)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { d -> scope.launch { swipe.snapTo(swipe.value + d * 0.9f) } },
                onDragStopped = { v ->
                    val go = swipe.value
                    if (abs(go) > threshold || abs(v) > 2400f) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val dir = if ((if (abs(go) > threshold) go else v) < 0) -1f else 1f
                        swipe.animateTo(dir * travel, tween(140))
                        if (dir < 0) onNext() else onPrevious()
                        swipe.snapTo(-dir * travel * 0.6f)
                        swipe.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 380f))
                    } else {
                        swipe.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 420f))
                    }
                },
            )
            .liquidGlass(
                shape,
                GlassStyle.Panel,
                tint = accent,
                tintAlpha = 0.10f,
                press = { maxOf(press.value, (abs(swipe.value) / travel).coerceIn(0f, 1f) * 0.6f) },
            ),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = swipe.value
                    alpha = 1f - (abs(swipe.value) / travel).coerceIn(0f, 1f) * 0.85f
                }
                .padding(start = 11.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(track, RoundedCornerShape(17.dp), artModifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            AnimatedContent(
                targetState = track,
                contentKey = { it.id },
                transitionSpec = { fadeIn(tween(240, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                label = "miniText",
            ) { t ->
                Column {
                    Text(t.title, style = ObsidianType.MiniTitle, color = Obsidian.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.artist, style = ObsidianType.MiniSub, color = Obsidian.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(Modifier.size(42.dp).bouncyClick(0.8f, onPrevious), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = Obsidian.TextPrimary, modifier = Modifier.size(26.dp))
            }
            PlayRing(isPlaying, accent, positionProvider, durationMs, onToggle)
            Box(Modifier.size(42.dp).bouncyClick(0.8f, onNext), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Obsidian.TextPrimary, modifier = Modifier.size(26.dp))
            }
        }
    }
}

/**
 * Play/pause with the song's progress drawn as a ring around it. The ring is painted in the
 * draw phase, so the ticking position never recomposes anything.
 */
@Composable
private fun PlayRing(
    isPlaying: Boolean,
    accent: Color,
    positionProvider: () -> Long,
    durationMs: Long,
    onToggle: () -> Unit,
) {
    Box(
        Modifier
            .size(46.dp)
            .bouncyClick(0.86f, onToggle)
            .drawBehind {
                val stroke = 2.5.dp.toPx()
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(Color.White.copy(alpha = 0.14f), 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
                val f = (positionProvider() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                drawArc(accent, -90f, 360f * f, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            },
        contentAlignment = Alignment.Center,
    ) {
        PlayPauseIcon(isPlaying, size = 22)
    }
}

/** Play/pause glyph that morphs with a springy spin, scale and fade. */
@Composable
fun PlayPauseIcon(isPlaying: Boolean, modifier: Modifier = Modifier, size: Int = 32, tint: Color = Obsidian.TextPrimary) {
    AnimatedContent(
        targetState = isPlaying,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(140)) + scaleIn(spring(dampingRatio = 0.5f, stiffness = 520f), initialScale = 0.4f)) togetherWith
                (fadeOut(tween(90)) + scaleOut(tween(120), targetScale = 0.4f))
        },
        label = "playPause",
    ) { playing ->
        Icon(
            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            tint = tint,
            modifier = Modifier.size(size.dp),
        )
    }
}
