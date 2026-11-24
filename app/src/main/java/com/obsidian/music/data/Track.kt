package com.obsidian.music.data

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAddedSec: Long,
    val uri: Uri,
    val mime: String = "",
    val bitrateKbps: Int = 0,
) {
    val artworkUri: Uri
        get() = Uri.parse("content://media/external/audio/albumart/$albumId")

    /** Short, honest description of the file: "FLAC", "MP3 · 320 kbps", ... */
    val qualityLabel: String
        get() {
            val m = mime.lowercase()
            val (format, lossless) = when {
                "flac" in m -> "FLAC" to true
                "wav" in m -> "WAV" to true
                "mpeg" in m || "mp3" in m -> "MP3" to false
                "opus" in m -> "OPUS" to false
                "ogg" in m || "vorbis" in m -> "OGG" to false
                "mp4" in m || "aac" in m || "m4a" in m -> "AAC" to false
                m.isBlank() -> "AUDIO" to true
                else -> m.substringAfterLast('/').removePrefix("x-").uppercase() to false
            }
            return if (!lossless && bitrateKbps > 0) "$format · $bitrateKbps kbps" else format
        }
}
