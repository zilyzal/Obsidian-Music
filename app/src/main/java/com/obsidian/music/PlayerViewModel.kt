package com.obsidian.music

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.obsidian.music.data.MusicRepository
import com.obsidian.music.data.Track
import com.obsidian.music.playback.PlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val loading: Boolean = true,
    val tracks: List<Track> = emptyList(),
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val shuffle: Boolean = false,
    val durationMs: Long = 0L,
)

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MusicRepository(app)

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var queueLoaded = false

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromPlayer(player)
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync().also { future ->
            future.addListener({
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = c
                c.addListener(playerListener)
                // A service that outlived the UI may already hold a queue.
                queueLoaded = c.mediaItemCount > 0
                syncFromPlayer(c)
            }, ContextCompat.getMainExecutor(app))
        }
    }

    fun loadLibrary() {
        viewModelScope.launch {
            // Only show the spinner on first load; refreshes on resume stay silent.
            _state.update { it.copy(loading = it.tracks.isEmpty()) }
            val tracks = runCatching { repository.loadTracks() }.getOrDefault(emptyList())
            _state.update { it.copy(tracks = tracks, loading = false) }
            controller?.let { syncFromPlayer(it) }
        }
    }

    /** Tap a song: queue the whole list (newest first) and start from that song. */
    fun play(track: Track) {
        val c = controller ?: return
        val tracks = _state.value.tracks
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index < 0) return
        if (!queueLoaded || c.mediaItemCount != tracks.size) {
            c.setMediaItems(tracks.map(::toMediaItem), index, 0L)
            queueLoaded = true
        } else {
            c.seekTo(index, 0L)
        }
        c.prepare()
        c.play()
    }

    /** Turns shuffle on and starts a random song. */
    fun shufflePlay() {
        val c = controller ?: return
        val tracks = _state.value.tracks
        if (tracks.isEmpty()) return
        val start = tracks.indices.random()
        c.shuffleModeEnabled = true
        c.setMediaItems(tracks.map(::toMediaItem), start, 0L)
        queueLoaded = true
        c.prepare()
        c.play()
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
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

    private fun syncFromPlayer(p: Player) {
        val currentId = p.currentMediaItem?.mediaId?.toLongOrNull()
        _state.update { s ->
            s.copy(
                current = currentId?.let { id -> s.tracks.firstOrNull { it.id == id } } ?: s.current,
                isPlaying = p.isPlaying,
                shuffle = p.shuffleModeEnabled,
                durationMs = p.duration.takeIf { it > 0 } ?: s.current?.durationMs ?: 0L,
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
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }
}
