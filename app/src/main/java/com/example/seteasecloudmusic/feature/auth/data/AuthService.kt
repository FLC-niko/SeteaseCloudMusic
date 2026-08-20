package com.example.seteasecloudmusic.feature.auth.data

import com.example.seteasecloudmusic.feature.auth.data.model.LoginResponse
import com.example.seteasecloudmusic.feature.auth.data.model.QrCodeResponse
import com.example.seteasecloudmusic.feature.auth.data.model.QrKeyResponse
import com.example.seteasecloudmusic.feature.auth.data.model.QrStatusResponse
import com.example.seteasecloudmusic.feature.auth.data.model.UserAccountResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 认证接口定义，用于登录与会话相关 API。
 */
interface AuthService {
    @POST("/login/cellphone")
    suspend fun loginWithPassword(
        @Query("phone") phone: String,
        @Query("password") password: String
    ): Response<LoginResponse>

    @POST("/login/cellphone")
    suspend fun loginWithCaptcha(
        @Query("phone") phone: String,
        @Query("captcha") captcha: String
    ): Response<LoginResponse>

    @POST("/captcha/sent")
    suspend fun sendCaptcha(
        @Query("phone") phone: String
    ): Response<BaseResponse>

    @POST("/login/qr/key")
    suspend fun getQrKey(
        @Query("timestamp") timestamp: Long
    ): Response<QrKeyResponse>

    @POST("/login/qr/create")
    suspend fun getQrCode(
        @Query("key") key: String,
        @Query("timestamp") timestamp: Long,
        @Query("qrimg") qrimg: Int
    ): Response<QrCodeResponse>

    @POST("/login/qr/check")
    suspend fun checkQrCodeStatus(
        @Query("key") key: String,
        @Query("timestamp") timestamp: Long
    ): Response<QrStatusResponse>

    @GET("/user/account")
    suspend fun getUserAccount(
        @Query("timestamp") timestamp: Long,
        @Query("cookie") cookie: String?
    ): Response<UserAccountResponse>

    @POST("/logout")
    suspend fun logout(
        @Query("timestamp") timestamp: Long
    ): Response<BaseResponse>
}

data class BaseResponse(
    val code: Int = 0,
    val message: String = ""
)
