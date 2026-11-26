package com.example.seteasecloudmusic.core.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.seteasecloudmusic.core.settings.AppThemeMode
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.core.ui.components.MiuixThemeHost

/**
 * 根据浅色/深色主题调校封面取色 HSL：
 * - 浅色模式：保持较高饱和度与鲜艳通透的浅色氛围底色（明度 0.75..0.84），搭配深色文字。
 * - 深色模式：生成深邃沉静的暗色氛围底色（明度 0.13..0.21），搭配浅色文字。
 */
fun tuneAmbientHslForTheme(hsl: FloatArray, isDark: Boolean) {
    if (isDark) {
        val isNearNeutral = hsl[1] < 0.08f
        hsl[1] = if (isNearNeutral) hsl[1].coerceIn(0.02f, 0.08f) else hsl[1].coerceIn(0.16f, 0.38f)
        hsl[2] = (hsl[2] * 0.14f + 0.12f).coerceIn(0.13f, 0.21f)
    } else {
        val isNearNeutral = hsl[1] < 0.06f
        hsl[1] = if (isNearNeutral) {
            hsl[1].coerceIn(0.02f, 0.08f)
        } else {
            (hsl[1] * 0.85f + 0.20f).coerceIn(0.32f, 0.68f)
        }
        hsl[2] = (hsl[2] * 0.10f + 0.75f).coerceIn(0.75f, 0.84f)
    }
}

/**
 * 根据用户偏好与系统深色模式状态解析当前是否启用深色外观。
 */
fun resolveDarkTheme(themeMode: AppThemeMode, isSystemDark: Boolean): Boolean = when (themeMode) {
    AppThemeMode.SYSTEM -> isSystemDark
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
}

/**
 * 应用语义化色彩令牌（对齐 Apple HIG & iOS 26 Liquid Glass 规范）。
 */
@Immutable
data class AppColorPalette(
    val isDark: Boolean,
    val accent: Color,
    val accentSubtle: Color,
    val accentBlue: Color,
    val destructive: Color,
    val background: Color,
    val backgroundSecondary: Color,
    val groupedBackground: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
    val divider: Color,
    val dividerSubtle: Color,
    val placeholder: Color,
    val coverBorder: Color,
    val errorContainer: Color,
    val errorContent: Color,
    val errorIcon: Color,
    val tagContainer: Color,
    val tagContent: Color,
    val glassSurface: Color,
    val glassPanelContainer: Color,
    val scrim: Color,
    val handleBar: Color
)

val LightAppColors = AppColorPalette(
    isDark = false,
    accent = Color(0xFFFA233B),
    accentSubtle = Color(0x1FFA233B),
    accentBlue = Color(0xFF007AFF),
    destructive = Color(0xFFFF3B30),
    background = Color(0xFFFFFFFF),
    backgroundSecondary = Color(0xFFF7F7FA),
    groupedBackground = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF5F5F7),
    surfaceElevated = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF8E8E93),
    textTertiary = Color(0xFFC7C7CC),
    iconPrimary = Color(0xFF1C1C1E),
    iconSecondary = Color(0xFF494950),
    divider = Color(0xFFE5E5EA),
    dividerSubtle = Color(0xFFF0F0F2),
    placeholder = Color(0xFFE4E4E8),
    coverBorder = Color(0x12000000),
    errorContainer = Color(0xFFFFF2F0),
    errorContent = Color(0xFFD32F2F),
    errorIcon = Color(0xFFE53935),
    tagContainer = Color(0xFFFFF0F2),
    tagContent = Color(0xFFE53935),
    glassSurface = Color(0xFFF7F7FA),
    glassPanelContainer = Color(0xFFFFFFFF),
    scrim = Color.Black.copy(alpha = 0.24f),
    handleBar = Color(0xFFD2D2D8)
)

val DarkAppColors = AppColorPalette(
    isDark = true,
    accent = Color(0xFFFF2D55),
    accentSubtle = Color(0x29FF2D55),
    accentBlue = Color(0xFF0A84FF),
    destructive = Color(0xFFFF453A),
    background = Color(0xFF000000),
    backgroundSecondary = Color(0xFF0D0D10),
    groupedBackground = Color(0xFF141416),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    surfaceElevated = Color(0xFF2C2C2E),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF98989F),
    textTertiary = Color(0xFF636366),
    iconPrimary = Color(0xFFF5F5F7),
    iconSecondary = Color(0xFFC7C7CC),
    divider = Color(0xFF38383A),
    dividerSubtle = Color(0xFF262629),
    placeholder = Color(0xFF242428),
    coverBorder = Color(0x1FFFFFFF),
    errorContainer = Color(0xFF2C1618),
    errorContent = Color(0xFFFF6961),
    errorIcon = Color(0xFFFF453A),
    tagContainer = Color(0xFFFA233B).copy(alpha = 0.18f),
    tagContent = Color(0xFFFF6482),
    glassSurface = Color(0xFF1C1C24),
    glassPanelContainer = Color(0xFF1C1C1E),
    scrim = Color.Black.copy(alpha = 0.52f),
    handleBar = Color(0xFF48484A)
)

@Immutable
data class TopAmbientAppearance(
    val enabled: Boolean = true,
    val colorArgb: Int = PlayerSettingsManager.DEFAULT_TOP_AMBIENT_COLOR_ARGB
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
val LocalTopAmbientAppearance = staticCompositionLocalOf { TopAmbientAppearance() }

object AppTheme {
    val colors: AppColorPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.isDark

    val topAmbient: TopAmbientAppearance
        @Composable
        @ReadOnlyComposable
        get() = LocalTopAmbientAppearance.current
}

@Composable
fun SeteaseCloudMusicTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    topAmbientAppearance: TopAmbientAppearance = TopAmbientAppearance(),
    content: @Composable () -> Unit
) {
    val darkTheme = resolveDarkTheme(themeMode, isSystemInDarkTheme())
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
                window.decorView.setBackgroundColor(
                    if (darkTheme) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalTopAmbientAppearance provides topAmbientAppearance
    ) {
        MiuixThemeHost(
            darkTheme = darkTheme,
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
