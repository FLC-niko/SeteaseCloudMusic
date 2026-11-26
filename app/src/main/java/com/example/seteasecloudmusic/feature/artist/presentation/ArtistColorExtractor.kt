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

/** 浅色主题下封面未加载或取色失败时的默认氛围背景色 */
val DefaultArtistAmbientLightBg = Color(0xFFF0F2F5)
/** 深色主题下封面未加载或取色失败时的默认氛围背景色 */
val DefaultArtistAmbientDarkBg = Color(0xFF171E24)
/** 默认氛围背景色别名（指向深色兜底色） */
val DefaultArtistAmbientBg = DefaultArtistAmbientDarkBg

/**
 * 歌手封面氛围色内存 LRU 缓存，按深浅主题与图片 URL 隔离键。
 */
private object ArtistCoverColorCache {
    // 缓存最大条目数（条，超出按 LRU 淘汰）
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

/**
 * 从歌手封面 Bitmap 提取主题适配的氛围背景色，并经 HSL 调优确保文字对比度。
 */
private fun extractArtistAmbientDarkColor(
    bitmap: Bitmap,
    isDark: Boolean,
    defaultColor: Color = if (isDark) DefaultArtistAmbientDarkBg else DefaultArtistAmbientLightBg
): Color {
    // Palette 量化采样最大颜色数（越多越精确但耗时略增）
    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
    val swatch = if (isDark) {
        palette.mutedSwatch
            ?: palette.dominantSwatch
            ?: palette.darkMutedSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.vibrantSwatch
    } else {
        val dominant = palette.dominantSwatch
        // 浅色主题主色饱和度阈值（低于 0.18 视为偏灰，优先选用鲜艳色板避免背景发灰）
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

/**
 * 异步加载歌手封面并提取、缓存及平滑过渡当前主题的氛围背景色。
 */
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
                    // 封面解码降采样尺寸（px，控制取色内存与耗时）
                    .size(128)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                        drawable.bitmap
                    } else {
                        // 非 BitmapDrawable 回退渲染到小尺寸软件位图（最大 128px）
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
        // 氛围底色切换渐变时长（ms）
        animationSpec = tween(durationMillis = 420),
        label = "ArtistAmbientDarkColor"
    )
}
