package com.example.seteasecloudmusic.feature.player.presentation

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.seteasecloudmusic.feature.player.presentation.mesh.MeshAlbum
import com.example.seteasecloudmusic.feature.player.presentation.mesh.MeshGradientRenderer
import com.example.seteasecloudmusic.feature.player.presentation.mesh.runMeshFrames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 封面动态取色流体网格背景：
 * 1. 对封面进行 32x32 采样并经过色彩滤镜矩阵（对比度、饱和度、亮度及 4x 模糊）处理
 * 2. 使用双三次 Hermite 曲面网格扭曲，结合 AGSL RuntimeShader 流体旋转、防色带抖动噪点与边缘暗角绘制
 * 3. 切歌时通过余弦曲线在前后纹理间跨帧叠化过渡
 */
@Composable
fun AppleMusicBackground(
    coverUrl: String?,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val renderer = remember { MeshGradientRenderer() }
    val meshLayer = rememberGraphicsLayer()
    val frameVersion = remember { mutableIntStateOf(0) }

    val placeholderAlbum by produceState<MeshAlbum?>(initialValue = null, key1 = renderer) {
        value = withContext(Dispatchers.Default) {
            val bmp = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
                eraseColor(0xFF343844.toInt())
            }
            try {
                // 无封面占位背景的网格细分密度
                MeshGradientRenderer.prepareAlbum(bmp, subdivision = 24)
            } finally {
                bmp.recycle()
            }
        }
    }

    val loadedAlbum by produceState<MeshAlbum?>(initialValue = null, key1 = coverUrl) {
        if (coverUrl.isNullOrBlank()) {
            value = null
            return@produceState
        }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(coverUrl)
                    // 封面取色降采样尺寸（px，影响取色精度与解码开销）
                    .size(256)
                    .memoryCacheKey("$coverUrl#mesh_256")
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable && drawable.bitmap != null) {
                        drawable.bitmap
                    } else {
                        val w = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(256)
                        val h = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(256)
                        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
                            val canvas = android.graphics.Canvas(bmp)
                            drawable.setBounds(0, 0, w, h)
                            drawable.draw(canvas)
                        }
                    }
                } else null
            }.getOrNull()
        }
        if (bitmap != null) {
            value = withContext(Dispatchers.Default) {
                // 网格细分密度（越大曲面越精细，顶点计算开销越高）
                MeshGradientRenderer.prepareAlbum(bitmap, subdivision = 28)
            }
        } else if (coverUrl.isBlank()) {
            value = null
        }
    }

    val activeAlbum = loadedAlbum ?: placeholderAlbum

    LaunchedEffect(renderer, isPlaying) {
        renderer.isPlaying = isPlaying
        frameVersion.intValue++
    }

    LaunchedEffect(renderer, activeAlbum) {
        activeAlbum?.let {
            renderer.setAlbum(it)
            frameVersion.intValue++
        }
    }

    LaunchedEffect(renderer, lifecycleOwner, activeAlbum, isPlaying) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            renderer.resetFrameClock()
            runMeshFrames(
                // 流体动画帧间隔（ns，约 30fps，越小越流畅但 GPU 负载越高）
                intervalNanos = 33_333_333L,
                needsAnimation = { renderer.needsAnimation }
            ) { frameNanos ->
                renderer.advanceFrame(frameNanos)
                frameVersion.intValue++
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    frameVersion.intValue
                    if (renderer.hasAlbum) {
                        // 离屏渲染缩放比例（降低像素填充率并柔化网格边缘）
                        renderer.draw(this, meshLayer, renderScale = 0.72f)
                    } else {
                        drawRect(Color(0xFF1C2029))
                    }
                }
        )

        // 纵向压暗渐变遮罩，保障白色歌词与底部控制栏的可读性
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Black.copy(alpha = 0.14f),
                        0.45f to Color.Black.copy(alpha = 0.10f),
                        0.78f to Color.Black.copy(alpha = 0.24f),
                        1.00f to Color.Black.copy(alpha = 0.42f)
                    )
                )
        )
    }
}
