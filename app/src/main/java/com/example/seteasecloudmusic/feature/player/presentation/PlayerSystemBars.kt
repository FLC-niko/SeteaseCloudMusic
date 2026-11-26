package com.example.seteasecloudmusic.feature.player.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.seteasecloudmusic.core.ui.theme.AppTheme

/**
 * 进入全屏播放器时将系统状态栏与导航栏图标设为浅色以适配深色背景，退出时恢复应用主题设定。
 */
@Composable
internal fun PlayerSystemBars() {
    val view = LocalView.current
    val isAppDark by rememberUpdatedState(AppTheme.isDark)
    DisposableEffect(view) {
        fun findActivity(context: Context): Activity? = when (context) {
            is Activity -> context
            is ContextWrapper -> findActivity(context.baseContext)
            else -> null
        }
        val controller = findActivity(view.context)?.let { WindowCompat.getInsetsController(it.window, view) }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = !isAppDark
            controller?.isAppearanceLightNavigationBars = !isAppDark
        }
    }
}
