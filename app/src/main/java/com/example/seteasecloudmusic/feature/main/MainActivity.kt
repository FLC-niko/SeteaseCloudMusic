package com.example.seteasecloudmusic.feature.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.seteasecloudmusic.core.download.DownloadEvent
import com.example.seteasecloudmusic.core.download.TrackDownloadManager
import com.example.seteasecloudmusic.core.player.FavoriteTracksManager
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.core.ui.components.ProvideDeviceTilt
import com.example.seteasecloudmusic.core.ui.theme.LiquidGlassAppearance
import com.example.seteasecloudmusic.core.ui.theme.SeteaseCloudMusicTheme
import com.example.seteasecloudmusic.core.ui.theme.TopAmbientAppearance
import com.example.seteasecloudmusic.feature.mine.domain.repository.MineRepository
import kotlinx.coroutines.launch

/**
 * 应用主入口 Activity：初始化播放器与点赞回调、请求通知权限、启用边到边布局并挂载 Compose 根界面。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var musicPlayerController: MusicPlayerController

    @Inject
    lateinit var mineRepository: MineRepository

    @Inject
    lateinit var playerSettingsManager: PlayerSettingsManager

    @Inject
    lateinit var trackDownloadManager: TrackDownloadManager

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    /**
     * 创建页面并挂载应用导航。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlayerEntryPoints.init(musicPlayerController, trackDownloadManager)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                trackDownloadManager.eventsFlow.collect { event ->
                    val message = when (event) {
                        is DownloadEvent.Completed -> "下载完成：${event.title}"
                        is DownloadEvent.Failed -> "下载失败：${event.title}（${event.reason}）"
                    }
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        FavoriteTracksManager.setLikeApiHandler { trackId, like ->
            mineRepository.likeTrack(trackId, like)
        }
        requestNotificationPermissionOnceIfNeeded()
        enableEdgeToEdge()

        // 将窗口首选刷新率上限设为 120Hz（帧预算 8.3ms），为常驻动效与实时毛玻璃保留帧时间余量
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val modes = currentDisplay?.supportedModes
            val cappedMode = modes
                ?.filter { it.refreshRate <= 120.01f } // 刷新率上限阈值（Hz，保留 0.01f 浮点容差）
                ?.maxByOrNull { it.refreshRate }
                ?: modes?.minByOrNull { it.refreshRate }
            if (cappedMode != null) {
                val params = window.attributes
                params.preferredRefreshRate = cappedMode.refreshRate
                window.attributes = params
            }
        }

        setContent {
            val themeMode by playerSettingsManager.themeMode.collectAsStateWithLifecycle()
            val topAmbientEnabled by playerSettingsManager.topAmbientEnabled.collectAsStateWithLifecycle()
            val topAmbientColorArgb by playerSettingsManager.topAmbientColorArgb.collectAsStateWithLifecycle()
            val liquidGlassOpacity by playerSettingsManager.liquidGlassOpacity.collectAsStateWithLifecycle()
            SeteaseCloudMusicTheme(
                themeMode = themeMode,
                topAmbientAppearance = TopAmbientAppearance(
                    enabled = topAmbientEnabled,
                    colorArgb = topAmbientColorArgb
                ),
                liquidGlassAppearance = LiquidGlassAppearance(
                    opacityLevel = liquidGlassOpacity
                )
            ) {
                ProvideDeviceTilt {
                    com.example.seteasecloudmusic.core.ui.adaptive.ProvideAdaptiveWindow(this) {
                        AppNavigation()
                    }
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
