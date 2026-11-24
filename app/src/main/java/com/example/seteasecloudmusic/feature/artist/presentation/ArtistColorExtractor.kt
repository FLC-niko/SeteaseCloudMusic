package com.example.seteasecloudmusic.feature.artist.presentation

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.example.seteasecloudmusic.core.ui.theme.tuneAmbientHslForTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val DefaultArtistAmbientLightBg = Color(0xFFF0F2F5)
val DefaultArtistAmbientDarkBg = Color(0xFF171E24)
val DefaultArtistAmbientBg = DefaultArtistAmbientDarkBg

private object ArtistCoverColorCache {
    private val cache = LruCache<String, Color>(128)

    private fun key(url: String, isDark: Boolean): String = "${if (isDark) "d" else "l"}|$url"

    fun get(url: String?, isDark: Boolean): Color? {
        if (url.isNullOrBlank()) return null
        return synchronized(cache) { cache.get(key(url, isDark)) }
    }

    fun put(url: String?, color: Color, isDark: Boolean) {
        if (!url.isNullOrBlank()) {
            synchronized(cache) { cache.put(key(url, isDark), color) }
        }
    }
}

private fun extractArtistAmbientDarkColor(
    bitmap: Bitmap,
    isDark: Boolean,
    defaultColor: Color = if (isDark) DefaultArtistAmbientDarkBg else DefaultArtistAmbientLightBg
): Color {
    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
    val swatch = if (isDark) {
        palette.mutedSwatch
            ?: palette.dominantSwatch
            ?: palette.darkMutedSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.vibrantSwatch
    } else {
        val dominant = palette.dominantSwatch
        if (dominant != null && dominant.hsl[1] >= 0.18f) {
            dominant
        } else {
            palette.vibrantSwatch
                ?: palette.lightVibrantSwatch
                ?: dominant
                ?: palette.mutedSwatch
                ?: palette.darkVibrantSwatch
                ?: palette.darkMutedSwatch
        }
    } ?: return defaultColor

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(swatch.rgb, hsl)
    tuneAmbientHslForTheme(hsl, isDark)
    return Color(ColorUtils.HSLToColor(hsl)).copy(alpha = 1f)
}

@Composable
fun rememberArtistAmbientDarkColor(
    imageUrl: String?,
    isDark: Boolean = AppTheme.isDark,
    defaultColor: Color = if (isDark) DefaultArtistAmbientDarkBg else DefaultArtistAmbientLightBg
): State<Color> {
    val context = LocalContext.current
    val initialColor = remember(imageUrl, isDark, defaultColor) {
        ArtistCoverColorCache.get(imageUrl, isDark) ?: defaultColor
    }

    val extractedState = produceState(
        initialValue = initialColor,
        key1 = imageUrl,
        key2 = isDark,
        key3 = defaultColor
    ) {
        if (imageUrl.isNullOrBlank()) {
            value = defaultColor
            return@produceState
        }

        val cached = ArtistCoverColorCache.get(imageUrl, isDark)
        if (cached != null) {
            value = cached
            return@produceState
        }

        val targetColor = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(128)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                        drawable.bitmap
                    } else {
                        val w = drawable.intrinsicWidth.coerceIn(1, 128)
                        val h = drawable.intrinsicHeight.coerceIn(1, 128)
                        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
                            val canvas = android.graphics.Canvas(bmp)
                            drawable.setBounds(0, 0, w, h)
                            drawable.draw(canvas)
                        }
                    }
                    extractArtistAmbientDarkColor(bitmap, isDark, defaultColor).also { color ->
                        ArtistCoverColorCache.put(imageUrl, color, isDark)
                    }
                } else {
                    defaultColor
                }
            }.getOrElse { defaultColor }
        }

        value = targetColor
    }

    return animateColorAsState(
        targetValue = extractedState.value,
        animationSpec = tween(durationMillis = 420),
        label = "ArtistAmbientDarkColor"
    )
}
