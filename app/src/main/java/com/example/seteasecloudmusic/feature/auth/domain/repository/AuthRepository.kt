package com.example.seteasecloudmusic.feature.auth.domain.repository

import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.core.auth.AuthStateProvider
import com.example.seteasecloudmusic.feature.auth.domain.model.QrLoginStart
import com.example.seteasecloudmusic.feature.auth.domain.model.QrPollResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository : AuthStateProvider {

    suspend fun loginByPhone(
        phone: String,
        password: String
    ): Result<AuthSession>

    suspend fun loginByEmail(
        email: String,
        password: String
    ): Result<AuthSession>

    suspend fun loginByCaptcha(
        phone: String,
        captcha: String
    ): Result<AuthSession>

    //启动二维码登录流程：返回 key + 展示用二维码信息。
    suspend fun startQrLogin(): Result<QrLoginStart>

    /**
     * 轮询二维码状态：
     * - 801 等待扫码
     * - 802 待确认
     * - 803 登录成功
     * - 800 二维码过期
     */
    suspend fun pollQrStatus(key: String): Result<QrPollResult>

    suspend fun guestLogin(): Result<AuthSession>

    suspend fun logout(): Result<Unit>

    suspend fun refreshSessionIfNeeded(): Result<AuthSession>

    suspend fun sendCaptcha(phone: String): Result<Unit>

    //订阅当前登录态；null 代表未登录。
    override fun observeAuthState(): Flow<AuthSession?>
}
