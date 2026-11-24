package com.obsidian.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.obsidian.music.ui.model.DefaultEqPresets
import com.obsidian.music.ui.model.EqBandState
import com.obsidian.music.ui.model.EqPreset
import com.obsidian.music.ui.model.LyricLine
import com.obsidian.music.ui.model.ObsidianTrack
import com.obsidian.music.ui.model.UtilitySheetType
import com.obsidian.music.ui.theme.ObsidianGlassStyle
import com.obsidian.music.ui.theme.ObsidianMotion
import com.obsidian.music.ui.theme.PillShape
import com.obsidian.music.ui.theme.liquidGelPress
import com.obsidian.music.ui.theme.obsidianInnerGlassCard
import com.obsidian.music.ui.theme.obsidianLiquidGlass
import kotlin.math.roundToInt

/**
 * Floating Liquid Glass Overlay Sheet hosting:
 * - Synced Interactive Lyrics (auto-scroll, tap-to-seek, scale & luminance depth)
 * - 5-Band Spatial Equalizer (live Bezier curve, tactile vertical glass faders, presets)
 * - Up-Next Queue (live EQ indicator, tap-to-jump, remove)
 * - Sleep Timer (refracting chips + live countdown)
 */
@Composable
fun LiquidUtilitySheetOverlay(
    sheetType: UtilitySheetType,
    backdrop: Backdrop,
    currentTrack: ObsidianTrack?,
    positionMs: Long,
    isPlaying: Boolean,
    queue: List<ObsidianTrack>,
    eqBands: List<EqBandState>,
    activeEqPreset: String,
    spatialAudioEnabled: Boolean,
    sleepTimerRemainingMs: Long?,
    onDismiss: () -> Unit,
    onSeekToMs: (Long) -> Unit,
    onJumpToQueueIndex: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onUpdateEqBand: (Int, Float) -> Unit,
    onApplyEqPreset: (EqPreset) -> Unit,
    onToggleSpatialAudio: (Boolean) -> Unit,
    onSetSleepTimerMinutes: (Int?) -> Unit,
    artworkSlot: @Composable (ObsidianTrack, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = sheetType != UtilitySheetType.NONE
    val accent = currentTrack?.accentColor ?: Color(0xFF7B61FF)

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim fade
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.44f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // Glass Drawer
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.78f,
                    stiffness = 350f
                )
            ) + fadeIn(tween(220)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(240)
            ) + fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(if (sheetType == UtilitySheetType.SLEEP_TIMER) 0.44f else 0.74f)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding()
                    .obsidianLiquidGlass(
                        backdrop = backdrop,
                        shape = RoundedCornerShape(34.dp),
                        style = ObsidianGlassStyle.ObsidianSheet,
                        accentGlowColor = accent
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.34f))
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount > 22f) onDismiss()
                            }
                        }
                )

                Spacer(Modifier.height(12.dp))

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (sheetType) {
                            UtilitySheetType.LYRICS -> "Synced Lyrics"
                            UtilitySheetType.EQUALIZER -> "Spatial Equalizer"
                            UtilitySheetType.QUEUE -> "Up Next · ${queue.size} tracks"
                            UtilitySheetType.SLEEP_TIMER -> "Sleep Timer"
                            UtilitySheetType.NONE -> ""
                        },
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .liquidGelPress()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                AnimatedContent(
                    targetState = sheetType,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "sheetContent",
                    modifier = Modifier.weight(1f)
                ) { currentType ->
                    when (currentType) {
                        UtilitySheetType.LYRICS -> SyncedLyricsContent(
                            lyrics = currentTrack?.lyrics.orEmpty(),
                            positionMs = positionMs,
                            accentColor = accent,
                            onSeekToMs = onSeekToMs
                        )
                        UtilitySheetType.EQUALIZER -> SpatialEqualizerContent(
                            bands = eqBands,
                            activePreset = activeEqPreset,
                            spatialAudioEnabled = spatialAudioEnabled,
                            accentColor = accent,
                            onUpdateBand = onUpdateEqBand,
                            onApplyPreset = onApplyEqPreset,
                            onToggleSpatial = onToggleSpatialAudio
                        )
                        UtilitySheetType.QUEUE -> UpNextQueueContent(
                            queue = queue,
                            currentTrackId = currentTrack?.id,
                            isPlaying = isPlaying,
                            accentColor = accent,
                            onJumpToIndex = onJumpToQueueIndex,
                            onRemoveIndex = onRemoveFromQueue,
                            artworkSlot = artworkSlot
                        )
                        UtilitySheetType.SLEEP_TIMER -> SleepTimerContent(
                            remainingMs = sleepTimerRemainingMs,
                            accentColor = accent,
                            onSetMinutes = onSetSleepTimerMinutes
                        )
                        UtilitySheetType.NONE -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricsContent(
    lyrics: List<LyricLine>,
    positionMs: Long,
    accentColor: Color,
    onSeekToMs: (Long) -> Unit
) {
    if (lyrics.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "No lyrics for this song",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Synced lyrics will appear here when available",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    val activeIndex = remember(lyrics, positionMs) {
        lyrics.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.05f else 0.94f,
                animationSpec = ObsidianMotion.LiquidMorphSpring,
                label = "lyricScale"
            )
            val color by animateColorAsState(
                targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.34f),
                animationSpec = tween(260),
                label = "lyricColor"
            )

            Text(
                text = line.text,
                color = color,
                fontSize = if (isActive) 24.sp else 20.sp,
                lineHeight = if (isActive) 31.sp else 26.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSeekToMs(line.timeMs) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SpatialEqualizerContent(
    bands: List<EqBandState>,
    activePreset: String,
    spatialAudioEnabled: Boolean,
    accentColor: Color,
    onUpdateBand: (Int, Float) -> Unit,
    onApplyPreset: (EqPreset) -> Unit,
    onToggleSpatial: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Preset Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DefaultEqPresets) { preset ->
                val selected = preset.name == activePreset
                Box(
                    modifier = Modifier
                        .liquidGelPress()
                        .clip(PillShape)
                        .background(
                            if (selected) accentColor.copy(alpha = 0.34f)
                            else Color.White.copy(alpha = 0.10f)
                        )
                        .border(
                            0.9.dp,
                            if (selected) Color.White.copy(alpha = 0.60f)
                            else Color.White.copy(alpha = 0.16f),
                            PillShape
                        )
                        .clickable { onApplyPreset(preset) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = preset.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // Live Smooth Frequency Response Curve
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .obsidianInnerGlassCard(RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Zero dB guide
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.dp.toPx()
                )

                if (bands.size >= 2) {
                    val points = bands.mapIndexed { idx, band ->
                        val x = (idx.toFloat() / (bands.size - 1)) * w
                        val y = midY - (band.gainNormalized.coerceIn(-1f, 1f) * (h * 0.42f))
                        Offset(x, y)
                    }

                    val curve = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.lastIndex) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }

                    val fill = Path().apply {
                        addPath(curve)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }

                    drawPath(
                        path = fill,
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.36f), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = curve,
                        brush = Brush.horizontalGradient(listOf(accentColor, Color.White, accentColor)),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    points.forEach { pt ->
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
                    }
                }
            }
        }

        // 5-Band Interactive Vertical Liquid Glass Sliders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bands.forEachIndexed { idx, band ->
                val animatedGain by animateFloatAsState(
                    targetValue = band.gainNormalized.coerceIn(-1f, 1f),
                    animationSpec = ObsidianMotion.LiquidMorphSpring,
                    label = "eqBand$idx"
                )
                val dbText = "${(animatedGain * 12f).roundToInt()}dB"

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(
                        text = dbText,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(42.dp)
                            .clip(PillShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.22f), PillShape)
                            .pointerInput(idx) {
                                detectVerticalDragGestures { change, _ ->
                                    change.consume()
                                    val fracFromBottom = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                    val newGain = (fracFromBottom * 2f) - 1f
                                    onUpdateBand(idx, newGain.coerceIn(-1f, 1f))
                                }
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val fillFraction = ((animatedGain + 1f) / 2f).coerceIn(0.08f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fillFraction)
                                .clip(PillShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.90f), accentColor)
                                    )
                                )
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = band.label,
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Spatial Glass Audio Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .obsidianInnerGlassCard(RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.SurroundSound, contentDescription = null, tint = accentColor)
                Column {
                    Text("Spatial Soundstage", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Wide virtualized stereo depth", color = Color.White.copy(alpha = 0.60f), fontSize = 11.sp)
                }
            }
            Switch(
                checked = spatialAudioEnabled,
                onCheckedChange = onToggleSpatial,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor
                )
            )
        }
    }
}

@Composable
private fun UpNextQueueContent(
    queue: List<ObsidianTrack>,
    currentTrackId: String?,
    isPlaying: Boolean,
    accentColor: Color,
    onJumpToIndex: (Int) -> Unit,
    onRemoveIndex: (Int) -> Unit,
    artworkSlot: @Composable (ObsidianTrack, Modifier) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(queue, key = { idx, t -> "${t.id}_$idx" }) { index, track ->
            val active = track.id == currentTrackId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGelPress(pressedScale = 0.98f)
                    .obsidianInnerGlassCard(
                        shape = RoundedCornerShape(18.dp),
                        selected = active,
                        accentColor = accentColor
                    )
                    .clickable { onJumpToIndex(index) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    artworkSlot(track, Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = Color.White.copy(alpha = 0.64f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (active) {
                    LiveEqualizerBars(
                        isPlaying = isPlaying,
                        tint = accentColor,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(16.dp)
                    )
                } else {
                    IconButton(onClick = { onRemoveIndex(index) }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove from queue",
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepTimerContent(
    remainingMs: Long?,
    accentColor: Color,
    onSetMinutes: (Int?) -> Unit
) {
    val options = listOf(10, 15, 30, 45, 60, 90)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .obsidianInnerGlassCard(RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Icon(Icons.Rounded.NightlightRound, contentDescription = null, tint = accentColor)
            Text(
                text = if (remainingMs != null && remainingMs > 0L) {
                    "Pausing playback in ${formatDurationMs(remainingMs)}"
                } else {
                    "Timer is off — choose a fade-out duration"
                },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { mins ->
                Box(
                    modifier = Modifier
                        .liquidGelPress()
                        .clip(PillShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(0.9.dp, Color.White.copy(alpha = 0.28f), PillShape)
                        .clickable { onSetMinutes(mins) }
                        .padding(horizontal = 18.dp, vertical = 11.dp)
                ) {
                    Text("$mins min", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (remainingMs != null) {
                Box(
                    modifier = Modifier
                        .liquidGelPress()
                        .clip(PillShape)
                        .background(Color(0xFFFF4D6D).copy(alpha = 0.28f))
                        .border(0.9.dp, Color(0xFFFF4D6D).copy(alpha = 0.65f), PillShape)
                        .clickable { onSetMinutes(null) }
                        .padding(horizontal = 18.dp, vertical = 11.dp)
                ) {
                    Text("Turn Off", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
