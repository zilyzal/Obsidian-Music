package com.obsidian.music.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads every music file the system media scanner knows about.
 * One MediaStore query, newest first; no background polling, so no idle battery cost.
 */
class MusicRepository(private val context: Context) {

    suspend fun loadTracks(): List<Track> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val hasBitrate = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.MIME_TYPE)
            if (hasBitrate) add(MediaStore.Audio.Media.BITRATE)
        }.toTypedArray()

        // Real music only: skips ringtones, notification sounds and clips under 30 seconds.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= 30000"
        val sort = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val result = ArrayList<Track>()
        context.contentResolver.query(collection, projection, selection, null, sort)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val bitrateCol = if (hasBitrate) c.getColumnIndex(MediaStore.Audio.Media.BITRATE) else -1

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val artist = c.getString(artistCol).orEmpty()
                result += Track(
                    id = id,
                    title = c.getString(titleCol).orEmpty().ifBlank { "Unknown title" },
                    artist = if (artist.isBlank() || artist == "<unknown>") "Unknown artist" else artist,
                    album = c.getString(albumCol).orEmpty(),
                    albumId = c.getLong(albumIdCol),
                    durationMs = c.getLong(durCol),
                    dateAddedSec = c.getLong(addedCol),
                    uri = ContentUris.withAppendedId(collection, id),
                    mime = c.getString(mimeCol).orEmpty(),
                    bitrateKbps = if (bitrateCol >= 0) (c.getInt(bitrateCol) / 1000).coerceAtLeast(0) else 0,
                )
            }
        }
        result
    }
}
