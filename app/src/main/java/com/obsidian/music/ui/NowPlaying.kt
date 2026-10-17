package com.obsidian.music.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.obsidian.music.data.Track
import kotlinx.coroutines.launch

@Composable
fun NowPlaying(
    track: Track,
    isPlaying: Boolean,
    shuffle: Boolean,
    favorite: Boolean,
    accent: Color,
    durationMs: Long,
    positionProvider: () -> Long,
    onSeek: (Long) -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }

    // The artwork "breathes": full size while playing, gently receded while paused.
    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow),
        label = "artScale",
    )

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = offsetY.value
                val p = (offsetY.value / size.height).coerceIn(0f, 1f)
                alpha = 1f - p * 0.5f
                val s = 1f - p * 0.06f
                scaleX = s; scaleY = s
            }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    scope.launch { offsetY.snapTo((offsetY.value + delta).coerceAtLeast(0f)) }
                },
                onDragStopped = { velocity ->
                    if (offsetY.value > 260f || velocity > 1800f) onClose()
                    else offsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium))
                },
            )
    ) {
        AmbientBackground(accent)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.Rounded.KeyboardArrowDown, "Close", 44, onClose)
                Text(
                    "Now playing",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = Obsidian.TextSecondary,
                )
                Spacer(Modifier.size(44.dp))
            }

            Spacer(Modifier.weight(0.6f))

            Artwork(
                track,
                RoundedCornerShape(32.dp),
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer { scaleX = artScale; scaleY = artScale }
                    .shadow(32.dp, RoundedCornerShape(32.dp), ambientColor = accent, spotColor = accent),
            )

            Spacer(Modifier.weight(0.7f))

            Text(
                track.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Obsidian.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                track.artist,
                style = MaterialTheme.typography.titleMedium,
                color = Obsidian.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )

            Spacer(Modifier.height(22.dp))

            GlassSeekBar(positionProvider, durationMs, accent, onSeek)

            Spacer(Modifier.height(18.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val shuffleTint by animateColorAsState(
                    if (shuffle) accent else Obsidian.TextTertiary, tween(250), label = "shuffleTint",
                )
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = shuffleTint,
                    modifier = Modifier
                        .bouncyClick(0.85f, onShuffle)
                        .padding(12.dp)
                        .size(24.dp),
                )
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Obsidian.TextPrimary,
                    modifier = Modifier
                        .bouncyClick(0.85f, onPrevious)
                        .padding(10.dp)
                        .size(40.dp),
                )
                Box(
                    Modifier
                        .size(76.dp)
                        .bouncyClick(0.92f, onToggle)
                        .glass(CircleShape, fillAlpha = 0.14f, borderAlpha = 0.5f)
                        .background(accent.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center,
                ) {
                    PlayPauseIcon(isPlaying, size = 38, tint = Color.White)
                }
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = Obsidian.TextPrimary,
                    modifier = Modifier
                        .bouncyClick(0.85f, onNext)
                        .padding(10.dp)
                        .size(40.dp),
                )
                FavoriteStar(favorite = favorite, onClick = onFavorite)
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Star that pops with a springy overshoot when a song is added to Favorites. */
@Composable
private fun FavoriteStar(favorite: Boolean, onClick: () -> Unit) {
    val pop = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(favorite) {
        if (first) {
            first = false
        } else if (favorite) {
            pop.snapTo(0.6f)
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
    }
    val tint by animateColorAsState(
        // Same neutral tones as the other controls: filled and bright when starred.
        if (favorite) Obsidian.TextPrimary else Obsidian.TextTertiary,
        tween(220),
        label = "starTint",
    )
    Icon(
        if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
        contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
        tint = tint,
        modifier = Modifier
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .bouncyClick(0.85f, onClick)
            .padding(12.dp)
            .size(24.dp),
    )
}

@Composable
fun GlassIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, size: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .bouncyClick(0.9f, onClick)
            .glass(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Obsidian.TextPrimary, modifier = Modifier.size((size * 0.55f).dp))
    }
}

/** Slim glass seek bar: tap to jump, drag to scrub. The thumb swells while touched. */
@Composable
private fun GlassSeekBar(
    positionProvider: () -> Long,
    durationMs: Long,
    accent: Color,
    onSeek: (Long) -> Unit,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val dragging = dragFraction != null
    val thumb by animateDpAsState(
        if (dragging) 16.dp else 10.dp,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "thumb",
    )
    val dur = durationMs.coerceAtLeast(1L)

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .pointerInput(dur) {
                    detectTapGestures { o -> onSeek((o.x / size.width * dur).toLong().coerceIn(0L, dur)) }
                }
                .pointerInput(dur) {
                    detectHorizontalDragGestures(
                        onDragStart = { o -> dragFraction = (o.x / size.width).coerceIn(0f, 1f) },
                        onDragCancel = { dragFraction = null },
                        onDragEnd = {
                            dragFraction?.let { onSeek((it * dur).toLong()) }
                            dragFraction = null
                        },
                        onHorizontalDrag = { change, _ ->
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                    )
                }
                .drawBehind {
                    val f = dragFraction ?: (positionProvider().toFloat() / dur).coerceIn(0f, 1f)
                    val h = 6.dp.toPx()
                    val y = (size.height - h) / 2f
                    drawRoundRect(
                        Color.White.copy(alpha = 0.14f),
                        topLeft = Offset(0f, y), size = Size(size.width, h),
                        cornerRadius = CornerRadius(h / 2),
                    )
                    drawRoundRect(
                        accent,
                        topLeft = Offset(0f, y), size = Size(size.width * f, h),
                        cornerRadius = CornerRadius(h / 2),
                    )
                    drawCircle(
                        Color.White,
                        radius = thumb.toPx() / 2f,
                        center = Offset(size.width * f, size.height / 2f),
                    )
                }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shown = dragFraction?.let { (it * dur).toLong() } ?: positionProvider()
            Text(formatTime(shown), style = MaterialTheme.typography.labelMedium, color = Obsidian.TextTertiary)
            Text(formatTime(dur), style = MaterialTheme.typography.labelMedium, color = Obsidian.TextTertiary)
        }
    }
}
