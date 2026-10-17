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
import com.obsidian.music.data.FavoritesStore
import com.obsidian.music.data.LibraryView
import com.obsidian.music.data.MusicRepository
import com.obsidian.music.data.Track
import com.obsidian.music.data.arranged
import com.obsidian.music.playback.PlaybackService
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
    /** What the home list shows: the library arranged by [view]. */
    val tracks: List<Track> = emptyList(),
    val view: LibraryView = LibraryView.LATEST,
    val favorites: Set<Long> = emptySet(),
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val shuffle: Boolean = false,
    val durationMs: Long = 0L,
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

    private val _state = MutableStateFlow(PlayerUiState(favorites = favoritesStore.load()))
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    /** Ids of the songs currently queued in the player, in list order. */
    private var queueIds: List<Long> = emptyList()

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
                syncFromPlayer(c)
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
            _state.update { it.arrangedWith(library = library).copy(loading = false) }
            controller?.let { syncFromPlayer(it) }
        }
    }

    fun setView(view: LibraryView) {
        _state.update { it.arrangedWith(view = view) }
    }

    fun toggleFavorite(track: Track) {
        _state.update { s ->
            val next = if (track.id in s.favorites) s.favorites - track.id else s.favorites + track.id
            s.arrangedWith(favorites = next)
        }
        favoritesStore.save(_state.value.favorites)
    }

    /** Tap a song: queue the list as shown (Latest, Favorites, ...) and start from that song. */
    fun play(track: Track) {
        val c = controller ?: return
        val tracks = _state.value.tracks
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index < 0) return
        val ids = tracks.map { it.id }
        // Reuse the running queue only if it is exactly the list on screen.
        if (queueIds != ids || c.mediaItemCount != ids.size) {
            c.setMediaItems(tracks.map(::toMediaItem), index, 0L)
            queueIds = ids
        } else {
            c.seekTo(index, 0L)
        }
        c.prepare()
        c.play()
    }

    /** Turns shuffle on and starts a random song from the list on screen. */
    fun shufflePlay() {
        val c = controller ?: return
        val tracks = _state.value.tracks
        if (tracks.isEmpty()) return
        val start = tracks.indices.random()
        c.shuffleModeEnabled = true
        c.setMediaItems(tracks.map(::toMediaItem), start, 0L)
        queueIds = tracks.map { it.id }
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
                // Look in the whole library so the song stays "current" even when a filter hides it.
                current = currentId?.let { id -> s.library.firstOrNull { it.id == id } } ?: s.current,
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
