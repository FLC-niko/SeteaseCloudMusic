package com.example.seteasecloudmusic.feature.auth.usecase

import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository

class RefreshSessionUseCase (
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<AuthSession> {
        return authRepository.refreshSessionIfNeeded()
    }
}
