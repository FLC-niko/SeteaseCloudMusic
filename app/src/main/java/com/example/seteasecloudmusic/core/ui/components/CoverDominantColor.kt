package com.example.seteasecloudmusic.core.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** 主暗色调内存 LRU 缓存容量（按封面 URL 缓存 256 条）。 */
private val CoverDominantColorCache = LruCache<String, Int>(256)
/** 底部边缘延伸色内存 LRU 缓存容量（按封面 URL 缓存 256 条）。 */
private val BottomEdgeColorCache = LruCache<String, Int>(256)
/** 限制并发取色任务数（最多 2 个），避免列表快速滚动时解码占用过高 CPU/内存。 */
private val CoverColorExtractSemaphore = Semaphore(2)

internal fun getCachedDominantDarkColor(coverUrl: String?): Color? {
    val validUrl = coverUrl?.takeIf(String::isNotBlank) ?: return null
    return CoverDominantColorCache.get(validUrl)?.let(::Color)
}

internal fun getCachedBottomEdgeColor(coverUrl: String?): Color? {
    val validUrl = coverUrl?.takeIf(String::isNotBlank) ?: return null
    return BottomEdgeColorCache.get(validUrl)?.let(::Color)
}

/** 根据歌单或歌曲 ID 哈希从 6 组深色基础色调中生成确定性兜底底色。 */
internal fun deriveDarkHueFromId(id: Long): Color {
    val hues = listOf(
        Color(0xFF1E2F38), // deep teal
        Color(0xFF27382B), // deep forest
        Color(0xFF38262F), // deep plum
        Color(0xFF363220), // deep olive
        Color(0xFF202C38), // deep navy
        Color(0xFF382A20)  // deep sienna
    )
    val index = (abs(id) % hues.size).toInt()
    return hues[index]
}

internal suspend fun extractCachedDominantDarkColor(context: Context, coverUrl: String): Color? {
    return extractCachedColor(
        context = context,
        coverUrl = coverUrl,
        cache = CoverDominantColorCache,
        extractor = ::extractDominantDarkColor
    )
}

internal suspend fun extractCachedBottomEdgeColor(context: Context, coverUrl: String): Color? {
    return extractCachedColor(
        context = context,
        coverUrl = coverUrl,
        cache = BottomEdgeColorCache,
        extractor = ::extractBottomEdgeExtendColor
    )
}

private suspend fun extractCachedColor(
    context: Context,
    coverUrl: String,
    cache: LruCache<String, Int>,
    extractor: (Bitmap) -> Color
): Color? {
    cache.get(coverUrl)?.let { return Color(it) }
    return CoverColorExtractSemaphore.withPermit {
        cache.get(coverUrl)?.let { return@withPermit Color(it) }
        val bitmap = withContext(Dispatchers.IO) {
            try {
                // 请求 100px 缩略图并在 24x24 软件位图下采样，兼顾取色速度与内存开销
                val req = ImageRequest.Builder(context)
                    .data(coverUrl.toCoverThumbnailUrl(100))
                    .size(24, 24)
                    .allowHardware(false)
                    .build()
                (context.imageLoader.execute(req).drawable as? BitmapDrawable)?.bitmap
            } catch (_: Exception) {
                null
            }
        } ?: return@withPermit null
        val extracted = withContext(Dispatchers.Default) {
            extractor(bitmap)
        }
        cache.put(coverUrl, extracted.toArgb())
        extracted
    }
}

/** 采样封面底部 1/4 区域（y: 18..23）平均颜色并调校 HSV 明度/饱和度，用于底部文字延伸底色。 */
private fun extractBottomEdgeExtendColor(bitmap: Bitmap): Color {
    val needsScale = bitmap.width != 24 || bitmap.height != 24
    val scaled = if (needsScale) Bitmap.createScaledBitmap(bitmap, 24, 24, false) else bitmap
    val width = 24
    val height = 24
    val pixels = IntArray(width * height)
    try {
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
    } finally {
        if (needsScale && scaled !== bitmap) {
            scaled.recycle()
        }
    }

    var sumR = 0L
    var sumG = 0L
    var sumB = 0L
    var count = 0L
    for (y in 18 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val p = pixels[rowOffset + x]
            sumR += android.graphics.Color.red(p)
            sumG += android.graphics.Color.green(p)
            sumB += android.graphics.Color.blue(p)
            count++
        }
    }

    if (count <= 0L) return Color(0xFF262C30)

    val avgR = (sumR / count).toInt().coerceIn(0, 255)
    val avgG = (sumG / count).toInt().coerceIn(0, 255)
    val avgB = (sumB / count).toInt().coerceIn(0, 255)
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(avgR, avgG, avgB, hsv)
    // 保证底部白色文字对比度：明度超过 0.50 时乘 0.68 压暗并限制上限 0.48，饱和度限制在 [0, 0.85]
    if (hsv[2] > 0.50f) {
        hsv[2] = (hsv[2] * 0.68f).coerceAtMost(0.48f)
    }
    hsv[1] = hsv[1].coerceIn(0f, 0.85f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** 按 12 个 30° 色相桶统计封面主色调，过滤低饱和/极暗像素后将饱和度与明度收束至深色卡片范围。 */
private fun extractDominantDarkColor(bitmap: Bitmap): Color {
    val needsScale = bitmap.width != 24 || bitmap.height != 24
    val scaled = if (needsScale) Bitmap.createScaledBitmap(bitmap, 24, 24, false) else bitmap
    val count = 24 * 24
    val pixels = IntArray(count)
    try {
        scaled.getPixels(pixels, 0, 24, 0, 0, 24, 24)
    } finally {
        if (needsScale && scaled !== bitmap) {
            scaled.recycle()
        }
    }

    val hsv = FloatArray(3)
    val hueBuckets = IntArray(12)
    val bucketSat = FloatArray(12)
    val bucketVal = FloatArray(12)

    var validPixels = 0
    for (p in pixels) {
        android.graphics.Color.colorToHSV(p, hsv)
        val s = hsv[1]
        val v = hsv[2]
        // 排除低饱和灰阶（s < 0.16）与极暗噪点像素（v < 0.15）
        if (s >= 0.16f && v >= 0.15f) {
            val bucket = ((hsv[0] % 360f) / 30f).toInt().coerceIn(0, 11)
            hueBuckets[bucket]++
            bucketSat[bucket] += s
            bucketVal[bucket] += v
            validPixels++
        }
    }

    if (validPixels > 0) {
        var bestBucket = 0
        var maxCount = -1
        for (i in 0 until 12) {
            if (hueBuckets[i] > maxCount) {
                maxCount = hueBuckets[i]
                bestBucket = i
            }
        }
        val countInBucket = hueBuckets[bestBucket].coerceAtLeast(1)
        // 饱和度钳制在 [0.40, 0.75]，明度按均值 0.55x 压暗并钳制在 [0.24, 0.40]
        val avgSat = (bucketSat[bestBucket] / countInBucket).coerceIn(0.40f, 0.75f)
        val targetHue = bestBucket * 30f + 15f
        val avgVal = bucketVal[bestBucket] / countInBucket
        val targetVal = (avgVal * 0.55f).coerceIn(0.24f, 0.40f)
        val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(targetHue, avgSat, targetVal))
        return Color(colorInt)
    }

    return Color(0xFF262C30)
}

/**
 * 漫游卡片三张错落叠放专辑封面（左/右两张带小倾角，中间主封面居中置顶）
 */
@Composable
internal fun RoamStaggeredCoverStack(
    firstCoverUrl: String?,
    secondCoverUrl: String?,
    thirdCoverUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val second = secondCoverUrl ?: firstCoverUrl
    val third = thirdCoverUrl ?: second ?: firstCoverUrl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(94.dp),
        contentAlignment = Alignment.Center
    ) {
        // 左侧第 2 首封面（向左偏转小倾角）
        AsyncImage(
            model = second?.toCoverThumbnailUrl(180),
            contentDescription = null,
            modifier = Modifier
                .offset(x = (-28).dp, y = 3.dp)
                .size(74.dp)
                .graphicsLayer {
                    rotationZ = -8f
                    shape = RoundedCornerShape(8.dp)
                    clip = true
                }
                .background(Color.White.copy(alpha = 0.14f))
                .border(0.6.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // 右侧第 3 首封面（向右偏转小倾角）
        AsyncImage(
            model = third?.toCoverThumbnailUrl(180),
            contentDescription = null,
            modifier = Modifier
                .offset(x = 28.dp, y = 3.dp)
                .size(74.dp)
                .graphicsLayer {
                    rotationZ = 8f
                    shape = RoundedCornerShape(8.dp)
                    clip = true
                }
                .background(Color.White.copy(alpha = 0.14f))
                .border(0.6.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // 中间第 1 首主封面
        AsyncImage(
            model = firstCoverUrl?.toCoverThumbnailUrl(240),
            contentDescription = contentDescription,
            modifier = Modifier
                .zIndex(1f)
                .size(90.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(9.dp), clip = false)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .border(0.6.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(9.dp)),
            contentScale = ContentScale.Crop
        )
    }
}
