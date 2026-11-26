package com.example.seteasecloudmusic.feature.auth.usecase

import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository

/**
 * 邮箱密码登录用例，执行输入格式校验后委托仓储登录。
 */
class EmailLoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthSession> {
        val e = AuthInputValidator.normalizeEmail(email)

        if (!AuthInputValidator.isValidEmail(e)) {
            return Result.failure(IllegalArgumentException("invalid email"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("password is blank"))
        }

        return authRepository.loginByEmail(e, password)
    }
}
