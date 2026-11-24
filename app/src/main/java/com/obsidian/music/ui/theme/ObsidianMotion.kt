package com.obsidian.music.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Unified 120Hz-tuned spring physics for Obsidian Music's Liquid Glass interactions.
 */
object ObsidianMotion {
    /** Hero container morph: Mini-Player <-> Full Now Playing */
    val LiquidMorphSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.76f,
        stiffness = 340f
    )

    /** Tactile gel press & rebound for glass buttons and pills */
    val GelBounceSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.46f,
        stiffness = 480f
    )

    /** Sliding refracting capsule inside the bottom navigation dock */
    val DockIndicatorSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.68f,
        stiffness = 380f
    )

    /** 3D swipe-to-skip artwork card spring */
    val ArtworkThrowSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.66f,
        stiffness = 320f
    )

    /** Utility bottom sheet glass drawer spring */
    val SheetSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.78f,
        stiffness = 360f
    )

    /** Favorite heart overshoot bounce */
    val HeartPopSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.38f,
        stiffness = 520f
    )

    val AmbientColorSpec = tween<androidx.compose.ui.graphics.Color>(
        durationMillis = 1050,
        easing = FastOutSlowInEasing
    )
}

/**
 * Tactile gel-press physics: compresses cleanly on touch with an elastic rebound
 * and subtle haptic tick so every glass control feels physically buoyant.
 */
fun Modifier.liquidGelPress(
    pressedScale: Float = 0.93f,
    enableHaptics: Boolean = true
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = ObsidianMotion.GelBounceSpring,
        label = "liquidGelScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enableHaptics) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    if (enableHaptics) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}

/**
 * Fluid staggered entrance animation for track rows, album tiles, and search results.
 */
fun Modifier.staggeredLiquidEntrance(
    index: Int,
    maxStaggerItems: Int = 10
): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(26f) }
    val scale = remember { Animatable(0.96f) }

    LaunchedEffect(Unit) {
        val cappedDelay = (index.coerceAtMost(maxStaggerItems) * 28L)
        if (cappedDelay > 0L) delay(cappedDelay)
        launch {
            alpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
        launch {
            offsetY.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 360f))
        }
        launch {
            scale.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 360f))
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        translationY = offsetY.value
        scaleX = scale.value
        scaleY = scale.value
    }
}
