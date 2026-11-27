package com.example.seteasecloudmusic.feature.main

import com.example.seteasecloudmusic.core.download.TrackDownloadManager
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 播放器控制器、下载管理器与底部导航显隐状态的进程内共享入口。
 */
object PlayerEntryPoints {
    // 由 MainActivity.onCreate 注入全局播放器与下载管理器实例
    private var controller: MusicPlayerController? = null
    private var downloadMgr: TrackDownloadManager? = null

    private val _isBottomChromeHidden = MutableStateFlow(false)
    val isBottomChromeHidden: StateFlow<Boolean> = _isBottomChromeHidden.asStateFlow()

    fun init(
        playerController: MusicPlayerController,
        downloadManager: TrackDownloadManager? = null
    ) {
        controller = playerController
        if (downloadManager != null) {
            downloadMgr = downloadManager
        }
    }

    fun playerController(): MusicPlayerController {
        return requireNotNull(controller) {
            "PlayerEntryPoints is not initialized. Call PlayerEntryPoints.init(...) first."
        }
    }

    fun downloadManagerOrNull(): TrackDownloadManager? = downloadMgr

    fun setBottomChromeHidden(hidden: Boolean) {
        _isBottomChromeHidden.value = hidden
    }
}