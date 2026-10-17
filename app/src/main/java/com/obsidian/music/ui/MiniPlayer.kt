package com.obsidian.music.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.obsidian.music.data.Track

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
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .height(68.dp)
            .bouncyClick(pressedScale = 0.985f, onClick = onOpen)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = Color.Black,
                spotColor = Color.Black,
            )
            // Opaque glass: list text can no longer show through and clash with the title.
            .glassPanel(shape, tint = accent, tintAlpha = 0.16f, sheen = 0.11f, edge = 0.34f)
            .padding(start = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(track, RoundedCornerShape(16.dp), Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(end = 4.dp)) {
            Text(
                track.title,
                style = ObsidianType.MiniTitle,
                color = Obsidian.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track.artist,
                style = ObsidianType.MiniSub,
                color = Obsidian.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .size(42.dp)
                .bouncyClick(0.85f, onPrevious),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = Obsidian.TextPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
        PlayRing(isPlaying, accent, positionProvider, durationMs, onToggle)
        Box(
            Modifier
                .size(42.dp)
                .bouncyClick(0.85f, onNext),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = Obsidian.TextPrimary,
                modifier = Modifier.size(26.dp),
            )
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
            .bouncyClick(0.9f, onToggle)
            .drawBehind {
                val stroke = 2.5.dp.toPx()
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = Color.White.copy(alpha = 0.14f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arc,
                    style = Stroke(stroke),
                )
                val f = (positionProvider() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        PlayPauseIcon(isPlaying, size = 22)
    }
}

/** Play/pause glyph that morphs with a quick scale and fade. */
@Composable
fun PlayPauseIcon(isPlaying: Boolean, modifier: Modifier = Modifier, size: Int = 32, tint: Color = Obsidian.TextPrimary) {
    AnimatedContent(
        targetState = isPlaying,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(160)) + scaleIn(tween(200), initialScale = 0.6f)) togetherWith
                (fadeOut(tween(100)) + scaleOut(tween(120), targetScale = 0.6f))
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
