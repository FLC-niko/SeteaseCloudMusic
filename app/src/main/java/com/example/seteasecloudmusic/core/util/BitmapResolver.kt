package com.example.seteasecloudmusic.core.util

import android.graphics.Bitmap

/** 位图处理工具，用于将封面裁剪并压缩为低内存占用的缩略图。 */
object BitmapResolver {
    /**
     * 居中裁剪为正方形后按 [targetSize] 下采样，并转为 `RGB_565` 以降低内存占用。
     *
     * @param targetSize 目标最大边长（像素，默认 96；越小内存越省但细节更少）
     */
    fun bitmapCompress(bitmap: Bitmap, targetSize: Int = 96): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val size = minOf(originalWidth, originalHeight)
        val xOffset = (originalWidth - size) / 2
        val yOffset = (originalHeight - size) / 2
        val squareBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)

        val compressedBitmap = if (size > targetSize) {
            val scaleFactor = size / targetSize
            val scaledSize = size / scaleFactor
            Bitmap.createScaledBitmap(squareBitmap, scaledSize, scaledSize, true)
        } else {
            squareBitmap
        }

        return compressedBitmap.copy(Bitmap.Config.RGB_565, false)
    }
}
