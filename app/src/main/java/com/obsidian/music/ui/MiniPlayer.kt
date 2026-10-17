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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .bouncyClick(pressedScale = 0.985f, onClick = onOpen)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = 0.5f),
                spotColor = accent.copy(alpha = 0.5f),
            )
            // Opaque glass: list text can no longer show through and clash with the title.
            .glassPanel(shape, tint = accent, tintAlpha = 0.18f, borderAlpha = 0.42f)
            // Progress hairline is drawn in the draw phase, so ticking position never recomposes the bar.
            .drawBehind {
                val d = durationMs.coerceAtLeast(1L).toFloat()
                val f = (positionProvider() / d).coerceIn(0f, 1f)
                val inset = 22.dp.toPx()
                val w = size.width - inset * 2
                val y = size.height - 5.dp.toPx()
                drawRoundRect(
                    Color.White.copy(alpha = 0.10f),
                    topLeft = Offset(inset, y), size = Size(w, 2.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
                drawRoundRect(
                    accent,
                    topLeft = Offset(inset, y), size = Size(w * f, 2.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
            }
            .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(track, RoundedCornerShape(16.dp), Modifier.size(50.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Obsidian.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Obsidian.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PlayPauseIcon(isPlaying, Modifier.bouncyClick(0.88f, onToggle).padding(10.dp), size = 30)
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = Obsidian.TextPrimary,
                modifier = Modifier
                    .bouncyClick(0.85f, onNext)
                    .padding(10.dp)
                    .size(28.dp),
            )
        }
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
