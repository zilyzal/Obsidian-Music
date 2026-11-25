package com.obsidian.music.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.obsidian.music.data.LibraryView
import com.obsidian.music.data.Track
import com.obsidian.music.data.arranged
import com.obsidian.music.data.matching
import kotlinx.coroutines.launch

/** Height of the floating top bar's controls; the list starts below it and scrolls under it. */
val TopBarHeight = 44.dp

/**
 * The song list. It lives inside the glass backdrop layer, so the floating top bar and
 * mini player refract it as it scrolls underneath.
 *
 * Switching views cross-dissolves whole lists (with a small lift), instead of shuffling
 * hundreds of rows around.
 */
@Composable
fun HomeList(
    library: List<Track>,
    favorites: Set<Long>,
    view: LibraryView,
    query: String,
    current: Track?,
    isPlaying: Boolean,
    accent: Color,
    bottomPadding: Dp,
    onPlay: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    onBrowseLatest: () -> Unit,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    AnimatedContent(
        targetState = view,
        transitionSpec = {
            (fadeIn(tween(260, delayMillis = 60)) +
                slideInVertically(spring(dampingRatio = 0.86f, stiffness = 380f)) { it / 14 } +
                scaleIn(spring(dampingRatio = 0.86f, stiffness = 380f), initialScale = 0.97f, transformOrigin = TransformOrigin(0.5f, 0f))) togetherWith
                (fadeOut(tween(140)) + scaleOut(tween(180), targetScale = 0.985f, transformOrigin = TransformOrigin(0.5f, 0f)))
        },
        label = "viewSwitch",
    ) { v ->
        val tracks = remember(library, favorites, v, query) { library.arranged(v, favorites).matching(query) }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = top + 12.dp + TopBarHeight + 14.dp, bottom = bottom + bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (tracks.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    val searching = query.isNotBlank()
                    StateCard(
                        icon = if (searching) Icons.Rounded.SearchOff else Icons.Rounded.Star,
                        title = if (searching) "No matches" else "Nothing starred yet",
                        body = if (searching) "Nothing in ${v.label} matches “${query.trim()}”."
                        else "Open a song's player and tap the star to keep it here.",
                        accent = accent,
                        actionLabel = if (searching) null else "Browse Latest",
                        onAction = if (searching) null else onBrowseLatest,
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .padding(top = 40.dp)
                            .animateItem(),
                    )
                }
            }
            items(tracks, key = { it.id }, contentType = { "track" }) { track ->
                val isCurrent = track.id == current?.id
                TrackRow(
                    track = track,
                    isCurrent = isCurrent,
                    // Only the playing row needs to know about play/pause or the accent, so
                    // the rest of the list is skipped when they change.
                    isPlaying = isPlaying && isCurrent,
                    accent = if (isCurrent) accent else Obsidian.Accent,
                    onClick = { onPlay(track) },
                    onLongClick = { onPlayNext(track) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(220),
                        placementSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                        fadeOutSpec = tween(160),
                    ),
                )
            }
        }
    }
}

/**
 * Floating glass bar: view pill on the left, search and shuffle on the right.
 * Opening search stretches the search lens across the bar like a drop of liquid spreading out.
 */
@Composable
fun TopBar(
    view: LibraryView,
    query: String,
    searchOpen: Boolean,
    canShuffle: Boolean,
    shuffle: Boolean,
    accent: Color,
    menuOpen: Boolean,
    onOpenMenu: () -> Unit,
    onSearchOpenChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = top + 12.dp, start = 18.dp, end = 18.dp)
            .height(TopBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = !searchOpen,
            enter = fadeIn(tween(220, delayMillis = 80)) + expandHorizontally(spring(dampingRatio = 0.8f, stiffness = 380f), expandFrom = Alignment.Start),
            exit = fadeOut(tween(120)) + shrinkHorizontally(spring(dampingRatio = 0.9f, stiffness = 500f), shrinkTowards = Alignment.Start),
        ) {
            ViewPill(view = view, open = menuOpen, onClick = onOpenMenu)
        }
        Spacer(Modifier.weight(1f).width(8.dp))
        SearchLens(
            open = searchOpen,
            query = query,
            onOpenChange = onSearchOpenChange,
            onQueryChange = onQueryChange,
        )
        AnimatedVisibility(
            visible = !searchOpen,
            enter = fadeIn(tween(200, delayMillis = 100)) + scaleIn(spring(dampingRatio = 0.6f, stiffness = 420f), initialScale = 0.6f),
            exit = fadeOut(tween(100)) + scaleOut(tween(140), targetScale = 0.6f),
        ) {
            Row {
                Spacer(Modifier.width(10.dp))
                GlassRoundButton(
                    icon = Icons.Rounded.Shuffle,
                    description = "Shuffle play",
                    enabled = canShuffle,
                    tint = if (shuffle) accent else Color.Transparent,
                    tintAlpha = if (shuffle) 0.32f else 0f,
                    onClick = onShuffle,
                )
            }
        }
    }
}

/** Glass pill showing the current view. Tap to open the menu of other views. */
@Composable
private fun ViewPill(view: LibraryView, open: Boolean, onClick: () -> Unit) {
    val press = rememberLiquidPress()
    val chevron by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "chevron",
    )
    Row(
        Modifier
            .liquidClickable(press, onClick = onClick)
            .liquidGlass(CircleShape, GlassStyle.Pill, press = { press.value })
            .height(TopBarHeight)
            .animateContentSize(spring(dampingRatio = 0.75f, stiffness = 420f))
            .padding(start = 18.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        AnimatedContent(
            targetState = view,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(spring(dampingRatio = 0.7f, stiffness = 420f)) { it / 2 }) togetherWith
                    (fadeOut(tween(120)) + slideOutVertically(tween(160)) { -it / 2 })
            },
            label = "viewLabel",
        ) { v ->
            Text(v.label, style = ObsidianType.Pill, color = Obsidian.TextPrimary)
        }
        Icon(
            Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (open) "Close menu" else "Change view",
            tint = Obsidian.TextSecondary,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = chevron },
        )
    }
}

/** Round search lens that spreads into a full-width glass search field. */
@Composable
private fun SearchLens(
    open: Boolean,
    query: String,
    onOpenChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
) {
    val press = rememberLiquidPress()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(open) {
        if (open) runCatching { focus.requestFocus() } else keyboard?.hide()
    }
    Row(
        Modifier
            .then(if (open) Modifier.fillMaxWidth() else Modifier)
            .liquidClickable(press, enabled = !open) { onOpenChange(true) }
            .liquidGlass(CircleShape, GlassStyle.Pill, press = { press.value })
            .animateContentSize(spring(dampingRatio = 0.72f, stiffness = 340f))
            .height(TopBarHeight)
            .then(if (open) Modifier.fillMaxWidth() else Modifier.width(TopBarHeight)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(TopBarHeight), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Obsidian.TextPrimary, modifier = Modifier.size(21.dp))
        }
        if (open) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text("Songs, artists, albums", style = ObsidianType.BodySmall, color = Obsidian.TextTertiary, maxLines = 1)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = ObsidianType.BodySmall.copy(color = Obsidian.TextPrimary),
                    cursorBrush = SolidColor(Obsidian.TextPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                )
            }
            Box(
                Modifier
                    .size(TopBarHeight)
                    .bouncyClick(0.85f) { onOpenChange(false) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close search", tint = Obsidian.TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Round liquid-glass icon button. */
@Composable
fun GlassRoundButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = TopBarHeight,
    iconSize: Dp = 21.dp,
    enabled: Boolean = true,
    tint: Color = Color.Transparent,
    tintAlpha: Float = 0f,
    iconTint: Color = Obsidian.TextPrimary,
    style: GlassStyle = GlassStyle.Pill,
) {
    val press = rememberLiquidPress()
    val animatedTint by animateColorAsState(tint, tween(300), label = "btnTint")
    val animatedAlpha by animateFloatAsState(tintAlpha, tween(300), label = "btnTintAlpha")
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .liquidClickable(press, enabled = enabled, onClick = onClick)
            .liquidGlass(CircleShape, style, tint = animatedTint, tintAlpha = animatedAlpha, press = { press.value })
            .size(size),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = iconTint, modifier = Modifier.size(iconSize))
    }
}

/**
 * Glass menu of views. It drops out of the pill with a springy, top-left-anchored stretch;
 * no descriptions under the items, just icon, name and a check.
 */
@Composable
fun ViewMenu(selected: LibraryView, accent: Color, onSelect: (LibraryView) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(236.dp)
            .liquidGlass(RoundedCornerShape(30.dp), GlassStyle.Panel, tint = accent, tintAlpha = 0.08f)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        LibraryView.entries.forEachIndexed { i, v ->
            // Rows cascade in one after another.
            val appear = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(30L * i)
                appear.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 420f))
            }
            MenuRow(
                view = v,
                selected = v == selected,
                accent = accent,
                onClick = { onSelect(v) },
                modifier = Modifier.graphicsLayer {
                    alpha = appear.value.coerceIn(0f, 1f)
                    translationY = (1f - appear.value) * -14.dp.toPx()
                },
            )
        }
    }
}

@Composable
private fun MenuRow(view: LibraryView, selected: Boolean, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(
        if (selected) accent.copy(alpha = 0.24f) else Color.Transparent,
        tween(200),
        label = "menuRow",
    )
    val edge by animateColorAsState(
        if (selected) Color.White.copy(alpha = 0.22f) else Color.Transparent,
        tween(200),
        label = "menuRowEdge",
    )
    val shape = RoundedCornerShape(23.dp)
    Row(
        modifier
            .fillMaxWidth()
            .bouncyClick(pressedScale = 0.96f, onClick = onClick)
            .clip(shape)
            .background(bg)
            .border(1.dp, edge, shape)
            .padding(start = 10.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (selected) 0.14f else 0.07f)),
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
        Text(view.label, style = ObsidianType.RowTitle, color = Obsidian.TextPrimary, modifier = Modifier.weight(1f))
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 500f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Obsidian.TextPrimary, modifier = Modifier.size(20.dp))
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
private fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlight by animateColorAsState(
        if (isCurrent) accent.copy(alpha = 0.15f) else Color.Transparent,
        tween(350),
        label = "rowHighlight",
    )
    val edge by animateColorAsState(
        if (isCurrent) Color.White.copy(alpha = 0.16f) else Color.Transparent,
        tween(350),
        label = "rowEdge",
    )
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .pointerInput(track.id) {
                detectTapGestures(
                    onPress = {
                        scope.launch { scale.animateTo(0.97f, spring(dampingRatio = 0.6f, stiffness = 600f)) }
                        tryAwaitRelease()
                        scope.launch { scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 380f)) }
                    },
                    onTap = { onClick() },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                )
            }
            .clip(shape)
            .background(highlight)
            .border(1.dp, edge, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(track, RoundedCornerShape(15.dp), Modifier.size(52.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = ObsidianType.RowTitle, color = Obsidian.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = ObsidianType.RowSub, color = Obsidian.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        AnimatedContent(
            targetState = isCurrent,
            transitionSpec = {
                (fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.55f, stiffness = 500f), initialScale = 0.5f)) togetherWith
                    (fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.5f))
            },
            label = "rowTrailing",
        ) { cur ->
            if (cur) {
                Equalizer(playing = isPlaying, color = accent)
            } else {
                Text(formatTime(track.durationMs), style = ObsidianType.Duration, color = Obsidian.TextTertiary)
            }
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
            listOf(420, 560, 340).forEachIndexed { i, ms ->
                val h by t.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Reverse),
                    label = "bar$i",
                )
                Bar(h, color)
            }
        } else {
            repeat(3) { Bar(0.35f, color) }
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
            .background(Brush.verticalGradient(listOf(Color.White, color)))
    )
}
