package com.example.seteasecloudmusic.feature.player.presentation.mesh

import android.graphics.Bitmap
import android.graphics.Color

/**
 * 1:1 复刻 AMLL (@applemusic-like-lyrics/core MeshGradientRenderer) 的封面取色预处理管线：
 * 1. 缩放至 32x32 纹理
 * 2. 浮点空间合并色彩变换：contrast(0.4) -> saturate(3.0) -> contrast(1.7) -> brightness(0.75)
 * 3. 4 次半径为 2 的盒式高斯近似模糊
 */
internal object AlbumTextureProcessor {

    fun process(bitmap: Bitmap): Bitmap {
        if (bitmap.isRecycled) {
            return Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
                eraseColor(0xFF282830.toInt())
            }
        }
        val w = 32
        val h = 32
        var img = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val pixels = IntArray(w * h)
        img.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            var r = Color.red(pixels[i]).toFloat()
            var g = Color.green(pixels[i]).toFloat()
            var b = Color.blue(pixels[i]).toFloat()
            val a = 255

            // 1. contrast(0.4)
            r = (r - 128f) * 0.4f + 128f
            g = (g - 128f) * 0.4f + 128f
            b = (b - 128f) * 0.4f + 128f

            // 2. saturate(3.0)
            val gray = r * 0.3f + g * 0.59f + b * 0.11f
            r = gray * -2.0f + r * 3.0f
            g = gray * -2.0f + g * 3.0f
            b = gray * -2.0f + b * 3.0f

            // 3. contrast(1.7)
            r = (r - 128f) * 1.7f + 128f
            g = (g - 128f) * 1.7f + 128f
            b = (b - 128f) * 1.7f + 128f

            // 4. brightness(0.75)
            r *= 0.75f
            g *= 0.75f
            b *= 0.75f

            pixels[i] = Color.argb(
                a,
                r.toInt().coerceIn(0, 255),
                g.toInt().coerceIn(0, 255),
                b.toInt().coerceIn(0, 255)
            )
        }

        if (img !== bitmap) img.recycle()
        img = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        img.setPixels(pixels, 0, w, 0, 0, w, h)

        repeat(4) {
            img = boxBlurPass(img, 2)
        }

        return img
    }

    private fun boxBlurPass(bitmap: Bitmap, radius: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)

        val r = radius.coerceAtLeast(1)
        val invSize = 1f / (r * 2 + 1)

        val hR = FloatArray(w * h)
        val hG = FloatArray(w * h)
        val hB = FloatArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                var rr = 0f
                var gg = 0f
                var bb = 0f
                for (kx in -r..r) {
                    val sx = (x + kx).coerceIn(0, w - 1)
                    val pixel = src[y * w + sx]
                    rr += Color.red(pixel).toFloat()
                    gg += Color.green(pixel).toFloat()
                    bb += Color.blue(pixel).toFloat()
                }
                val idx = y * w + x
                hR[idx] = rr * invSize
                hG[idx] = gg * invSize
                hB[idx] = bb * invSize
            }
        }

        val dst = IntArray(w * h)
        for (x in 0 until w) {
            for (y in 0 until h) {
                var rr = 0f
                var gg = 0f
                var bb = 0f
                for (ky in -r..r) {
                    val sy = (y + ky).coerceIn(0, h - 1)
                    val idx = sy * w + x
                    rr += hR[idx]
                    gg += hG[idx]
                    bb += hB[idx]
                }
                dst[y * w + x] = Color.argb(
                    255,
                    (rr * invSize + 0.5f).toInt().coerceIn(0, 255),
                    (gg * invSize + 0.5f).toInt().coerceIn(0, 255),
                    (bb * invSize + 0.5f).toInt().coerceIn(0, 255)
                )
            }
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        if (bitmap !== result) bitmap.recycle()
        return result
    }
}

