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

val DefaultPlaylistLightBg = Color.White
val DefaultPlaylistDarkBg = Color(0xFF14181E)

/**
 * 默认底色：白底（若尚未获取到取色结果，避免使用暗沉灰底导致第一帧视觉突兀）
 */
val DefaultDarkNavyBg = DefaultPlaylistLightBg

/**
 * 歌单封面提取主色内存缓存，按浅色/深色外观分开缓存，保证再次进入或已采样封面直接在首帧出图，避免过渡前闪现底色
 */
object PlaylistCoverColorCache {
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
 * 从已有的 Bitmap 中快速提取歌单/专辑主色调（计算量极小，通常 < 1ms）。
 * 浅色模式下输出高明度莫兰迪浅色底，深色模式下输出低明度沉静深色底。
 */
fun extractDominantDarkColor(
    bitmap: Bitmap,
    defaultColor: Color = Color.White,
    isDark: Boolean = false
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
    }

    val rawColorInt = swatch?.rgb ?: return defaultColor

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(rawColorInt, hsl)
    tuneAmbientHslForTheme(hsl, isDark)

    return Color(ColorUtils.HSLToColor(hsl)).copy(alpha = 1f)
}

/**
 * 尝试同步从内存主色缓存中立即获取颜色，保证已访问过的封面在首帧即可渲染正确取色直接滑入。
 * 未缓存时立即返回 null，绝不阻塞主线程磁盘 IO 或色彩提取，保证进入动效 100% 满帧丝滑。
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
 * 根据歌单/专辑封面 URL 与当前主题模式提取适合背景的主色调。
 * 浅色模式提取浅色柔和底色，深色模式提取深色沉静底色。
 * 优先同步命中内存/已解码缓存以保证首帧正确取色直接滑入；若无缓存则以默认底色滑入并在就绪后平滑渐变。
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

        // 若已命中缓存且当前值已经是正确取色，跳过重复解码
        val cached = PlaylistCoverColorCache.get(imageUrl, isDark)
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
        animationSpec = tween(durationMillis = 400),
        label = "CoverDominantDarkColorAnimation"
    )
}

