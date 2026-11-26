package com.example.seteasecloudmusic.feature.auth.usecase

import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository

/**
 * 发送手机短信验证码用例，发起请求前先校验大陆手机号格式。
 */
class SendCaptchaUseCase (
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phone: String): Result<Unit> {
        val p = AuthInputValidator.normalizePhone(phone)

        if (!AuthInputValidator.isValidCnPhone(p)) {
            return Result.failure(IllegalArgumentException("invalid phone"))
        }

        return authRepository.sendCaptcha(p)
    }
}