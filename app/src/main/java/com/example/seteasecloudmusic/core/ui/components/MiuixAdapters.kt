package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.example.seteasecloudmusic.core.ui.theme.DarkAppColors
import com.example.seteasecloudmusic.core.ui.theme.LightAppColors
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.PullToRefreshDefaults
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

private val LightMiuixColorScheme = miuixLightColorScheme(
    primary = LightAppColors.accent,
    onPrimary = Color.White,
    primaryVariant = LightAppColors.accent,
    disabledPrimary = LightAppColors.accent.copy(alpha = 0.38f),
    disabledPrimaryButton = LightAppColors.accent.copy(alpha = 0.42f),
    primaryContainer = LightAppColors.accent,
    secondaryVariant = Color(0xFFF2F2F7),
    onSecondaryVariant = LightAppColors.textPrimary,
    tertiaryContainer = LightAppColors.accentSubtle,
    onTertiaryContainer = LightAppColors.accent,
    error = LightAppColors.destructive,
    errorContainer = LightAppColors.errorContainer,
    onErrorContainer = LightAppColors.errorContent,
    background = LightAppColors.background,
    onBackground = LightAppColors.textPrimary,
    onBackgroundVariant = LightAppColors.textSecondary,
    surface = LightAppColors.surface,
    onSurface = LightAppColors.textPrimary,
    surfaceVariant = LightAppColors.surfaceVariant,
    onSurfaceSecondary = LightAppColors.textSecondary,
    onSurfaceVariantSummary = LightAppColors.textTertiary,
    onSurfaceVariantActions = LightAppColors.textTertiary,
    surfaceContainer = LightAppColors.groupedBackground,
    onSurfaceContainer = LightAppColors.textPrimary,
    surfaceContainerHigh = LightAppColors.surfaceVariant,
    outline = LightAppColors.divider,
    dividerLine = LightAppColors.divider,
    windowDimming = LightAppColors.scrim
)

private val DarkMiuixColorScheme = miuixDarkColorScheme(
    primary = DarkAppColors.accent,
    onPrimary = Color.White,
    primaryVariant = DarkAppColors.accent,
    disabledPrimary = DarkAppColors.accent.copy(alpha = 0.38f),
    disabledPrimaryButton = DarkAppColors.accent.copy(alpha = 0.42f),
    primaryContainer = DarkAppColors.accent,
    secondaryVariant = Color(0xFF1C1C1E),
    onSecondaryVariant = DarkAppColors.textPrimary,
    tertiaryContainer = DarkAppColors.accentSubtle,
    onTertiaryContainer = DarkAppColors.accent,
    error = DarkAppColors.destructive,
    errorContainer = DarkAppColors.errorContainer,
    onErrorContainer = DarkAppColors.errorContent,
    background = DarkAppColors.background,
    onBackground = DarkAppColors.textPrimary,
    onBackgroundVariant = DarkAppColors.textSecondary,
    surface = DarkAppColors.surface,
    onSurface = DarkAppColors.textPrimary,
    surfaceVariant = DarkAppColors.surfaceVariant,
    onSurfaceSecondary = DarkAppColors.textSecondary,
    onSurfaceVariantSummary = DarkAppColors.textTertiary,
    onSurfaceVariantActions = DarkAppColors.textTertiary,
    surfaceContainer = DarkAppColors.groupedBackground,
    onSurfaceContainer = DarkAppColors.textPrimary,
    surfaceContainerHigh = DarkAppColors.surfaceElevated,
    outline = DarkAppColors.divider,
    dividerLine = DarkAppColors.divider,
    windowDimming = DarkAppColors.scrim
)

/**
 * 将 Miuix 主题配置与语义色彩桥接隔离到组件层，避免主题工具类初始化时提前加载 Java 21 Miuix 字节码。
 */
@Composable
internal fun MiuixThemeHost(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val miuixColorScheme = if (darkTheme) DarkMiuixColorScheme else LightMiuixColorScheme
    MiuixTheme(colors = miuixColorScheme) {
        CompositionLocalProvider(
            LocalContentColor provides appColors.textPrimary,
            content = content
        )
    }
}

/**
 * 统一的 Miuix 加载指示器适配封装，兼容自定义尺寸与线宽。
 */
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.accent,
    strokeWidth: Dp = 2.dp,
    size: Dp = 24.dp
) {
    CircularProgressIndicator(
        modifier = modifier,
        progress = null,
        colors = ProgressIndicatorDefaults.progressIndicatorColors(
            foregroundColor = color,
            backgroundColor = Color.Transparent
        ),
        strokeWidth = strokeWidth,
        size = size
    )
}

private val AppPullToRefreshTexts = listOf(
    "下拉刷新",
    "释放立即刷新",
    "正在刷新...",
    "刷新成功"
)

/**
 * 统一的 Miuix 下拉刷新容器封装，内置中文提示与主题配色。
 */
@Composable
fun AppPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        contentPadding = contentPadding,
        color = colors.textSecondary,
        refreshTexts = AppPullToRefreshTexts,
        refreshTextStyle = PullToRefreshDefaults.refreshTextStyle.copy(color = colors.textSecondary),
        content = content
    )
}
