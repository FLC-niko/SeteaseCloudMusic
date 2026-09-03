package com.example.seteasecloudmusic.feature.auth.usecase

import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveAuthStateUseCase (
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<AuthSession?> {
        return authRepository.observeAuthState()
    }
}
