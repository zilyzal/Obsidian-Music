package com.obsidian.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.obsidian.music.ui.components.FloatingGlassBottomOverlay
import com.obsidian.music.ui.components.LivingObsidianCanvas
import com.obsidian.music.ui.components.ProceduralObsidianArtwork
import com.obsidian.music.ui.model.ObsidianActions
import com.obsidian.music.ui.model.ObsidianAlbum
import com.obsidian.music.ui.model.ObsidianTab
import com.obsidian.music.ui.model.ObsidianTrack
import com.obsidian.music.ui.model.ObsidianUiState
import com.obsidian.music.ui.screens.DefaultAlbumArtwork
import com.obsidian.music.ui.screens.LiquidAlbumsScreen
import com.obsidian.music.ui.screens.LiquidHomeScreen
import com.obsidian.music.ui.screens.LiquidNowPlayingScreen
import com.obsidian.music.ui.screens.LiquidSearchScreen
import com.obsidian.music.ui.screens.LiquidTracksLibraryScreen
import com.obsidian.music.ui.theme.LocalReduceTransparency
import com.obsidian.music.ui.theme.ObsidianMotion

/**
 * Top-level entry point for Obsidian Music's True Liquid Glass UI.
 *
 * Plug your ViewModel/ExoPlayer state into [state] and callbacks into [actions].
 * Override [trackArtworkSlot] and [albumArtworkSlot] with your Coil/Glide AsyncImage composable
 * to render real embedded album covers.
 */
@Composable
fun ObsidianRootShell(
    state: ObsidianUiState,
    actions: ObsidianActions,
    modifier: Modifier = Modifier,
    trackArtworkSlot: @Composable (ObsidianTrack, Modifier) -> Unit = { track, mod ->
        ProceduralObsidianArtwork(
            primary = track.accentColor,
            secondary = track.secondaryColor,
            modifier = mod
        )
    },
    albumArtworkSlot: @Composable (ObsidianAlbum, Modifier) -> Unit = { album, mod ->
        DefaultAlbumArtwork(album = album, modifier = mod)
    },
    heroArtworkSlot: @Composable (ObsidianTrack, Modifier) -> Unit = trackArtworkSlot
) {
    val libraryBackdrop = rememberLayerBackdrop()
    var selectedTab by rememberSaveable { mutableStateOf(ObsidianTab.HOME) }
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isPlayerExpanded) {
        isPlayerExpanded = false
    }

    // Depth recession: library subtly scales down into the obsidian depth when Now Playing opens
    val libraryDepthScale by animateFloatAsState(
        targetValue = if (isPlayerExpanded) 0.93f else 1.0f,
        animationSpec = ObsidianMotion.LiquidMorphSpring,
        label = "libraryDepthScale"
    )
    val libraryDepthAlpha by animateFloatAsState(
        targetValue = if (isPlayerExpanded) 0.55f else 1.0f,
        animationSpec = tween(260),
        label = "libraryDepthAlpha"
    )

    CompositionLocalProvider(LocalReduceTransparency provides state.reduceTransparency) {
        Box(modifier = modifier.fillMaxSize()) {
            // =====================================================================
            // LIBRARY STAGE (Sibling 1: layerBackdrop + Sibling 2: Floating Glass)
            // =====================================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = libraryDepthScale
                        scaleY = libraryDepthScale
                        alpha = libraryDepthAlpha
                    }
            ) {
                // Sampled backdrop canvas containing the drifting 3-color mesh + scrollable screens
                LivingObsidianCanvas(
                    backdrop = libraryBackdrop,
                    artworkBitmap = state.currentArtworkBitmap,
                    isPlaying = state.isPlaying,
                    fallbackPrimary = state.currentTrack?.accentColor ?: Color(0xFF6B3DFF),
                    fallbackSecondary = state.currentTrack?.secondaryColor ?: Color(0xFF00C8FF)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val forward = targetState.ordinal >= initialState.ordinal
                            val enterOffset: (Int) -> Int = { if (forward) it / 8 else -it / 8 }
                            val exitOffset: (Int) -> Int = { if (forward) -it / 8 else it / 8 }
                            (fadeIn(tween(240)) + slideInHorizontally(tween(260), enterOffset)) togetherWith
                                (fadeOut(tween(180)) + slideOutHorizontally(tween(220), exitOffset))
                        },
                        label = "tabTransition"
                    ) { tab ->
                        when (tab) {
                            ObsidianTab.HOME -> LiquidHomeScreen(
                                tracks = state.tracks,
                                recentlyPlayed = state.recentlyPlayed,
                                currentTrackId = state.currentTrack?.id,
                                isPlaying = state.isPlaying,
                                reduceTransparency = state.reduceTransparency,
                                onPlayTrack = actions.onPlayTrack,
                                onShuffleAll = actions.onShuffleAll,
                                onToggleReduceTransparency = actions.onToggleReduceTransparency,
                                artworkSlot = trackArtworkSlot
                            )
                            ObsidianTab.LIBRARY -> LiquidTracksLibraryScreen(
                                tracks = state.viewTracks,
                                view = state.libraryView,
                                favoriteCount = state.favoriteCount,
                                currentTrackId = state.currentTrack?.id,
                                isPlaying = state.isPlaying,
                                onViewChange = actions.onSelectLibraryView,
                                onPlayTrack = actions.onPlayTrack,
                                artworkSlot = trackArtworkSlot
                            )
                            ObsidianTab.ALBUMS -> LiquidAlbumsScreen(
                                albums = state.albums,
                                onPlayAlbum = actions.onPlayAlbum,
                                albumArtworkSlot = albumArtworkSlot
                            )
                            ObsidianTab.SEARCH -> LiquidSearchScreen(
                                query = state.searchQuery,
                                tracks = state.tracks,
                                currentTrackId = state.currentTrack?.id,
                                isPlaying = state.isPlaying,
                                onQueryChange = actions.onSearchQueryChange,
                                onPlayTrack = actions.onPlayTrack,
                                artworkSlot = trackArtworkSlot
                            )
                        }
                    }
                }

                // Floating Liquid Glass Mini-Player + Pill Dock (SIBLING on top of LivingObsidianCanvas)
                val fraction = if (state.durationMs > 0L) {
                    (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                FloatingGlassBottomOverlay(
                    backdrop = libraryBackdrop,
                    currentTrack = state.currentTrack,
                    progress = fraction,
                    isPlaying = state.isPlaying,
                    selectedTab = selectedTab,
                    onExpandPlayer = { isPlayerExpanded = true },
                    onPlayPause = actions.onPlayPause,
                    onSkipNext = actions.onNext,
                    onSkipPrevious = actions.onPrevious,
                    onSelectTab = { selectedTab = it },
                    artworkSlot = trackArtworkSlot,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // =====================================================================
            // FULL-SCREEN LIQUID GLASS NOW PLAYING STAGE
            // =====================================================================
            AnimatedVisibility(
                visible = isPlayerExpanded && state.currentTrack != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f)
                ) + fadeIn(tween(220)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 400f)
                ) + fadeOut(tween(180))
            ) {
                state.currentTrack?.let { activeTrack ->
                    LiquidNowPlayingScreen(
                        track = activeTrack,
                        artworkBitmap = state.currentArtworkBitmap,
                        isPlaying = state.isPlaying,
                        positionMs = state.positionMs,
                        durationMs = state.durationMs.coerceAtLeast(activeTrack.durationMs),
                        shuffleEnabled = state.shuffleEnabled,
                        repeatMode = state.repeatMode,
                        queue = state.queue.ifEmpty { state.tracks },
                        eqBands = state.eqBands,
                        activeEqPreset = state.activeEqPreset,
                        spatialAudioEnabled = state.spatialAudioEnabled,
                        sleepTimerRemainingMs = state.sleepTimerRemainingMs,
                        onDismiss = { isPlayerExpanded = false },
                        onPlayPause = actions.onPlayPause,
                        onPrevious = actions.onPrevious,
                        onNext = actions.onNext,
                        onSeekToMs = actions.onSeekToMs,
                        onToggleShuffle = actions.onToggleShuffle,
                        onCycleRepeat = actions.onCycleRepeat,
                        onToggleFavorite = { actions.onToggleFavorite(activeTrack.id) },
                        onJumpToQueueIndex = actions.onJumpToQueueIndex,
                        onRemoveFromQueue = actions.onRemoveFromQueue,
                        onUpdateEqBand = actions.onUpdateEqBand,
                        onApplyEqPreset = actions.onApplyEqPreset,
                        onToggleSpatialAudio = actions.onToggleSpatialAudio,
                        onSetSleepTimerMinutes = actions.onSetSleepTimerMinutes,
                        artworkSlot = trackArtworkSlot,
                        heroArtworkSlot = heroArtworkSlot
                    )
                }
            }
        }
    }
}
