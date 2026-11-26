package com.example.seteasecloudmusic.feature.auth.data.model

import com.google.gson.annotations.SerializedName

/**
 * 登录接口返回体。
 */
data class LoginResponse(
    val code: Int = 0,
    val account: AccountResponse? = null,
    val profile: ProfileResponse? = null,
    val cookie: String? = null,
    val token: String? = null
)

/**
 * 账号基础信息响应体（同时兼容接口拼写字段 anonimousUser 与 anonymousUser）。
 */
data class AccountResponse(
    val id: Long? = null,
    val anonimousUser: Boolean? = null,
    val anonymousUser: Boolean? = null
)

/**
 * 用户资料响应体。
 */
data class ProfileResponse(
    @SerializedName(value = "userId", alternate = ["user_id"])
    val userId: Long? = null,
    val nickname: String? = null,
    @SerializedName(value = "avatarUrl", alternate = ["avatar_url"])
    val avatarUrl: String? = null
)

/**
 * 用户账号信息接口响应体。
 */
data class UserAccountResponse(
    val code: Int = 0,
    val account: AccountResponse? = null,
    val profile: ProfileResponse? = null
)
