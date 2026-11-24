package com.obsidian.music

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.obsidian.music.data.FavoritesStore
import com.obsidian.music.data.LibraryView
import com.obsidian.music.data.MusicRepository
import com.obsidian.music.data.PlayerPrefs
import com.obsidian.music.data.Track
import com.obsidian.music.data.arranged
import com.obsidian.music.playback.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val loading: Boolean = true,
    /** Every song on the phone, newest first. */
    val library: List<Track> = emptyList(),
    /** What the Tracks tab shows: the library arranged by [view]. */
    val tracks: List<Track> = emptyList(),
    val view: LibraryView = LibraryView.LATEST,
    val favorites: Set<Long> = emptySet(),
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val shuffle: Boolean = false,
    /** One of [Player.REPEAT_MODE_OFF], [Player.REPEAT_MODE_ONE], [Player.REPEAT_MODE_ALL]. */
    val repeatMode: Int = Player.REPEAT_MODE_ALL,
    val durationMs: Long = 0L,
    /** Name of the view the queue was started from ("Latest", "Favorites", ...). */
    val playingFrom: String? = null,
    /** Ids of the songs in the running queue, in order. */
    val queue: List<Long> = emptyList(),
    /** Most recently started songs, newest first. */
    val recent: List<Long> = emptyList(),
    val query: String = "",
    val eqGains: List<Float> = listOf(0.28f, 0.14f, -0.06f, 0.22f, 0.34f),
    val eqPreset: String = "Obsidian",
    val spatial: Boolean = false,
    val sleepRemainingMs: Long? = null,
    val reduceTransparency: Boolean = false,
) {
    fun arrangedWith(
        library: List<Track> = this.library,
        view: LibraryView = this.view,
        favorites: Set<Long> = this.favorites,
    ): PlayerUiState = copy(
        library = library,
        view = view,
        favorites = favorites,
        tracks = library.arranged(view, favorites),
    )
}

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MusicRepository(app)
    private val favoritesStore = FavoritesStore(app)
    private val prefs = PlayerPrefs(app)

    private val _state = MutableStateFlow(
        PlayerUiState(
            favorites = favoritesStore.load(),
            recent = prefs.recent,
            eqGains = prefs.eqGains,
            eqPreset = prefs.eqPreset,
            spatial = prefs.spatial,
            reduceTransparency = prefs.reduceTransparency,
        )
    )
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    /** Ids of the songs currently queued in the player, in list order. */
    private var queueIds: List<Long> = emptyList()

    /** Library by id, so every player event finds the current song without scanning the list. */
    private var libraryById: Map<Long, Track> = emptyMap()

    private var lastRecordedId: Long? = null
    private var sleepJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromPlayer(player, timelineChanged = events.contains(Player.EVENT_TIMELINE_CHANGED))
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync().also { future ->
            future.addListener({
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = c
                c.addListener(playerListener)
                syncFromPlayer(c, timelineChanged = true)
                // The service forgets effects when it restarts, so hand them over on every connect.
                pushEqualizer(_state.value.eqGains)
                pushSpatial(_state.value.spatial)
            }, ContextCompat.getMainExecutor(app))
        }
    }

    fun loadLibrary() {
        viewModelScope.launch {
            // Only show the spinner on first load; refreshes on resume stay silent.
            _state.update { it.copy(loading = it.library.isEmpty()) }
            val library = runCatching { repository.loadTracks() }.getOrNull()
            if (library == null) {
                // A failed scan keeps whatever is already on screen.
                _state.update { it.copy(loading = false) }
                return@launch
            }
            libraryById = library.associateBy { it.id }
            _state.update { it.arrangedWith(library = library).copy(loading = false) }
            controller?.let { syncFromPlayer(it, timelineChanged = false) }
        }
    }

    fun setView(view: LibraryView) {
        _state.update { it.arrangedWith(view = view) }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun toggleFavorite(track: Track) {
        _state.update { s ->
            val next = if (track.id in s.favorites) s.favorites - track.id else s.favorites + track.id
            s.arrangedWith(favorites = next)
        }
        favoritesStore.save(_state.value.favorites)
    }

    /**
     * Tap a song: queue the list on the Tracks tab (Latest, Favorites, ...) and start from that
     * song. A song outside that list (say, a favourite-only view) queues the whole library.
     */
    fun play(track: Track) {
        val s = _state.value
        val inView = s.tracks.any { it.id == track.id }
        startQueue(if (inView) s.tracks else s.library, track, if (inView) s.view.label else LibraryView.LATEST.label)
    }

    fun playAlbum(albumId: Long) {
        val tracks = _state.value.library.filter { it.albumId == albumId }
        val first = tracks.firstOrNull() ?: return
        val c = controller ?: return
        c.shuffleModeEnabled = false
        startQueue(tracks, first, tracks.first().album.ifBlank { "Album" })
    }

    private fun startQueue(tracks: List<Track>, from: Track, label: String) {
        val c = controller ?: return
        val index = tracks.indexOfFirst { it.id == from.id }
        if (index < 0) return
        val ids = tracks.map { it.id }
        // Reuse the running queue only if it is exactly the list we want.
        if (queueIds != ids || c.mediaItemCount != ids.size) {
            c.setMediaItems(tracks.map(::toMediaItem), index, 0L)
            queueIds = ids
        } else {
            c.seekTo(index, 0L)
        }
        _state.update { it.copy(playingFrom = label) }
        c.prepare()
        c.play()
    }

    /** Turns shuffle on and starts a random song from the list on the Tracks tab. */
    fun shufflePlay() {
        val c = controller ?: return
        val tracks = _state.value.tracks.ifEmpty { _state.value.library }
        if (tracks.isEmpty()) return
        val start = tracks.indices.random()
        c.shuffleModeEnabled = true
        c.setMediaItems(tracks.map(::toMediaItem), start, 0L)
        queueIds = tracks.map { it.id }
        _state.update { it.copy(playingFrom = it.view.label) }
        c.prepare()
        c.play()
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    /** Off -> loop all -> repeat one -> off. */
    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNextMediaItem() }

    fun previous() {
        val c = controller ?: return
        // Standard behaviour: restart the song if we're past 3s, otherwise go back.
        if (c.currentPosition > 3_000L) c.seekTo(0L) else c.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    fun jumpToQueueIndex(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.seekTo(index, 0L)
        c.play()
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount || c.mediaItemCount <= 1) return
        c.removeMediaItem(index)
    }

    fun setEqBand(index: Int, gain: Float) {
        val next = _state.value.eqGains.toMutableList().also { if (index in it.indices) it[index] = gain }
        applyEq(next, "Custom")
    }

    fun applyEqPreset(name: String, gains: List<Float>) = applyEq(gains, name)

    private fun applyEq(gains: List<Float>, preset: String) {
        _state.update { it.copy(eqGains = gains, eqPreset = preset) }
        prefs.eqGains = gains
        prefs.eqPreset = preset
        pushEqualizer(gains)
    }

    fun setSpatial(on: Boolean) {
        _state.update { it.copy(spatial = on) }
        prefs.spatial = on
        pushSpatial(on)
    }

    fun setReduceTransparency(on: Boolean) {
        _state.update { it.copy(reduceTransparency = on) }
        prefs.reduceTransparency = on
    }

    /** Pauses playback after [minutes]; null cancels the timer. */
    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null) {
            _state.update { it.copy(sleepRemainingMs = null) }
            return
        }
        sleepJob = viewModelScope.launch {
            var remaining = minutes * 60_000L
            while (remaining > 0L) {
                _state.update { it.copy(sleepRemainingMs = remaining) }
                delay(1_000L)
                remaining -= 1_000L
            }
            controller?.pause()
            _state.update { it.copy(sleepRemainingMs = null) }
        }
    }

    fun currentPositionMs(): Long = controller?.currentPosition ?: 0L

    /**
     * Emits position updates only while the caller is collecting (screen on and
     * song playing), so there is no polling when the app is in the background.
     */
    suspend fun trackPosition(onPosition: (Long) -> Unit) {
        while (true) {
            onPosition(currentPositionMs())
            val c = controller
            delay(if (c?.isPlaying == true) 200L else 500L)
        }
    }

    private fun pushEqualizer(gains: List<Float>) {
        val c = controller ?: return
        val args = Bundle().apply { putFloatArray(PlaybackService.KEY_GAINS, gains.toFloatArray()) }
        c.sendCustomCommand(SessionCommand(PlaybackService.CMD_EQ, Bundle.EMPTY), args)
    }

    private fun pushSpatial(on: Boolean) {
        val c = controller ?: return
        val args = Bundle().apply { putBoolean(PlaybackService.KEY_ENABLED, on) }
        c.sendCustomCommand(SessionCommand(PlaybackService.CMD_SPATIAL, Bundle.EMPTY), args)
    }

    private fun readQueue(p: Player): List<Long> =
        List(p.mediaItemCount) { p.getMediaItemAt(it).mediaId.toLongOrNull() ?: -1L }

    private fun syncFromPlayer(p: Player, timelineChanged: Boolean) {
        val currentId = p.currentMediaItem?.mediaId?.toLongOrNull()
        if (timelineChanged) queueIds = readQueue(p)

        var recent = _state.value.recent
        if (currentId != null && currentId != lastRecordedId) {
            lastRecordedId = currentId
            recent = (listOf(currentId) + recent.filter { it != currentId }).take(MAX_RECENT)
            prefs.recent = recent
        }

        _state.update { s ->
            s.copy(
                // Look in the whole library so the song stays "current" even when a filter hides it.
                current = currentId?.let { id -> libraryById[id] } ?: s.current,
                isPlaying = p.isPlaying,
                shuffle = p.shuffleModeEnabled,
                repeatMode = p.repeatMode,
                durationMs = p.duration.takeIf { it > 0 } ?: s.current?.durationMs ?: 0L,
                queue = if (timelineChanged) queueIds else s.queue,
                recent = recent,
            )
        }
    }

    private fun toMediaItem(t: Track): MediaItem =
        MediaItem.Builder()
            .setMediaId(t.id.toString())
            .setUri(t.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(t.title)
                    .setArtist(t.artist)
                    .setAlbumTitle(t.album)
                    .setArtworkUri(t.artworkUri)
                    .build()
            )
            .build()

    override fun onCleared() {
        sleepJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }

    private companion object {
        const val MAX_RECENT = 12
    }
}
