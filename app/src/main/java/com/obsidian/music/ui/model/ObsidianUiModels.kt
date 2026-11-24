package com.obsidian.music.ui.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.obsidian.music.data.LibraryView

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class ObsidianTab(val id: String, val label: String) {
    HOME("home", "Home"),
    LIBRARY("library", "Tracks"),
    ALBUMS("albums", "Albums"),
    SEARCH("search", "Search")
}

enum class UtilitySheetType {
    NONE, LYRICS, EQUALIZER, QUEUE, SLEEP_TIMER
}

@Immutable
data class ObsidianTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUri: String? = null,
    val accentHex: Long = 0xFF6C4DFF,
    val secondaryHex: Long = 0xFF00D2FF,
    val audioQualityBadge: String = "",
    val isFavorite: Boolean = false,
    val lyrics: List<LyricLine> = emptyList()
) {
    val accentColor: Color get() = Color(accentHex)
    val secondaryColor: Color get() = Color(secondaryHex)
}

@Immutable
data class ObsidianAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val artworkUri: String? = null,
    val accentHex: Long = 0xFF6C4DFF,
    val secondaryHex: Long = 0xFF00D2FF
)

@Immutable
data class LyricLine(
    val timeMs: Long,
    val text: String
)

@Immutable
data class EqBandState(
    val label: String,
    val frequencyHz: Int,
    /** Normalized gain from -1.0f (-12dB) to +1.0f (+12dB) */
    val gainNormalized: Float
)

val DefaultEqBands = listOf(
    EqBandState("60Hz", 60, 0.28f),
    EqBandState("230Hz", 230, 0.14f),
    EqBandState("910Hz", 910, -0.06f),
    EqBandState("3.6kHz", 3600, 0.22f),
    EqBandState("14kHz", 14000, 0.34f)
)

@Immutable
data class EqPreset(
    val name: String,
    val gains: List<Float>
)

val DefaultEqPresets = listOf(
    EqPreset("Obsidian", listOf(0.28f, 0.14f, -0.06f, 0.22f, 0.34f)),
    EqPreset("Studio Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    EqPreset("Deep Bass", listOf(0.72f, 0.42f, -0.10f, 0.08f, 0.16f)),
    EqPreset("Vocal Glass", listOf(-0.18f, 0.08f, 0.48f, 0.56f, 0.24f)),
    EqPreset("Night Drive", listOf(0.46f, 0.20f, -0.15f, 0.18f, 0.45f))
)

@Immutable
data class ObsidianUiState(
    val tracks: List<ObsidianTrack> = emptyList(),
    /** The library as arranged by [libraryView] (Latest, Favorites, ...), shown on the Tracks tab. */
    val viewTracks: List<ObsidianTrack> = emptyList(),
    val recentlyPlayed: List<ObsidianTrack> = emptyList(),
    val albums: List<ObsidianAlbum> = emptyList(),
    val queue: List<ObsidianTrack> = emptyList(),
    val libraryView: LibraryView = LibraryView.LATEST,
    val favoriteCount: Int = 0,
    val currentTrack: ObsidianTrack? = null,
    val currentArtworkBitmap: Bitmap? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val searchQuery: String = "",
    val eqBands: List<EqBandState> = DefaultEqBands,
    val activeEqPreset: String = "Obsidian",
    val spatialAudioEnabled: Boolean = true,
    val sleepTimerRemainingMs: Long? = null,
    val reduceTransparency: Boolean = false
)

@Immutable
data class ObsidianActions(
    val onPlayTrack: (ObsidianTrack) -> Unit = {},
    val onPlayAlbum: (ObsidianAlbum) -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onSeekToMs: (Long) -> Unit = {},
    val onToggleShuffle: () -> Unit = {},
    val onShuffleAll: () -> Unit = {},
    val onCycleRepeat: () -> Unit = {},
    val onToggleFavorite: (String) -> Unit = {},
    val onSearchQueryChange: (String) -> Unit = {},
    val onJumpToQueueIndex: (Int) -> Unit = {},
    val onRemoveFromQueue: (Int) -> Unit = {},
    val onUpdateEqBand: (index: Int, gainNormalized: Float) -> Unit = { _, _ -> },
    val onApplyEqPreset: (EqPreset) -> Unit = {},
    val onToggleSpatialAudio: (Boolean) -> Unit = {},
    val onSetSleepTimerMinutes: (Int?) -> Unit = {},
    val onToggleReduceTransparency: (Boolean) -> Unit = {},
    val onSelectLibraryView: (LibraryView) -> Unit = {}
)
