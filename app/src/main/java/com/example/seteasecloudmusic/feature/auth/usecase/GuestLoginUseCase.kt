package com.example.seteasecloudmusic.feature.auth.usecase

import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository

class GuestLoginUseCase (
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<AuthSession> {
        return authRepository.guestLogin()
    }
}
