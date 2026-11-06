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

/** Window information is separate from pane constraints: a lyric pane can be narrow on a wide screen. */
@Immutable
data class AdaptiveWindowInfo(
    val width: Dp = 0.dp,
    val height: Dp = 0.dp,
    val separatingFold: Rect? = null,
    val horizontalFold: Boolean = false,
    val isFoldable: Boolean = false,
    val sideControlsAtStart: Boolean = false
) {
    val isRegularWidth get() = width >= 600.dp
    // Duo: side controls on the cover/landscape; horizontal bars on inner portrait/tabletop.
    // 600dp is Android's compact/medium window boundary, independent of a device's pixel size.
    val useNavigationRail get() = height >= 400.dp && !isTabletop &&
        (width > height || (isFoldable && !isRegularWidth))
    val useExpandedPlayer get() = isBook || isTabletop || (isRegularWidth && width > height)
    val sideControlWidth get() = if (isRegularWidth) 84.dp else 72.dp
    val isTabletop get() = separatingFold != null && horizontalFold
    val isBook get() = separatingFold != null && !horizontalFold
}

val LocalAdaptiveWindow = staticCompositionLocalOf { AdaptiveWindowInfo() }
val LocalContentBottomPadding = staticCompositionLocalOf { 120.dp }
val LocalPaneAlreadySplit = staticCompositionLocalOf { false }

@Composable
fun ProvideAdaptiveWindow(activity: Activity, content: @Composable () -> Unit) {
    val tracker = remember(activity) { WindowInfoTracker.getOrCreate(activity) }
    val supportsFolding = remember(tracker) { tracker.supportedPostures.isNotEmpty() }
    var foldingSeen by rememberSaveable { mutableStateOf(supportsFolding) }
    val layoutFlow = remember(tracker, activity) { tracker.windowLayoutInfo(activity).map { it.displayFeatures } }
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

/** A centred reading surface keeps long song rows and forms comfortable on very wide windows. */
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
 * Uses measured window coordinates for hinges, including when hosted inside a navigation rail.
 * Both panes retain their composition slot when switching between horizontal and vertical layouts.
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
