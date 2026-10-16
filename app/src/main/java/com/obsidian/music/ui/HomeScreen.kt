package com.obsidian.music.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.obsidian.music.data.Track

@Composable
fun HomeScreen(
    tracks: List<Track>,
    current: Track?,
    isPlaying: Boolean,
    shuffle: Boolean,
    accent: Color,
    bottomPadding: Dp,
    onPlay: (Track) -> Unit,
    onShufflePlay: () -> Unit,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        contentPadding = PaddingValues(top = top + 12.dp, bottom = bottom + bottomPadding + 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "header") {
            Header(count = tracks.size, shuffle = shuffle, accent = accent, onShuffle = onShufflePlay)
        }
        items(tracks, key = { it.id }) { track ->
            TrackRow(
                track = track,
                isCurrent = track.id == current?.id,
                isPlaying = isPlaying,
                accent = accent,
                onClick = { onPlay(track) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun Header(count: Int, shuffle: Boolean, accent: Color, onShuffle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 20.dp, bottom = 18.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Latest", style = MaterialTheme.typography.displayMedium, color = Obsidian.TextPrimary)
            Text(
                if (count == 1) "1 song" else "$count songs",
                style = MaterialTheme.typography.bodyMedium,
                color = Obsidian.TextTertiary,
            )
        }
        ShufflePill(active = shuffle, accent = accent, onClick = onShuffle)
    }
}

@Composable
private fun ShufflePill(active: Boolean, accent: Color, onClick: () -> Unit) {
    val tint by animateColorAsState(
        if (active) accent.copy(alpha = 0.35f) else Color.Transparent,
        tween(300),
        label = "shuffleTint",
    )
    Row(
        Modifier
            .bouncyClick(pressedScale = 0.92f, onClick = onClick)
            .glass(CircleShape)
            .background(tint)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Obsidian.TextPrimary, modifier = Modifier.size(18.dp))
        Text("Shuffle", style = MaterialTheme.typography.labelMedium, color = Obsidian.TextPrimary)
    }
}

@Composable
private fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlight by animateColorAsState(
        if (isCurrent) accent.copy(alpha = 0.16f) else Color.Transparent,
        tween(350),
        label = "rowHighlight",
    )
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .bouncyClick(pressedScale = 0.975f, onClick = onClick)
            .clip(shape)
            .background(highlight)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(track, RoundedCornerShape(14.dp), Modifier.size(54.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) Color.White else Obsidian.TextPrimary,
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
        Spacer(Modifier.width(12.dp))
        if (isCurrent) {
            Equalizer(playing = isPlaying, color = accent)
        } else {
            Text(
                formatTime(track.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = Obsidian.TextTertiary,
            )
        }
    }
}

/** Three-bar equalizer. Animates only while this row is the playing song. */
@Composable
private fun Equalizer(playing: Boolean, color: Color) {
    Row(
        Modifier
            .height(18.dp)
            .width(20.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (playing) {
            val t = rememberInfiniteTransition(label = "eq")
            val specs = listOf(420, 560, 340)
            specs.forEachIndexed { i, ms ->
                val h by t.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Reverse),
                    label = "bar$i",
                )
                Bar(h, color)
            }
        } else {
            listOf(0.35f, 0.35f, 0.35f).forEach { Bar(it, color) }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(
        Modifier
            .width(4.dp)
            .fillMaxHeight(fraction)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}
