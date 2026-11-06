package com.example.seteasecloudmusic.feature.player.presentation

import android.graphics.CornerPathEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.example.seteasecloudmusic.core.player.PlaybackMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 纯填充圆角多边形绘制（仅使用单次 STYLE.FILL + CornerPathEffect，绝不叠加 Stroke 描边，杜绝半透明边缘叠加发亮）
 */
internal fun DrawScope.drawRoundedFilledPath(
    path: Path,
    color: Color,
    cornerRadiusPx: Float
) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.FILL
            this.color = color.toArgb()
            if (cornerRadiusPx > 0.1f) {
                pathEffect = CornerPathEffect(cornerRadiusPx)
            }
        }
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
    }
}

/**
 * 1:1 来自 applemusic-like-lyrics-main/packages/react-full/src/components/PrebuiltLyricPlayer/icon_play.svg
 * 的播放图标路径 (viewBox 0 0 38 38)
 */
private val AmllPlayIconPath by lazy {
    PathParser.createPathFromPathData(
        "M5.80762 32.4896V5.4925C5.80762 4.305 6.12305 3.41438 6.75391 2.82063C7.38477 2.22688 8.13932 1.93 9.01758 1.93C9.78451 1.93 10.5391 2.14029 11.2812 2.56086L33.7324 15.6605C34.5859 16.1553 35.223 16.6562 35.6436 17.1634C36.0641 17.6582 36.2744 18.2705 36.2744 19.0003C36.2744 19.7054 36.0641 20.3177 35.6436 20.8372C35.223 21.3444 34.5859 21.8392 33.7324 22.3216L11.2812 35.4212C10.5391 35.8542 9.78451 36.0706 9.01758 36.0706C8.13932 36.0706 7.38477 35.7676 6.75391 35.1614C6.12305 34.5677 5.80762 33.6771 5.80762 32.4896Z"
    ).asComposePath()
}

/**
 * 1:1 复刻 Apple 经典播放实心圆角三角图标（基于 AMLL icon_play.svg 矢量路径）
 */
@Composable
fun AppleMusicPlayTriangleIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Black,
    size: Dp = 16.dp
) {
    Canvas(modifier = modifier.size(size)) {
        drawSvgPathCentered(
            path = AmllPlayIconPath,
            color = tint,
            style = Fill,
            viewBoxSize = 38f,
            scaleFactor = 1.0f
        )
    }
}

private val HeartFilledPath by lazy {
    PathParser.createPathFromPathData(
        "M707.392 93.355c-77.264 0-149.104 38.759-196.389 102.673-47.258-63.929-119.083-102.673-196.389-102.673-139.281 0-252.596 123.888-252.596 276.181 0 90.853 40.585 154.588 73.209 205.797 94.816 148.743 333.217 333.849 343.314 341.653 9.727 7.52 21.1 11.282 32.447 11.282 11.373 0 22.73-3.761 32.447-11.282 10.11-7.804 248.524-192.91 343.313-341.653 32.64-51.209 73.238-114.944 73.238-205.797-0.001-152.292-113.316-276.181-252.596-276.181z"
    ).asComposePath()
}

private val ShufflePath by lazy {
    PathParser.createPathFromPathData(
        "M768.863 325.733c-48.775 0-170.409 121.182-259.229 207.077-134.661 130.244-261.862 256.802-363.67 256.802h-62.62C57.75 789.612 37 768.828 37 743.223c0-25.604 20.749-46.387 46.342-46.387h62.622c64.335 0 194.592-129.518 299.256-230.745 127.382-123.198 237.392-233.135 323.643-233.135h59.769l-63.418-61.55c-18.113-18.088-18.142-45.665-0.062-63.788 18.08-18.128 47.422-18.164 65.535-0.07l142.708 142.495A46.306 46.306 0 0 1 987 282.83c0 12.308-4.893 24.06-13.604 32.759l-142.71 142.4c-9.043 9.032-20.89 13.328-32.735 13.328-11.875 0-23.749-4.977-32.799-14.05-18.08-18.121-18.05-48.358 0.062-66.445l63.417-65.09h-59.768z m61.824 238.627c-18.113-18.095-47.456-18.066-65.535 0.062-18.08 18.121-18.05 49.237 0.062 67.323l63.417 65.09h-59.768c-38.06 0-101.643-56.357-164.685-115.353-18.69-17.493-48.021-17.394-65.495 1.306-17.486 18.7-16.507 49.382 2.183 66.87 96.042 89.898 160.826 139.952 227.997 139.952h59.769l-63.418 61.549c-18.113 18.087-18.142 46.552-0.062 64.674 9.051 9.072 20.924 13.167 32.799 13.167 11.845 0 23.692-4.74 32.736-13.773l142.708-142.619c8.71-8.7 13.604-20.56 13.604-32.87a46.44 46.44 0 0 0-13.604-32.842L830.687 564.36zM83.342 325.733h62.622c48.84 0 121.646 62.22 179.612 114.893 8.88 8.065 20.026 12.923 31.147 12.923 12.604 0 25.172-4.677 34.315-14.75 17.208-18.954 15.8-49.839-3.146-67.055-90.646-82.365-166.477-138.787-241.928-138.787H83.342C57.749 232.957 37 253.743 37 279.346s20.75 46.387 46.342 46.387z"
    ).asComposePath()
}

/**
 * 循环播放矢量路径（轨道与箭头顶点完全连续无缝融合，92f 粗线宽，绝无空心点）
 */
private val RepeatPath by lazy {
    Path().apply {
        // 顶部顺时针循环轨道：从左竖线向上，经左上圆角拐向右侧横线，直达右侧箭头顶点
        moveTo(100f, 512f)
        lineTo(100f, 370f)
        quadraticTo(100f, 210f, 260f, 210f)
        lineTo(880f, 210f)
        // 顶部向右箭头顶点向两侧展开的斜边（45°，顶点与横线在 (880, 210) 严格重合，绝无空心点）
        moveTo(750f, 80f)
        lineTo(880f, 210f)
        lineTo(750f, 340f)

        // 底部顺时针循环轨道：从右竖线向下，经右下圆角拐向左侧横线，直达左侧箭头顶点
        moveTo(924f, 512f)
        lineTo(924f, 654f)
        quadraticTo(924f, 814f, 764f, 814f)
        lineTo(144f, 814f)
        // 底部向左箭头顶点向两侧展开的斜边（45°，顶点与横线在 (144, 814) 严格重合，绝无空心点）
        moveTo(274f, 684f)
        lineTo(144f, 814f)
        lineTo(274f, 944f)
    }
}

private fun DrawScope.drawSvgPathCentered(
    path: Path,
    color: Color,
    style: androidx.compose.ui.graphics.drawscope.DrawStyle = Fill,
    viewBoxSize: Float = 1024f,
    scaleFactor: Float = 0.88f
) {
    val canvasW = size.width
    val canvasH = size.height
    val scale = (min(canvasW, canvasH) / viewBoxSize) * scaleFactor
    val tx = (canvasW - viewBoxSize * scale) * 0.5f
    val ty = (canvasH - viewBoxSize * scale) * 0.5f
    withTransform({
        translate(tx, ty)
        scale(scale, scale, Offset.Zero)
    }) {
        drawPath(path = path, color = color, style = style)
    }
}

/**
 * 1:1 复刻截图收藏爱心图标 (替换原 AppleMusicStarIcon)：
 * - 未收藏状态：粗线条加粗空心心形 (Stroke width = 86f)
 * - 已收藏状态：实心红心 (0xFFFA233B) + 心跳弹性缩放动效
 */
@Composable
fun AppleMusicHeartIcon(
    isLiked: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.88f),
    size: Dp = 24.dp
) {
    val scale = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(isLiked) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        scale.snapTo(0.72f)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    val effectiveAlpha = if (isLiked) 1f else tint.alpha
    val solidColor = if (isLiked) {
        if (tint == Color.White.copy(alpha = 0.88f) || tint == Color.White.copy(alpha = 0.92f)) {
            Color(0xFFFA233B)
        } else {
            tint.copy(alpha = 1f)
        }
    } else {
        tint.copy(alpha = 1f)
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = effectiveAlpha
                scaleX = scale.value
                scaleY = scale.value
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            if (isLiked) {
                drawSvgPathCentered(
                    path = HeartFilledPath,
                    color = solidColor,
                    style = Fill,
                    scaleFactor = 0.86f
                )
            } else {
                // 加粗空心描边爱心，线宽 86f 完美匹配粗字体与操作栏视觉份量
                drawSvgPathCentered(
                    path = HeartFilledPath,
                    color = solidColor,
                    style = Stroke(
                        width = 86f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    ),
                    scaleFactor = 0.81f
                )
            }
        }
    }
}

/**
 * 保持向后兼容：AppleMusicStarIcon 统一委托给最新的 AppleMusicHeartIcon
 */
@Composable
fun AppleMusicStarIcon(
    isLiked: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.88f),
    size: Dp = 24.dp
) {
    AppleMusicHeartIcon(
        isLiked = isLiked,
        modifier = modifier,
        tint = tint,
        size = size
    )
}

/**
 * 底部播放模式切换图标 (循环 ↔ 随机)：
 * - [PlaybackMode.SEQUENTIAL]: 列表循环双箭头 (加粗、放大、箭头无空心点)
 * - [PlaybackMode.SHUFFLE]: 随机播放交叉双箭头
 */
@Composable
fun AppleMusicPlaybackModeIcon(
    mode: PlaybackMode,
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.65f),
    size: Dp = 24.dp
) {
    val scale = remember { Animatable(1f) }
    var previousMode by remember { mutableStateOf(mode) }

    LaunchedEffect(mode) {
        if (previousMode != mode) {
            previousMode = mode
            scale.snapTo(0.74f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val solidColor = tint.copy(alpha = 1f)
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = tint.alpha
                scaleX = scale.value
                scaleY = scale.value
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            if (mode == PlaybackMode.SHUFFLE) {
                drawSvgPathCentered(
                    path = ShufflePath,
                    color = solidColor,
                    style = Fill,
                    scaleFactor = 0.90f
                )
            } else {
                drawSvgPathCentered(
                    path = RepeatPath,
                    color = solidColor,
                    style = Stroke(
                        width = 92f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    ),
                    scaleFactor = 0.92f
                )
            }
        }
    }
}

/**
 * 1:1 复刻截图右侧三圆点更多按钮 (•••)
 */
@Composable
fun AppleMusicMoreIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.88f),
    size: Dp = 24.dp
) {
    val solidColor = tint.copy(alpha = 1f)
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = tint.alpha
            }
    ) {
        val w = this.size.width
        val cy = this.size.height * 0.5f
        val r = w * 0.082f
        drawCircle(color = solidColor, radius = r, center = Offset(w * 0.21f, cy))
        drawCircle(color = solidColor, radius = r, center = Offset(w * 0.50f, cy))
        drawCircle(color = solidColor, radius = r, center = Offset(w * 0.79f, cy))
    }
}

/**
 * 1:1 复刻镂空 [E] Explicit 标识
 */
@Composable
fun AppleMusicExplicitBadge(
    modifier: Modifier = Modifier,
    size: Dp = 15.dp
) {
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = 0.65f
            }
    ) {
        val w = this.size.width
        val h = this.size.height
        val r = w * 0.21f

        drawRoundRect(
            color = Color.White,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(r, r)
        )

        val strokeW = w * 0.125f
        val left = w * 0.32f
        val right = w * 0.70f
        val midRight = w * 0.64f
        val top = h * 0.27f
        val mid = h * 0.50f
        val bottom = h * 0.73f

        drawLine(
            color = Color.Black,
            start = Offset(left, top),
            end = Offset(left, bottom),
            strokeWidth = strokeW,
            cap = StrokeCap.Square,
            blendMode = BlendMode.DstOut
        )
        drawLine(
            color = Color.Black,
            start = Offset(left, top),
            end = Offset(right, top),
            strokeWidth = strokeW,
            cap = StrokeCap.Square,
            blendMode = BlendMode.DstOut
        )
        drawLine(
            color = Color.Black,
            start = Offset(left, mid),
            end = Offset(midRight, mid),
            strokeWidth = strokeW * 0.92f,
            cap = StrokeCap.Square,
            blendMode = BlendMode.DstOut
        )
        drawLine(
            color = Color.Black,
            start = Offset(left, bottom),
            end = Offset(right, bottom),
            strokeWidth = strokeW,
            cap = StrokeCap.Square,
            blendMode = BlendMode.DstOut
        )
    }
}

/**
 * 1:1 复刻截图底部左侧歌词切换按钮：
 * - 未激活（截图 1）：空心圆角气泡 + 双引号轮廓
 * - 激活（截图 2、4）：半透明白色实心大圆底 + 中心镂空（DstOut）对话气泡 + 内部白色双引号
 */
@Composable
fun AppleMusicLyricsToggleIcon(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    inactiveTint: Color = Color.White.copy(alpha = 0.55f)
) {
    if (isActive) {
        Canvas(
            modifier = modifier
                .size(buttonSize)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    alpha = 0.68f
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. 外层半透明白色圆形底
            drawCircle(
                color = Color.White,
                radius = w * 0.5f,
                center = Offset(w * 0.5f, h * 0.5f)
            )

            // 2. 中心镂空对话气泡
            val bx = w * 0.24f
            val by = h * 0.25f
            val bw = w * 0.52f
            val bh = h * 0.42f
            val br = bw * 0.25f

            val bubblePath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(bx, by, bx + bw, by + bh),
                        cornerRadius = CornerRadius(br, br)
                    )
                )
                moveTo(bx + bw * 0.20f, by + bh - 1f)
                lineTo(bx + bw * 0.14f, by + bh + bh * 0.24f)
                lineTo(bx + bw * 0.42f, by + bh - 1f)
                close()
            }
            drawPath(
                path = bubblePath,
                color = Color.Black,
                style = Fill,
                blendMode = BlendMode.DstOut
            )

            // 3. 在镂空区域内回填两枚圆润双引号
            drawQuoteMarks(
                leftCenter = Offset(bx + bw * 0.36f, by + bh * 0.45f),
                rightCenter = Offset(bx + bw * 0.64f, by + bh * 0.45f),
                dotRadius = bw * 0.082f,
                color = Color.White,
                blendMode = BlendMode.SrcOver
            )
        }
    } else {
        val solidColor = inactiveTint.copy(alpha = 1f)
        Box(
            modifier = modifier
                .size(buttonSize)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    alpha = inactiveTint.alpha
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(iconSize)) {
                val w = this.size.width
                val h = this.size.height
                val r = w * 0.22f
                val rectBottom = h * 0.78f

                val bubblePath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = w * 0.07f,
                            top = h * 0.10f,
                            right = w * 0.93f,
                            bottom = rectBottom,
                            radiusX = r,
                            radiusY = r
                        )
                    )
                }
                val tailPath = Path().apply {
                    moveTo(w * 0.25f, rectBottom)
                    lineTo(w * 0.18f, h * 0.93f)
                    lineTo(w * 0.40f, rectBottom)
                }
                drawPath(
                    bubblePath,
                    color = solidColor,
                    style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    tailPath,
                    color = solidColor,
                    style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                drawQuoteMarks(
                    leftCenter = Offset(w * 0.38f, h * 0.41f),
                    rightCenter = Offset(w * 0.62f, h * 0.41f),
                    dotRadius = w * 0.070f,
                    color = solidColor,
                    blendMode = BlendMode.SrcOver
                )
            }
        }
    }
}

private fun DrawScope.drawQuoteMarks(
    leftCenter: Offset,
    rightCenter: Offset,
    dotRadius: Float,
    color: Color,
    blendMode: BlendMode
) {
    drawSingleQuoteMark(center = leftCenter, dotRadius = dotRadius, color = color, blendMode = blendMode)
    drawSingleQuoteMark(center = rightCenter, dotRadius = dotRadius, color = color, blendMode = blendMode)
}

private fun DrawScope.drawSingleQuoteMark(
    center: Offset,
    dotRadius: Float,
    color: Color,
    blendMode: BlendMode
) {
    // 使用单一闭合轮廓 Fill 绘制逗号形引号，绝不用 Circle + Stroke 叠加
    drawCircle(
        color = color,
        radius = dotRadius,
        center = center,
        blendMode = blendMode
    )
    val commaTail = Path().apply {
        moveTo(center.x + dotRadius * 0.92f, center.y + dotRadius * 0.05f)
        quadraticTo(
            center.x + dotRadius * 0.96f,
            center.y + dotRadius * 1.85f,
            center.x - dotRadius * 0.48f,
            center.y + dotRadius * 2.35f
        )
        quadraticTo(
            center.x - dotRadius * 0.85f,
            center.y + dotRadius * 2.15f,
            center.x - dotRadius * 0.58f,
            center.y + dotRadius * 1.82f
        )
        quadraticTo(
            center.x + dotRadius * 0.12f,
            center.y + dotRadius * 1.38f,
            center.x - dotRadius * 0.05f,
            center.y + dotRadius * 0.55f
        )
        close()
    }
    drawPath(
        path = commaTail,
        color = color,
        style = Fill,
        blendMode = blendMode
    )
}

/**
 * 1:1 复刻截图 AirPlay 音频投播图标（无描边纯填充实心圆角三角 + 3 层同心圆弧，Offscreen 统一透明度）
 */
@Composable
fun AppleMusicAirPlayIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.55f),
    size: Dp = 23.dp
) {
    val solidColor = tint.copy(alpha = 1f)
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = tint.alpha
            }
    ) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = w * 0.080f
        val center = Offset(w * 0.50f, h * 0.47f)

        val radii = floatArrayOf(w * 0.43f, w * 0.29f, w * 0.15f)
        val sweeps = floatArrayOf(270f, 260f, 246f)

        for (i in radii.indices) {
            val r = radii[i]
            val sweep = sweeps[i]
            val start = 270f - sweep * 0.5f
            drawArc(
                color = solidColor,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        // 底部实心向上等腰圆角三角：仅单次 Fill + CornerPathEffect，彻底去除外圈 Stroke 描边
        val triangle = Path().apply {
            moveTo(w * 0.50f, h * 0.57f)
            lineTo(w * 0.26f, h * 0.93f)
            lineTo(w * 0.74f, h * 0.93f)
            close()
        }
        drawRoundedFilledPath(
            path = triangle,
            color = solidColor,
            cornerRadiusPx = w * 0.055f
        )
    }
}

/**
 * 1:1 复刻截图底部右侧待播列表图标（纯 Fill 圆点 + 纯 Fill 胶囊横条，无描边，Offscreen 统一透明度）
 */
@Composable
fun AppleMusicQueueIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.55f),
    size: Dp = 22.dp
) {
    val solidColor = tint.copy(alpha = 1f)
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = tint.alpha
            }
    ) {
        val w = this.size.width
        val h = this.size.height
        val barH = w * 0.092f
        val dotRadius = w * 0.060f

        val ys = floatArrayOf(h * 0.24f, h * 0.50f, h * 0.76f)
        val dotX = w * 0.14f
        val startX = w * 0.30f
        val endX = w * 0.92f

        for (y in ys) {
            drawCircle(color = solidColor, radius = dotRadius, center = Offset(dotX, y))
            drawRoundRect(
                color = solidColor,
                topLeft = Offset(startX, y - barH * 0.5f),
                size = Size(endX - startX, barH),
                cornerRadius = CornerRadius(barH * 0.5f, barH * 0.5f)
            )
        }
    }
}

/**
 * 1:1 复刻音量条左侧静音/低音量小喇叭（纯 Fill 圆角路径，完全去除描边）
 */
@Composable
fun AppleMusicVolumeLowIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.55f),
    size: Dp = 15.dp
) {
    val solidColor = tint.copy(alpha = 1f)
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = tint.alpha
            }
    ) {
        val w = this.size.width
        val h = this.size.height
        val speaker = Path().apply {
            moveTo(w * 0.16f, h * 0.35f)
            lineTo(w * 0.42f, h * 0.35f)
            lineTo(w * 0.76f, h * 0.12f)
            lineTo(w * 0.76f, h * 0.88f)
            lineTo(w * 0.42f, h * 0.65f)
            lineTo(w * 0.16f, h * 0.65f)
            close()
        }
        drawRoundedFilledPath(
            path = speaker,
            color = solidColor,
            cornerRadiusPx = w * 0.055f
        )
    }
}

/**
 * 1:1 复刻音量条右侧高音量喇叭（纯 Fill 圆角喇叭无描边 + 3 道向外辐射声波弧线，Offscreen 统一透明度）
 */
@Composable
fun AppleMusicVolumeHighIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.55f),
    size: Dp = 18.dp
) {
    val solidColor = tint.copy(alpha = 1f)
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = tint.alpha
            }
    ) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = w * 0.072f

        val speaker = Path().apply {
            moveTo(w * 0.05f, h * 0.35f)
            lineTo(w * 0.25f, h * 0.35f)
            lineTo(w * 0.49f, h * 0.13f)
            lineTo(w * 0.49f, h * 0.87f)
            lineTo(w * 0.25f, h * 0.65f)
            lineTo(w * 0.05f, h * 0.65f)
            close()
        }
        drawRoundedFilledPath(
            path = speaker,
            color = solidColor,
            cornerRadiusPx = w * 0.045f
        )

        val waveCenter = Offset(w * 0.44f, h * 0.50f)
        val waveRadii = floatArrayOf(w * 0.17f, w * 0.31f, w * 0.45f)
        val sweeps = floatArrayOf(64f, 74f, 82f)
        for (i in waveRadii.indices) {
            val r = waveRadii[i]
            val sw = sweeps[i]
            drawArc(
                color = solidColor,
                startAngle = -sw * 0.5f,
                sweepAngle = sw,
                useCenter = false,
                topLeft = Offset(waveCenter.x - r, waveCenter.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }
    }
}
