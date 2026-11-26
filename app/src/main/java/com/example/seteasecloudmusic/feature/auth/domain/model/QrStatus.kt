package com.example.seteasecloudmusic.feature.auth.domain.model

/**
 * 二维码扫码登录状态枚举（对应服务端状态码）。
 */
enum class QrStatus {
    WAIT_SCAN,      // 801：等待扫码
    WAIT_CONFIRM,   // 802：已扫码待确认
    SUCCESS,        // 803：登录成功
    EXPIRED         // 800：二维码已过期
}