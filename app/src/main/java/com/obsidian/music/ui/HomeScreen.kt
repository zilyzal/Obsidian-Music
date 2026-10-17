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
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onViewChange: (LibraryView) -> Unit,
    onPlay: (Track) -> Unit,
    onShufflePlay: () -> Unit,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        contentPadding = PaddingValues(top = top + 12.dp, bottom = bottom + bottomPadding + 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "header", contentType = "header") {
            Header(
                view = view,
                shuffle = shuffle,
                canShuffle = tracks.isNotEmpty(),
                accent = accent,
                menuOpen = menuOpen,
                onMenuOpenChange = onMenuOpenChange,
                onViewChange = onViewChange,
                onShuffle = onShufflePlay,
            )
        }
        if (tracks.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                StateCard(
                    icon = Icons.Rounded.Star,
                    title = "Nothing starred yet",
                    body = "Open a song's player and tap the star to keep it here.",
                    accent = accent,
                    actionLabel = "Browse Latest",
                    onAction = { onViewChange(LibraryView.LATEST) },
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .padding(top = 56.dp),
                )
            }
        }
        items(tracks, key = { it.id }, contentType = { "track" }) { track ->
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
    canShuffle: Boolean,
    accent: Color,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onViewChange: (LibraryView) -> Unit,
    onShuffle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ViewPill(
            view = view,
            accent = accent,
            open = menuOpen,
            onOpenChange = onMenuOpenChange,
            onSelect = onViewChange,
        )
        Spacer(Modifier.weight(1f))
        ShufflePill(active = shuffle, enabled = canShuffle, accent = accent, onClick = onShuffle)
    }
}

/** Glass pill showing the current view. Tap to open the menu of other views. */
@Composable
private fun ViewPill(
    view: LibraryView,
    accent: Color,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onSelect: (LibraryView) -> Unit,
) {
    // Keeps the popup composed until its exit animation has finished.
    val menuState = remember { MutableTransitionState(false) }
    menuState.targetState = open

    Box {
        PillBody(
            view = view,
            accent = accent,
            open = false,
            modifier = Modifier.bouncyClick(pressedScale = 0.95f) { onOpenChange(true) },
        )

        if (menuState.currentState || menuState.targetState) {
            // The popup sits exactly over the pill: a raised copy of it, with the menu underneath.
            // The screen behind is dimmed by the host (see ObsidianApp).
            Popup(
                popupPositionProvider = AtAnchor,
                onDismissRequest = { onOpenChange(false) },
                properties = PopupProperties(focusable = true),
            ) {
                Column {
                    PillBody(
                        view = view,
                        accent = accent,
                        open = true,
                        modifier = Modifier.bouncyClick(pressedScale = 0.95f) { onOpenChange(false) },
                    )
                    Spacer(Modifier.height(10.dp))
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
                                onOpenChange(false)
                                onSelect(v)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillBody(view: LibraryView, accent: Color, open: Boolean, modifier: Modifier = Modifier) {
    val chevron by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "chevron",
    )
    val look = if (open) {
        Modifier.glassPanel(CircleShape, accent, tintAlpha = 0.22f, sheen = 0.18f, edge = 0.40f)
    } else {
        Modifier.glass(CircleShape)
    }
    Row(
        modifier
            .then(look)
            .height(46.dp)
            .padding(start = 22.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AnimatedContent(
            targetState = view,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
            label = "viewLabel",
        ) { v ->
            Text(v.label, style = ObsidianType.Pill, color = Obsidian.TextPrimary)
        }
        Icon(
            Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (open) "Close menu" else "Change view",
            tint = if (open) Obsidian.TextPrimary else Obsidian.TextSecondary,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = chevron },
        )
    }
}

/** Puts the popup's top-left corner on the pill's top-left corner. */
private object AtAnchor : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
        y = anchorBounds.top,
    )
}

@Composable
private fun ViewMenu(selected: LibraryView, accent: Color, onSelect: (LibraryView) -> Unit) {
    Column(
        Modifier
            .width(236.dp)
            .glassPanel(RoundedCornerShape(32.dp), accent, tintAlpha = 0.14f, sheen = 0.11f, edge = 0.34f)
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
        if (selected) accent.copy(alpha = 0.24f) else Color.Transparent,
        tween(200),
        label = "menuRow",
    )
    val edge by animateColorAsState(
        if (selected) accent.copy(alpha = 0.38f) else Color.Transparent,
        tween(200),
        label = "menuRowEdge",
    )
    val shape = RoundedCornerShape(25.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(pressedScale = 0.97f, onClick = onClick)
            .clip(shape)
            .background(bg)
            .border(1.dp, edge, shape)
            .padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (selected) 0.12f else 0.07f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                view.icon(),
                contentDescription = null,
                tint = if (selected) Obsidian.TextPrimary else Color(0xFFB9B8C6),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            view.label,
            style = ObsidianType.RowTitle,
            color = Obsidian.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = Obsidian.TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun LibraryView.icon(): ImageVector = when (this) {
    LibraryView.LATEST -> Icons.Rounded.Schedule
    LibraryView.FAVORITES -> Icons.Rounded.Star
    LibraryView.OLDEST -> Icons.Rounded.History
    LibraryView.LONGEST -> Icons.Rounded.Timer
}

@Composable
private fun ShufflePill(active: Boolean, enabled: Boolean, accent: Color, onClick: () -> Unit) {
    val tint by animateColorAsState(
        if (active) accent.copy(alpha = 0.35f) else Color.Transparent,
        tween(300),
        label = "shuffleTint",
    )
    Row(
        Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .then(if (enabled) Modifier.bouncyClick(pressedScale = 0.92f, onClick = onClick) else Modifier)
            .glass(CircleShape, tint = tint)
            .height(46.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Obsidian.TextPrimary, modifier = Modifier.size(19.dp))
        Text("Shuffle", style = ObsidianType.Button, color = Obsidian.TextPrimary)
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
        if (isCurrent) accent.copy(alpha = 0.30f) else Color.Transparent,
        tween(350),
        label = "rowEdge",
    )
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .bouncyClick(pressedScale = 0.975f, onClick = onClick)
            .clip(shape)
            .background(highlight)
            .border(1.dp, edge, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(track, RoundedCornerShape(15.dp), Modifier.size(52.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = ObsidianType.RowTitle,
                color = Obsidian.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track.artist,
                style = ObsidianType.RowSub,
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
                style = ObsidianType.Duration,
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
