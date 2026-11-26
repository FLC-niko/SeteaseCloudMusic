package com.example.seteasecloudmusic.feature.auth.usecase

import com.example.seteasecloudmusic.feature.auth.domain.model.QrLoginStart
import com.example.seteasecloudmusic.feature.auth.domain.repository.AuthRepository

/**
 * 初始化二维码登录流程、获取轮询 Key 与二维码图片的用例。
 */
class StartQrLoginUseCase (
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<QrLoginStart> {
        return authRepository.startQrLogin()
    }
}