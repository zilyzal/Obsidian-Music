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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.obsidian.music.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads embedded or album artwork once per album.
 *
 * Two sizes, because a list row needs a few KB of pixels and the full player needs a lot
 * more: small covers (rows, mini player, accent colour) and large covers (full player).
 * Both caches are sized in bytes, so memory stays bounded however big the covers are.
 */
object ArtworkLoader {
    private const val SMALL_PX = 192
    private const val LARGE_PX = 720

    private fun bytesCache(maxBytes: Int) = object : LruCache<Long, Bitmap>(maxBytes) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }

    private val smallCache = bytesCache(12 * 1024 * 1024)
    private val largeCache = bytesCache(16 * 1024 * 1024)
    private val smallMisses = HashSet<Long>()
    private val largeMisses = HashSet<Long>()
    private val accents = HashMap<Long, Color>()

    private fun key(t: Track) = if (t.albumId != 0L) t.albumId else -t.id

    /** Already-decoded small cover, if there is one. Lets rows draw on their first frame. */
    fun peekSmall(track: Track): Bitmap? = smallCache.get(key(track))

    suspend fun small(context: Context, track: Track): Bitmap? =
        load(context, track, SMALL_PX, smallCache, smallMisses)

    suspend fun large(context: Context, track: Track): Bitmap? =
        load(context, track, LARGE_PX, largeCache, largeMisses)

    /** Accent colour taken from the cover. Computed only for the song that is playing. */
    suspend fun accent(context: Context, track: Track): Color? {
        val k = key(track)
        synchronized(accents) { accents[k] }?.let { return it }
        val bmp = small(context, track) ?: return null
        val color = withContext(Dispatchers.Default) {
            runCatching {
                val p = Palette.from(bmp).maximumColorCount(12).generate()
                val swatch = p.vibrantSwatch ?: p.lightVibrantSwatch ?: p.mutedSwatch ?: p.dominantSwatch
                swatch?.let { vivid(it.rgb) }
            }.getOrNull()
        } ?: return null
        synchronized(accents) { accents[k] = color }
        return color
    }

    /** Keeps glows and progress readable on black: not too dark, not too grey. */
    private fun vivid(rgb: Int): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(rgb, hsl)
        if (hsl[1] < 0.10f) return Obsidian.Accent // greyscale cover: keep the house colour
        hsl[1] = hsl[1].coerceAtLeast(0.45f)
        hsl[2] = hsl[2].coerceIn(0.60f, 0.76f)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    private suspend fun load(
        context: Context,
        track: Track,
        px: Int,
        cache: LruCache<Long, Bitmap>,
        misses: HashSet<Long>,
    ): Bitmap? {
        val k = key(track)
        cache.get(k)?.let { return it }
        if (synchronized(misses) { k in misses }) return null

        val bmp = withContext(Dispatchers.IO) { decode(context, track, px) }
        if (bmp != null) cache.put(k, bmp) else synchronized(misses) { misses += k }
        return bmp
    }

    private fun decode(context: Context, track: Track, px: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val thumb = runCatching {
                context.contentResolver.loadThumbnail(track.uri, Size(px, px), null)
            }.getOrNull()
            if (thumb != null) return thumb
        }
        return runCatching {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(context, track.uri)
                r.embeddedPicture?.let { bytes -> decodeSampled(bytes, px) }
            } finally {
                r.release()
            }
        }.getOrNull()
    }

    /** Decodes embedded art at the smallest power-of-two scale that is still at least [px]. */
    private fun decodeSampled(bytes: ByteArray, px: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= px && bounds.outHeight / (sample * 2) >= px) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}

@Composable
fun rememberAccent(track: Track?): Color {
    val context = LocalContext.current.applicationContext
    val accent by produceState(Obsidian.Accent, track?.id) {
        value = if (track == null) Obsidian.Accent else ArtworkLoader.accent(context, track) ?: Obsidian.Accent
    }
    return accent
}

/**
 * Square artwork with a soft crossfade; falls back to a generated gradient tile.
 * With [large] set it shows the small cover straight away and swaps in the sharp one.
 */
@Composable
fun Artwork(track: Track?, shape: Shape, modifier: Modifier = Modifier, large: Boolean = false) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<Bitmap?>(track?.let { ArtworkLoader.peekSmall(it) }, track?.id, large) {
        if (track == null) {
            value = null
        } else if (large) {
            value = ArtworkLoader.large(context, track) ?: ArtworkLoader.small(context, track)
        } else if (value == null) {
            // Rows scrolled back into view already hold their cover: nothing to load.
            value = ArtworkLoader.small(context, track)
        }
    }

    Crossfade(targetState = bitmap, animationSpec = tween(280), label = "artwork", modifier = modifier.clip(shape)) { bmp ->
        if (bmp != null) {
            val image = remember(bmp) { bmp.asImageBitmap() }
            Image(
                bitmap = image,
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
