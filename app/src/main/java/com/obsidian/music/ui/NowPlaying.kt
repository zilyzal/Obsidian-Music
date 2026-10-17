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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
    playingFrom: String,
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
        AmbientBackground(accent, backdrop = Backdrop.Player)
        Box(Modifier.fillMaxSize().background(Obsidian.Base.copy(alpha = 0.22f)))

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.Rounded.KeyboardArrowDown, "Close player", onClose)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "PLAYING FROM",
                        style = ObsidianType.Overline,
                        color = Obsidian.TextSecondary,
                    )
                    Text(
                        playingFrom,
                        style = ObsidianType.MiniTitle,
                        color = Obsidian.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.size(46.dp))
            }

            // The artwork takes whatever room is left and stays square, so short screens still fit.
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                val side = minOf(maxWidth, (maxHeight - 24.dp).coerceAtLeast(0.dp))
                val shape = RoundedCornerShape(38.dp)
                Artwork(
                    track,
                    shape,
                    Modifier
                        .size(side)
                        .graphicsLayer { scaleX = artScale; scaleY = artScale }
                        .shadow(36.dp, shape, ambientColor = accent, spotColor = accent),
                    large = true,
                )
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text(
                    track.title,
                    style = ObsidianType.PlayerTitle,
                    color = Obsidian.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    track.artist,
                    style = ObsidianType.PlayerArtist,
                    color = Obsidian.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                )
                Spacer(Modifier.height(20.dp))
                GlassSeekBar(positionProvider, durationMs, accent, onSeek)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(88.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val shuffleTint by animateColorAsState(
                    if (shuffle) accent else Obsidian.TextTertiary, tween(250), label = "shuffleTint",
                )
                Box(
                    Modifier
                        .size(48.dp)
                        .bouncyClick(0.85f, onShuffle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = if (shuffle) "Shuffle on" else "Shuffle off",
                        tint = shuffleTint,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Box(
                    Modifier
                        .size(56.dp)
                        .bouncyClick(0.85f, onPrevious),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Obsidian.TextPrimary,
                        modifier = Modifier.size(38.dp),
                    )
                }
                Box(
                    Modifier
                        .size(88.dp)
                        .shadow(24.dp, CircleShape, ambientColor = accent, spotColor = accent)
                        .bouncyClick(0.92f, onToggle)
                        .glassPanel(CircleShape, accent, tintAlpha = 0.42f, sheen = 0.26f, edge = 0.50f),
                    contentAlignment = Alignment.Center,
                ) {
                    PlayPauseIcon(isPlaying, size = 38, tint = Color.White)
                }
                Box(
                    Modifier
                        .size(56.dp)
                        .bouncyClick(0.85f, onNext),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Obsidian.TextPrimary,
                        modifier = Modifier.size(38.dp),
                    )
                }
                FavoriteStar(favorite = favorite, onClick = onFavorite)
            }

            Spacer(Modifier.height(24.dp))
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
    Box(
        Modifier
            .size(48.dp)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .bouncyClick(0.85f, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
            tint = tint,
            modifier = Modifier.size(27.dp),
        )
    }
}

@Composable
fun GlassIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .bouncyClick(0.9f, onClick)
            .glass(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Obsidian.TextPrimary, modifier = Modifier.size(26.dp))
    }
}

/** Slim seek bar: tap to jump, drag to scrub. The thumb swells while touched. */
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
        if (dragging) 22.dp else 18.dp,
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
                    val r = thumb.toPx() / 2f
                    // Keep the thumb inside the bar's ends.
                    val x = r + (size.width - 2f * r) * f
                    drawRoundRect(
                        Color.White.copy(alpha = 0.16f),
                        topLeft = Offset(0f, y), size = Size(size.width, h),
                        cornerRadius = CornerRadius(h / 2),
                    )
                    drawRoundRect(
                        Brush.horizontalGradient(
                            listOf(accent, Color(0xFFEDE6FF)),
                            startX = 0f,
                            endX = size.width.coerceAtLeast(1f),
                        ),
                        topLeft = Offset(0f, y), size = Size(x, h),
                        cornerRadius = CornerRadius(h / 2),
                    )
                    drawCircle(
                        accent.copy(alpha = 0.30f),
                        radius = r + 4.dp.toPx(),
                        center = Offset(x, size.height / 2f),
                    )
                    drawCircle(
                        Color.White,
                        radius = r,
                        center = Offset(x, size.height / 2f),
                    )
                }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shown = dragFraction?.let { (it * dur).toLong() } ?: positionProvider()
            Text(formatTime(shown), style = ObsidianType.Duration, color = Obsidian.TextSecondary)
            Text(formatTime(dur), style = ObsidianType.Duration, color = Obsidian.TextSecondary)
        }
    }
}
