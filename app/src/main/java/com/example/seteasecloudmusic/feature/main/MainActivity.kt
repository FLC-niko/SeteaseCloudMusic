package com.example.seteasecloudmusic.feature.main

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

/**
 * `presentation` 模块说明：
 *
 * 这一层只关心“界面如何展示”和“用户如何交互”，
 * 不直接处理网络细节，也不直接实现业务规则。
 *
 * `MainActivity` 是 Android 入口，职责很单一：
 * 1. 初始化 Compose 宿主。
 * 2. 打开沉浸式边到边布局。
 * 3. 把应用的根界面 `AppNavigation()` 挂载出来。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * 创建页面并挂载应用导航。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // P1a：这台小米默认跑 144Hz（帧预算仅 6.9ms），而常驻动效
        // （海报墙无限滚动 + backdrop + 进度动画）稳态约 7ms， chronic  miss vsync。
        // 上限 120Hz（预算 8.3ms，视觉无差）换 20% 余量；常驻动效的根治见 P1b/c。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val modes = currentDisplay?.supportedModes
            val cappedMode = modes
                ?.filter { it.refreshRate <= 120.01f }
                ?.maxByOrNull { it.refreshRate }
                ?: modes?.minByOrNull { it.refreshRate }
            if (cappedMode != null) {
                val params = window.attributes
                params.preferredRefreshRate = cappedMode.refreshRate
                window.attributes = params
            }
        }

        setContent {
            com.example.seteasecloudmusic.core.ui.adaptive.ProvideAdaptiveWindow(this) {
                AppNavigation()
            }
        }
    }
}
