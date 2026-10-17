package com.obsidian.music.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Obsidian {
    val Base = Color(0xFF08080C)
    val Surface = Color(0xFF111118)
    val Accent = Color(0xFF8B7CFF)
    val TextPrimary = Color(0xFFF4F3FA)
    val TextSecondary = Color(0xB3F4F3FA)
    val TextTertiary = Color(0x73F4F3FA)
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
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.1).sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 19.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp,
    ),
)

@Composable
fun ObsidianTheme(content: @Composable () -> Unit) {
    // The app is intentionally dark-only: glass reads best over deep colour.
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
