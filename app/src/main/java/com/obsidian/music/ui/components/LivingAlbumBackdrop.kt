package com.obsidian.music.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.obsidian.music.ui.theme.ObsidianMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Immutable
data class ObsidianAmbientColors(
    val primary: Color = Color(0xFF6B3DFF),
    val secondary: Color = Color(0xFF00C8FF),
    val tertiary: Color = Color(0xFFFF3D8A),
    val deep: Color = Color(0xFF06070D)
)

/**
 * Extracts a 3-color palette from the active track's artwork Bitmap off the main thread,
 * falling back smoothly to the track's accent colors if no Bitmap is available yet.
 */
@Composable
fun rememberAlbumPalette(
    artworkBitmap: Bitmap?,
    fallbackPrimary: Color = Color(0xFF6B3DFF),
    fallbackSecondary: Color = Color(0xFF00C8FF)
): ObsidianAmbientColors {
    var paletteColors by remember(fallbackPrimary, fallbackSecondary) {
        mutableStateOf(
            ObsidianAmbientColors(
                primary = fallbackPrimary,
                secondary = fallbackSecondary,
                tertiary = fallbackPrimary.copy(red = (fallbackPrimary.red + 0.25f).coerceAtMost(1f))
            )
        )
    }

    LaunchedEffect(artworkBitmap, fallbackPrimary, fallbackSecondary) {
        if (artworkBitmap == null) {
            paletteColors = ObsidianAmbientColors(
                primary = fallbackPrimary,
                secondary = fallbackSecondary,
                tertiary = Color(
                    red = (fallbackPrimary.red * 0.6f + fallbackSecondary.red * 0.4f).coerceIn(0f, 1f),
                    green = (fallbackPrimary.green * 0.4f + fallbackSecondary.green * 0.6f).coerceIn(0f, 1f),
                    blue = (fallbackPrimary.blue * 0.7f + 0.25f).coerceIn(0f, 1f)
                )
            )
            return@LaunchedEffect
        }

        paletteColors = withContext(Dispatchers.Default) {
            val palette = Palette.from(artworkBitmap).maximumColorCount(16).generate()
            val primary = palette.vibrantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
            val secondary = palette.mutedSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb
                ?: palette.lightMutedSwatch?.rgb
            val tertiary = palette.lightVibrantSwatch?.rgb
                ?: palette.vibrantSwatch?.rgb
            val deep = palette.darkMutedSwatch?.rgb

            ObsidianAmbientColors(
                primary = primary?.let { Color(it) } ?: fallbackPrimary,
                secondary = secondary?.let { Color(it) } ?: fallbackSecondary,
                tertiary = tertiary?.let { Color(it) } ?: fallbackPrimary,
                deep = deep?.let { Color(it) } ?: Color(0xFF06070D)
            )
        }
    }

    return paletteColors
}

/**
 * Living 3-Orb Liquid Mesh Backdrop wrapped in `Modifier.layerBackdrop(backdrop)`.
 * Provides rich drifting ambient light behind every refracting glass lens in the app.
 */
@Composable
fun LivingObsidianCanvas(
    backdrop: LayerBackdrop,
    artworkBitmap: Bitmap?,
    isPlaying: Boolean,
    fallbackPrimary: Color = Color(0xFF6B3DFF),
    fallbackSecondary: Color = Color(0xFF00C8FF),
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val target = rememberAlbumPalette(artworkBitmap, fallbackPrimary, fallbackSecondary)

    val c1 by animateColorAsState(target.primary, ObsidianMotion.AmbientColorSpec, label = "orbC1")
    val c2 by animateColorAsState(target.secondary, ObsidianMotion.AmbientColorSpec, label = "orbC2")
    val c3 by animateColorAsState(target.tertiary, ObsidianMotion.AmbientColorSpec, label = "orbC3")
    val deep by animateColorAsState(target.deep, ObsidianMotion.AmbientColorSpec, label = "orbDeep")

    // Gently bloom intensity when playing, rest subtly when paused
    val energy by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.70f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "ambientEnergy"
    )

    val infinite = rememberInfiniteTransition(label = "livingMesh")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "meshPhase"
    )
    val breathe by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meshBreathe"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .layerBackdrop(backdrop)
            .background(Color(0xFF05060B))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val breathFactor = if (isPlaying) breathe else 0.96f

            // Obsidian dark foundation tinted lightly by track mood
            drawRect(color = deep.copy(alpha = 0.24f))

            // Primary luminous orb (upper-left drift)
            val orb1 = Offset(
                x = w * (0.24f + 0.18f * cos(phase)),
                y = h * (0.18f + 0.12f * sin(phase))
            )
            val r1 = w * 1.05f * breathFactor
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c1.copy(alpha = 0.60f * energy), Color.Transparent),
                    center = orb1,
                    radius = r1
                ),
                center = orb1,
                radius = r1
            )

            // Secondary orb (lower-right counter-drift — refracts beautifully behind mini-player & dock!)
            val orb2 = Offset(
                x = w * (0.78f + 0.16f * sin(phase * 0.85f)),
                y = h * (0.76f + 0.13f * cos(phase * 0.85f))
            )
            val r2 = w * 0.96f * breathFactor
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c2.copy(alpha = 0.54f * energy), Color.Transparent),
                    center = orb2,
                    radius = r2
                ),
                center = orb2,
                radius = r2
            )

            // Tertiary accent shimmer orb (mid-screen sweep)
            val orb3 = Offset(
                x = w * (0.50f + 0.28f * cos(phase * 1.25f + 1.4f)),
                y = h * (0.48f + 0.18f * sin(phase * 1.10f + 0.7f))
            )
            val r3 = w * 0.72f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c3.copy(alpha = 0.28f * energy), Color.Transparent),
                    center = orb3,
                    radius = r3
                ),
                center = orb3,
                radius = r3
            )

            // Subtle obsidian vignette to keep edges rich and crisp
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.28f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.52f)
                    )
                )
            )
        }
        content()
    }
}

/**
 * Fallback procedural artwork cover when a track doesn't have an image URI / Bitmap yet.
 */
@Composable
fun ProceduralObsidianArtwork(
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(primary, secondary, Color(0xFF0A0B14))
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.36f), Color.Transparent),
                center = Offset(size.width * 0.3f, size.height * 0.28f),
                radius = size.minDimension * 0.65f
            ),
            center = Offset(size.width * 0.3f, size.height * 0.28f),
            radius = size.minDimension * 0.65f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(size.width * 0.78f, size.height * 0.76f),
                radius = size.minDimension * 0.55f
            ),
            center = Offset(size.width * 0.78f, size.height * 0.76f),
            radius = size.minDimension * 0.55f
        )
    }
}
