package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

/**
 * 液态玻璃（Liquid Glass）统一参数中心。
 *
 * 本项目的玻璃质感由 [com.kyant.backdrop] 驱动，包含“模糊 + 鲜艳度 + 透镜折射”三段效果组合，
 * 以及表面半透明白色铺底。这些参数在底栏、搜索栏、迷你播放条、我的页卡片等十余处重复出现，
 * 因此统一收敛到这里，避免各处以魔法数字各写一遍导致质感漂移。
 */
object GlassDefaults {
    /** 基础模糊半径：让底层的色块变成磨砂质感 */
    val BlurRadius: Dp = 2.dp

    /** 透镜折射高度（越大边缘的玻璃厚度感越强） */
    val LensRefractionHeight: Dp = 16.dp

    /** 透镜折射量（越大内容扭曲越明显） */
    val LensRefractionAmount: Dp = 32.dp

    /** 表面白色铺底透明度：主导航条/卡片的主用值 */
    const val SurfaceAlpha: Float = 0.5f

    /** 搜索栏一类的浅色面板需要更亮一点的铺底 */
    const val SurfaceAlphaBright: Float = 0.56f

    /** 玻璃边缘高光描边透明度 */
    const val BorderAlpha: Float = 0.62f
}

/**
 * 为任意 Modifier 叠加液态玻璃背景（模糊 + 鲜艳度 + 透镜折射 + 表面铺底）。
 *
 * 用法：
 * ```
 * Modifier
 *     .size(56.dp)
 *     .liquidGlass(backdrop = backdrop, cornerRadius = 28.dp)
 * ```
 *
 * @param backdrop 底层采样源，通常由 `rememberLayerBackdrop` 提供
 * @param cornerRadius G² 连续圆角半径（使用 [RoundedRectangle] 实现 squircle）
 * @param surfaceAlpha 表面白色铺底透明度；传 0f 表示只要折射不要铺底
 * @param refractionHeight 透镜折射高度
 * @param refractionAmount 透镜折射量
 * @param blurRadius 模糊半径
 * @param layerBlock 采样层的额外绘制变换（例如底栏按下放大时，需要让被采样的内容同步缩放）。
 *                   与外部 `graphicsLayer` 作用对象不同：后者作用于组件本身，前者作用于 backdrop 采样层。
 */
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    cornerRadius: Dp,
    surfaceAlpha: Float = GlassDefaults.SurfaceAlpha,
    refractionHeight: Dp = GlassDefaults.LensRefractionHeight,
    refractionAmount: Dp = GlassDefaults.LensRefractionAmount,
    blurRadius: Dp = GlassDefaults.BlurRadius,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { RoundedRectangle(cornerRadius) },
    effects = {
        vibrancy()
        blur(blurRadius.toPx())
        lens(refractionHeight.toPx(), refractionAmount.toPx())
    },
    layerBlock = layerBlock ?: {},
    onDrawSurface = {
        if (surfaceAlpha > 0f) {
            drawRect(Color.White.copy(alpha = surfaceAlpha))
        }
    }
)

/**
 * 与 [liquidGlass] 配套的高光描边：模拟玻璃边缘的棱边反光。
 */
fun Modifier.glassBorder(
    cornerRadius: Dp,
    width: Dp = 1.dp,
    color: Color = Color.White.copy(alpha = GlassDefaults.BorderAlpha)
): Modifier = this.border(width, color, RoundedCornerShape(cornerRadius))

/**
 * 液态玻璃卡片容器：玻璃背景 + 可选高光描边 + 内容槽位。
 *
 * 这是「我的」页各卡片与播放列表 Tab 的统一外壳，替代原先散落在页面内的 drawBackdrop 重复写法。
 *
 * @param cornerRadius 圆角半径
 * @param surfaceAlpha 表面铺底透明度
 * @param borderWidth 描边宽度；传 0.dp 可关闭描边
 * @param content 卡片内容
 */
@Composable
fun GlassSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    surfaceAlpha: Float = GlassDefaults.SurfaceAlpha,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .liquidGlass(
                backdrop = backdrop,
                cornerRadius = cornerRadius,
                surfaceAlpha = surfaceAlpha
            )
            .then(
                if (borderWidth > 0.dp) Modifier.glassBorder(cornerRadius, borderWidth) else Modifier
            ),
        content = content
    )
}
