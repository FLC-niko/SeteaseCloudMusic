package com.example.seteasecloudmusic.feature.player.presentation

import android.graphics.CornerPathEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import top.yukonga.miuix.kmp.basic.Icon
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.player.PlaybackMode
import kotlin.math.min

/**
 * 纯填充圆角多边形绘制（仅使用单次 Fill + CornerPathEffect，避免叠加描边导致半透明边缘发亮）
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
 * 播放图标矢量路径（viewBox 0 0 38 38）
 */
private val AmllPlayIconPath by lazy {
    PathParser.createPathFromPathData(
        "M5.80762 32.4896V5.4925C5.80762 4.305 6.12305 3.41438 6.75391 2.82063C7.38477 2.22688 8.13932 1.93 9.01758 1.93C9.78451 1.93 10.5391 2.14029 11.2812 2.56086L33.7324 15.6605C34.5859 16.1553 35.223 16.6562 35.6436 17.1634C36.0641 17.6582 36.2744 18.2705 36.2744 19.0003C36.2744 19.7054 36.0641 20.3177 35.6436 20.8372C35.223 21.3444 34.5859 21.8392 33.7324 22.3216L11.2812 35.4212C10.5391 35.8542 9.78451 36.0706 9.01758 36.0706C8.13932 36.0706 7.38477 35.7676 6.75391 35.1614C6.12305 34.5677 5.80762 33.6771 5.80762 32.4896Z"
    ).asComposePath()
}

/**
 * 播放按钮实心圆角三角图标
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
        "M703.488 42.496c-69.12 0-136.192 22.528-191.488 63.488-55.296-40.96-122.368-63.488-191.488-63.488C143.872 42.496 0 185.856 0 363.008 0 757.76 483.84 970.752 504.832 979.968c3.072 1.536 6.656 2.048 9.728 2.048 4.096 0 8.704-1.024 12.288-3.584 20.48-11.776 497.152-291.84 497.152-615.424 0-177.152-143.872-320.512-320.512-320.512z"
    ).asComposePath()
}

private val HeartHollowPath by lazy {
    PathParser.createPathFromPathData(
        "M703.488 42.496c-69.12 0-136.192 22.528-191.488 63.488-55.296-40.96-122.368-63.488-191.488-63.488C143.872 42.496 0 185.856 0 363.008 0 757.76 483.84 970.752 504.832 979.968c3.072 1.536 6.656 2.048 9.728 2.048 4.096 0 8.704-1.024 12.288-3.584 20.48-11.776 497.152-291.84 497.152-615.424 0-177.152-143.872-320.512-320.512-320.512z m-190.464 886.784c-72.192-34.304-463.36-235.52-463.36-566.272 0-149.504 121.856-271.36 271.36-271.36 64 0 126.464 23.04 175.616 64.512 9.216 7.68 23.04 7.68 32.256 0 49.152-41.472 111.104-64.512 175.616-64.512 149.504 0 271.36 121.856 271.36 271.36-1.536 269.824-392.192 522.752-462.848 566.272z"
    ).asComposePath()
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
 * 收藏爱心图标：
 * - 未收藏状态：空心心形轮廓
 * - 已收藏状态：实心红心 (0xFFFA233B)，切换时触发缩放弹簧动效
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
        // 切换收藏态时先缩至 0.72x 再按中等弹性回弹
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
                    viewBoxSize = 1024f,
                    scaleFactor = 0.88f
                )
            } else {
                drawSvgPathCentered(
                    path = HeartHollowPath,
                    color = solidColor,
                    style = Fill,
                    viewBoxSize = 1024f,
                    scaleFactor = 0.88f
                )
            }
        }
    }
}

/**
 * 收藏星标图标别名，统一委托给 [AppleMusicHeartIcon] 绘制
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
 * 底部播放模式切换图标：
 * - [PlaybackMode.SEQUENTIAL]: 列表循环双箭头
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
        Icon(
            painter = painterResource(
                id = if (mode == PlaybackMode.SHUFFLE) R.drawable.ic_shuffle else R.drawable.ic_repeat
            ),
            contentDescription = null,
            tint = solidColor,
            modifier = Modifier.size(size)
        )
    }
}

/**
 * 右侧三圆点更多操作图标 (•••)
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
 * 镂空 [E] Explicit 标识图标
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
 * 底部歌词切换按钮图标：
 * - 未激活：圆角气泡轮廓与引号
 * - 激活：半透明白色实心圆底与中心镂空气泡引号
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
        Icon(
            painter = painterResource(id = R.drawable.ic_lyrics_active),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.68f),
            modifier = modifier.size(buttonSize)
        )
    } else {
        Box(
            modifier = modifier.size(buttonSize),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_lyrics),
                contentDescription = null,
                tint = inactiveTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * 音频投播图标（实心圆角三角与三层同心圆弧，Offscreen 合成统一透明度）
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

        // 底部实心向上等腰圆角三角：仅单次填充，不叠加外圈描边
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
 * 底部待播列表图标
 */
@Composable
fun AppleMusicQueueIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.55f),
    size: Dp = 19.dp
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_mini_playlist),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size)
    )
}

/**
 * 音量条左侧低音量图标
 */
@Composable
fun AppleMusicVolumeLowIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.55f),
    size: Dp = 15.dp
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_volume_low),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size)
    )
}

/**
 * 音量条右侧高音量图标
 */
@Composable
fun AppleMusicVolumeHighIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.55f),
    size: Dp = 18.dp
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_volume_high),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size)
    )
}
