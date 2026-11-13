package com.example.seteasecloudmusic.feature.main

import com.example.seteasecloudmusic.core.player.MusicPlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlayerEntryPoints {
    // 由应用启动处注入
    private var controller: MusicPlayerController? = null

    private val _isBottomChromeHidden = MutableStateFlow(false)
    val isBottomChromeHidden: StateFlow<Boolean> = _isBottomChromeHidden.asStateFlow()

    fun init(playerController: MusicPlayerController) {
        controller = playerController
    }

    fun playerController(): MusicPlayerController {
        return requireNotNull(controller) {
            "PlayerEntryPoints is not initialized. Call PlayerEntryPoints.init(...) first."
        }
    }

    fun setBottomChromeHidden(hidden: Boolean) {
        _isBottomChromeHidden.value = hidden
    }
}