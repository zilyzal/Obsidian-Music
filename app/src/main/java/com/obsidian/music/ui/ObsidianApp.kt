package com.obsidian.music.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import com.obsidian.music.PlayerViewModel
import com.obsidian.music.data.Track
import com.obsidian.music.ui.model.ObsidianActions
import com.obsidian.music.ui.model.ObsidianAlbum
import com.obsidian.music.ui.model.ObsidianTrack
import com.obsidian.music.ui.model.ObsidianUiState
import com.obsidian.music.ui.model.RepeatMode
import com.obsidian.music.ui.model.EqBandState
import com.obsidian.music.ui.model.DefaultEqBands

private val audioPermission =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ObsidianApp(vm: PlayerViewModel) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var hasAudio by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED)
    }
    // True once Android will no longer show the permission dialog: the only way forward is Settings.
    var blocked by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // Re-check the real state: the result map only holds what was requested.
        hasAudio = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        val activity = context.findActivity()
        blocked = !hasAudio && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, audioPermission)
        if (hasAudio) vm.loadLibrary()
    }

    // Pick up newly added songs whenever the app comes back to the foreground
    // (this also catches the person returning from Settings after allowing access).
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        hasAudio = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        if (hasAudio) vm.loadLibrary()
    }

    // Position ticks only while a song is playing and the screen is visible.
    LaunchedEffect(state.isPlaying, state.current?.id) {
        position = vm.currentPositionMs()
        if (state.isPlaying) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.trackPosition { position = it }
            }
        }
    }

    val accent = rememberAccent(state.current)

    when {
        !hasAudio -> Box(Modifier.fillMaxSize()) {
            AmbientBackground(accent, backdrop = Backdrop.Welcome)
            WelcomeScreen(accent = accent, blocked = blocked) {
                if (blocked) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                } else {
                    // Notifications are what lets the media notification show on Android 13+.
                    val wanted = buildList {
                        add(audioPermission)
                        if (Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(wanted.toTypedArray())
                }
            }
        }

        state.loading && state.library.isEmpty() -> Box(Modifier.fillMaxSize()) {
            AmbientBackground(accent, backdrop = Backdrop.Home)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            }
        }

        // Judge the whole library, so an empty Favorites list still shows the home screen.
        state.library.isEmpty() -> Box(Modifier.fillMaxSize()) {
            AmbientBackground(accent, backdrop = Backdrop.Home)
            Box(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                StateCard(
                    icon = Icons.Rounded.MusicNote,
                    title = "No music yet",
                    body = "Add some songs to your phone and they'll show up here, newest first.",
                    accent = accent,
                    actionLabel = "Scan again",
                    onAction = vm::loadLibrary,
                )
            }
        }

        else -> LiquidGlassPlayer(vm = vm, state = state, accent = accent, position = position, onSeekLocal = { position = it })
    }
}

@Composable
private fun LiquidGlassPlayer(
    vm: PlayerViewModel,
    state: com.obsidian.music.PlayerUiState,
    accent: androidx.compose.ui.graphics.Color,
    position: Long,
    onSeekLocal: (Long) -> Unit,
) {
    val context = LocalContext.current.applicationContext

    // Everything below is rebuilt only when the library, stars or view actually change.
    val trackById = remember(state.library) { state.library.associateBy { it.id } }
    val uiTracks = remember(state.library, state.favorites) {
        state.library.map { it.toUi(it.id in state.favorites) }
    }
    val uiById = remember(uiTracks) { uiTracks.associateBy { it.id } }
    val viewTracks = remember(state.tracks, uiById) { state.tracks.mapNotNull { uiById[it.id.toString()] } }
    val recent = remember(state.recent, uiById) { state.recent.mapNotNull { uiById[it.toString()] } }
    val queue = remember(state.queue, uiById) { state.queue.mapNotNull { uiById[it.toString()] } }
    val albumData = remember(state.library) { buildAlbums(state.library) }

    val current = state.current?.let { t -> uiById[t.id.toString()]?.withAccent(accent) }

    val artwork by produceState<Bitmap?>(null, state.current?.id) {
        value = state.current?.let { ArtworkLoader.small(context, it) }
    }

    val uiState = ObsidianUiState(
        tracks = uiTracks,
        viewTracks = viewTracks,
        libraryView = state.view,
        favoriteCount = state.favorites.size,
        recentlyPlayed = recent,
        albums = albumData.first,
        queue = queue,
        currentTrack = current,
        currentArtworkBitmap = artwork,
        isPlaying = state.isPlaying,
        positionMs = position,
        durationMs = state.durationMs,
        shuffleEnabled = state.shuffle,
        repeatMode = when (state.repeatMode) {
            Player.REPEAT_MODE_OFF -> RepeatMode.OFF
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.ALL
        },
        searchQuery = state.query,
        eqBands = remember(state.eqGains) {
            DefaultEqBands.mapIndexed { i, band -> EqBandState(band.label, band.frequencyHz, state.eqGains.getOrElse(i) { 0f }) }
        },
        activeEqPreset = state.eqPreset,
        spatialAudioEnabled = state.spatial,
        sleepTimerRemainingMs = state.sleepRemainingMs,
        reduceTransparency = state.reduceTransparency,
    )

    val actions = remember(vm, trackById, albumData) {
        ObsidianActions(
            onPlayTrack = { t -> t.id.toLongOrNull()?.let(trackById::get)?.let(vm::play) },
            onPlayAlbum = { a: ObsidianAlbum -> a.id.toLongOrNull()?.let(vm::playAlbum) },
            onPlayPause = vm::togglePlayPause,
            onNext = vm::next,
            onPrevious = vm::previous,
            onSeekToMs = { ms -> vm.seekTo(ms); onSeekLocal(ms) },
            onToggleShuffle = vm::toggleShuffle,
            onShuffleAll = vm::shufflePlay,
            onCycleRepeat = vm::cycleRepeat,
            onToggleFavorite = { id -> id.toLongOrNull()?.let(trackById::get)?.let(vm::toggleFavorite) },
            onSearchQueryChange = vm::setQuery,
            onJumpToQueueIndex = vm::jumpToQueueIndex,
            onRemoveFromQueue = vm::removeFromQueue,
            onUpdateEqBand = { index, gain -> vm.setEqBand(index, gain) },
            onApplyEqPreset = { preset -> vm.applyEqPreset(preset.name, preset.gains) },
            onToggleSpatialAudio = vm::setSpatial,
            onSetSleepTimerMinutes = vm::setSleepTimer,
            onToggleReduceTransparency = vm::setReduceTransparency,
            onSelectLibraryView = vm::setView,
        )
    }

    val trackSlot: @Composable (ObsidianTrack, Modifier) -> Unit = remember(trackById) {
        { t, mod -> Artwork(t.id.toLongOrNull()?.let(trackById::get), RectangleShape, mod) }
    }
    val heroSlot: @Composable (ObsidianTrack, Modifier) -> Unit = remember(trackById) {
        { t, mod -> Artwork(t.id.toLongOrNull()?.let(trackById::get), RectangleShape, mod, large = true) }
    }
    val albumSlot: @Composable (ObsidianAlbum, Modifier) -> Unit = remember(albumData) {
        { a, mod -> Artwork(albumData.second[a.id], RectangleShape, mod) }
    }

    ObsidianRootShell(
        state = uiState,
        actions = actions,
        trackArtworkSlot = trackSlot,
        albumArtworkSlot = albumSlot,
        heroArtworkSlot = heroSlot,
    )
}
