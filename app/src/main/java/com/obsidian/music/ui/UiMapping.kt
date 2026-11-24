package com.obsidian.music.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.obsidian.music.data.Track
import com.obsidian.music.ui.model.ObsidianAlbum
import com.obsidian.music.ui.model.ObsidianTrack

private fun Color.toHex(): Long = toArgb().toLong() and 0xFFFFFFFFL

/** Stable colours per song or album, so rows that have no cover colour yet still look intentional. */
private fun accentFor(id: Long): Pair<Long, Long> {
    val hue = ((id * 47) % 360).toFloat().let { if (it < 0) it + 360f else it }
    return Color.hsv(hue, 0.55f, 0.92f).toHex() to Color.hsv((hue + 40f) % 360f, 0.65f, 0.95f).toHex()
}

fun Track.toUi(isFavorite: Boolean): ObsidianTrack {
    val (accent, secondary) = accentFor(if (albumId != 0L) albumId else id)
    return ObsidianTrack(
        id = id.toString(),
        title = title,
        artist = artist,
        album = album.ifBlank { "Unknown album" },
        durationMs = durationMs,
        accentHex = accent,
        secondaryHex = secondary,
        audioQualityBadge = qualityLabel,
        isFavorite = isFavorite,
    )
}

/** The playing song takes its colours from its cover. */
fun ObsidianTrack.withAccent(accent: Color): ObsidianTrack {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(accent.toArgb(), hsv)
    hsv[0] = (hsv[0] + 40f) % 360f
    val secondary = Color.hsv(hsv[0], hsv[1].coerceIn(0.4f, 0.9f), 0.95f)
    return copy(accentHex = accent.toHex(), secondaryHex = secondary.toHex())
}

fun buildAlbums(library: List<Track>): Pair<List<ObsidianAlbum>, Map<String, Track>> {
    val representative = HashMap<String, Track>()
    val albums = library
        .filter { it.albumId != 0L && it.album.isNotBlank() }
        .groupBy { it.albumId }
        .map { (albumId, tracks) ->
            val artists = tracks.map { it.artist }.distinct()
            val (accent, secondary) = accentFor(albumId)
            val id = albumId.toString()
            representative[id] = tracks.first()
            ObsidianAlbum(
                id = id,
                title = tracks.first().album,
                artist = if (artists.size == 1) artists.first() else "Various artists",
                trackCount = tracks.size,
                accentHex = accent,
                secondaryHex = secondary,
            )
        }
        .sortedBy { it.title.lowercase() }
    return albums to representative
}
