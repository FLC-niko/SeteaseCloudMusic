package com.example.seteasecloudmusic.core.common

/**
 * 将网易云音乐图片 URL 转为指定尺寸的高性能缩略图。
 *
 * 网易云音乐 CDN 原图通常高达 1000x1000 像素，在网格或列表中同时加载数十张原图会导致大量的
 * 内存占用、网络开销与 CPU 解码耗时，从而引起滑动掉帧和启动卡顿。
 * 添加 `param=${size}y${size}` 参数后，CDN 将直接返回轻量级缩略图（体积缩小 90%+，解码快 10 倍）。
 */
fun String?.toCoverThumbnailUrl(size: Int = 180): String? {
    if (this.isNullOrBlank()) return this
    // 本地 URI 或已包含尺寸参数的不做处理
    if (startsWith("content:") || startsWith("file:") || contains("param=")) {
        return this
    }
    // 仅针对网络图片添加参数
    if (startsWith("http://") || startsWith("https://")) {
        val sep = if (contains("?")) "&" else "?"
        return "$this${sep}param=${size}y${size}"
    }
    return this
}
