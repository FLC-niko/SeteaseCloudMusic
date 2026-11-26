package com.example.seteasecloudmusic.feature.auth.usecase

import com.example.seteasecloudmusic.feature.auth.domain.model.QrPollResult
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository

/**
 * 轮询单次二维码扫码登录状态的用例。
 */
class PollQrStatusUseCase (
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(key: String): Result<QrPollResult> {
        val k = key.trim()
        if (k.isEmpty()) return Result.failure(IllegalArgumentException("qr key is blank"))
        return authRepository.pollQrStatus(k)
    }
}