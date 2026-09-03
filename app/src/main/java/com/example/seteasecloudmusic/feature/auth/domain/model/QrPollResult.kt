package com.example.seteasecloudmusic.feature.auth.domain.model

import com.example.seteasecloudmusic.core.auth.AuthSession

data class QrPollResult(
    val state: QrStatus,
    val session: AuthSession? = null,
    val message: String? = null
)
