package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue

/**
 * 液态玻璃分段滑块（LSPosed 阻尼水滴物理 + 双层采样透镜）统一参数与组件。
 *
 * - **静息态**：轻量半透明柔和胶囊，零额外透镜/色散 GPU 开销。
 * - **按压/拖动激活态**：弹起放大（1.22x~1.35x）、速度耦合果冻拉伸/挤压、
 *   球面 `depthEffect` + 7 光谱色散折射底层文字与图标（`CombinedBackdrop`）、
 *   `InnerShadow` 内阴影厚度感与 `+90°` 重力旋转 `BloomStroke` 高光。
 */
object GlassSliderDefaults {
    /** 滑块与容器内壁的间距 */
    val ThumbPadding: Dp = 4.dp

    /** 兼容旧调用的透明度默认值 */
    const val ThumbSurfaceAlpha: Float = 0.6f

    /** 跟手时的弹簧：偏硬，保证滑块紧贴手指 */
    const val TRACKING_STIFFNESS = 800f
    const val TRACKING_DAMPING = 0.8f

    /** 回弹吸附时的弹簧：偏软，产生自然的落位感 */
    const val SETTLE_STIFFNESS = 300f
    const val SETTLE_DAMPING = 0.6f

    /** 兼容旧 API 的拉伸系数 */
    const val STRETCH_FACTOR = 0.35f

    /** 兼容旧 API 的模糊半径 */
    val BlurRadius: Dp = 3.dp

    /** 底部主导航栏水滴按压最大放大倍率（接近 LSPosed 1.3929x 并兼顾边缘留白） */
    const val MaxPressScale: Float = 1.35f

    /** 紧凑分段条/弹出菜单水滴按压最大放大倍率 */
    const val CompactMaxPressScale: Float = 1.22f

    /** 激活态水滴凸透镜折射高度与折射量 */
    val DropletRefractionHeight: Dp = 12.dp
    val DropletRefractionAmount: Dp = 18.dp

    /** 激活态水滴内阴影半径 */
    val DropletInnerShadowRadius: Dp = 8.dp

    /** 静息态半透明胶囊底色透明度 */
    const val RestingPillAlphaLight: Float = 0.135f
    const val RestingPillAlphaDark: Float = 0.19f
}

/**
 * 根据按压进度 [pressProgress] 与主轴速度 [velocity]（单位：槽位/秒）计算阻尼果冻形变缩放比例。
 *
 * 按下时整体水滴等比弹起至 [maxPressScale]，高速拖动时沿运动方向拉伸、垂直方向轻微压缩，
 * 最大形变量钳制在 20% 内以保证形状与物理真实感。
 */
internal fun computeJellyScale(
    pressProgress: Float,
    velocity: Float,
    maxPressScale: Float,
    orientation: Orientation
): Pair<Float, Float> {
    val p = pressProgress.coerceIn(0f, 1f)
    val baseScale = lerp(1f, maxPressScale, p)
    val vNorm = velocity / 10f
    val stretch = (0.75f * abs(vNorm)).coerceIn(0f, 0.20f)
    val squish = (0.25f * abs(vNorm)).coerceIn(0f, 0.20f)
    val primaryScale = baseScale / (1f - stretch)
    val crossScale = baseScale * (1f - squish)
    return if (orientation == Orientation.Vertical) {
        crossScale to primaryScale
    } else {
        primaryScale to crossScale
    }
}

/**
 * 滑块胶囊当前所在像素区域，用于驱动静息态与拖拽过程中的双色精确裁剪分割。
 */
internal data class SegmentPillBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/**
 * 计算滑块当前（或插值滑动中）胶囊在容器内的精确像素矩形。
 */
internal fun computeSegmentPillBounds(
    offsetFraction: Float,
    itemCount: Int,
    containerWidthPx: Float,
    containerHeightPx: Float,
    thumbPaddingPx: Float,
    orientation: Orientation
): SegmentPillBounds {
    if (itemCount <= 0 || containerWidthPx <= 0f || containerHeightPx <= 0f) {
        return SegmentPillBounds(0f, 0f, 0f, 0f)
    }
    val clampedOffset = offsetFraction.coerceIn(0f, (itemCount - 1).toFloat())
    val pad = thumbPaddingPx.coerceAtLeast(0f)
    val left: Float
    val top: Float
    val width: Float
    val height: Float
    if (orientation == Orientation.Vertical) {
        val slotH = containerHeightPx / itemCount.toFloat()
        left = pad
        top = pad + clampedOffset * slotH
        width = (containerWidthPx - pad * 2f).coerceAtLeast(0f)
        height = (slotH - pad * 2f).coerceAtLeast(0f)
    } else {
        val slotW = containerWidthPx / itemCount.toFloat()
        left = pad + clampedOffset * slotW
        top = pad
        width = (slotW - pad * 2f).coerceAtLeast(0f)
        height = (containerHeightPx - pad * 2f).coerceAtLeast(0f)
    }
    return SegmentPillBounds(
        left = left,
        top = top,
        right = left + width,
        bottom = top + height
    )
}

/**
 * LSPosed 阻尼水滴拖拽状态机：位置、按压进度、速度全部托管在 [Animatable] 中，
 * 仅在 Draw/Layer 阶段读取，拖拽和动画期间零重组。
 */
@Stable
class LiquidSegmentState(
    internal val itemCount: Int,
    initialIndex: Int,
    private val scope: CoroutineScope
) {
    private val maxIndex: Int = (itemCount - 1).coerceAtLeast(0)
    private var activeTargetIndex: Int = initialIndex.coerceIn(0, maxIndex)

    val offsetFraction = Animatable(activeTargetIndex.toFloat())
    val pressProgress = Animatable(0f)
    val velocity = Animatable(0f)

    internal var isDragging by mutableStateOf(false)
        private set

    private var pressJob: Job? = null
    private var settleJob: Job? = null
    private val pressSpring = spring<Float>(dampingRatio = 0.65f, stiffness = 420f)
    private val tapPopSpring = spring<Float>(dampingRatio = 0.65f, stiffness = 480f)
    private val releaseTween = tween<Float>(durationMillis = 150, easing = FastOutSlowInEasing)
    private val trackingSpring = spring<Float>(
        dampingRatio = GlassSliderDefaults.TRACKING_DAMPING,
        stiffness = GlassSliderDefaults.TRACKING_STIFFNESS
    )
    private val settleSpring = spring<Float>(
        dampingRatio = GlassSliderDefaults.SETTLE_DAMPING,
        stiffness = GlassSliderDefaults.SETTLE_STIFFNESS
    )
    private val velocitySpring = spring<Float>(dampingRatio = 0.7f, stiffness = 250f)

    fun onPressStart() {
        settleJob?.cancel()
        pressJob?.cancel()
        pressJob = scope.launch {
            pressProgress.animateTo(1f, pressSpring)
        }
    }

    fun onDragToFraction(fraction: Float, velocitySlotsPerSec: Float) {
        settleJob?.cancel()
        isDragging = true
        val clamped = fraction.coerceIn(0f, maxIndex.toFloat())
        scope.launch {
            launch { offsetFraction.animateTo(clamped, trackingSpring) }
            launch { velocity.animateTo(velocitySlotsPerSec, velocitySpring) }
        }
    }

    fun onSettleTo(index: Int) {
        val clampedIndex = index.coerceIn(0, maxIndex)
        val target = clampedIndex.toFloat()
        val wasDragging = isDragging
        isDragging = false
        activeTargetIndex = clampedIndex
        val travelDistance = abs(offsetFraction.value - target)
        settleJob?.cancel()

        if (travelDistance > 0.05f) {
            settleJob = scope.launch {
                pressJob?.cancel()
                pressJob = launch {
                    pressProgress.animateTo(1f, tapPopSpring)
                }
                launch {
                    offsetFraction.animateTo(target, settleSpring) {
                        if (!wasDragging) {
                            val currentVel = this.velocity * 0.45f
                            launch { this@LiquidSegmentState.velocity.snapTo(currentVel) }
                        }
                    }
                    if (!wasDragging) {
                        velocity.animateTo(0f, velocitySpring)
                    }
                }
                if (wasDragging) {
                    launch { velocity.animateTo(0f, velocitySpring) }
                }
                if (!wasDragging) {
                    delay(110L)
                }
                snapshotFlow { offsetFraction.value }.first { abs(it - target) <= 0.12f }
                pressJob?.cancel()
                pressJob = launch {
                    pressProgress.animateTo(0f, releaseTween)
                }
            }
        } else {
            pressJob?.cancel()
            pressJob = scope.launch {
                pressProgress.animateTo(0f, releaseTween)
            }
            scope.launch {
                velocity.animateTo(0f, velocitySpring)
            }
        }
    }

    fun onCancel(fallbackIndex: Int) {
        onSettleTo(fallbackIndex)
    }

    suspend fun syncSelectedIndex(selectedIndex: Int) {
        val clampedIndex = selectedIndex.coerceIn(0, maxIndex)
        if (!isDragging && clampedIndex != activeTargetIndex) {
            onSettleTo(clampedIndex)
        } else if (!isDragging && settleJob?.isActive != true) {
            val target = clampedIndex.toFloat()
            if (abs(offsetFraction.value - target) > 0.001f) {
                offsetFraction.animateTo(target, settleSpring)
            }
        }
    }
}

@Composable
fun rememberLiquidSegmentState(
    itemCount: Int,
    selectedIndex: Int,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): LiquidSegmentState {
    val state = remember(itemCount, coroutineScope) {
        LiquidSegmentState(itemCount = itemCount, initialIndex = selectedIndex, scope = coroutineScope)
    }
    LaunchedEffect(selectedIndex, itemCount) {
        state.syncSelectedIndex(selectedIndex)
    }
    return state
}

/**
 * LSPosed 阻尼水滴分段交互手势：
 * 按下立即触发水滴弹起（`pressProgress → 1f`），支持单击直接切换与越过 `touchSlop` 后的跟手拖动，
 * 结合 [VelocityTracker] 实时驱动果冻拉丝形变，松手吸附提交 [onSettle]。
 */
fun Modifier.liquidSegmentDrag(
    state: LiquidSegmentState,
    itemCount: Int,
    selectedIndex: Int,
    onSettle: (Int) -> Unit,
    onHoverIndexChange: ((Int?) -> Unit)? = null,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Horizontal
): Modifier = this.pointerInput(state, itemCount, enabled, orientation) {
    if (!enabled || itemCount <= 0) return@pointerInput

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val totalAxisPx = (if (orientation == Orientation.Vertical) size.height else size.width).toFloat()
        val slotAxisPx = if (itemCount > 0) totalAxisPx / itemCount.toFloat() else 0f
        val downAxis = if (orientation == Orientation.Vertical) down.position.y else down.position.x
        var currentAxis = downAxis
        var dragging = false
        var released = false
        val touchSlop = viewConfiguration.touchSlop
        val velocityTracker = VelocityTracker()
        velocityTracker.addPosition(down.uptimeMillis, down.position)

        try {
            state.onPressStart()
            if (slotAxisPx > 0f) {
                onHoverIndexChange?.invoke((downAxis / slotAxisPx).toInt().coerceIn(0, itemCount - 1))
            }

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                if (change == null) {
                    break
                } else if (change.pressed) {
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    if (!dragging && (change.position - down.position).getDistance() > touchSlop) {
                        dragging = true
                    }
                    if (dragging && slotAxisPx > 0f) {
                        currentAxis = if (orientation == Orientation.Vertical) change.position.y else change.position.x
                        val targetFraction = ((currentAxis / slotAxisPx) - 0.5f)
                            .coerceIn(0f, (itemCount - 1).toFloat())
                        val velPx = velocityTracker.calculateVelocity()
                        val axisVelPx = if (orientation == Orientation.Vertical) velPx.y else velPx.x
                        val velSlotsPerSec = axisVelPx / slotAxisPx
                        state.onDragToFraction(targetFraction, velSlotsPerSec)
                        onHoverIndexChange?.invoke((currentAxis / slotAxisPx).toInt().coerceIn(0, itemCount - 1))
                        change.consume()
                    }
                } else {
                    released = true
                    break
                }
            }
        } finally {
            onHoverIndexChange?.invoke(null)
            if (!released) {
                state.onCancel(selectedIndex)
            }
        }

        if (released && slotAxisPx > 0f) {
            val settleAxis = if (dragging) currentAxis else downAxis
            val settledIndex = (settleAxis / slotAxisPx).toInt().coerceIn(0, itemCount - 1)
            state.onSettleTo(settledIndex)
            onSettle(settledIndex)
        } else {
            state.onCancel(selectedIndex)
        }
    }
}

/**
 * 静息态底层柔和半透明胶囊：置于外框容器内、Tab 图标与文字之下。
 *
 * 随着 [LiquidSegmentState.pressProgress] 从 0→1 自动淡出，由顶层 [LiquidGlassActiveDroplet] 接管。
 * 静息时零折射、零模糊、零阴影开销。
 */
@Composable
fun LiquidGlassRestingThumb(
    state: LiquidSegmentState,
    containerWidth: Dp,
    containerHeight: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    thumbPadding: Dp = GlassSliderDefaults.ThumbPadding,
    isDarkSurface: Boolean = AppTheme.isDark,
    pillColor: Color = if (isDarkSurface) Color.White else Color.Black,
    pillAlpha: Float = if (isDarkSurface) GlassSliderDefaults.RestingPillAlphaDark else GlassSliderDefaults.RestingPillAlphaLight,
    alphaProvider: (() -> Float)? = null,
    orientation: Orientation = Orientation.Horizontal
) {
    if (state.itemCount <= 0 || containerWidth <= 0.dp || containerHeight <= 0.dp) return

    val innerCornerRadius = (cornerRadius - thumbPadding).coerceAtLeast(0.dp)
    val thumbShape = remember(innerCornerRadius) { RoundedRectangle(innerCornerRadius) }
    val slotWidth = if (orientation == Orientation.Vertical) containerWidth else containerWidth / state.itemCount
    val slotHeight = if (orientation == Orientation.Vertical) containerHeight / state.itemCount else containerHeight
    val thumbWidth = (slotWidth - thumbPadding * 2).coerceAtLeast(0.dp)
    val thumbHeight = (slotHeight - thumbPadding * 2).coerceAtLeast(0.dp)

    if (thumbWidth <= 0.dp || thumbHeight <= 0.dp) return

    Box(
        modifier = modifier
            .drawWithContent {
                val outerAlpha = alphaProvider?.invoke() ?: 1f
                val restingAlpha = (1f - state.pressProgress.value).coerceIn(0f, 1f) * outerAlpha
                if (restingAlpha > 0.005f) {
                    drawContent()
                }
            }
            .graphicsLayer {
                val outerAlpha = alphaProvider?.invoke() ?: 1f
                alpha = ((1f - state.pressProgress.value).coerceIn(0f, 1f) * outerAlpha).coerceIn(0f, 1f)
                val padPx = thumbPadding.toPx()
                if (orientation == Orientation.Vertical) {
                    translationX = padPx
                    translationY = padPx + state.offsetFraction.value * slotHeight.toPx()
                } else {
                    translationX = padPx + state.offsetFraction.value * slotWidth.toPx()
                    translationY = padPx
                }
            }
            .size(thumbWidth, thumbHeight)
            .background(color = pillColor.copy(alpha = pillAlpha), shape = thumbShape)
    )
}

/**
 * LSPosed 风格像素级双色滑块内容容器：
 * 1. [unselectedContent] 通过 `ClipOp.Difference` 裁剪掉滑块胶囊内部区域，在胶囊外渲染普通深/浅色图标与文字；
 * 2. 中间渲染轻量半透明静息胶囊 [LiquidGlassRestingThumb]（按下时平滑淡出）；
 * 3. [selectedContent] 通过 `ClipOp.Intersect` 仅在胶囊内部渲染高亮强调色（即使滑块半跨某个图标/文字也能精确分割双色）。
 */
@Composable
fun LiquidGlassDualColorTabs(
    state: LiquidSegmentState,
    containerWidth: Dp,
    containerHeight: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    thumbPadding: Dp = GlassSliderDefaults.ThumbPadding,
    isDarkSurface: Boolean = AppTheme.isDark,
    pillColor: Color = if (isDarkSurface) Color.White else Color.Black,
    pillAlpha: Float = if (isDarkSurface) GlassSliderDefaults.RestingPillAlphaDark else GlassSliderDefaults.RestingPillAlphaLight,
    alphaProvider: (() -> Float)? = null,
    orientation: Orientation = Orientation.Horizontal,
    unselectedContent: @Composable BoxScope.() -> Unit,
    selectedContent: @Composable BoxScope.() -> Unit
) {
    val pillPath = remember { Path() }
    val innerCornerRadius = (cornerRadius - thumbPadding).coerceAtLeast(0.dp)

    fun ContentDrawScope.populatePillPath() {
        val bounds = computeSegmentPillBounds(
            offsetFraction = state.offsetFraction.value,
            itemCount = state.itemCount,
            containerWidthPx = size.width,
            containerHeightPx = size.height,
            thumbPaddingPx = thumbPadding.toPx(),
            orientation = orientation
        )
        val rPx = innerCornerRadius.toPx().coerceAtLeast(0f)
        pillPath.rewind()
        pillPath.addRoundRect(
            RoundRect(
                left = bounds.left,
                top = bounds.top,
                right = bounds.right,
                bottom = bounds.bottom,
                cornerRadius = CornerRadius(rPx, rPx)
            )
        )
    }

    Box(modifier = modifier.size(containerWidth, containerHeight)) {
        // 1. 胶囊外部：普通态图标与文字（裁剪掉胶囊覆盖区域，杜绝穿透叠色）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val outerAlpha = alphaProvider?.invoke() ?: 1f
                    if (outerAlpha > 0.005f) {
                        populatePillPath()
                        clipPath(pillPath, clipOp = ClipOp.Difference) {
                            this@drawWithContent.drawContent()
                        }
                    } else {
                        drawContent()
                    }
                },
            content = unselectedContent
        )

        // 2. 静息态半透明胶囊
        LiquidGlassRestingThumb(
            state = state,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            cornerRadius = cornerRadius,
            thumbPadding = thumbPadding,
            isDarkSurface = isDarkSurface,
            pillColor = pillColor,
            pillAlpha = pillAlpha,
            alphaProvider = alphaProvider,
            orientation = orientation,
            modifier = Modifier.align(Alignment.TopStart)
        )

        // 3. 胶囊内部（静息与交叉淡入阶段）：精准裁剪渲染高亮激活色图标与文字
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (alphaProvider != null) {
                        Modifier.graphicsLayer {
                            alpha = alphaProvider().coerceIn(0f, 1f)
                        }
                    } else {
                        Modifier
                    }
                )
                .drawWithContent {
                    val outerAlpha = alphaProvider?.invoke() ?: 1f
                    if (outerAlpha > 0.005f && state.pressProgress.value < 0.995f) {
                        populatePillPath()
                        clipPath(pillPath, clipOp = ClipOp.Intersect) {
                            this@drawWithContent.drawContent()
                        }
                    }
                },
            content = selectedContent
        )
    }
}

/**
 * LSPosed 同款离屏激活层录制器（对应 `Lyl0` 模式，作为外框与水滴之间的隐形同级节点）：
 * 仅在按压或拖动阶段将「圆角微模糊玻璃底板 + 清晰高亮激活色 Tab 内容」一起录制进 [activeTabsBackdrop]，
 * 供上层 [LiquidGlassActiveDroplet] 通过 `CombinedBackdrop` 折射放大，彻底避免水滴内部“全透漏底”。
 * 静息时直接跳过录制，保持零 GPU 开销。
 */
@Composable
fun LiquidGlassActiveTabsRecorder(
    pageBackdrop: Backdrop?,
    activeTabsBackdrop: LayerBackdrop,
    state: LiquidSegmentState,
    containerWidth: Dp,
    containerHeight: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    isDarkSurface: Boolean = AppTheme.isDark,
    surfaceAlpha: Float = GlassDefaults.SurfaceAlpha,
    surfaceColor: Color = if (isDarkSurface) Color(0xFF1C1C24) else Color(0xFFF7F7FA),
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    alphaProvider: (() -> Float)? = null,
    selectedContent: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(containerWidth, containerHeight)
            .drawWithContent {
                val outerAlpha = alphaProvider?.invoke() ?: 1f
                if (state.pressProgress.value * outerAlpha > 0.005f) {
                    drawContent()
                }
            }
            .graphicsLayer { alpha = 0f }
            .layerBackdrop(activeTabsBackdrop)
            .then(
                if (pageBackdrop != null) {
                    Modifier.drawPlainBackdrop(
                        backdrop = pageBackdrop,
                        shape = { RoundedRectangle(cornerRadius) },
                        layerBlock = layerBlock,
                        effects = {
                            vibrancy()
                            blur(GlassDefaults.BlurRadius.toPx())
                        },
                        onDrawSurface = {
                            val effectiveAlpha = if (isRenderEffectSupported()) {
                                surfaceAlpha
                            } else {
                                maxOf(surfaceAlpha, if (isDarkSurface) 0.72f else 0.76f)
                            }
                            if (effectiveAlpha > 0f) {
                                drawRect(color = surfaceColor.copy(alpha = effectiveAlpha))
                            }
                        }
                    )
                } else {
                    Modifier
                }
            ),
        content = selectedContent
    )
}

/**
 * 激活态 3D 放大镜水滴滑块：作为外框玻璃条的**同级兄弟节点**覆盖在其上方（不被外框裁剪）。
 *
 * 采样双层合成背景 [combinedBackdrop]（底层页面 + [LiquidGlassActiveTabsRecorder] 录制的高亮 Tab 与玻璃表面），在按下/拖动时：
 * 1. 弹起放大至 [maxPressScale] 并叠加速度耦合果冻形变
 * 2. 启用球面 `depthEffect` + 7 光谱色散透镜，真实扭曲放大下方高亮图标与文字
 * 3. 叠加晶莹玻璃光泽表面、`InnerShadow` 厚度感与 `+90°` 重力旋转 `BloomStroke` 高光
 * 4. 静息时（`pressProgress <= 0.005f`）跳过整棵子树绘制，实现零 GPU 开销。
 */
@Composable
fun LiquidGlassActiveDroplet(
    combinedBackdrop: Backdrop?,
    state: LiquidSegmentState,
    containerWidth: Dp,
    containerHeight: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    thumbPadding: Dp = GlassSliderDefaults.ThumbPadding,
    maxPressScale: Float = GlassSliderDefaults.MaxPressScale,
    isDarkSurface: Boolean = AppTheme.isDark,
    alphaProvider: (() -> Float)? = null,
    orientation: Orientation = Orientation.Horizontal
) {
    if (state.itemCount <= 0 || containerWidth <= 0.dp || containerHeight <= 0.dp) return

    val innerCornerRadius = (cornerRadius - thumbPadding).coerceAtLeast(0.dp)
    val shape = remember(innerCornerRadius) { RoundedRectangle(innerCornerRadius) }
    val slotWidth = if (orientation == Orientation.Vertical) containerWidth else containerWidth / state.itemCount
    val slotHeight = if (orientation == Orientation.Vertical) containerHeight / state.itemCount else containerHeight
    val thumbWidth = (slotWidth - thumbPadding * 2).coerceAtLeast(0.dp)
    val thumbHeight = (slotHeight - thumbPadding * 2).coerceAtLeast(0.dp)

    if (thumbWidth <= 0.dp || thumbHeight <= 0.dp) return

    val dropletLayerBlock: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {
        val outerAlpha = (alphaProvider?.invoke() ?: 1f).coerceIn(0f, 1f)
        val p = state.pressProgress.value.coerceIn(0f, 1f)
        alpha = (p * outerAlpha).coerceIn(0f, 1f)
        val padPx = thumbPadding.toPx()
        if (orientation == Orientation.Vertical) {
            translationX = padPx
            translationY = padPx + state.offsetFraction.value * slotHeight.toPx()
        } else {
            translationX = padPx + state.offsetFraction.value * slotWidth.toPx()
            translationY = padPx
        }
        val (sx, sy) = computeJellyScale(
            pressProgress = p,
            velocity = state.velocity.value,
            maxPressScale = maxPressScale,
            orientation = orientation
        )
        scaleX = sx
        scaleY = sy
        transformOrigin = TransformOrigin.Center
    }

    val dropletBloomStyle = remember(isDarkSurface) {
        if (isDarkSurface) {
            BloomStrokeDefaults.DropletPill.copy(lightSurfaceSubtleOutlineAlpha = 0f)
        } else {
            BloomStrokeDefaults.DropletPill
        }
    }

    val visibilityModifier = modifier.drawWithContent {
        val outerAlpha = alphaProvider?.invoke() ?: 1f
        val p = state.pressProgress.value
        if (p * outerAlpha > 0.005f) {
            drawContent()
        }
    }

    if (combinedBackdrop != null) {
        Box(
            modifier = visibilityModifier
                .drawBackdrop(
                    backdrop = combinedBackdrop,
                    shape = { shape },
                    highlight = null,
                    shadow = {
                        val p = state.pressProgress.value.coerceIn(0f, 1f)
                        if (p > 0.01f) {
                            Shadow(
                                radius = 10.dp * p,
                                offset = DpOffset(0.dp, 3.dp),
                                color = Color.Black.copy(alpha = 0.16f * p)
                            )
                        } else null
                    },
                    innerShadow = {
                        val p = state.pressProgress.value.coerceIn(0f, 1f)
                        if (p > 0.01f) {
                            InnerShadow(
                                radius = GlassSliderDefaults.DropletInnerShadowRadius * p,
                                offset = DpOffset(0.dp, 1.dp),
                                color = Color.Black.copy(alpha = 0.14f),
                                alpha = p
                            )
                        } else null
                    },
                    effects = {
                        val p = state.pressProgress.value.coerceIn(0f, 1f)
                        if (p > 0.01f) {
                            val rawHeightPx = (GlassSliderDefaults.DropletRefractionHeight * p).toPx()
                            val effectiveHeightPx = clampLensRefractionHeightPx(rawHeightPx, size.minDimension)
                            if (effectiveHeightPx > 0f) {
                                val scaleRatio = (effectiveHeightPx / rawHeightPx).coerceIn(0f, 1f)
                                lens(
                                    refractionHeight = effectiveHeightPx,
                                    refractionAmount = (GlassSliderDefaults.DropletRefractionAmount * p).toPx() * scaleRatio,
                                    depthEffect = true,
                                    chromaticAberration = true
                                )
                            }
                        }
                    },
                    layerBlock = dropletLayerBlock,
                    onDrawSurface = {
                        val p = state.pressProgress.value.coerceIn(0f, 1f)
                        if (p > 0.01f) {
                            if (isDarkSurface) {
                                drawRect(Color.White.copy(alpha = 0.09f * p))
                                drawRect(
                                    Brush.verticalGradient(
                                        0f to Color.White.copy(alpha = 0.18f * p),
                                        0.5f to Color.Transparent,
                                        1f to Color.White.copy(alpha = 0.07f * p)
                                    )
                                )
                            } else {
                                drawRect(Color.White.copy(alpha = 0.16f * p))
                                drawRect(
                                    Brush.verticalGradient(
                                        0f to Color.White.copy(alpha = 0.28f * p),
                                        0.48f to Color.Transparent,
                                        1f to Color.White.copy(alpha = 0.12f * p)
                                    )
                                )
                            }
                        }
                    }
                )
                .bloomStroke(
                    cornerRadius = innerCornerRadius,
                    style = dropletBloomStyle,
                    extraDegrees = 90f,
                    alphaProvider = {
                        val outerAlpha = (alphaProvider?.invoke() ?: 1f).coerceIn(0f, 1f)
                        (state.pressProgress.value * outerAlpha).coerceIn(0f, 1f)
                    }
                )
                .size(thumbWidth, thumbHeight)
        )
    } else {
        Box(
            modifier = visibilityModifier
                .graphicsLayer(dropletLayerBlock)
                .size(thumbWidth, thumbHeight)
                .background(Color.White.copy(alpha = if (isDarkSurface) 0.18f else 0.28f), shape)
                .bloomStroke(
                    cornerRadius = innerCornerRadius,
                    style = dropletBloomStyle,
                    extraDegrees = 90f
                )
        )
    }
}

/**
 * 通用 LSPosed 风格液态玻璃分段条容器（适用于页面内水平/垂直分段切换条）。
 *
 * 自动组装无递归的双层采样架构与像素级双色切割：
 * - 外框玻璃条（`pageBackdrop` + `BloomStroke(-45°)` + `interactiveGlassHighlight` + 呼吸缩放）
 * - [LiquidGlassDualColorTabs]：静息态按胶囊精确裁剪分割未选中（`isInsideSlider = false`）与高亮选中（`isInsideSlider = true`）双色内容
 * - 同级隐形激活录制层（[LiquidGlassActiveTabsRecorder]，将通透微模糊底板 + 高亮色 Tab 写入 `tabsBackdrop`）
 * - 同级覆盖水滴滑块（[LiquidGlassActiveDroplet]，采样 `CombinedBackdrop(pageBackdrop, tabsBackdrop)`，`+90°` 重力棱光）
 */
@Composable
fun LiquidGlassSegmentedBar(
    backdrop: Backdrop?,
    itemCount: Int,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 48.dp,
    cornerRadius: Dp = barHeight / 2,
    thumbPadding: Dp = GlassSliderDefaults.ThumbPadding,
    maxPressScale: Float = GlassSliderDefaults.CompactMaxPressScale,
    isDarkSurface: Boolean = AppTheme.isDark,
    surfaceAlpha: Float = GlassDefaults.SurfaceAlpha,
    orientation: Orientation = Orientation.Horizontal,
    content: @Composable BoxScope.(isInsideSlider: Boolean) -> Unit
) {
    val segmentState = rememberLiquidSegmentState(itemCount = itemCount, selectedIndex = selectedIndex)
    val tabsBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = if (backdrop != null) {
        rememberCombinedBackdrop(backdrop, tabsBackdrop)
    } else {
        null
    }
    val currentOnSelect by rememberUpdatedState(onSelect)

    BoxWithConstraints(
        modifier = modifier
    ) {
        val containerWidth = maxWidth
        val containerHeight = if (orientation == Orientation.Vertical) maxHeight else barHeight
        val outerShape = remember(cornerRadius) { RoundedRectangle(cornerRadius) }

        val outerLayerBlock: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {
            val progress = segmentState.pressProgress.value.coerceIn(0f, 1f)
            if (size.width > 0f) {
                val maxScale = (size.width + 8.dp.toPx()) / size.width
                val s = lerp(1f, maxScale, progress)
                scaleX = s
                scaleY = s
            }
        }

        // 1. 外框玻璃容器（带背景采样、呼吸微放大、指尖聚光灯与 -45° 重力棱边高光）
        Box(
            modifier = Modifier
                .size(containerWidth, containerHeight)
                .then(
                    if (backdrop != null) {
                        Modifier.liquidGlass(
                            backdrop = backdrop,
                            cornerRadius = cornerRadius,
                            surfaceAlpha = surfaceAlpha,
                            isDarkSurface = isDarkSurface,
                            layerBlock = outerLayerBlock,
                            enableShadow = true,
                            enableBloomStroke = true,
                            bloomExtraDegrees = -45f
                        )
                    } else {
                        Modifier
                            .graphicsLayer(outerLayerBlock)
                            .background(
                                color = if (isDarkSurface) Color(0xFF23232A).copy(alpha = 0.84f)
                                else Color(0xFFF2F2F7).copy(alpha = 0.88f),
                                shape = outerShape
                            )
                            .bloomStroke(
                                cornerRadius = cornerRadius,
                                style = if (isDarkSurface) BloomStrokeDefaults.OuterDark else BloomStrokeDefaults.OuterLight,
                                extraDegrees = -45f
                            )
                    }
                )
                .interactiveGlassHighlight(
                    shape = outerShape,
                    isDarkSurface = isDarkSurface
                )
                .liquidSegmentDrag(
                    state = segmentState,
                    itemCount = itemCount,
                    selectedIndex = selectedIndex,
                    onSettle = { currentOnSelect(it) },
                    orientation = orientation
                )
        ) {
            LiquidGlassDualColorTabs(
                state = segmentState,
                containerWidth = containerWidth,
                containerHeight = containerHeight,
                cornerRadius = cornerRadius,
                thumbPadding = thumbPadding,
                isDarkSurface = isDarkSurface,
                orientation = orientation,
                unselectedContent = { content(false) },
                selectedContent = { content(true) }
            )
        }

        // 2. 激活态高亮层录制器（同级隐形层，拖动/按压时为水滴提供玻璃表面 + 高亮内容）
        LiquidGlassActiveTabsRecorder(
            pageBackdrop = backdrop,
            activeTabsBackdrop = tabsBackdrop,
            state = segmentState,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            cornerRadius = cornerRadius,
            isDarkSurface = isDarkSurface,
            surfaceAlpha = surfaceAlpha,
            layerBlock = outerLayerBlock,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            content(true)
        }

        // 3. 激活态 3D 水滴放大镜（同级兄弟层，不被外框裁剪）
        LiquidGlassActiveDroplet(
            combinedBackdrop = combinedBackdrop,
            state = segmentState,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            cornerRadius = cornerRadius,
            thumbPadding = thumbPadding,
            maxPressScale = maxPressScale,
            isDarkSurface = isDarkSurface,
            orientation = orientation,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

// =============================================================================
// 兼容层：保留既有 API 签名以防外部或测试过渡期依赖
// =============================================================================

data class GlassThumbGeometry(
    val width: Dp,
    val offsetX: Dp,
    val stretchScale: Float,
    val anchorAtEnd: Boolean,
    val height: Dp,
    val innerCornerRadius: Dp,
    val isTracking: Boolean
)

@Composable
fun rememberGlassThumbGeometry(
    containerWidth: Dp,
    itemCount: Int,
    selectedIndex: Int,
    dragOffsetX: Float?,
    barHeight: Dp,
    cornerRadius: Dp,
    thumbPadding: Dp = GlassSliderDefaults.ThumbPadding
): GlassThumbGeometry {
    if (itemCount <= 0) {
        return GlassThumbGeometry(0.dp, 0.dp, 1f, false, 0.dp, 0.dp, false)
    }

    val slotWidth = containerWidth / itemCount
    val activeIndex = selectedIndex.coerceIn(0, itemCount - 1)
    val targetThumbWidth = (slotWidth - thumbPadding * 2).coerceAtLeast(0.dp)
    val baseOffsetX = slotWidth * activeIndex + thumbPadding

    val density = LocalDensity.current
    val targetThumbOffsetX = if (dragOffsetX != null) {
        val fingerXDp = with(density) { dragOffsetX.toDp() }
        val halfThumb = targetThumbWidth / 2
        (fingerXDp - halfThumb)
            .coerceIn(thumbPadding, (containerWidth - targetThumbWidth - thumbPadding).coerceAtLeast(thumbPadding))
    } else {
        baseOffsetX
    }

    val isTracking = dragOffsetX != null
    val springSpec = if (isTracking) {
        spring<Dp>(stiffness = GlassSliderDefaults.TRACKING_STIFFNESS, dampingRatio = GlassSliderDefaults.TRACKING_DAMPING)
    } else {
        spring<Dp>(stiffness = GlassSliderDefaults.SETTLE_STIFFNESS, dampingRatio = GlassSliderDefaults.SETTLE_DAMPING)
    }

    val animatedThumbWidth by animateDpAsState(targetValue = targetThumbWidth, animationSpec = springSpec, label = "glassThumbWidth")
    val animatedThumbOffsetX by animateDpAsState(targetValue = targetThumbOffsetX, animationSpec = springSpec, label = "glassThumbOffset")
    val offsetDiff = targetThumbOffsetX - animatedThumbOffsetX
    val stretchAmount = offsetDiff.value.absoluteValue.dp * GlassSliderDefaults.STRETCH_FACTOR
    val stretchScale = if (animatedThumbWidth.value > 0f) 1f + (stretchAmount / animatedThumbWidth) else 1f

    return GlassThumbGeometry(
        width = animatedThumbWidth,
        offsetX = animatedThumbOffsetX,
        stretchScale = stretchScale,
        anchorAtEnd = offsetDiff.value < 0f,
        height = (barHeight - thumbPadding * 2).coerceAtLeast(0.dp),
        innerCornerRadius = (cornerRadius - thumbPadding).coerceAtLeast(0.dp),
        isTracking = isTracking
    )
}

@Composable
fun GlassThumb(
    backdrop: Backdrop,
    geometry: GlassThumbGeometry,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    alphaProvider: (() -> Float)? = null,
    surfaceColor: Color = if (AppTheme.isDark) Color(0xFF2C2C34) else Color.White,
    surfaceAlpha: Float = GlassSliderDefaults.ThumbSurfaceAlpha,
    blurRadius: Dp = GlassSliderDefaults.BlurRadius,
    orientation: Orientation = Orientation.Horizontal
) {
    if (geometry.width <= 0.dp || geometry.height <= 0.dp || (alphaProvider == null && alpha <= 0f)) return

    val innerCornerRadius = geometry.innerCornerRadius
    Box(
        modifier
            .drawWithContent {
                val currentAlpha = alphaProvider?.invoke() ?: alpha
                if (currentAlpha > 0.001f) {
                    drawContent()
                }
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(innerCornerRadius) },
                highlight = null,
                shadow = null,
                effects = {
                    vibrancy()
                    if (blurRadius > 0.dp) {
                        blur(blurRadius.toPx())
                    }
                    lens(
                        refractionHeight = GlassSliderDefaults.DropletRefractionHeight.toPx(),
                        refractionAmount = GlassSliderDefaults.DropletRefractionAmount.toPx(),
                        depthEffect = true,
                        chromaticAberration = true
                    )
                },
                layerBlock = {
                    this.alpha = (alphaProvider?.invoke() ?: alpha).coerceIn(0f, 1f)
                    if (orientation == Orientation.Vertical) {
                        translationY = geometry.offsetX.toPx()
                        scaleY = geometry.stretchScale
                    } else {
                        translationX = geometry.offsetX.toPx()
                        scaleX = geometry.stretchScale
                    }
                    transformOrigin = if (orientation == Orientation.Vertical) {
                        TransformOrigin(0.5f, if (geometry.anchorAtEnd) 1f else 0f)
                    } else if (geometry.anchorAtEnd) {
                        TransformOrigin(1f, 0.5f)
                    } else {
                        TransformOrigin(0f, 0.5f)
                    }
                },
                onDrawSurface = {
                    if (surfaceAlpha > 0f) {
                        val rPx = innerCornerRadius.toPx()
                        drawRoundRect(
                            color = surfaceColor.copy(alpha = surfaceAlpha),
                            cornerRadius = CornerRadius(rPx, rPx)
                        )
                    }
                }
            )
            .bloomStroke(
                cornerRadius = innerCornerRadius,
                style = BloomStrokeDefaults.DropletPill,
                extraDegrees = 90f,
                alphaProvider = alphaProvider ?: { alpha }
            )
            .size(
                if (orientation == Orientation.Vertical) geometry.height else geometry.width,
                if (orientation == Orientation.Vertical) geometry.width else geometry.height
            )
    )
}

fun Modifier.glassSegmentDrag(
    itemCount: Int,
    onDragOffsetChange: (Float?) -> Unit,
    onSettle: (Int) -> Unit,
    onPressChanged: (Boolean) -> Unit,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Horizontal
): Modifier = this.pointerInput(itemCount, enabled, orientation) {
    if (!enabled || itemCount <= 0) return@pointerInput

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downAxis = if (orientation == Orientation.Vertical) down.position.y else down.position.x
        var currentX = downAxis
        var dragging = false
        var released = false
        val touchSlop = viewConfiguration.touchSlop

        try {
            onPressChanged(true)

            var inGesture = true
            while (inGesture) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull()
                if (change == null) {
                    inGesture = false
                } else if (change.pressed) {
                    if (!dragging && (change.position - down.position).getDistance() > touchSlop) {
                        dragging = true
                    }
                    if (dragging) {
                        currentX = if (orientation == Orientation.Vertical) change.position.y else change.position.x
                        onDragOffsetChange(currentX)
                        change.consume()
                    }
                } else {
                    released = true
                    inGesture = false
                }
            }
        } finally {
            onDragOffsetChange(null)
            onPressChanged(false)
        }

        if (released) {
            val slotWidthPx = (if (orientation == Orientation.Vertical) size.height else size.width).toFloat() / itemCount.toFloat()
            if (slotWidthPx > 0f) {
                val settleX = if (dragging) currentX else downAxis
                onSettle((settleX / slotWidthPx).toInt().coerceIn(0, itemCount - 1))
            }
        }
    }
}
