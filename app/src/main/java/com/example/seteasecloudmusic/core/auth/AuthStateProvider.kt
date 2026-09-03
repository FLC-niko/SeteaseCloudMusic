package com.example.seteasecloudmusic.core.auth

import kotlinx.coroutines.flow.Flow

/** 供其它 Feature 读取登录态的最小共享契约。 */
interface AuthStateProvider {
    fun observeAuthState(): Flow<AuthSession?>
}
