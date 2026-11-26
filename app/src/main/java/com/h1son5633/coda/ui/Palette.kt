package com.h1son5633.coda.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class ArtworkPalette(val primary: Color, val secondary: Color) {
    companion object {
        val Fallback = ArtworkPalette(
            primary = Color(0.16f, 0.16f, 0.20f),
            secondary = Color(0.09f, 0.09f, 0.12f)
        )
    }
}

private val cache = mutableMapOf<String, ArtworkPalette>()

/**
 * Port of the original's ArtworkPalette (kaset-style): shrink the cover to
 * 10×10, weight each pixel by saturation × (1 − |lightness − 0.55|), take the
 * circular mean hue; saturation ×1.15 (cap 0.72); primary lightness clamped
 * to 0.32–0.50, secondary = hue +0.06, lightness ×0.5 clamped to 0.12–0.26.
 */
@Composable
fun rememberArtworkPalette(url: String): ArtworkPalette {
    val context = LocalContext.current
    val state = produceState(ArtworkPalette.Fallback, url) {
        if (url.isBlank()) return@produceState
        cache[url]?.let { value = it; return@produceState }
        val palette = extractPalette(context, url)
        if (palette != null) {
            cache[url] = palette
            value = palette
        }
    }
    return state.value
}

private suspend fun extractPalette(context: Context, url: String): ArtworkPalette? {
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(10, 10)
        .allowHardware(false)
        .build()
    val result = ImageLoader(context).execute(request)
    if (result !is SuccessResult) return null
    val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        ?: return null
    return paletteFromBitmap(bitmap)
}

fun paletteFromBitmap(bitmap: Bitmap): ArtworkPalette {
    val scaled = Bitmap.createScaledBitmap(bitmap, 10, 10, true)
    var weightSum = 0.0
    var hueX = 0.0
    var hueY = 0.0
    var satSum = 0.0
    var lightSum = 0.0
    val hsl = FloatArray(3)
    for (x in 0 until 10) {
        for (y in 0 until 10) {
            val pixel = scaled.getPixel(x, y)
            ColorUtils.RGBToHSL(
                android.graphics.Color.red(pixel),
                android.graphics.Color.green(pixel),
                android.graphics.Color.blue(pixel),
                hsl
            )
            val saturation = hsl[1].toDouble()
            val lightness = hsl[2].toDouble()
            val weight = saturation * (1 - abs(lightness - 0.55))
            if (weight <= 0.001) continue
            val hueRad = Math.toRadians(hsl[0].toDouble())
            hueX += cos(hueRad) * weight
            hueY += sin(hueRad) * weight
            satSum += saturation * weight
            lightSum += lightness * weight
            weightSum += weight
        }
    }
    if (weightSum <= 0) return ArtworkPalette.Fallback
    var hue = Math.toDegrees(atan2(hueY, hueX))
    if (hue < 0) hue += 360
    val saturation = (satSum / weightSum * 1.15).coerceAtMost(0.72)
    val lightness = lightSum / weightSum
    val primary = hslColor(hue, saturation, lightness.coerceIn(0.32, 0.50))
    val secondary = hslColor(
        (hue + 21.6) % 360,
        (saturation * 1.1).coerceAtMost(0.8),
        (lightness * 0.5).coerceIn(0.12, 0.26)
    )
    return ArtworkPalette(primary, secondary)
}

private fun hslColor(hue: Double, saturation: Double, lightness: Double): Color =
    Color(
        ColorUtils.HSLToColor(
            floatArrayOf(hue.toFloat(), saturation.toFloat(), lightness.toFloat())
        )
    )
