package com.example.seteasecloudmusic.feature.auth.domain.model

/**
 * 二维码登录初始化结果，包含轮询标识与展示用二维码数据。
 */
data class QrLoginStart(
    val key: String,
    val qrUrl: String? = null,
    val qrImageBase64: String? = null
)