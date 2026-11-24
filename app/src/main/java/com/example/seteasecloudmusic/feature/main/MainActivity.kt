package com.example.seteasecloudmusic.feature.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.seteasecloudmusic.core.player.FavoriteTracksManager
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.ui.components.ProvideDeviceTilt
import com.example.seteasecloudmusic.feature.mine.domain.repository.MineRepository

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

    @Inject
    lateinit var musicPlayerController: MusicPlayerController

    @Inject
    lateinit var mineRepository: MineRepository

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    /**
     * 创建页面并挂载应用导航。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlayerEntryPoints.init(musicPlayerController)
        FavoriteTracksManager.setLikeApiHandler { trackId, like ->
            mineRepository.likeTrack(trackId, like)
        }
        requestNotificationPermissionOnceIfNeeded()
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
            ProvideDeviceTilt {
                com.example.seteasecloudmusic.core.ui.adaptive.ProvideAdaptiveWindow(this) {
                    AppNavigation()
                }
            }
        }
    }

    private fun requestNotificationPermissionOnceIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val prefs = getSharedPreferences("app_permissions", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("post_notifications_requested", false)) {
            prefs.edit().putBoolean("post_notifications_requested", true).apply()
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
