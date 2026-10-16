package com.obsidian.music.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.obsidian.music.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Art(val bitmap: Bitmap, val accent: Color)

/** Loads embedded or album artwork once per album and keeps a small in-memory cache. */
object ArtworkLoader {
    private val cache = object : LruCache<Long, Art>(48) {}
    private val misses = HashSet<Long>()

    private fun key(t: Track) = if (t.albumId != 0L) t.albumId else -t.id

    suspend fun load(context: Context, track: Track): Art? {
        val k = key(track)
        cache.get(k)?.let { return it }
        if (synchronized(misses) { k in misses }) return null

        val art = withContext(Dispatchers.IO) {
            val bmp = decode(context, track) ?: return@withContext null
            val swatch = Palette.from(bmp).maximumColorCount(12).generate().let { p ->
                p.vibrantSwatch ?: p.lightVibrantSwatch ?: p.mutedSwatch ?: p.dominantSwatch
            }
            Art(bmp, swatch?.let { Color(it.rgb) } ?: Obsidian.Accent)
        }
        if (art != null) cache.put(k, art) else synchronized(misses) { misses += k }
        return art
    }

    private fun decode(context: Context, track: Track): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                return context.contentResolver.loadThumbnail(track.uri, Size(512, 512), null)
            }
        }
        return runCatching {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(context, track.uri)
                r.embeddedPicture?.let { bytes ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                }
            } finally {
                r.release()
            }
        }.getOrNull()
    }
}

@Composable
fun rememberArt(track: Track?): State<Art?> {
    val context = LocalContext.current.applicationContext
    return produceState<Art?>(null, track?.id) {
        value = if (track == null) null else ArtworkLoader.load(context, track)
    }
}

@Composable
fun rememberAccent(track: Track?): Color {
    val art by rememberArt(track)
    return art?.accent ?: Obsidian.Accent
}

/** Square artwork with a soft crossfade; falls back to a generated gradient tile. */
@Composable
fun Artwork(track: Track?, shape: Shape, modifier: Modifier = Modifier) {
    val art by rememberArt(track)
    Crossfade(targetState = art, animationSpec = tween(320), label = "artwork", modifier = modifier.clip(shape)) { a ->
        if (a != null) {
            Image(
                bitmap = a.bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val hue = ((track?.id ?: 0L) * 47 % 360).toFloat()
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.hsv(hue, 0.55f, 0.55f),
                                Color.hsv((hue + 40f) % 360f, 0.7f, 0.22f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxSize(0.42f),
                )
            }
        }
    }
}
