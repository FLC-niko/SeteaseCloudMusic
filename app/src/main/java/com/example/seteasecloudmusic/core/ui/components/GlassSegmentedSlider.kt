package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.shapes.RoundedRectangle
import kotlin.math.absoluteValue

/**
 * 玻璃分段滑块（Glass Segmented Slider）的共用几何与手势实现。
 *
 * 底栏「首页 / 电台 / 我的」与「我的」页「创建 / 收藏 / 本地」两处使用的是同一套
 * 分段滑块视觉：等分词条 + 跟手玻璃滑块 + 松手吸附，以及苹果风格的
 * Gooey Stretch（运动时按位移差拉长滑块，产生“拉丝”弹性）。
 *
 * 由于两处的容器布局结构不同（底栏的滑块是覆盖在导航条之上的独立图层，
 * 「我的」页的滑块位于容器内部），这里只抽取**完全一致**的两部分：
 * 1. 滑块几何计算与 Gooey 拉伸渲染 —— [rememberGlassThumbGeometry] + [GlassThumb]
 * 2. 拖拽手势与松手吸附 —— [Modifier.glassSegmentDrag]
 *
 * 容器本身的玻璃背景与按压动画由调用方各自组装。
 */
object GlassSliderDefaults {
    /** 滑块与容器内壁的间距 */
    val ThumbPadding: Dp = 4.dp

    /** 跟手时的弹簧：偏硬，保证滑块紧贴手指 */
    const val TRACKING_STIFFNESS = 800f
    const val TRACKING_DAMPING = 0.8f

    /** 回弹吸附时的弹簧：偏软，产生自然的落位感 */
    const val SETTLE_STIFFNESS = 300f
    const val SETTLE_DAMPING = 0.6f

    /** Gooey 拉伸强度：0f 关闭拉丝效果 */
    const val STRETCH_FACTOR = 0.35f
}

/**
 * 滑块在一帧内的完整几何描述。
 *
 * @property width 滑块宽度
 * @property offsetX 滑块左边缘相对容器左侧的偏移
 * @property height 滑块高度
 * @property innerCornerRadius 滑块内部圆角（容器圆角减去内边距）
 * @property isTracking 当前是否处于跟手拖拽状态
 */
data class GlassThumbGeometry(
    val width: Dp,
    val offsetX: Dp,
    val height: Dp,
    val innerCornerRadius: Dp,
    val isTracking: Boolean
)

/**
 * 计算分段滑块的位置与尺寸，并自动处理 Gooey 拉伸。
 *
 * 拖拽中（[dragOffsetX] 非空）滑块中心跟随手指，并夹在容器可用范围内；
 * 松手后吸附到 [selectedIndex] 对应的分格中心，两个阶段使用不同的弹簧参数。
 *
 * @param containerWidth 可用宽度（不含 [thumbPadding]，由调用方传入 `maxWidth`）
 * @param itemCount 分段数量
 * @param selectedIndex 当前选中项，用于计算静止时的落位
 * @param dragOffsetX 拖拽中手指的 X 坐标（px，相对容器左侧）；null 表示未拖拽
 * @param barHeight 容器高度
 * @param cornerRadius 容器圆角
 * @param thumbPadding 滑块与容器内壁的间距
 */
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
        return GlassThumbGeometry(0.dp, 0.dp, 0.dp, 0.dp, false)
    }

    val slotWidth = containerWidth / itemCount
    val activeIndex = selectedIndex.coerceIn(0, itemCount - 1)
    val targetThumbWidth = (slotWidth - thumbPadding * 2).coerceAtLeast(0.dp)
    val baseOffsetX = slotWidth * activeIndex + thumbPadding

    val density = LocalDensity.current
    val targetThumbOffsetX = if (dragOffsetX != null) {
        // 跟手：以手指为中心，夹在容器内壁之间，避免滑块溢出
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

    val animatedThumbWidth by animateDpAsState(
        targetValue = targetThumbWidth,
        animationSpec = springSpec,
        label = "glassThumbWidth"
    )
    val animatedThumbOffsetX by animateDpAsState(
        targetValue = targetThumbOffsetX,
        animationSpec = springSpec,
        label = "glassThumbOffset"
    )

    // Gooey Stretch：按“目标位置 - 当前位置”的差值拉长滑块，
    // 向左运动时起点前探、向右运动时右缘后延，形成被液体拖拽的观感。
    val offsetDiff = targetThumbOffsetX - animatedThumbOffsetX
    val renderedOffsetX = if (offsetDiff.value < 0f) {
        animatedThumbOffsetX + offsetDiff * GlassSliderDefaults.STRETCH_FACTOR
    } else {
        animatedThumbOffsetX
    }
    val renderedWidth =
        animatedThumbWidth + offsetDiff.value.absoluteValue.dp * GlassSliderDefaults.STRETCH_FACTOR

    return GlassThumbGeometry(
        width = renderedWidth,
        offsetX = renderedOffsetX,
        height = (barHeight - thumbPadding * 2).coerceAtLeast(0.dp),
        innerCornerRadius = (cornerRadius - thumbPadding).coerceAtLeast(0.dp),
        isTracking = isTracking
    )
}

/**
 * 分段滑块的可视滑块本体：只保留透镜折射，不带表面铺底，因此能“透出”下方内容。
 *
 * @param alpha 整体透明度（例如搜索态下隐藏主滑块）
 */
@Composable
fun GlassThumb(
    backdrop: Backdrop,
    geometry: GlassThumbGeometry,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    if (geometry.width <= 0.dp || geometry.height <= 0.dp) return

    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .offset(x = geometry.offsetX)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(geometry.innerCornerRadius) },
                effects = {
                    // 滑块只做透镜折射、不做铺底，才能“透出”下方内容产生液体玻璃感
                    lens(
                        refractionHeight = 6f.dp.toPx(),
                        refractionAmount = 12f.dp.toPx(),
                        chromaticAberration = true
                    )
                }
            )
            .size(geometry.width, geometry.height)
    )
}

/**
 * 分段滑块的拖拽手势：按下即跟手，松开才把最终停留位置通过 [onSettle] 提交。
 *
 * 拖动过程中只回调 [onDragOffsetChange]（用于滑块视觉跟随），不会触发选中项变更，
 * 因此页面/列表内容不会在拖拽期间被反复重组。
 *
 * 视觉复位写在 `finally` 中：手势被取消（例如 pointerInput 的 key 变化）时也能回位。
 * 而 [onSettle] 的提交刻意放在 `finally` 之后 —— 手势被打断时不应提交选中变更。
 *
 * @param itemCount 分段数量
 * @param onDragOffsetChange 手指 X 坐标变化（px）；null 表示拖拽结束
 * @param onSettle 松手后落在第几段（已夹在 `0 until itemCount`）
 * @param onPressChanged 按下 / 抬起状态变化，供调用方驱动按压动画
 * @param enabled 为 false 时不响应拖动（例如底栏在搜索展开态下交给 clickable 处理）
 */
fun Modifier.glassSegmentDrag(
    itemCount: Int,
    onDragOffsetChange: (Float?) -> Unit,
    onSettle: (Int) -> Unit,
    onPressChanged: (Boolean) -> Unit,
    enabled: Boolean = true
): Modifier = this.pointerInput(itemCount, enabled) {
    if (!enabled || itemCount <= 0) return@pointerInput

    awaitEachGesture {
        val down = awaitFirstDown()
        var currentX = down.position.x

        try {
            onDragOffsetChange(currentX)
            onPressChanged(true)

            var inGesture = true
            while (inGesture) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull()
                if (change != null && change.pressed) {
                    currentX = change.position.x
                    onDragOffsetChange(currentX)
                    change.consume()
                } else {
                    inGesture = false
                }
            }
        } finally {
            onDragOffsetChange(null)
            onPressChanged(false)
        }

        val slotWidthPx = size.width.toFloat() / itemCount.toFloat()
        if (slotWidthPx > 0f) {
            onSettle((currentX / slotWidthPx).toInt().coerceIn(0, itemCount - 1))
        }
    }
}
