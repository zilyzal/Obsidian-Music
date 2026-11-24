@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.obsidian.music.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.obsidian.music.ui.components.LiquidUtilitySheetOverlay
import com.obsidian.music.ui.components.LiquidWaveformScrubber
import com.obsidian.music.ui.components.LivingObsidianCanvas
import com.obsidian.music.ui.components.formatDurationMs
import com.obsidian.music.ui.model.EqBandState
import com.obsidian.music.ui.model.EqPreset
import com.obsidian.music.ui.model.ObsidianTrack
import com.obsidian.music.ui.model.RepeatMode
import com.obsidian.music.ui.model.UtilitySheetType
import com.obsidian.music.ui.theme.DeckShape
import com.obsidian.music.ui.theme.ObsidianGlassStyle
import com.obsidian.music.ui.theme.ObsidianMotion
import com.obsidian.music.ui.theme.PillShape
import com.obsidian.music.ui.theme.liquidGelPress
import com.obsidian.music.ui.theme.obsidianLiquidGlass
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Full-Screen True Liquid Glass Now Playing Experience:
 * - 3D interactive swipe-to-skip hero artwork with play/pause breathing lift
 * - Vertical drag-to-dismiss smoothly returning to the floating mini-player
 * - Custom Liquid Waveform Scrubber with tactile droplet thumb
 * - Chromatic refracting transport pod + Heart spring pop
 * - Refracting Glass utility sheets for Lyrics, Spatial EQ, Sleep Timer, and Queue
 */
@Composable
fun LiquidNowPlayingScreen(
    track: ObsidianTrack,
    artworkBitmap: Bitmap?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    queue: List<ObsidianTrack>,
    eqBands: List<EqBandState>,
    activeEqPreset: String,
    spatialAudioEnabled: Boolean,
    sleepTimerRemainingMs: Long?,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekToMs: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onJumpToQueueIndex: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onUpdateEqBand: (Int, Float) -> Unit,
    onApplyEqPreset: (EqPreset) -> Unit,
    onToggleSpatialAudio: (Boolean) -> Unit,
    onSetSleepTimerMinutes: (Int?) -> Unit,
    artworkSlot: @Composable (ObsidianTrack, Modifier) -> Unit,
    modifier: Modifier = Modifier,
    heroArtworkSlot: @Composable (ObsidianTrack, Modifier) -> Unit = artworkSlot
) {
    val backdrop = rememberLayerBackdrop()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var activeSheet by remember { mutableStateOf(UtilitySheetType.NONE) }
    val artworkDragX = remember { Animatable(0f) }
    val dismissDragY = remember { Animatable(0f) }
    val heartScale = remember { Animatable(1f) }

    // Breathing lift: scales up proudly when playing, settles softly when paused
    val playScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.90f,
        animationSpec = ObsidianMotion.LiquidMorphSpring,
        label = "artworkPlayScale"
    )

    // Pop animation when favorited
    LaunchedEffect(track.isFavorite) {
        if (track.isFavorite) {
            heartScale.snapTo(0.72f)
            heartScale.animateTo(1f, ObsidianMotion.HeartPopSpring)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = dismissDragY.value.coerceAtLeast(0f)
                val progress = (dismissDragY.value / 900f).coerceIn(0f, 1f)
                val s = 1f - (progress * 0.08f)
                scaleX = s
                scaleY = s
            }
    ) {
        // LAYER 1: Sampled Backdrop (Living 3-Color Mesh + 3D Interactive Hero Artwork)
        LivingObsidianCanvas(
            backdrop = backdrop,
            artworkBitmap = artworkBitmap,
            isPlaying = isPlaying,
            fallbackPrimary = track.accentColor,
            fallbackSecondary = track.secondaryColor
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 74.dp, start = 26.dp, end = 26.dp)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        val dx = artworkDragX.value
                        translationX = dx
                        rotationY = (dx / 24f).coerceIn(-16f, 16f)
                        rotationZ = (dx / 55f).coerceIn(-6f, 6f)
                        cameraDistance = 16f * density
                        scaleX = playScale
                        scaleY = playScale
                        alpha = (1f - abs(dx) / 720f).coerceIn(0.4f, 1f)
                    }
                    .shadow(
                        elevation = if (isPlaying) 36.dp else 14.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = track.accentColor,
                        spotColor = track.accentColor
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .pointerInput(track.id) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                scope.launch {
                                    artworkDragX.snapTo(
                                        (artworkDragX.value + dragAmount * 0.85f).coerceIn(-340f, 340f)
                                    )
                                }
                            },
                            onDragEnd = {
                                val dx = artworkDragX.value
                                scope.launch {
                                    if (dx < -115f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        artworkDragX.animateTo(-420f, tween(130))
                                        onNext()
                                        artworkDragX.snapTo(220f)
                                        artworkDragX.animateTo(0f, ObsidianMotion.ArtworkThrowSpring)
                                    } else if (dx > 115f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        artworkDragX.animateTo(420f, tween(130))
                                        onPrevious()
                                        artworkDragX.snapTo(-220f)
                                        artworkDragX.animateTo(0f, ObsidianMotion.ArtworkThrowSpring)
                                    } else {
                                        artworkDragX.animateTo(0f, ObsidianMotion.ArtworkThrowSpring)
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    artworkDragX.animateTo(0f, ObsidianMotion.ArtworkThrowSpring)
                                }
                            }
                        )
                    }
            ) {
                AnimatedContent(
                    targetState = track,
                    transitionSpec = {
                        (fadeIn(tween(300)) + scaleIn(initialScale = 0.94f)) togetherWith
                            (fadeOut(tween(220)) + scaleOut(targetScale = 1.04f))
                    },
                    label = "heroArtworkSwap"
                ) { activeTrack ->
                    heroArtworkSlot(activeTrack, Modifier.fillMaxSize())
                }
            }
        }

        // LAYER 2: Floating Liquid Glass UI Chrome (Sibling above layerBackdrop!)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Floating Glass Header (also supports vertical swipe-down to dismiss)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                scope.launch {
                                    dismissDragY.snapTo((dismissDragY.value + dragAmount).coerceAtLeast(0f))
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    if (dismissDragY.value > 180f) {
                                        onDismiss()
                                    }
                                    dismissDragY.animateTo(0f, ObsidianMotion.LiquidMorphSpring)
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    dismissDragY.animateTo(0f, ObsidianMotion.LiquidMorphSpring)
                                }
                            }
                        )
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .liquidGelPress()
                        .obsidianLiquidGlass(
                            backdrop = backdrop,
                            shape = CircleShape,
                            style = ObsidianGlassStyle.GlassLensButton
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = Color.White
                    )
                }

                // Album + Hi-Res Quality Glass Pill
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .obsidianLiquidGlass(
                            backdrop = backdrop,
                            shape = PillShape,
                            style = ObsidianGlassStyle.GlassChip
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = track.album.uppercase(),
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 11.sp,
                        letterSpacing = 1.1.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.audioQualityBadge,
                        color = track.accentColor.copy(alpha = 0.95f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                // Favorite Glass Lens with Spring Overshoot
                val heartColor by animateColorAsState(
                    targetValue = if (track.isFavorite) Color(0xFFFF4D6D) else Color.White,
                    animationSpec = tween(220),
                    label = "heartColor"
                )
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFavorite()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .liquidGelPress()
                        .obsidianLiquidGlass(
                            backdrop = backdrop,
                            shape = CircleShape,
                            style = ObsidianGlassStyle.GlassLensButton
                        )
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = heartColor,
                        modifier = Modifier.graphicsLayer {
                            scaleX = heartScale.value
                            scaleY = heartScale.value
                        }
                    )
                }
            }

            // Bottom Liquid Glass Control Deck
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .obsidianLiquidGlass(
                        backdrop = backdrop,
                        shape = DeckShape,
                        style = ObsidianGlassStyle.ObsidianSheet,
                        accentGlowColor = track.accentColor
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Track Title & Artist (crossfades cleanly on skip)
                AnimatedContent(
                    targetState = track,
                    transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                    label = "trackMetaSwap"
                ) { t ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = t.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = t.artist,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Interactive Liquid Waveform Scrubber
                LiquidWaveformScrubber(
                    trackSeed = track.id + track.title,
                    positionMs = positionMs,
                    durationMs = durationMs.coerceAtLeast(track.durationMs),
                    isPlaying = isPlaying,
                    accentColor = track.accentColor,
                    onSeekToMs = onSeekToMs
                )

                // Transport Controls Pod
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.liquidGelPress()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleEnabled) track.accentColor else Color.White.copy(alpha = 0.62f)
                        )
                    }

                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(48.dp)
                            .liquidGelPress(pressedScale = 0.86f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Hero Refracting Play/Pause Chromatic Lens
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlayPause()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .liquidGelPress(pressedScale = 0.88f, enableHaptics = false)
                            .obsidianLiquidGlass(
                                backdrop = backdrop,
                                shape = CircleShape,
                                style = ObsidianGlassStyle.GlassLensButton,
                                tintColor = track.accentColor,
                                specularColor = Color.White,
                                accentGlowColor = track.accentColor
                            )
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                (scaleIn(initialScale = 0.6f) + fadeIn(tween(160))) togetherWith
                                    (scaleOut(targetScale = 0.6f) + fadeOut(tween(130)))
                            },
                            label = "heroPlayPauseIcon"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(48.dp)
                            .liquidGelPress(pressedScale = 0.86f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier.liquidGelPress()
                    ) {
                        Icon(
                            imageVector = if (repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatMode != RepeatMode.OFF) track.accentColor else Color.White.copy(alpha = 0.62f)
                        )
                    }
                }

                // Bottom Utility Glass Pills (Lyrics, EQ, Sleep Timer, Queue)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    UtilityGlassPill(
                        icon = Icons.Rounded.Lyrics,
                        label = "Lyrics",
                        active = activeSheet == UtilitySheetType.LYRICS,
                        accentColor = track.accentColor,
                        onClick = { activeSheet = UtilitySheetType.LYRICS }
                    )
                    UtilityGlassPill(
                        icon = Icons.Rounded.Equalizer,
                        label = "EQ",
                        active = activeSheet == UtilitySheetType.EQUALIZER,
                        accentColor = track.accentColor,
                        onClick = { activeSheet = UtilitySheetType.EQUALIZER }
                    )
                    UtilityGlassPill(
                        icon = Icons.Rounded.NightlightRound,
                        label = if (sleepTimerRemainingMs != null) formatDurationMs(sleepTimerRemainingMs) else "Timer",
                        active = sleepTimerRemainingMs != null || activeSheet == UtilitySheetType.SLEEP_TIMER,
                        accentColor = track.accentColor,
                        onClick = { activeSheet = UtilitySheetType.SLEEP_TIMER }
                    )
                    UtilityGlassPill(
                        icon = Icons.Rounded.QueueMusic,
                        label = "Queue",
                        active = activeSheet == UtilitySheetType.QUEUE,
                        accentColor = track.accentColor,
                        onClick = { activeSheet = UtilitySheetType.QUEUE }
                    )
                }
            }
        }

        // LAYER 3: Glass Utility Modal Sheets (Sibling on top, refracting the full player!)
        LiquidUtilitySheetOverlay(
            sheetType = activeSheet,
            backdrop = backdrop,
            currentTrack = track,
            positionMs = positionMs,
            isPlaying = isPlaying,
            queue = queue,
            eqBands = eqBands,
            activeEqPreset = activeEqPreset,
            spatialAudioEnabled = spatialAudioEnabled,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            onDismiss = { activeSheet = UtilitySheetType.NONE },
            onSeekToMs = onSeekToMs,
            onJumpToQueueIndex = onJumpToQueueIndex,
            onRemoveFromQueue = onRemoveFromQueue,
            onUpdateEqBand = onUpdateEqBand,
            onApplyEqPreset = onApplyEqPreset,
            onToggleSpatialAudio = onToggleSpatialAudio,
            onSetSleepTimerMinutes = onSetSleepTimerMinutes,
            artworkSlot = artworkSlot
        )
    }
}

@Composable
private fun UtilityGlassPill(
    icon: ImageVector,
    label: String,
    active: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .liquidGelPress(pressedScale = 0.92f)
            .clip(PillShape)
            .background(if (active) accentColor.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.09f))
            .border(
                width = 0.85.dp,
                color = if (active) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.16f),
                shape = PillShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) Color.White else Color.White.copy(alpha = 0.84f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = if (active) Color.White else Color.White.copy(alpha = 0.82f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
