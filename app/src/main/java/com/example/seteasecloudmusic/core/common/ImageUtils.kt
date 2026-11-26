package com.example.seteasecloudmusic.core.common

/**
 * 将网易云音乐图片 URL 转换为指定尺寸的缩略图 URL。
 *
 * 通过追加 `param=${size}y${size}` 参数请求 CDN 返回对应分辨率图片，
 * 以减少列表与网格场景下的内存占用及解码耗时。
 *
 * @param size 目标缩略图宽高像素值（默认 180，越大清晰度越高但内存与解码开销越大）
 */
fun String?.toCoverThumbnailUrl(size: Int = 180): String? {
    if (this.isNullOrBlank()) return this
    // 本地 URI 不做处理
    if (startsWith("content:") || startsWith("file:")) {
        return this
    }
    // 仅针对网络图片添加参数，并清理原有带未转义管道符等可能引起解析异常的查询参数
    if (startsWith("http://") || startsWith("https://")) {
        val cleanBase = substringBefore("?").replace("http://", "https://")
        return "$cleanBase?param=${size}y${size}"
    }
    return this
}
