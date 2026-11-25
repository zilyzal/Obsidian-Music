package com.obsidian.music.ui

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch

/** Real refraction needs runtime shaders (Android 13+). Older phones get frosted obsidian glass. */
val TrueGlassSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** The layer every floating glass element on the current screen refracts. */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** Optical presets. Thicker glass bends more light at its rim; heavier frost keeps text readable. */
enum class GlassStyle(
    val blur: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val surface: Float,
    val aberration: Boolean,
) {
    /** Small pills and round buttons that float over the list. */
    Pill(blur = 6.dp, refractionHeight = 14.dp, refractionAmount = 26.dp, surface = 0.30f, aberration = true),

    /** Mini player and menus: frosted enough that nothing behind competes with their text. */
    Panel(blur = 18.dp, refractionHeight = 18.dp, refractionAmount = 32.dp, surface = 0.50f, aberration = true),

    /** Thick lens for the big play button and the player deck. */
    Lens(blur = 3.dp, refractionHeight = 22.dp, refractionAmount = 40.dp, surface = 0.16f, aberration = true),

    /** Bottom sheets over the player: heavy frost, gentle rim. */
    Sheet(blur = 26.dp, refractionHeight = 20.dp, refractionAmount = 30.dp, surface = 0.58f, aberration = false),

    /** Seek thumb: clear glass that magnifies the bar beneath it. */
    Thumb(blur = 0.5.dp, refractionHeight = 8.dp, refractionAmount = 18.dp, surface = 0f, aberration = true),
}

/** Specular rim: a bright top-left catch-light that fades around the edge, with a faint lower glint. */
private fun rimBrush(strength: Float, accent: Color): Brush = Brush.linearGradient(
    0f to Color.White.copy(alpha = 0.55f * strength),
    0.32f to Color.White.copy(alpha = 0.10f * strength),
    0.68f to accent.copy(alpha = 0.16f * strength),
    1f to Color.White.copy(alpha = 0.30f * strength),
    start = Offset.Zero,
    end = Offset.Infinite,
)

private val sheen = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.10f),
    0.45f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.10f),
)

/**
 * Liquid glass: the content behind is sampled, bent at the rim like a thick lens, lightly
 * blurred, saturated and tinted, then lit with a specular rim.
 *
 * [press] (0..1) swells the glass while it is held, the way a drop of liquid bulges under a
 * finger; only the glass layer scales, so the refracted image stays put.
 */
fun Modifier.liquidGlass(
    shape: Shape,
    style: GlassStyle = GlassStyle.Pill,
    tint: Color = Color.Transparent,
    tintAlpha: Float = 0f,
    rim: Float = 1f,
    press: () -> Float = { 0f },
    backdrop: Backdrop? = null,
): Modifier = composed {
    val source = backdrop ?: LocalGlassBackdrop.current
    val rimBrush = remember(rim, tint) { rimBrush(rim, if (tint == Color.Transparent) Color.White else tint) }
    if (source == null || !TrueGlassSupported) {
        this
            .graphicsLayer {
                val s = 1f + 0.06f * press()
                scaleX = s; scaleY = s
            }
            .glassPanel(shape, tint = if (tint == Color.Transparent) Obsidian.Accent else tint, tintAlpha = 0.10f + tintAlpha * 0.6f)
    } else {
        this
            .drawBackdrop(
                backdrop = source,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(style.blur.toPx())
                    lens(
                        refractionHeight = style.refractionHeight.toPx(),
                        refractionAmount = style.refractionAmount.toPx(),
                        chromaticAberration = style.aberration,
                    )
                },
                layerBlock = {
                    val p = press()
                    // A drop bulges a little wider than it grows tall.
                    scaleX = 1f + 0.075f * p
                    scaleY = 1f + 0.055f * p
                },
                onDrawSurface = {
                    drawRect(Obsidian.Ink.copy(alpha = style.surface))
                    if (tintAlpha > 0f) drawRect(tint.copy(alpha = tintAlpha))
                    drawRect(sheen)
                },
            )
            .border(1.dp, rimBrush, shape)
    }
}

/**
 * Tap handling for liquid glass: tracks finger-down as a springy 0..1 value for [liquidGlass]'s
 * press, gives a light haptic tick and fires [onClick] on release.
 */
class LiquidPress internal constructor(internal val anim: Animatable<Float, AnimationVector1D>) {
    val value: Float get() = anim.value
}

@Composable
fun rememberLiquidPress(): LiquidPress = remember { LiquidPress(Animatable(0f)) }

fun Modifier.liquidClickable(
    press: LiquidPress,
    enabled: Boolean = true,
    haptic: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    if (!enabled) return@composed this
    val anim = press.anim
    val latest = rememberUpdatedState(onClick)
    this.pointerInput(press) {
        val down = spring<Float>(dampingRatio = 0.55f, stiffness = 420f)
        val up = spring<Float>(dampingRatio = 0.38f, stiffness = 260f)
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            scope.launch { anim.animateTo(1f, down) }
            val release = waitForUpOrCancellation()
            scope.launch { anim.animateTo(0f, up) }
            if (release != null) {
                release.consume()
                if (haptic) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                latest.value()
            }
        }
    }
}

/** Frosted look for lists drawn inside a glass sheet (nested refraction isn't possible). */
fun Modifier.frostRow(shape: Shape, selected: Boolean, accent: Color): Modifier = this
    .clip(shape)
    .background(
        if (selected) Brush.linearGradient(listOf(accent.copy(alpha = 0.26f), Color.White.copy(alpha = 0.06f)))
        else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)))
    )
    .border(
        0.8.dp,
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = if (selected) 0.36f else 0.12f),
                Color.White.copy(alpha = 0.02f),
                if (selected) accent.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.06f),
            )
        ),
        shape,
    )
