package com.example.seteasecloudmusic.core.auth

/** 应用各 Feature 共用的登录态快照。 */
data class AuthSession(
    val userId: Long? = null,
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val cookie: String? = null,
    val loginMethod: LoginMethod = LoginMethod.UNKNOWN,
    val isLoggedIn: Boolean = false
)
