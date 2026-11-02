package com.example.seteasecloudmusic.feature.player.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
internal fun PlayerSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        fun findActivity(context: Context): Activity? = when (context) {
            is Activity -> context
            is ContextWrapper -> findActivity(context.baseContext)
            else -> null
        }
        val controller = findActivity(view.context)?.let { WindowCompat.getInsetsController(it.window, view) }
        val lightStatus = controller?.isAppearanceLightStatusBars ?: true
        val lightNavigation = controller?.isAppearanceLightNavigationBars ?: true
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = lightStatus
            controller?.isAppearanceLightNavigationBars = lightNavigation
        }
    }
}
