package com.example.seteasecloudmusic.core.ui.adaptive

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

/** 当前窗口尺寸与折叠形态信息（独立于子面板约束，例如宽屏下的窄歌词面板）。 */
@Immutable
data class AdaptiveWindowInfo(
    val width: Dp = 0.dp,
    val height: Dp = 0.dp,
    val separatingFold: Rect? = null,
    val horizontalFold: Boolean = false,
    val isFoldable: Boolean = false,
    val sideControlsAtStart: Boolean = false
) {
    /** 宽度断点：>= 600.dp 为常规/宽屏窗口（平板或展开态折叠屏），小于此值视为紧凑手机布局。 */
    val isRegularWidth get() = width >= 600.dp

    /**
     * 是否启用侧边导航栏：高度 >= 400.dp 且非桌面半折叠（tabletop）形态时，
     * 横屏窗口或非展开态外屏折叠屏使用侧边栏，其余使用底栏。
     */
    val useNavigationRail get() = height >= 400.dp && !isTabletop &&
        (width > height || (isFoldable && !isRegularWidth))

    /** 书本折叠态、桌面半折叠态或宽屏横屏（width >= 600.dp 且横屏）时启用展开式双栏播放器。 */
    val useExpandedPlayer get() = isBook || isTabletop || (isRegularWidth && width > height)

    /** 侧边导航栏宽度：宽屏（>= 600.dp）为 84.dp，紧凑横屏为 72.dp。 */
    val sideControlWidth get() = if (isRegularWidth) 84.dp else 72.dp
    val isTabletop get() = separatingFold != null && horizontalFold
    val isBook get() = separatingFold != null && !horizontalFold

    /** 详情页与列表并排展示条件：内容区 >= 720.dp x 360.dp，或处于折叠屏半折叠形态。 */
    fun showDetailAlongside(hasSelection: Boolean, contentWidth: Dp, contentHeight: Dp): Boolean =
        hasSelection && ((contentWidth >= 720.dp && contentHeight >= 360.dp) || isBook || isTabletop)
}

/** 全局自适应窗口信息 CompositionLocal。 */
val LocalAdaptiveWindow = staticCompositionLocalOf { AdaptiveWindowInfo() }

/** 页面底部默认留白高度（dp，用于避让底栏与迷你播放栏）。 */
val LocalContentBottomPadding = staticCompositionLocalOf { 120.dp }

/** 标识当前子树是否已处于双栏拆分容器内，避免嵌套拆分。 */
val LocalPaneAlreadySplit = staticCompositionLocalOf { false }

@Composable
fun ProvideAdaptiveWindow(activity: Activity, content: @Composable () -> Unit) {
    // 小米 MIUI 等设备没有 Jetpack WindowManager extension（extension version 0），
    // 直接读 tracker.supportedPostures 会抛 UnsupportedOperationException 导致启动即崩溃。
    // 全部分支加 runCatching 兜底：取不到折叠信息就当普通直板机处理。
    val tracker = remember(activity) { runCatching { WindowInfoTracker.getOrCreate(activity) }.getOrNull() }
    val supportsFolding = remember(tracker) {
        runCatching { tracker?.supportedPostures?.isNotEmpty() }.getOrDefault(false) == true
    }
    var foldingSeen by rememberSaveable { mutableStateOf(supportsFolding) }
    val layoutFlow = remember(tracker, activity) {
        val upstream = tracker?.let { t ->
            runCatching { t.windowLayoutInfo(activity) }.getOrNull()
        } ?: kotlinx.coroutines.flow.emptyFlow()
        upstream.map { it.displayFeatures }
    }
    val features by layoutFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    LaunchedEffect(features) { if (features.any { it is FoldingFeature }) foldingSeen = true }
    val fold = features.filterIsInstance<FoldingFeature>().firstOrNull {
        it.isSeparating || it.occlusionType == FoldingFeature.OcclusionType.FULL
    }
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        val metrics = WindowMetricsCalculator.getOrCreate()
        val currentBounds = metrics.computeCurrentWindowMetrics(activity).bounds
        val maximumBounds = metrics.computeMaximumWindowMetrics(activity).bounds
        val controlsAtStart = activity.isInMultiWindowMode && currentBounds.centerX() < maximumBounds.centerX()
        val info = AdaptiveWindowInfo(
            width = maxWidth,
            height = maxHeight,
            separatingFold = fold?.bounds?.let { Rect(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat()) },
            horizontalFold = fold?.orientation == FoldingFeature.Orientation.HORIZONTAL,
            isFoldable = foldingSeen || features.any { it is FoldingFeature },
            sideControlsAtStart = controlsAtStart
        )
        CompositionLocalProvider(LocalAdaptiveWindow provides info, content = content)
    }
}

/**
 * 居中限制内容最大阅读宽度，防止超宽屏下列表与表单横向过度拉伸。
 *
 * @param maxWidth 内容区最大宽度上限（默认 1280.dp）
 */
@Composable
fun AdaptiveContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 1280.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxSize(), content = content)
    }
}

/**
 * 基于窗口坐标与折叠铰链区域的双栏自适应容器，支持水平（Book）与垂直（Tabletop）拆分。
 * 切换布局方向时保留主/副面板组合槽位，避免子状态重建。
 *
 * @param primaryFraction 无分离折痕时主面板宽度占比（0f..1f，默认 0.4f）
 * @param gap 两面板或折痕两侧的隔离间距（默认 24.dp）
 */
@Composable
fun AdaptiveTwoPane(
    modifier: Modifier = Modifier,
    primaryFraction: Float = 0.4f,
    gap: Dp = 24.dp,
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit
) {
    val window = LocalAdaptiveWindow.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    Layout(
        modifier = modifier.onGloballyPositioned { origin = it.positionInWindow() },
        content = {
            Box(Modifier.fillMaxSize().testTag("adaptive_primary")) {
                CompositionLocalProvider(LocalPaneAlreadySplit provides true) { primary() }
            }
            Box(Modifier.fillMaxSize().testTag("adaptive_secondary")) {
                CompositionLocalProvider(LocalPaneAlreadySplit provides true) { secondary() }
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val bounds = Rect(origin, androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat()))
        val fold = window.separatingFold?.takeIf { it.overlaps(bounds) ||
            (window.horizontalFold && it.top > bounds.top && it.top < bounds.bottom) ||
            (!window.horizontalFold && it.left > bounds.left && it.left < bounds.right)
        }
        val vertical = fold != null && window.horizontalFold
        val available = if (vertical) height else width
        val gapPx = with(density) { gap.roundToPx() }.coerceAtMost(available / 4)
        val foldStart = fold?.let { if (vertical) it.top - origin.y else it.left - origin.x }?.roundToInt()
        val foldEnd = fold?.let { if (vertical) it.bottom - origin.y else it.right - origin.x }?.roundToInt()
        val firstSize = (foldStart?.minus(gapPx / 2) ?: ((available - gapPx) * primaryFraction).roundToInt()).coerceIn(0, available)
        val secondStart = (foldEnd?.plus(gapPx / 2) ?: (firstSize + gapPx)).coerceIn(firstSize, available)
        val secondSize = available - secondStart
        val first = measurables[0].measure(Constraints.fixed(if (vertical) width else firstSize, if (vertical) firstSize else height))
        val second = measurables[1].measure(Constraints.fixed(if (vertical) width else secondSize, if (vertical) secondSize else height))
        layout(width, height) {
            first.placeRelative(0, 0)
            second.placeRelative(if (vertical) 0 else secondStart, if (vertical) secondStart else 0)
        }
    }
}
