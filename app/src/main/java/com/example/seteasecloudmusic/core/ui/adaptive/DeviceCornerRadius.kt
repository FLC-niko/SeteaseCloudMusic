package com.example.seteasecloudmusic.core.ui.adaptive

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.RoundedCorner
import android.view.View
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 屏幕物理硬件圆角（r 角）信息
 * 包含四角独立半径，精确贴合各类对称与非对称现代曲面屏
 */
@Immutable
data class DeviceCornerRadius(
    val topStart: Dp = 0.dp,
    val topEnd: Dp = 0.dp,
    val bottomStart: Dp = 0.dp,
    val bottomEnd: Dp = 0.dp
) {
    val maxRadius: Dp get() = maxOf(topStart, topEnd, bottomStart, bottomEnd)
    val hasRoundedCorners: Boolean get() = maxRadius > 0.dp

    fun toShape(): RoundedCornerShape = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart
    )
}

/**
 * 常见 Android 厂商系统圆角尺寸资源键名
 * 覆盖小米 MIUI/HyperOS, 三星 OneUI, 华为 HarmonyOS/EMUI, OPPO/vivo/Pixel 等
 */
private val SYSTEM_DIMEN_NAMES = listOf(
    "rounded_corner_radius",
    "rounded_corner_radius_top",
    "rounded_corner_radius_bottom",
    "config_rounded_corner_radius",
    "config_roundedCornerRadius",
    "config_rounded_corner_radius_top",
    "config_rounded_corner_radius_bottom",
    "display_corner_radius"
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun getSystemDimensionCornerRadiusPx(context: Context): Int {
    for (name in SYSTEM_DIMEN_NAMES) {
        try {
            val resId = context.resources.getIdentifier(name, "dimen", "android")
            if (resId > 0) {
                val px = context.resources.getDimensionPixelSize(resId)
                if (px > 0) return px
            }
        } catch (_: Throwable) {
            // ignore
        }
    }
    return 0
}

/**
 * 检测当前机型的屏幕硬件 r 角半径
 */
fun detectDeviceCornerRadius(
    context: Context,
    view: View,
    density: Density,
    isTabletOrRail: Boolean = false
): DeviceCornerRadius {
    // 1. Android 12+ (API 31+) 官方 WindowInsets.getRoundedCorner 硬件级精确读取
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val insets = view.rootWindowInsets ?: context.findActivity()?.window?.decorView?.rootWindowInsets
        if (insets != null) {
            val tl = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
            val tr = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0
            val bl = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
            val br = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0

            val maxRadius = maxOf(tl, tr, bl, br)
            if (maxRadius > 0) {
                with(density) {
                    val fallback = maxRadius.toDp()
                    return DeviceCornerRadius(
                        topStart = if (tl > 0) tl.toDp() else fallback,
                        topEnd = if (tr > 0) tr.toDp() else fallback,
                        bottomStart = if (bl > 0) bl.toDp() else fallback,
                        bottomEnd = if (br > 0) br.toDp() else fallback
                    )
                }
            }
        }
    }

    // 2. 检查各大 OEM 系统内部圆角尺寸资源（MIUI / OneUI / EMUI / ColorOS 等）
    val sysRadiusPx = getSystemDimensionCornerRadiusPx(context)
    if (sysRadiusPx > 0) {
        with(density) {
            val dp = sysRadiusPx.toDp()
            return DeviceCornerRadius(dp, dp, dp, dp)
        }
    }

    // 3. 回退策略：平板或侧边栏模式为平角 (0.dp)；主流全面屏手机回退至经典舒适的 32.dp 圆角
    if (isTabletOrRail) {
        return DeviceCornerRadius(0.dp, 0.dp, 0.dp, 0.dp)
    }

    val defaultRadius = 32.dp
    return DeviceCornerRadius(defaultRadius, defaultRadius, defaultRadius, defaultRadius)
}

/**
 * 响应式记忆并识别当前设备的屏幕 r 角
 */
@Composable
fun rememberDeviceCornerRadius(): DeviceCornerRadius {
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val window = LocalAdaptiveWindow.current

    val statusBarTop = WindowInsets.statusBars.getTop(density)
    val navBarBottom = WindowInsets.navigationBars.getBottom(density)

    return remember(view, context, density, statusBarTop, navBarBottom, window.useNavigationRail) {
        detectDeviceCornerRadius(context, view, density, isTabletOrRail = window.useNavigationRail)
    }
}
