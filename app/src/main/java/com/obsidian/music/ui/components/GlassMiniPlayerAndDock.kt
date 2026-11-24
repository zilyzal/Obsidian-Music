@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.obsidian.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.obsidian.music.ui.model.ObsidianTab
import com.obsidian.music.ui.model.ObsidianTrack
import com.obsidian.music.ui.theme.ObsidianGlassStyle
import com.obsidian.music.ui.theme.ObsidianMotion
import com.obsidian.music.ui.theme.PillShape
import com.obsidian.music.ui.theme.liquidGelPress
import com.obsidian.music.ui.theme.obsidianLiquidGlass
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private fun ObsidianTab.icon(): ImageVector = when (this) {
    ObsidianTab.HOME -> Icons.Rounded.Home
    ObsidianTab.LIBRARY -> Icons.Rounded.LibraryMusic
    ObsidianTab.ALBUMS -> Icons.Rounded.Album
    ObsidianTab.SEARCH -> Icons.Rounded.Search
}

/**
 * Detached Floating Liquid Glass Mini-Player + Sliding-Lens Pill Navigation Dock.
 * Must be placed as a SIBLING above `LivingObsidianCanvas` so `drawBackdrop` refracts live scrolling art.
 */
@Composable
fun FloatingGlassBottomOverlay(
    backdrop: Backdrop,
    currentTrack: ObsidianTrack?,
    progress: Float,
    isPlaying: Boolean,
    selectedTab: ObsidianTab,
    onExpandPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSelectTab: (ObsidianTab) -> Unit,
    artworkSlot: @Composable (ObsidianTrack, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val swipeOffsetX = remember { Animatable(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Floating Liquid Glass Mini-Player (Swipe left/right to skip, swipe up or tap to expand)
        AnimatedVisibility(
            visible = currentTrack != null,
            enter = fadeIn(tween(240)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.92f)
        ) {
            currentTrack?.let { track ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(swipeOffsetX.value.roundToInt(), 0) }
                        .graphicsLayer {
                            rotationZ = (swipeOffsetX.value / 140f).coerceIn(-2.5f, 2.5f)
                            alpha = (1f - abs(swipeOffsetX.value) / 480f).coerceIn(0.45f, 1f)
                        }
                        .liquidGelPress(pressedScale = 0.975f)
                        .obsidianLiquidGlass(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(26.dp),
                            style = ObsidianGlassStyle.LiquidPill,
                            accentGlowColor = track.accentColor
                        )
                        .pointerInput(track.id) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount ->
                                    scope.launch {
                                        swipeOffsetX.snapTo(
                                            (swipeOffsetX.value + dragAmount * 0.7f).coerceIn(-180f, 180f)
                                        )
                                    }
                                },
                                onDragEnd = {
                                    val dx = swipeOffsetX.value
                                    scope.launch {
                                        if (dx < -78f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSkipNext()
                                        } else if (dx > 78f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSkipPrevious()
                                        }
                                        swipeOffsetX.animateTo(0f, ObsidianMotion.ArtworkThrowSpring)
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        swipeOffsetX.animateTo(0f, ObsidianMotion.ArtworkThrowSpring)
                                    }
                                }
                            )
                        }
                        .pointerInput(track.id) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -18f) {
                                    onExpandPlayer()
                                }
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onExpandPlayer
                        )
                        .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Artwork thumbnail with subtle spin/pulse highlight
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    0.8.dp,
                                    Color.White.copy(alpha = 0.28f),
                                    RoundedCornerShape(14.dp)
                                )
                        ) {
                            artworkSlot(track, Modifier.fillMaxSize())
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isPlaying) {
                                    LiveEqualizerBars(
                                        isPlaying = true,
                                        tint = track.accentColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = track.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                            Text(
                                text = track.artist,
                                color = Color.White.copy(alpha = 0.68f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Morphing Play/Pause Lens Button
                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier
                                .size(42.dp)
                                .liquidGelPress(pressedScale = 0.88f)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.34f), CircleShape)
                        ) {
                            AnimatedContent(
                                targetState = isPlaying,
                                transitionSpec = {
                                    (scaleIn(initialScale = 0.65f) + fadeIn(tween(150))) togetherWith
                                        (scaleOut(targetScale = 0.65f) + fadeOut(tween(120)))
                                },
                                label = "miniPlayPause"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (playing) "Pause" else "Play",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(Modifier.width(2.dp))

                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier
                                .size(38.dp)
                                .liquidGelPress(pressedScale = 0.88f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next track",
                                tint = Color.White.copy(alpha = 0.90f)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Luminous liquid progress filament
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress.coerceIn(0f, 1f),
                        animationSpec = tween(220),
                        label = "miniProgress"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            track.accentColor,
                                            Color.White.copy(alpha = 0.95f)
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }

        // 2. Floating Liquid Glass Pill Dock with Fluid Sliding Mercury Capsule
        val tabs = ObsidianTab.entries
        val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .obsidianLiquidGlass(
                    backdrop = backdrop,
                    shape = PillShape,
                    style = ObsidianGlassStyle.LiquidPill
                )
                .padding(6.dp)
        ) {
            val slotWidth = maxWidth / tabs.size
            val indicatorOffset by animateDpAsState(
                targetValue = slotWidth * selectedIndex,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.68f,
                    stiffness = 380f
                ),
                label = "dockCapsuleOffset"
            )

            // Sliding luminous glass indicator pill underneath the active tab
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(slotWidth)
                    .fillMaxHeight()
                    .clip(PillShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.24f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .border(
                        width = 0.9.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.52f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        ),
                        shape = PillShape
                    )
            )

            // Interactive Tab Items
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    val selected = tab == selectedTab
                    val tint by animateColorAsState(
                        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.56f),
                        animationSpec = tween(220),
                        label = "tabTint"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 0.94f,
                        animationSpec = ObsidianMotion.GelBounceSpring,
                        label = "tabIconScale"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .liquidGelPress(pressedScale = 0.92f)
                            .clip(PillShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelectTab(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon(),
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            color = tint,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
