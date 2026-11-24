package com.obsidian.music.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * Global accessibility / performance toggle for True Liquid Glass vs. high-contrast obsidian glass.
 */
val LocalReduceTransparency = compositionLocalOf { false }

/**
 * Optical presets tuned specifically for dark-mode music playback over vibrant artwork.
 */
enum class ObsidianGlassStyle(
    val blurRadius: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val surfaceScrimAlpha: Float,
    val chromaticAberration: Boolean,
    val topSpecularAlpha: Float,
    val bottomSpecularAlpha: Float
) {
    /** Floating mini-player & bottom navigation dock */
    LiquidPill(
        blurRadius = 16.dp,
        refractionHeight = 20.dp,
        refractionAmount = 34.dp,
        surfaceScrimAlpha = 0.42f,
        chromaticAberration = true,
        topSpecularAlpha = 0.44f,
        bottomSpecularAlpha = 0.18f
    ),
    /** Full-width control decks, lyrics, EQ, and queue sheets (readability-first) */
    ObsidianSheet(
        blurRadius = 24.dp,
        refractionHeight = 14.dp,
        refractionAmount = 22.dp,
        surfaceScrimAlpha = 0.48f,
        chromaticAberration = false,
        topSpecularAlpha = 0.34f,
        bottomSpecularAlpha = 0.12f
    ),
    /** Circular play/pause lens, back button, favorite lens */
    GlassLensButton(
        blurRadius = 6.dp,
        refractionHeight = 18.dp,
        refractionAmount = 28.dp,
        surfaceScrimAlpha = 0.22f,
        chromaticAberration = true,
        topSpecularAlpha = 0.56f,
        bottomSpecularAlpha = 0.24f
    ),
    /** Small filter chips, quality badges, and sliding dock indicator */
    GlassChip(
        blurRadius = 8.dp,
        refractionHeight = 12.dp,
        refractionAmount = 20.dp,
        surfaceScrimAlpha = 0.24f,
        chromaticAberration = true,
        topSpecularAlpha = 0.40f,
        bottomSpecularAlpha = 0.16f
    )
}

val PillShape: Shape = CircleShape
val DeckShape: Shape = RoundedCornerShape(34.dp)
val CardGlassShape: Shape = RoundedCornerShape(24.dp)

/**
 * Applies True AGSL Liquid Glass (io.github.kyant0:backdrop:2.0.1) on Android 13+ (API 33+)
 * with automatic high-craft translucent obsidian fallback on older devices or when
 * Reduce Transparency is enabled.
 *
 * IMPORTANT: The sampled background MUST be a sibling composable using Modifier.layerBackdrop(backdrop),
 * NEVER an ancestor of this modifier.
 */
fun Modifier.obsidianLiquidGlass(
    backdrop: Backdrop,
    shape: Shape = CardGlassShape,
    style: ObsidianGlassStyle = ObsidianGlassStyle.LiquidPill,
    tintColor: Color = Color(0xFF0A0C14),
    specularColor: Color = Color.White,
    accentGlowColor: Color = Color.Transparent,
    forceReduceTransparency: Boolean? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null
): Modifier = composed {
    val reduceTransparency = forceReduceTransparency ?: LocalReduceTransparency.current

    val rimBrush = remember(specularColor, style, accentGlowColor) {
        Brush.linearGradient(
            colors = listOf(
                specularColor.copy(alpha = style.topSpecularAlpha),
                specularColor.copy(alpha = 0.09f),
                if (accentGlowColor != Color.Transparent) {
                    accentGlowColor.copy(alpha = 0.18f)
                } else {
                    specularColor.copy(alpha = 0.04f)
                },
                specularColor.copy(alpha = style.bottomSpecularAlpha)
            ),
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val sheenBrush = remember(specularColor) {
        Brush.verticalGradient(
            colors = listOf(
                specularColor.copy(alpha = 0.12f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.16f)
            )
        )
    }

    val useFallback = reduceTransparency || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    if (useFallback) {
        this
            .clip(shape)
            .background(
                tintColor.copy(
                    alpha = if (reduceTransparency) 0.90f else (style.surfaceScrimAlpha + 0.42f).coerceAtMost(0.84f)
                )
            )
            .background(sheenBrush)
            .border(width = 1.dp, brush = rimBrush, shape = shape)
    } else {
        this
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(style.blurRadius.toPx())
                    lens(
                        refractionHeight = style.refractionHeight.toPx(),
                        refractionAmount = style.refractionAmount.toPx(),
                        chromaticAberration = style.chromaticAberration
                    )
                },
                layerBlock = layerBlock,
                onDrawSurface = {
                    drawRect(tintColor.copy(alpha = style.surfaceScrimAlpha))
                    drawRect(sheenBrush)
                }
            )
            .border(width = 1.dp, brush = rimBrush, shape = shape)
    }
}

/**
 * Subtle frosted card for list rows & tiles inside a scrollable layerBackdrop (no nested drawBackdrop!).
 */
fun Modifier.obsidianInnerGlassCard(
    shape: Shape = RoundedCornerShape(20.dp),
    selected: Boolean = false,
    accentColor: Color = Color(0xFF7B61FF)
): Modifier = composed {
    val bg = if (selected) {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.24f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.085f),
                Color.White.copy(alpha = 0.035f)
            )
        )
    }

    val rim = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (selected) 0.40f else 0.18f),
            Color.White.copy(alpha = 0.03f),
            if (selected) accentColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)
        )
    )

    this
        .clip(shape)
        .background(bg)
        .border(width = 0.85.dp, brush = rim, shape = shape)
}

@Composable
fun GlassSurfaceBox(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = CardGlassShape,
    style: ObsidianGlassStyle = ObsidianGlassStyle.LiquidPill,
    tintColor: Color = Color(0xFF0A0C14),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.obsidianLiquidGlass(
            backdrop = backdrop,
            shape = shape,
            style = style,
            tintColor = tintColor
        ),
        contentAlignment = contentAlignment,
        content = content
    )
}
