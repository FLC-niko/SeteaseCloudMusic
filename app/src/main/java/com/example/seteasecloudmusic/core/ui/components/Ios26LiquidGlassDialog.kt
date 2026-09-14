package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

/**
 * 通用 iOS 26 液态玻璃弹窗 (Liquid Glass Alert)。
 *
 * 严格按照拆解图复刻三层物理结构：
 * 1. 底部投影层：尺寸 +50px 软弥散深层阴影（模拟 3D 浮动感）
 * 2. 中间层：#F2F2F2 减淡半透明底板
 * 3. 玻璃层（kyant0/backdrop）：圆角 34px（RoundedRectangle 连续超圆角）、lens 透镜折射、chromaticAberration 色散、vibrancy 与边缘高光
 *
 * 支持高度复用：
 * - 默认纯净 Alert 模式（标题 + 描述 + 按钮）
 * - 支持通过 [content] 插槽嵌入任意中间内容（例如 [Ios26DialogInputCard] 输入框容器，或 [Ios26ValueContainer] 状态栏）
 * - 支持双按钮或单按钮模式（[secondaryButtonText] 为 null 时居中单按钮）
 */
@Composable
fun Ios26LiquidGlassDialog(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    title: String? = null,
    description: String? = null,
    secondaryButtonText: String? = "取消",
    primaryButtonText: String = "确定",
    primaryButtonColor: Color = Color(0xFF007AFF),
    isPrimaryLoading: Boolean = false,
    onSecondaryClick: (() -> Unit)? = onDismissRequest,
    onPrimaryClick: () -> Unit = {},
    content: (@Composable () -> Unit)? = null
) {
    val cornerRadius = 34.dp

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(220)) + scaleIn(
            initialScale = 0.86f,
            animationSpec = spring(dampingRatio = 0.74f, stiffness = 420f)
        ),
        exit = fadeOut(animationSpec = tween(180)) + scaleOut(
            targetScale = 0.90f,
            animationSpec = spring(dampingRatio = 0.88f, stiffness = 500f)
        )
    ) {
        // 全屏半透明蒙层（保留背后磨砂与采样）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.22f))
                .pointerInput(Unit) {
                    detectTapGestures {
                        // 点击蒙层关闭
                        onDismissRequest()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 弹窗主卡片容器（阻止事件穿透）
            Box(
                modifier = modifier
                    .width(320.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { /* 拦截点击，防止点击弹窗关闭 */ }
                    }
                    // 1. 【底部投影层：尺寸+50px】
                    .shadow(
                        elevation = 32.dp,
                        shape = RoundedCornerShape(cornerRadius),
                        clip = false,
                        spotColor = Color(0x4D000000),
                        ambientColor = Color(0x33000000)
                    )
                    .drawIos26DropShadow(cornerRadius = cornerRadius)
                    // 2. 【玻璃层与中间层（Kyant Backdrop + #F2F2F2 颜色减淡）】
                    .then(
                        if (backdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(cornerRadius) },
                                effects = {
                                    vibrancy()
                                    blur(18f.dp.toPx())
                                    lens(
                                        refractionHeight = 8f.dp.toPx(),
                                        refractionAmount = 18f.dp.toPx(),
                                        chromaticAberration = true
                                    )
                                },
                                onDrawSurface = {
                                    // 中间层：#F2F2F2 减淡色半透明铺底
                                    drawRect(Color(0xFFF2F2F7).copy(alpha = 0.78f))
                                    // 玻璃表面微反射高光渐变
                                    drawRect(
                                        Brush.verticalGradient(
                                            0.0f to Color.White.copy(alpha = 0.65f),
                                            0.35f to Color.White.copy(alpha = 0.25f),
                                            1.0f to Color.White.copy(alpha = 0.45f)
                                        )
                                    )
                                }
                            )
                        } else {
                            Modifier
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color(0xFFF7F7FA).copy(alpha = 0.94f),
                                        1.0f to Color(0xFFEDEDF2).copy(alpha = 0.94f)
                                    ),
                                    RoundedCornerShape(cornerRadius)
                                )
                        }
                    )
                    // 细致高光边缘（各向异性棱边）
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = 0.85f),
                            0.45f to Color.White.copy(alpha = 0.25f),
                            1.0f to Color.White.copy(alpha = 0.60f)
                        ),
                        shape = RoundedCornerShape(cornerRadius)
                    )
                    .clip(RoundedCornerShape(cornerRadius))
                    .padding(horizontal = 22.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Title 标题（靠左，加粗黑体）
                    if (!title.isNullOrBlank()) {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111111),
                            letterSpacing = (-0.4).sp,
                            lineHeight = 22.sp
                        )
                    }

                    // Description 描述说明（靠左，灰色完成句子）
                    if (!description.isNullOrBlank()) {
                        if (!title.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = description,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF48484A),
                            lineHeight = 19.sp
                        )
                    }

                    // 【可选内容槽位：例如输入框卡片 / 状态卡片】
                    if (content != null) {
                        Spacer(modifier = Modifier.height(18.dp))
                        content()
                    }

                    // 按钮上间距
                    Spacer(modifier = Modifier.height(20.dp))

                    // 【底部药丸双按钮：Secondary & Primary】
                    if (!secondaryButtonText.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Secondary 按钮（左边浅灰色药丸胶囊）
                            Ios26CapsuleButton(
                                text = secondaryButtonText,
                                isPrimary = false,
                                modifier = Modifier.weight(1f),
                                onClick = { onSecondaryClick?.invoke() }
                            )

                            // Primary 按钮（右边高饱和经典 iOS 蓝药丸胶囊）
                            Ios26CapsuleButton(
                                text = primaryButtonText,
                                isPrimary = true,
                                backgroundColor = primaryButtonColor,
                                isLoading = isPrimaryLoading,
                                modifier = Modifier.weight(1f),
                                onClick = onPrimaryClick
                            )
                        }
                    } else {
                        // 单按钮模式
                        Ios26CapsuleButton(
                            text = primaryButtonText,
                            isPrimary = true,
                            backgroundColor = primaryButtonColor,
                            isLoading = isPrimaryLoading,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onPrimaryClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * 专用于断网场景的 iOS 26 液态玻璃重试小弹窗（清爽无多余中栏）。
 */
@Composable
fun Ios26NetworkOfflineDialog(
    isVisible: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isRetrying: Boolean = false,
    title: String = "网络连接已断开",
    description: String = "未能连接到互联网，部分内容无法加载。\n请检查网络设置后重试。"
) {
    Ios26LiquidGlassDialog(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        modifier = modifier,
        backdrop = backdrop,
        title = title,
        description = description,
        secondaryButtonText = "稍后",
        primaryButtonText = "重试加载",
        primaryButtonColor = Color(0xFF007AFF),
        isPrimaryLoading = isRetrying,
        onSecondaryClick = onDismiss,
        onPrimaryClick = onRetry,
        content = null // 断网场景无需中间多余信息栏，清爽干净！
    )
}

/**
 * iOS 26 原设计图输入框卡片（还原解析图中的 Value 输入栏）。
 *
 * 包含两个输入栏以及中间极细分割线，支持单行/双行输入。
 */
@Composable
fun Ios26DialogInputCard(
    value1: String,
    onValue1Change: (String) -> Unit,
    placeholder1: String = "Value",
    modifier: Modifier = Modifier,
    value2: String? = null,
    onValue2Change: ((String) -> Unit)? = null,
    placeholder2: String = "Value"
) {
    val containerShape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.055f), containerShape)
            .border(0.5.dp, Color.Black.copy(alpha = 0.04f), containerShape)
            .padding(horizontal = 16.dp)
    ) {
        // 第一行输入框
        Ios26InputFieldRow(
            value = value1,
            onValueChange = onValue1Change,
            placeholder = placeholder1
        )

        // 若存在第二行输入项，渲染分割线和第二行
        if (value2 != null && onValue2Change != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(Color.Black.copy(alpha = 0.08f))
            )

            Ios26InputFieldRow(
                value = value2,
                onValueChange = onValue2Change,
                placeholder = placeholder2
            )
        }
    }
}

/**
 * 单行原生 iOS 风格无下划线文本输入框
 */
@Composable
private fun Ios26InputFieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            fontSize = 15.sp,
            color = Color(0xFF1C1C1E),
            fontWeight = FontWeight.Normal
        ),
        cursorBrush = SolidColor(Color(0xFF007AFF)),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 15.sp,
                        color = Color(0xFF8E8E93),
                        fontWeight = FontWeight.Normal
                    )
                }
                innerTextField()
            }
        }
    )
}

/**
 * iOS 26 只读 Value 展示卡片（供信息展示、状态诊断等复用场景使用）。
 */
@Composable
fun Ios26ValueContainer(
    valueTopLabel: String,
    valueTopTrailing: (@Composable () -> Unit)? = null,
    valueBottomLabel: String? = null,
    valueBottomTrailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val containerShape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.055f), containerShape)
            .border(0.5.dp, Color.Black.copy(alpha = 0.04f), containerShape)
            .padding(horizontal = 16.dp)
    ) {
        // 第一行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = valueTopLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (valueTopTrailing != null) {
                valueTopTrailing()
            }
        }

        // 第二行（如果提供）
        if (valueBottomLabel != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(Color.Black.copy(alpha = 0.08f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = valueBottomLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF8E8E93),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (valueBottomTrailing != null) {
                    valueBottomTrailing()
                }
            }
        }
    }
}

/**
 * 药丸胶囊按钮（Secondary 浅灰底，Primary 鲜亮蓝/自定义高亮色）。
 */
@Composable
private fun Ios26CapsuleButton(
    text: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = if (isPrimary) Color(0xFF007AFF) else Color.Black.copy(alpha = 0.065f),
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 650f),
        label = "ios26ButtonScale"
    )

    val buttonShape = RoundedCornerShape(23.dp)

    Box(
        modifier = modifier
            .height(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(buttonShape)
            .background(backgroundColor)
            .then(
                if (isPrimary) {
                    Modifier.border(
                        0.5.dp,
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.40f),
                            1f to Color.Transparent
                        ),
                        buttonShape
                    )
                } else {
                    Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.04f), buttonShape)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (!isLoading) onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(19.dp),
                color = Color.White,
                strokeWidth = 2.2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPrimary) Color.White else Color(0xFF1C1C1E),
                letterSpacing = (-0.2).sp
            )
        }
    }
}

/**
 * 绘制符合解析图标注的【底部投影：尺寸+50px】的大范围软弥散环境光阴影。
 */
private fun Modifier.drawIos26DropShadow(cornerRadius: Dp): Modifier = drawBehind {
    val spreadPx = 25f.dp.toPx() // +50px spread
    val blurRadiusPx = 36f.dp.toPx()

    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                color = android.graphics.Color.argb(55, 0, 0, 0)
                isAntiAlias = true
                maskFilter = android.graphics.BlurMaskFilter(
                    blurRadiusPx,
                    android.graphics.BlurMaskFilter.Blur.NORMAL
                )
            }
        }

        // 外扩 50px 绘制阴影层
        val left = -spreadPx / 2
        val top = 10f.dp.toPx() - spreadPx / 2
        val right = size.width + spreadPx / 2
        val bottom = size.height + 10f.dp.toPx() + spreadPx / 2
        val cornerPx = cornerRadius.toPx()

        canvas.drawRoundRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            radiusX = cornerPx + 8.dp.toPx(),
            radiusY = cornerPx + 8.dp.toPx(),
            paint = paint
        )
    }
}
