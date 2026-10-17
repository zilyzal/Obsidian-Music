@file:OptIn(ExperimentalTextApi::class)

package com.obsidian.music.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.obsidian.music.R

object Obsidian {
    val Base = Color(0xFF08080C)
    val Surface = Color(0xFF14141D)
    val Accent = Color(0xFFA78BFA)
    val Glacier = Color(0xFF4E78A8)
    val TextPrimary = Color(0xFFF4F3FA)
    val TextSecondary = Color(0xA8F4F3FA) // 66%
    val TextTertiary = Color(0x85F4F3FA) // 52%
    val Scrim = Color(0xA8050509)
    val Ink = Color(0xFF0B0B12)
}

// One variable font file per family; each weight is a variation of it.
private fun variable(resId: Int, weight: FontWeight) =
    Font(resId, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

private val Manrope = FontFamily(
    variable(R.font.manrope, FontWeight.Normal),
    variable(R.font.manrope, FontWeight.Medium),
    variable(R.font.manrope, FontWeight.SemiBold),
    variable(R.font.manrope, FontWeight.Bold),
)

private val Sora = FontFamily(
    variable(R.font.sora, FontWeight.Medium),
    variable(R.font.sora, FontWeight.SemiBold),
    variable(R.font.sora, FontWeight.Bold),
)

/** Sora for headers and titles, Manrope for everything you read in a list. */
object ObsidianType {
    val Pill = TextStyle(
        fontFamily = Sora, fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp, lineHeight = 22.sp, letterSpacing = (-0.015).em,
    )
    val CardTitle = TextStyle(
        fontFamily = Sora, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.02).em,
    )
    val Welcome = TextStyle(
        fontFamily = Sora, fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.025).em,
    )
    val PlayerTitle = TextStyle(
        fontFamily = Sora, fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp, lineHeight = 32.sp, letterSpacing = (-0.02).em,
    )
    val PlayerArtist = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 22.sp,
    )
    val RowTitle = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 21.sp,
    )
    val RowSub = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 19.sp,
    )
    val Duration = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 16.sp, fontFeatureSettings = "tnum",
    )
    val MiniTitle = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp,
    )
    val MiniSub = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp,
    )
    val Overline = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Bold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.14.em,
    )
    val Body = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 24.sp,
    )
    val BodySmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 22.sp,
    )
    val Button = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, lineHeight = 15.sp,
    )
    val ButtonLarge = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Bold,
        fontSize = 17.sp, lineHeight = 17.sp,
    )
    val Caption = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp,
    )
}

private val scheme = darkColorScheme(
    primary = Obsidian.Accent,
    onPrimary = Color.White,
    background = Obsidian.Base,
    onBackground = Obsidian.TextPrimary,
    surface = Obsidian.Surface,
    onSurface = Obsidian.TextPrimary,
)

private val type = Typography(
    headlineSmall = ObsidianType.Pill,
    titleMedium = ObsidianType.RowTitle,
    bodyMedium = ObsidianType.RowSub,
    labelMedium = ObsidianType.Duration,
)

@Composable
fun ObsidianTheme(content: @Composable () -> Unit) {
    // The app is intentionally dark-only: glass reads best over deep colour.
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
