package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

/** 共享专辑封面解码图片缓存，用于全屏与迷你播放器间复用已加载画笔。 */
internal val LocalPlayerArtwork = staticCompositionLocalOf<Pair<String, Painter>?> { null }

/** 云端媒体域名移除缩略图 param 参数以加载原图；本地文件及其他来源保持原始地址。 */
internal fun normalizeHighResCoverUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    if (url.startsWith("http://") || url.startsWith("https://")) {
        val httpsUrl = url.replaceFirst("http://", "https://")
        return if (httpsUrl.contains("?param=")) {
            httpsUrl.substringBefore("?")
        } else {
            httpsUrl
        }
    }
    return url
}

@Composable
internal fun rememberHighResArtworkRequest(url: String?): ImageRequest? {
    val context = LocalContext.current
    return remember(context, url) {
        normalizeHighResCoverUrl(url)?.let { normalized ->
            // 封面解码目标尺寸限制（1200px，兼顾高分屏清晰度与内存占用）
            ImageRequest.Builder(context).data(normalized).size(1200).crossfade(false).build()
        }
    }
}
