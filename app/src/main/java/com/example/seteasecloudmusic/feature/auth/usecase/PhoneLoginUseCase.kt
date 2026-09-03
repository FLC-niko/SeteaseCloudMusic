package com.example.seteasecloudmusic.feature.auth.usecase

import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository

class PhoneLoginUseCase(
    private val authRepository: AuthRepository
){
    suspend operator fun invoke(phone: String, password: String): Result<AuthSession> {
        val p = AuthInputValidator.normalizePhone(phone)

        if (!AuthInputValidator.isValidCnPhone(p)) {
            return Result.failure(IllegalArgumentException("invalid phone"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("password is blank"))
        }

        return authRepository.loginByPhone(p, password)
    }
}
