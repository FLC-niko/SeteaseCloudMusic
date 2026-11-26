package com.example.seteasecloudmusic.feature.auth.usecase

import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository

/**
 * 刷新本地登录态并按需补全缺失账号资料的用例。
 */
class RefreshSessionUseCase (
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<AuthSession> {
        return authRepository.refreshSessionIfNeeded()
    }
}
