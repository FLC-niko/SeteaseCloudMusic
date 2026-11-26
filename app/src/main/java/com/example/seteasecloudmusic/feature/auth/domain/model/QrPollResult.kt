package com.example.seteasecloudmusic.feature.auth.domain.model

import com.example.seteasecloudmusic.core.auth.AuthSession

/**
 * 单次二维码登录状态轮询的领域结果。
 */
data class QrPollResult(
    val state: QrStatus,
    val session: AuthSession? = null,
    val message: String? = null
)
