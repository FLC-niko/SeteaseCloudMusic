package com.example.seteasecloudmusic.feature.mine.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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

// 浅色/深色主题默认歌单底色
val DefaultPlaylistLightBg = Color.White
val DefaultPlaylistDarkBg = Color(0xFF14181E)

/**
 * 尚未提取出封面主色时的默认底色（白底，避免首帧出现暗沉色块）。
 */
val DefaultDarkNavyBg = DefaultPlaylistLightBg

/**
 * 歌单封面主色内存缓存，按浅色/深色主题分开存储，避免重复解码采样。
 */
object PlaylistCoverColorCache {
    // 内存缓存上限（条），按深浅外观隔离缓存键
    private val cache = LruCache<String, Color>(256)

    private fun key(url: String, isDark: Boolean): String = "${if (isDark) "d" else "l"}|$url"

    fun get(url: String?, isDark: Boolean = false): Color? {
        if (url.isNullOrBlank()) return null
        return synchronized(cache) { cache.get(key(url, isDark)) }
    }

    fun put(url: String?, color: Color, isDark: Boolean = false) {
        if (!url.isNullOrBlank()) {
            synchronized(cache) { cache.put(key(url, isDark), color) }
        }
    }
}

/**
 * 从已有 Bitmap 中提取适合做歌单背景的主色调。
 * 浅色模式输出高明度浅色底，深色模式输出低明度深色底。
 */
fun extractDominantDarkColor(
    bitmap: Bitmap,
    defaultColor: Color = Color.White,
    isDark: Boolean = false
): Color {
    // 调色板量化采样上限（16 色）
    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
    val swatch = if (isDark) {
        palette.mutedSwatch
            ?: palette.dominantSwatch
            ?: palette.darkMutedSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.vibrantSwatch
    } else {
        val dominant = palette.dominantSwatch
        // 浅色模式下要求主色饱和度不低于 0.18，避免提取出偏灰底色
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
    }

    val rawColorInt = swatch?.rgb ?: return defaultColor

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(rawColorInt, hsl)
    // 按深浅主题规范化 HSL 明度与饱和度区间
    tuneAmbientHslForTheme(hsl, isDark)

    return Color(ColorUtils.HSLToColor(hsl)).copy(alpha = 1f)
}

/**
 * 仅从内存缓存同步读取封面主色。
 * 未命中时立即返回 null，避免阻塞主线程磁盘 IO 或色彩提取。
 */
fun trySyncExtractColor(
    context: Context,
    imageUrl: String?,
    isDark: Boolean = false
): Color? {
    if (imageUrl.isNullOrBlank()) return null
    return PlaylistCoverColorCache.get(imageUrl, isDark)
}

/**
 * 根据封面 URL 与当前主题模式异步提取歌单背景主色调。
 * 优先命中内存缓存实现首帧同步渲染；未命中时先使用默认底色，提取完成后平滑渐变过渡。
 */
@Composable
fun rememberCoverDominantDarkColor(
    imageUrl: String?,
    isDark: Boolean = AppTheme.isDark,
    defaultColor: Color = if (isDark) DefaultPlaylistDarkBg else DefaultPlaylistLightBg
): State<Color> {
    val context = LocalContext.current
    val initialColor = remember(imageUrl, isDark, defaultColor) {
        trySyncExtractColor(context, imageUrl, isDark) ?: defaultColor
    }

    val extractedColorState = produceState(
        initialValue = initialColor,
        key1 = imageUrl,
        key2 = isDark,
        key3 = defaultColor
    ) {
        if (imageUrl.isNullOrBlank()) {
            value = defaultColor
            return@produceState
        }

        // 命中内存缓存时直接复用，跳过重复解码
        val cached = PlaylistCoverColorCache.get(imageUrl, isDark)
        if (cached != null) {
            value = cached
            return@produceState
        }

        val targetColor = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(128) // 取色采样缩略图边长（px，越小解码越快）
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

                    val color = extractDominantDarkColor(bitmap, defaultColor, isDark)
                    PlaylistCoverColorCache.put(imageUrl, color, isDark)
                    color
                } else {
                    defaultColor
                }
            }.getOrElse { defaultColor }
        }

        value = targetColor
    }

    return animateColorAsState(
        targetValue = extractedColorState.value,
        animationSpec = tween(durationMillis = 400), // 背景主色渐变动画时长（ms）
        label = "CoverDominantDarkColorAnimation"
    )
}

