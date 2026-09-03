package com.example.seteasecloudmusic.feature.auth.usecase

import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository

class VerifyCaptchaUseCase (
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phone: String, captcha: String): Result<AuthSession> {
        val p = AuthInputValidator.normalizePhone(phone)
        val c = AuthInputValidator.normalizeCaptcha(captcha)

        if (!AuthInputValidator.isValidCnPhone(p)) {
            return Result.failure(IllegalArgumentException("invalid phone"))
        }

        return authRepository.loginByCaptcha(p, c)
    }
}
