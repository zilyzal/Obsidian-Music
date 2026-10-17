package com.obsidian.music.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.obsidian.music.data.LibraryView
import com.obsidian.music.data.Track

@Composable
fun HomeScreen(
    tracks: List<Track>,
    view: LibraryView,
    current: Track?,
    isPlaying: Boolean,
    shuffle: Boolean,
    accent: Color,
    bottomPadding: Dp,
    onViewChange: (LibraryView) -> Unit,
    onPlay: (Track) -> Unit,
    onShufflePlay: () -> Unit,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        contentPadding = PaddingValues(top = top + 14.dp, bottom = bottom + bottomPadding + 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "header") {
            Header(
                view = view,
                shuffle = shuffle,
                accent = accent,
                onViewChange = onViewChange,
                onShuffle = onShufflePlay,
            )
        }
        if (tracks.isEmpty()) {
            item(key = "empty") { EmptyFavorites() }
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
private fun Header(
    view: LibraryView,
    shuffle: Boolean,
    accent: Color,
    onViewChange: (LibraryView) -> Unit,
    onShuffle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ViewPill(view = view, accent = accent, onSelect = onViewChange)
        Spacer(Modifier.weight(1f))
        ShufflePill(active = shuffle, accent = accent, onClick = onShuffle)
    }
}

/** Glass pill showing the current view. Tap to open the menu of other views. */
@Composable
private fun ViewPill(view: LibraryView, accent: Color, onSelect: (LibraryView) -> Unit) {
    var open by remember { mutableStateOf(false) }
    // Keeps the popup composed until its exit animation has finished.
    val menuState = remember { MutableTransitionState(false) }
    menuState.targetState = open

    val chevron by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "chevron",
    )
    val gap = with(LocalDensity.current) { 10.dp.roundToPx() }

    Box {
        Row(
            Modifier
                .bouncyClick(pressedScale = 0.95f) { open = true }
                .glass(CircleShape, fillAlpha = 0.12f, borderAlpha = 0.42f)
                .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AnimatedContent(
                targetState = view,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "viewLabel",
            ) { v ->
                Text(
                    v.label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Obsidian.TextPrimary,
                )
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Change view",
                tint = Obsidian.TextSecondary,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer { rotationZ = chevron },
            )
        }

        if (menuState.currentState || menuState.targetState) {
            Popup(
                popupPositionProvider = remember(gap) { BelowAnchor(gap) },
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = menuState,
                    enter = fadeIn(tween(160)) + scaleIn(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        initialScale = 0.88f,
                        transformOrigin = TransformOrigin(0f, 0f),
                    ),
                    exit = fadeOut(tween(120)) + scaleOut(
                        animationSpec = tween(140),
                        targetScale = 0.92f,
                        transformOrigin = TransformOrigin(0f, 0f),
                    ),
                ) {
                    ViewMenu(
                        selected = view,
                        accent = accent,
                        onSelect = { v ->
                            open = false
                            onSelect(v)
                        },
                    )
                }
            }
        }
    }
}

/** Puts the menu just under the pill, aligned to its left edge. */
private class BelowAnchor(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
        y = anchorBounds.bottom + gapPx,
    )
}

@Composable
private fun ViewMenu(selected: LibraryView, accent: Color, onSelect: (LibraryView) -> Unit) {
    Column(
        Modifier
            .width(232.dp)
            .glassPanel(RoundedCornerShape(28.dp), accent, tintAlpha = 0.14f)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        LibraryView.entries.forEach { v ->
            MenuRow(view = v, selected = v == selected, accent = accent, onClick = { onSelect(v) })
        }
    }
}

@Composable
private fun MenuRow(view: LibraryView, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) accent.copy(alpha = 0.22f) else Color.Transparent,
        tween(200),
        label = "menuRow",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(pressedScale = 0.97f, onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            view.icon(),
            contentDescription = null,
            tint = Obsidian.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            view.label,
            style = MaterialTheme.typography.titleMedium,
            color = Obsidian.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = Obsidian.TextPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun LibraryView.icon(): ImageVector = when (this) {
    LibraryView.LATEST -> Icons.Rounded.Schedule
    LibraryView.FAVORITES -> Icons.Rounded.Star
    LibraryView.TITLE -> Icons.Rounded.SortByAlpha
    LibraryView.ARTIST -> Icons.Rounded.Person
    LibraryView.OLDEST -> Icons.Rounded.History
    LibraryView.LONGEST -> Icons.Rounded.Timer
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
            .glass(CircleShape, fillAlpha = 0.12f, borderAlpha = 0.42f)
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
private fun EmptyFavorites() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Rounded.Star,
            contentDescription = null,
            tint = Obsidian.TextTertiary,
            modifier = Modifier.size(36.dp),
        )
        Text(
            "Nothing starred yet",
            style = MaterialTheme.typography.titleMedium,
            color = Obsidian.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            "Open a song's player and tap the star to keep it here.",
            style = MaterialTheme.typography.bodyMedium,
            color = Obsidian.TextSecondary,
            textAlign = TextAlign.Center,
        )
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
    val edge by animateColorAsState(
        if (isCurrent) accent.copy(alpha = 0.34f) else Color.Transparent,
        tween(350),
        label = "rowEdge",
    )
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .bouncyClick(pressedScale = 0.975f, onClick = onClick)
            .clip(shape)
            .background(highlight)
            .border(1.dp, edge, shape)
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
