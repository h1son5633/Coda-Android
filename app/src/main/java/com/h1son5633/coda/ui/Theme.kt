package com.h1son5633.coda.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Design tokens copied from the original's DesignSystem/Theme.swift. */
object CodaTheme {
    val accent = Color(0xFFEC4949)
    val accentDeep = Color(0xFFC92929)
    val accentGradientStart = Color(0xFFF65B5B)
    val heartRed = Color(0xFFFF3B30)

    val pagePadding = 20.dp
    val sectionSpacing = 32.dp

    object Radius {
        val badge = 4.dp
        val small = 6.dp
        val standard = 8.dp
        val large = 12.dp
        val cover = 22.dp
        val panel = 20.dp
    }
}

private val LightColors = lightColorScheme(
    primary = CodaTheme.accent,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111113),
    onSurface = Color(0xFF111113),
    surfaceVariant = Color(0xFFF2F2F6)
)

private val DarkColors = darkColorScheme(
    primary = CodaTheme.accent,
    background = Color(0xFF101012),
    surface = Color(0xFF1B1B1E),
    onBackground = Color(0xFFF5F5F7),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF2A2A2E)
)

@Composable
fun CodaAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

/** 1234567 → "123万", matching the original's compact play-count style. */
fun formatPlayCount(count: Long): String = when {
    count >= 100_000_000 -> {
        val v = count / 100_000_000.0
        if (v % 1.0 == 0.0) "${v.toInt()}亿" else String.format("%.1f亿", v)
    }
    count >= 10_000 -> {
        val v = count / 10_000.0
        if (v % 1.0 == 0.0) "${v.toInt()}万" else String.format("%.1f万", v)
    }
    else -> count.toString()
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

fun formatSignedDuration(ms: Long): String = formatDuration(ms)
