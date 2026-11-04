package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.core.model.Track
import com.kyant.backdrop.backdrops.LayerBackdrop

internal val LocalPlayerBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * 沉浸式模糊背景的降采样策略。
 *
 * 全屏 50dp 高斯模糊是播放页最大的单项 GPU 开销（1080x2460 全分辨率采样）。
 * 这里把背景图按 [BLUR_DOWNSCALE] 分之一直径渲染后再放大铺满：
 * 模糊本身会抹掉高频细节，因此观感几乎无损，而模糊的采样面积降到约 1/[BLUR_DOWNSCALE]²。
 * [BLUR_VISUAL_RADIUS] 是放大到全屏后希望呈现的模糊强度。
 */
private const val BLUR_DOWNSCALE = 6f
private const val BLUR_SOURCE_SIZE = 300

/**
 * AMLL 模式背景：`filter: blur(60px) saturate(180%)` 且封面放大到 140%。
 * 这里保持降采样模糊（见 [BLUR_DOWNSCALE]）以控制 GPU 开销，视觉半径与 WebView 完全一致。
 */
private val BLUR_VISUAL_RADIUS = 60.dp
private const val BLUR_BACKGROUND_SCALE = 1.4f

/** AMLL 背景遮罩：`background-color: rgba(0, 0, 0, 0.35)`。 */
private const val BLUR_SCRIM_ALPHA = 0.35f

/** 饱和度提升：`saturate(180%)`。 */
private const val BLUR_SATURATION = 1.8f

/** Shared album light and scrim keep compact and expanded playback on one visual surface. */
@Composable
internal fun PlayerBackdrop(track: Track?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color(0xFF141416))) {
        // ── Layer 0: 沉浸式背景（AMLL 规格的专辑封面模糊流光底色）──
        // 模糊背景按 1/BLUR_DOWNSCALE 分辨率渲染、再放大铺满屏幕：
        // 高斯模糊本身会抹掉高频细节，所以缩小渲染几乎不影响观感，
        // 但模糊的采样面积降到约 1/BLUR_DOWNSCALE²，播放页最主要的 GPU 开销随之消失。
        val blurArtworkUrl = remember(track?.coverUrl) {
            track?.coverUrl.toCoverThumbnailUrl(BLUR_SOURCE_SIZE)
        }
        if (!blurArtworkUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = blurArtworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    // saturate(180%)：饱和度提升必须在模糊之前语义上成立，
                    // 这里作为颜色滤镜作用在同一层上，等价于 CSS 的 filter 链
                    colorFilter = ColorFilter.colorMatrix(
                        ColorMatrix().apply { setToSaturation(BLUR_SATURATION) }
                    ),
                    modifier = Modifier
                        .fillMaxSize(BLUR_BACKGROUND_SCALE / BLUR_DOWNSCALE)
                        .graphicsLayer {
                            scaleX = BLUR_DOWNSCALE
                            scaleY = BLUR_DOWNSCALE
                        }
                        .blur(radius = BLUR_VISUAL_RADIUS / BLUR_DOWNSCALE)
                )
            }
        }

        // ── Layer 1: 暗色遮罩（AMLL 均匀 0.35 黑 + 底部加强保证控制栏可读）──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (track != null) {
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = BLUR_SCRIM_ALPHA + 0.10f),
                            0.45f to Color.Black.copy(alpha = BLUR_SCRIM_ALPHA),
                            0.75f to Color.Black.copy(alpha = BLUR_SCRIM_ALPHA + 0.22f),
                            1.0f to Color.Black.copy(alpha = BLUR_SCRIM_ALPHA + 0.45f)
                        )
                    } else {
                        // 未在播放：Apple Music 标志性深空自然环境渐变，杜绝纯黑死板
                        Brush.verticalGradient(
                            0.0f to Color(0xFF26262E),
                            0.45f to Color(0xFF1B1B20),
                            0.85f to Color(0xFF131316),
                            1.0f to Color(0xFF0F0F11)
                        )
                    }
                )
        )

    }
}
