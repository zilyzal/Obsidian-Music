package com.obsidian.music.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.obsidian.music.QueueEntry
import com.obsidian.music.data.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class PlayerSheet { None, UpNext, Sleep }

@Composable
fun NowPlaying(
    track: Track,
    isPlaying: Boolean,
    shuffle: Boolean,
    repeatOne: Boolean,
    favorite: Boolean,
    accent: Color,
    playingFrom: String,
    durationMs: Long,
    positionProvider: () -> Long,
    upNext: List<QueueEntry>,
    sleepAtMs: Long?,
    sleepAfterSong: Boolean,
    onSeek: (Long) -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onJumpTo: (QueueEntry) -> Unit,
    onSleep: (Int) -> Unit,
    onSleepAfterSong: () -> Unit,
    onClose: () -> Unit,
    artModifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val dismiss = remember { Animatable(0f) }
    var sheet by remember { mutableStateOf(PlayerSheet.None) }
    BackHandler(enabled = sheet != PlayerSheet.None) { sheet = PlayerSheet.None }

    // Two layers: the coloured background (refracted by the controls) and the whole player
    // (refracted by the sheets that slide over it).
    val backgroundLayer = rememberLayerBackdrop()
    val playerLayer = rememberLayerBackdrop()

    // The artwork "breathes": full size while playing, gently receded while paused.
    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.88f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "artScale",
    )

    // Remaining sleep time, refreshed every 20 s only while a timer is set.
    val sleepLeftMin by produceState<Int?>(null, sleepAtMs) {
        if (sleepAtMs == null) { value = null; return@produceState }
        while (true) {
            value = (((sleepAtMs - System.currentTimeMillis()) + 59_999L) / 60_000L).toInt().coerceAtLeast(0)
            delay(20_000L)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                // Layout-phase offset (not graphicsLayer), so the shared artwork knows where it is
                // when the player is dragged down and closed.
                .offset { IntOffset(0, dismiss.value.roundToInt()) }
                .graphicsLayer {
                    val p = (dismiss.value / size.height).coerceIn(0f, 1f)
                    alpha = 1f - p * 0.4f
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        scope.launch { dismiss.snapTo((dismiss.value + delta).coerceAtLeast(0f)) }
                    },
                    onDragStopped = { velocity ->
                        if (dismiss.value > 240f || velocity > 1800f) onClose()
                        else dismiss.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium))
                    },
                )
                .layerBackdrop(playerLayer),
        ) {
            PlayerBackground(track, accent, Modifier.layerBackdrop(backgroundLayer))

            CompositionLocalProvider(LocalGlassBackdrop provides backgroundLayer) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlassRoundButton(Icons.Rounded.KeyboardArrowDown, "Close player", onClose, iconSize = 26.dp)
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PLAYING FROM", style = ObsidianType.Overline, color = Obsidian.TextSecondary)
                            Text(
                                playingFrom,
                                style = ObsidianType.MiniTitle,
                                color = Obsidian.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(Modifier.size(TopBarHeight))
                    }

                    SwipeArtwork(
                        track = track,
                        artScale = { artScale },
                        accent = accent,
                        artModifier = artModifier,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )

                    // Title and artist slide in the direction the queue moved.
                    AnimatedContent(
                        targetState = track,
                        contentKey = { it.id },
                        transitionSpec = {
                            (fadeIn(tween(260, delayMillis = 40)) + slideInHorizontally(spring(dampingRatio = 0.8f, stiffness = 380f)) { it / 6 }) togetherWith
                                (fadeOut(tween(120)) + slideOutHorizontally(tween(160)) { -it / 6 })
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        label = "playerTitle",
                    ) { t ->
                        Column(Modifier.fillMaxWidth()) {
                            Text(t.title, style = ObsidianType.PlayerTitle, color = Obsidian.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                t.artist,
                                style = ObsidianType.PlayerArtist,
                                color = Obsidian.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    LiquidSeekBar(positionProvider, durationMs, accent, onSeek, Modifier.padding(horizontal = 8.dp))
                    Spacer(Modifier.height(8.dp))

                    Row(
                        Modifier.fillMaxWidth().height(96.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val shuffleTint by animateColorAsState(if (shuffle) accent else Obsidian.TextTertiary, tween(250), label = "shuffleTint")
                        Box(Modifier.size(48.dp).bouncyClick(0.8f, onShuffle), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = if (shuffle) "Shuffle on" else "Shuffle off",
                                tint = shuffleTint,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        SkipButton(Icons.Rounded.SkipPrevious, "Previous", direction = -1f, onClick = onPrevious)
                        PlayLens(isPlaying, accent, onToggle)
                        SkipButton(Icons.Rounded.SkipNext, "Next", direction = 1f, onClick = onNext)
                        FavoriteStar(favorite = favorite, accent = accent, onClick = onFavorite)
                    }

                    Spacer(Modifier.height(6.dp))
                    UtilityBar(
                        repeatOne = repeatOne,
                        sleepActive = sleepAtMs != null || sleepAfterSong,
                        sleepLabel = when {
                            sleepAfterSong -> "End"
                            sleepLeftMin != null -> "${sleepLeftMin}m"
                            else -> null
                        },
                        upNextCount = upNext.size,
                        accent = accent,
                        onRepeat = onRepeat,
                        onSleep = { sheet = PlayerSheet.Sleep },
                        onUpNext = { sheet = PlayerSheet.UpNext },
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }
        }

        // Sheets over the player. They refract the whole player beneath them.
        AnimatedVisibility(
            visible = sheet != PlayerSheet.None,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { sheet = PlayerSheet.None }
            )
        }
        CompositionLocalProvider(LocalGlassBackdrop provides playerLayer) {
            var shown by remember { mutableStateOf(PlayerSheet.UpNext) }
            if (sheet != PlayerSheet.None) shown = sheet
            AnimatedVisibility(
                visible = sheet != PlayerSheet.None,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(spring(dampingRatio = 0.78f, stiffness = 340f)) { it } + fadeIn(tween(120)),
                exit = slideOutVertically(spring(dampingRatio = 1f, stiffness = 500f)) { it } + fadeOut(tween(200, delayMillis = 60)),
            ) {
                when (shown) {
                    PlayerSheet.Sleep -> SleepSheet(
                        sleepLeftMin = sleepLeftMin,
                        afterSong = sleepAfterSong,
                        accent = accent,
                        onPick = { m ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSleep(m)
                            sheet = PlayerSheet.None
                        },
                        onAfterSong = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSleepAfterSong()
                            sheet = PlayerSheet.None
                        },
                    )
                    else -> UpNextSheet(
                        entries = upNext,
                        accent = accent,
                        onJump = { e ->
                            onJumpTo(e)
                            sheet = PlayerSheet.None
                        },
                    )
                }
            }
        }
    }
}

/**
 * The cover itself, blown up and heavily blurred, so the player is washed in the song's colours
 * and every glass control has something rich to refract.
 */
@Composable
private fun PlayerBackground(track: Track, accent: Color, modifier: Modifier = Modifier) {
    val blurOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(modifier.fillMaxSize().background(Obsidian.Base)) {
        AmbientBackground(accent, backdrop = AmbientScene.Player)
        Artwork(
            track,
            RectangleShape,
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.35f; scaleY = 1.35f
                    alpha = if (blurOk) 0.62f else 0.18f
                }
                .then(if (blurOk) Modifier.blur(80.dp) else Modifier),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Obsidian.Base.copy(alpha = 0.25f),
                        0.45f to Obsidian.Base.copy(alpha = 0.10f),
                        1f to Obsidian.Base.copy(alpha = 0.72f),
                    )
                )
        )
    }
}

/** Big square cover. Drag sideways to skip: it tilts with the finger and flies off. */
@Composable
private fun SwipeArtwork(
    track: Track,
    artScale: () -> Float,
    accent: Color,
    artModifier: Modifier,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val dragX = remember { Animatable(0f) }
    BoxWithConstraints(modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, (maxHeight - 28.dp).coerceAtLeast(0.dp))
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val shape = RoundedCornerShape(36.dp)
        Box(
            Modifier
                .size(side)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { d -> scope.launch { dragX.snapTo(dragX.value + d) } },
                    onDragStopped = { v ->
                        val x = dragX.value
                        if (abs(x) > widthPx * 0.28f || abs(v) > 2600f) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val dir = if ((if (abs(x) > widthPx * 0.28f) x else v) < 0) -1f else 1f
                            dragX.animateTo(dir * widthPx * 1.1f, tween(170))
                            if (dir < 0) onNext() else onPrevious()
                            dragX.snapTo(-dir * widthPx * 0.9f)
                            dragX.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 300f))
                        } else {
                            dragX.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Artwork(
                track,
                shape,
                artModifier
                    .size(side)
                    .graphicsLayer {
                        val s = artScale() * (1f - (abs(dragX.value) / widthPx).coerceIn(0f, 1f) * 0.12f)
                        scaleX = s; scaleY = s
                        translationX = dragX.value
                        rotationZ = dragX.value / widthPx * 9f
                        shadowElevation = 34.dp.toPx()
                        this.shape = shape
                        clip = true
                        ambientShadowColor = accent
                        spotShadowColor = accent
                    },
                large = true,
            )
        }
    }
}

/** The big play button: a thick, accent-tinted lens. */
@Composable
private fun PlayLens(isPlaying: Boolean, accent: Color, onToggle: () -> Unit) {
    val press = rememberLiquidPress()
    Box(
        Modifier
            .size(80.dp)
            .shadow(26.dp, CircleShape, ambientColor = accent, spotColor = accent)
            .liquidClickable(press, onClick = onToggle)
            .liquidGlass(CircleShape, GlassStyle.Lens, tint = accent, tintAlpha = 0.36f, rim = 1.3f, press = { press.value }),
        contentAlignment = Alignment.Center,
    ) {
        PlayPauseIcon(isPlaying, size = 38, tint = Color.White)
    }
}

/** Skip arrow that nudges in its direction when tapped. */
@Composable
private fun SkipButton(icon: ImageVector, description: String, direction: Float, onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val nudge = remember { Animatable(0f) }
    Box(
        Modifier
            .size(58.dp)
            .bouncyClick(0.82f) {
                onClick()
                scope.launch {
                    nudge.snapTo(0f)
                    nudge.animateTo(1f, tween(90))
                    nudge.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 420f))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = Obsidian.TextPrimary,
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer { translationX = nudge.value * direction * 9.dp.toPx() },
        )
    }
}

/** Star that pops with a springy overshoot and a soft glow when a song is added to Favorites. */
@Composable
private fun FavoriteStar(favorite: Boolean, accent: Color, onClick: () -> Unit) {
    val pop = remember { Animatable(1f) }
    val spin = remember { Animatable(0f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(favorite) {
        if (first) {
            first = false
        } else if (favorite) {
            launch { spin.snapTo(-40f); spin.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 300f)) }
            pop.snapTo(0.5f)
            pop.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessMedium))
        } else {
            pop.snapTo(0.8f)
            pop.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
        }
    }
    val tint by animateColorAsState(if (favorite) Obsidian.TextPrimary else Obsidian.TextTertiary, tween(220), label = "starTint")
    val glow by animateFloatAsState(if (favorite) 1f else 0f, tween(400), label = "starGlow")
    Box(
        Modifier
            .size(48.dp)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = spin.value }
            .bouncyClick(0.8f, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(48.dp)) {
            if (glow > 0f) {
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.35f * glow), Color.Transparent)),
                    radius = size.minDimension / 2f,
                )
            }
        }
        Icon(
            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
            tint = tint,
            modifier = Modifier.size(27.dp),
        )
    }
}

/** Glass capsule with repeat-one, sleep timer and Up Next. */
@Composable
private fun UtilityBar(
    repeatOne: Boolean,
    sleepActive: Boolean,
    sleepLabel: String?,
    upNextCount: Int,
    accent: Color,
    onRepeat: () -> Unit,
    onSleep: () -> Unit,
    onUpNext: () -> Unit,
) {
    Row(
        Modifier
            .liquidGlass(CircleShape, GlassStyle.Pill)
            .height(48.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        UtilityChip(
            icon = if (repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            label = if (repeatOne) "One" else null,
            active = repeatOne,
            accent = accent,
            description = if (repeatOne) "Repeat one on" else "Repeat one off",
            onClick = onRepeat,
        )
        UtilityChip(
            icon = Icons.Rounded.Bedtime,
            label = sleepLabel,
            active = sleepActive,
            accent = accent,
            description = "Sleep timer",
            onClick = onSleep,
        )
        UtilityChip(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            label = null,
            active = false,
            accent = accent,
            description = "Up next, $upNextCount songs",
            onClick = onUpNext,
        )
    }
}

@Composable
private fun UtilityChip(
    icon: ImageVector,
    label: String?,
    active: Boolean,
    accent: Color,
    description: String,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(if (active) accent.copy(alpha = 0.30f) else Color.Transparent, tween(260), label = "chipBg")
    val fg by animateColorAsState(if (active) Obsidian.TextPrimary else Obsidian.TextSecondary, tween(260), label = "chipFg")
    Row(
        Modifier
            .height(38.dp)
            .bouncyClick(0.88f, onClick)
            .background(bg, CircleShape)
            .animateContentSize(spring(dampingRatio = 0.7f, stiffness = 420f))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = description, tint = fg, modifier = Modifier.size(21.dp))
        AnimatedVisibility(visible = label != null) {
            Text(
                label.orEmpty(),
                style = ObsidianType.Caption,
                color = fg,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/**
 * Seek bar with a clear glass thumb. The bar is drawn into its own little layer and the thumb
 * refracts it, so the progress line visibly bends and magnifies under your finger.
 */
@Composable
private fun LiquidSeekBar(
    positionProvider: () -> Long,
    durationMs: Long,
    accent: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val dragging = dragFraction != null
    val thumbW by animateDpAsState(if (dragging) 44.dp else 26.dp, spring(dampingRatio = 0.5f, stiffness = 500f), label = "thumbW")
    val thumbH by animateDpAsState(if (dragging) 30.dp else 18.dp, spring(dampingRatio = 0.5f, stiffness = 500f), label = "thumbH")
    val solid by animateFloatAsState(if (dragging) 0.06f else 0.92f, tween(220), label = "thumbSolid")
    val barH by animateDpAsState(if (dragging) 9.dp else 6.dp, spring(dampingRatio = 0.6f, stiffness = 500f), label = "barH")
    val dur = durationMs.coerceAtLeast(1L)
    val seekLayer = rememberLayerBackdrop()
    val haptics = LocalHapticFeedback.current

    fun fraction(): Float = dragFraction ?: (positionProvider().toFloat() / dur).coerceIn(0f, 1f)

    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(dur) {
                    detectTapGestures { o -> onSeek((o.x / size.width * dur).toLong().coerceIn(0L, dur)) }
                }
                .pointerInput(dur) {
                    detectHorizontalDragGestures(
                        onDragStart = { o ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            dragFraction = (o.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragCancel = { dragFraction = null },
                        onDragEnd = {
                            dragFraction?.let { onSeek((it * dur).toLong()) }
                            dragFraction = null
                        },
                        onHorizontalDrag = { change, _ ->
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Canvas(Modifier.fillMaxSize().layerBackdrop(seekLayer)) {
                val h = barH.toPx()
                val y = (size.height - h) / 2f
                val x = size.width * fraction()
                drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(0f, y), Size(size.width, h), CornerRadius(h / 2))
                drawRoundRect(
                    Brush.horizontalGradient(listOf(accent, Color(0xFFF3EEFF)), startX = 0f, endX = size.width.coerceAtLeast(1f)),
                    Offset(0f, y),
                    Size(x, h),
                    CornerRadius(h / 2),
                )
            }
            Box(
                Modifier
                    .thumbPlacement { fraction() }
                    .size(width = thumbW, height = thumbH)
                    .then(
                        if (TrueGlassSupported) {
                            Modifier.liquidGlass(
                                CircleShape,
                                GlassStyle.Thumb,
                                tint = Color.White,
                                tintAlpha = solid,
                                rim = 1.2f,
                                backdrop = seekLayer,
                            )
                        } else {
                            Modifier.background(Color.White, CircleShape)
                        }
                    )
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shown = dragFraction?.let { (it * dur).toLong() } ?: positionProvider()
            Text(formatTime(shown), style = ObsidianType.Duration, color = Obsidian.TextSecondary)
            Text("-" + formatTime((dur - shown).coerceAtLeast(0L)), style = ObsidianType.Duration, color = Obsidian.TextSecondary)
        }
    }
}

/** Up Next: the songs coming in play order. Tap one to jump to it. */
@Composable
private fun UpNextSheet(entries: List<QueueEntry>, accent: Color, onJump: (QueueEntry) -> Unit) {
    SheetFrame(title = "Up next", subtitle = if (entries.isEmpty()) "Nothing queued after this song" else "${entries.size} songs") {
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(entries, key = { it.windowIndex }) { e ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .bouncyClick(0.97f) { onJump(e) }
                        .frostRow(RoundedCornerShape(20.dp), selected = false, accent = accent)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(e.track, RoundedCornerShape(13.dp), Modifier.size(44.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.track.title, style = ObsidianType.MiniTitle, color = Obsidian.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(e.track.artist, style = ObsidianType.MiniSub, color = Obsidian.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(formatTime(e.track.durationMs), style = ObsidianType.Duration, color = Obsidian.TextTertiary)
                }
            }
        }
    }
}

/** Sleep timer choices. */
@Composable
private fun SleepSheet(
    sleepLeftMin: Int?,
    afterSong: Boolean,
    accent: Color,
    onPick: (Int) -> Unit,
    onAfterSong: () -> Unit,
) {
    val status = when {
        afterSong -> "Stops after this song"
        sleepLeftMin != null -> "Pauses in $sleepLeftMin min"
        else -> "Off"
    }
    SheetFrame(title = "Sleep timer", subtitle = status) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(15, 30, 45, 60).forEach { m ->
                SheetOption("$m minutes", selected = false, accent = accent) { onPick(m) }
            }
            SheetOption("End of this song", selected = afterSong, accent = accent, onClick = onAfterSong)
            if (afterSong || sleepLeftMin != null) {
                SheetOption("Turn off", selected = false, accent = accent) { onPick(0) }
            }
        }
    }
}

@Composable
private fun SheetOption(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(0.97f, onClick)
            .frostRow(RoundedCornerShape(20.dp), selected = selected, accent = accent)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = ObsidianType.RowTitle, color = Obsidian.TextPrimary, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Obsidian.TextPrimary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SheetFrame(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(10.dp)
            .liquidGlass(RoundedCornerShape(34.dp), GlassStyle.Sheet)
            // Taps inside the sheet must not reach the scrim behind it.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 16.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 38.dp, height = 5.dp)
                .background(Color.White.copy(alpha = 0.28f), CircleShape)
        )
        Spacer(Modifier.height(14.dp))
        Text(title, style = ObsidianType.CardTitle, color = Obsidian.TextPrimary, modifier = Modifier.padding(start = 6.dp))
        Text(subtitle, style = ObsidianType.Caption, color = Obsidian.TextSecondary, modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 14.dp))
        content()
    }
}

/** Places the thumb along the full width at [fraction], in the layout phase (no recomposition). */
private fun Modifier.thumbPlacement(fraction: () -> Float): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val width = constraints.maxWidth
    layout(width, p.height) {
        val x = ((width - p.width) * fraction().coerceIn(0f, 1f)).roundToInt()
        p.place(x, 0)
    }
}
