package com.obsidian.music.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.draw.drawBehind

/**
 * Frosted-glass surface: a soft white-to-clear fill plus a bright top-left
 * hairline that fades toward the bottom-right, like light catching glass.
 * It sits on top of AmbientBackground, whose colour gives the glass something to refract.
 */
fun Modifier.glass(
    shape: Shape,
    fillAlpha: Float = 0.08f,
    borderAlpha: Float = 0.32f,
): Modifier = this
    .clip(shape)
    .background(
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = fillAlpha + 0.05f),
                Color.White.copy(alpha = fillAlpha * 0.6f),
            ),
            start = Offset.Zero,
            end = Offset.Infinite,
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha),
                Color.White.copy(alpha = 0.04f),
                Color.White.copy(alpha = borderAlpha * 0.35f),
            ),
            start = Offset.Zero,
            end = Offset.Infinite,
        ),
        shape = shape,
    )

/**
 * Deep obsidian base with two soft colour glows tinted by the current song's artwork.
 * Colours animate with a short tween when the song changes; nothing animates forever,
 * so an idle screen costs no battery.
 */
@Composable
fun AmbientBackground(accent: Color, modifier: Modifier = Modifier) {
    val glowA by animateColorAsState(accent, tween(900), label = "glowA")
    val glowB by animateColorAsState(
        accent.copy(red = accent.blue, blue = accent.red),
        tween(1200),
        label = "glowB",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(Obsidian.Base)
            .drawBehind {
                val r = size.maxDimension * 0.75f
                drawRect(
                    brush = ShaderBrush(
                        RadialGradientShader(
                            center = Offset(size.width * 0.15f, size.height * 0.08f),
                            radius = r,
                            colors = listOf(glowA.copy(alpha = 0.42f), Color.Transparent),
                        )
                    ),
                    size = Size(size.width, size.height),
                )
                drawRect(
                    brush = ShaderBrush(
                        RadialGradientShader(
                            center = Offset(size.width * 0.95f, size.height * 0.85f),
                            radius = r * 0.9f,
                            colors = listOf(glowB.copy(alpha = 0.30f), Color.Transparent),
                        )
                    ),
                    size = Size(size.width, size.height),
                )
            }
    )
}
