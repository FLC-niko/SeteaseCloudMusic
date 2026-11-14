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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val DefaultArtistAmbientBg = Color(0xFF28353E)

private object ArtistCoverColorCache {
    private val cache = LruCache<String, Color>(64)

    fun get(url: String?): Color? {
        if (url.isNullOrBlank()) return null
        return synchronized(cache) { cache.get(url) }
    }

    fun put(url: String?, color: Color) {
        if (!url.isNullOrBlank()) {
            synchronized(cache) { cache.put(url, color) }
        }
    }
}

private fun extractArtistAmbientDarkColor(
    bitmap: Bitmap,
    defaultColor: Color = DefaultArtistAmbientBg
): Color {
    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
    val swatch = palette.mutedSwatch
        ?: palette.dominantSwatch
        ?: palette.darkMutedSwatch
        ?: palette.darkVibrantSwatch
        ?: palette.vibrantSwatch
        ?: return defaultColor

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(swatch.rgb, hsl)
    hsl[1] = hsl[1].coerceIn(0.14f, 0.36f)
    hsl[2] = (hsl[2] * 0.46f + 0.16f).coerceIn(0.22f, 0.32f)
    return Color(ColorUtils.HSLToColor(hsl)).copy(alpha = 1f)
}

@Composable
fun rememberArtistAmbientDarkColor(
    imageUrl: String?,
    defaultColor: Color = DefaultArtistAmbientBg
): State<Color> {
    val context = LocalContext.current
    val initialColor = remember(imageUrl) {
        ArtistCoverColorCache.get(imageUrl) ?: defaultColor
    }

    val extractedState = produceState(initialValue = initialColor, key1 = imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            value = defaultColor
            return@produceState
        }

        val cached = ArtistCoverColorCache.get(imageUrl)
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
                    extractArtistAmbientDarkColor(bitmap, defaultColor).also { color ->
                        ArtistCoverColorCache.put(imageUrl, color)
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
