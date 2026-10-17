package com.obsidian.music.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/** Edge light: brightest along the top, settling to a soft hairline around the rest. */
private fun edgeBrush(strength: Float): Brush = Brush.verticalGradient(
    0f to Color.White.copy(alpha = strength),
    0.45f to Color.White.copy(alpha = strength * 0.55f),
    1f to Color.White.copy(alpha = strength * 0.45f),
)

/**
 * See-through glass for small controls that sit on empty background (view pill,
 * shuffle, close). Never use it over list content: text would show through.
 */
fun Modifier.glass(
    shape: Shape,
    tint: Color = Color.Transparent,
    edge: Float = 0.42f,
): Modifier = this
    .clip(shape)
    .background(
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.05f),
            ),
            start = Offset.Zero,
            end = Offset.Infinite,
        )
    )
    .background(tint)
    .border(width = 1.dp, brush = edgeBrush(edge), shape = shape)

/**
 * Glass for panels that float above scrolling content (mini player, dropdown menu, cards).
 *
 * A see-through fill lets list text show through and collide with the panel's own text,
 * so this sits on an opaque dark base and only *looks* like glass: an accent tint,
 * a soft top-left sheen and a bright top edge.
 */
fun Modifier.glassPanel(
    shape: Shape,
    tint: Color,
    tintAlpha: Float = 0.16f,
    sheen: Float = 0.11f,
    edge: Float = 0.32f,
): Modifier = this
    .clip(shape)
    .background(Obsidian.Surface)
    .background(tint.copy(alpha = tintAlpha))
    .background(
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = sheen),
                Color.White.copy(alpha = 0.02f),
            ),
            start = Offset.Zero,
            end = Offset.Infinite,
        )
    )
    .border(width = 1.dp, brush = edgeBrush(edge), shape = shape)

/** Which set of glass shards sits behind the screen. */
enum class Backdrop { Home, Player, Welcome }

/**
 * Deep obsidian base with two soft glows tinted by the current song's artwork and a few
 * faint glass shards. Colours animate with a short tween when the song changes; nothing
 * animates forever, so an idle screen costs no battery.
 */
@Composable
fun AmbientBackground(accent: Color, modifier: Modifier = Modifier, backdrop: Backdrop = Backdrop.Home) {
    val glowA by animateColorAsState(accent, tween(900), label = "glowA")
    val glowB by animateColorAsState(
        lerp(Obsidian.Glacier, accent, 0.45f),
        tween(1200),
        label = "glowB",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(Obsidian.Base)
            .drawBehind {
                val big = size.maxDimension
                val (ax, ay, ar) = when (backdrop) {
                    Backdrop.Home -> Triple(0.12f, 0.06f, 0.80f)
                    Backdrop.Player -> Triple(0.50f, 0.30f, 0.72f)
                    Backdrop.Welcome -> Triple(0.50f, 0.20f, 0.70f)
                }
                val (bx, by, br) = when (backdrop) {
                    Backdrop.Home -> Triple(0.96f, 0.90f, 0.72f)
                    Backdrop.Player -> Triple(1.00f, 1.00f, 0.80f)
                    Backdrop.Welcome -> Triple(0.96f, 0.92f, 0.72f)
                }
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glowA.copy(alpha = 0.48f), Color.Transparent),
                        center = Offset(size.width * ax, size.height * ay),
                        radius = big * ar,
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glowB.copy(alpha = 0.38f), Color.Transparent),
                        center = Offset(size.width * bx, size.height * by),
                        radius = big * br,
                    )
                )

                val s = size.width / 390f
                val tint = Color(0xFFC9B8FF)
                fun at(x: Float, y: Float) = Offset(x * s, y * s)
                when (backdrop) {
                    Backdrop.Home -> {
                        shard(listOf(at(226f, 0f), at(390f, 0f), at(390f, 262f), at(316f, 148f)), tint, 0.27f, 1f)
                        shard(listOf(at(292f, 0f), at(390f, 0f), at(390f, 118f)), tint, 0.27f, 1f)
                        shard(listOf(at(316f, 148f), at(390f, 262f), at(350f, 226f)), tint, 0.27f, 0.7f)
                    }
                    Backdrop.Player -> {
                        val t = Color(0xFFE7D6FF)
                        shard(listOf(at(0f, 150f), at(120f, 60f), at(150f, 320f), at(0f, 420f)), t, 0.25f, 1f)
                        shard(listOf(at(390f, 120f), at(270f, 230f), at(390f, 380f)), t, 0.25f, 1f)
                    }
                    Backdrop.Welcome -> {
                        shard(listOf(at(0f, 0f), at(150f, 0f), at(0f, 230f)), tint, 0.25f, 1f)
                        shard(listOf(at(390f, 250f), at(390f, 520f), at(300f, 380f)), tint, 0.25f, 1f)
                    }
                }
            }
    )
}

private fun DrawScope.shard(points: List<Offset>, color: Color, alpha: Float, opacity: Float) {
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        close()
    }
    val minX = points.minOf { it.x }
    val minY = points.minOf { it.y }
    val maxX = points.maxOf { it.x }
    val maxY = points.maxOf { it.y }
    drawPath(
        path,
        Brush.linearGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            start = Offset(minX, minY),
            end = Offset(maxX, maxY),
        ),
        alpha = opacity,
    )
}
