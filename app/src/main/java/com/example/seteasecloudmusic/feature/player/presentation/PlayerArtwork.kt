package com.example.seteasecloudmusic.feature.player.presentation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

internal val LocalPlayerArtwork = staticCompositionLocalOf<Pair<String, Painter>?> { null }

/** 云端媒体域名支持 param 缩略图参数；本地文件及其他来源保持原始地址。 */
internal fun normalizeHighResCoverUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val uri = Uri.parse(url)
    val host = uri.host.orEmpty()
    if (uri.scheme !in listOf("http", "https") || !(host == "music.126.net" || host.endsWith(".music.126.net"))) return url
    return uri.buildUpon().clearQuery().apply {
        uri.queryParameterNames.filter { it != "param" }.forEach { name ->
            uri.getQueryParameters(name).forEach { appendQueryParameter(name, it) }
        }
        appendQueryParameter("param", "1200y1200")
    }.build().toString()
}

@Composable
internal fun rememberHighResArtworkRequest(url: String?): ImageRequest? {
    val context = LocalContext.current
    return remember(context, url) {
        normalizeHighResCoverUrl(url)?.let { normalized ->
            ImageRequest.Builder(context).data(normalized).size(1200).crossfade(false).build()
        }
    }
}
