package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
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
 * 关键设计：**节点尺寸恒定**，拉伸与位移全部交给 draw 阶段的 `graphicsLayer` 变换。
 *
 * 实测对比（144Hz、帧预算 6.94ms）：若让 `.size()` 随拉伸每帧变化，
 * UI 线程会在 `postAndWait` 上阻塞约 9.4ms/帧（等 RenderThread 重录 backdrop 图层），
 * doFrame 中位数达到 8~14ms；改为恒定尺寸后 doFrame 中位数降至约 5ms、
 * `postAndWait` 降至 0.33ms，超预算帧从约 30% 降到 5~10%。
 * 且距离越长原本的拉伸量越大、重录代价越高，这正是「跨格切换比相邻切换更卡」的原因。
 *
 * @property width 静止宽度（节点尺寸，恒定）
 * @property offsetX 未拉伸的位移；拉伸带来的视觉偏移由 [stretchScale] + [anchorAtEnd] 表达
 * @property stretchScale 水平拉伸比例（Gooey 拉丝），1f 表示未拉伸
 * @property anchorAtEnd 拉伸锚点：true = 右端固定（向左运动），false = 左端固定（向右运动）
 * @property height 滑块高度
 * @property innerCornerRadius 滑块内部圆角（容器圆角减去内边距）
 * @property isTracking 当前是否处于跟手拖拽状态
 */
data class GlassThumbGeometry(
    val width: Dp,
    val offsetX: Dp,
    val stretchScale: Float,
    val anchorAtEnd: Boolean,
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
        return GlassThumbGeometry(0.dp, 0.dp, 1f, false, 0.dp, 0.dp, false)
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
    // 向左运动时左端前探、向右运动时右端后延，形成被液体拖拽的观感。
    //
    // 这里不改变节点尺寸，而是换算成「拉伸后宽度 / 静止宽度」的缩放比例，
    // 配合锚点即可精确复现原来的外形：
    //   向右（offsetDiff ≥ 0）：左端固定，右端外扩 |offsetDiff| * factor
    //   向左（offsetDiff < 0）：右端固定，左端外伸 |offsetDiff| * factor
    val offsetDiff = targetThumbOffsetX - animatedThumbOffsetX
    val stretchAmount = offsetDiff.value.absoluteValue.dp * GlassSliderDefaults.STRETCH_FACTOR
    val stretchScale = if (animatedThumbWidth.value > 0f) {
        1f + (stretchAmount / animatedThumbWidth)
    } else {
        1f
    }

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

/**
 * 分段滑块的可视滑块本体：只保留透镜折射，不带表面铺底，因此能“透出”下方内容。
 *
 * 位移与拉伸全部在 `graphicsLayer`（draw 阶段）完成，节点尺寸恒定：
 * 每帧既不触发测量/布局，也不会让 backdrop 图层重录尺寸。
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
            .graphicsLayer {
                this.alpha = alpha
                translationX = geometry.offsetX.toPx()
                scaleX = geometry.stretchScale
                // 拉伸锚点随运动方向切换，保证“前导边外伸、尾部边固定”
                transformOrigin = if (geometry.anchorAtEnd) {
                    TransformOrigin(1f, 0.5f)
                } else {
                    TransformOrigin(0f, 0.5f)
                }
            }
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
 * 只有位移越过 touch slop 才开始跟手：单纯点击不会先让滑块飞到手指、再吸附回目标分格，
 * 一次点击只产生一段滑块动画，省掉一整段透镜折射渲染，点击切换时明显更顺。
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
        val down = awaitFirstDown(requireUnconsumed = false)
        var currentX = down.position.x
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
                        currentX = change.position.x
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

        // 只有正常抬手才提交选中变更（手势被打断时不提交）
        if (released) {
            val slotWidthPx = size.width.toFloat() / itemCount.toFloat()
            if (slotWidthPx > 0f) {
                // 点击用按下位置判定目标分格，拖动用抬起位置
                val settleX = if (dragging) currentX else down.position.x
                onSettle((settleX / slotWidthPx).toInt().coerceIn(0, itemCount - 1))
            }
        }
    }
}
