package com.example.seteasecloudmusic.feature.mine.presentation

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 默认通透雅致蓝灰底色（明朗不暗沉，非死黑）
 */
val DefaultDarkNavyBg = Color(0xFF323E50)

/**
 * 根据歌单/专辑封面 URL 异步提取适合背景的主色调，并自带 600ms 平滑过渡动画。
 * 整体往浅色、明亮、柔和通透方向调校，避免深色封面导致界面过暗。
 */
@Composable
fun rememberCoverDominantDarkColor(
    imageUrl: String?,
    defaultColor: Color = DefaultDarkNavyBg
): State<Color> {
    val context = LocalContext.current

    val extractedColorState = produceState(initialValue = defaultColor, key1 = imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            value = defaultColor
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

                    // 使用 Palette 提取调色板（优先取柔和与主色，避免死黑）
                    val palette = Palette.from(bitmap).generate()
                    val swatch = palette.mutedSwatch
                        ?: palette.dominantSwatch
                        ?: palette.darkMutedSwatch
                        ?: palette.darkVibrantSwatch
                        ?: palette.vibrantSwatch

                    val rawColorInt = swatch?.rgb ?: defaultColor.toArgb()

                    // 转为 HSL 调校：整体往浅色明亮去调，避免深色封面导致界面过暗沉
                    val hsl = FloatArray(3)
                    ColorUtils.colorToHSL(rawColorInt, hsl)

                    // 饱和度控制在雅致区间 (0.12 .. 0.36)，呈现清爽高级的莫兰迪灰彩色系
                    hsl[1] = hsl[1].coerceIn(0.12f, 0.36f)
                    // 明度整体往浅色明亮方向调校，基准区间提高至 0.28 .. 0.42，即便原图深黑也保持明净通透
                    val boostedLightness = (hsl[2] * 0.55f + 0.22f).coerceIn(0.28f, 0.42f)
                    hsl[2] = boostedLightness

                    Color(ColorUtils.HSLToColor(hsl)).copy(alpha = 1f)
                } else {
                    defaultColor
                }
            }.getOrElse { defaultColor }
        }

        value = targetColor
    }

    return animateColorAsState(
        targetValue = extractedColorState.value,
        animationSpec = tween(durationMillis = 600),
        label = "CoverDominantDarkColorAnimation"
    )
}

