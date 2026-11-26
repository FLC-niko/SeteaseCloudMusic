package com.example.seteasecloudmusic.feature.auth.usecase

import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository

/**
 * 游客模式登录用例，重置当前账号会话并返回匿名会话。
 */
class GuestLoginUseCase (
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<AuthSession> {
        return authRepository.guestLogin()
    }
}
