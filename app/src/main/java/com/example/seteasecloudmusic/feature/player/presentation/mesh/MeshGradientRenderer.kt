package com.example.seteasecloudmusic.feature.player.presentation.mesh

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 1:1 复刻 AMLL MeshGradientRenderer：
 * - Android 13+ (API 33+) 使用 AGSL RuntimeShader + 双三次 Hermite 曲面网格 (BHPMesh) 渲染流体封面取色背景
 * - Android 10~12 (API 29~32) 自动降级为同款 32x32 预处理纹理 + 旋转镜像 BitmapShader 混合渲染
 */
internal class MeshAlbum internal constructor(
    internal val bitmap: Bitmap,
    internal val positions: FloatArray,
    internal val textureCoordinates: FloatArray,
    internal val indices: ShortArray,
    internal val runtimeShader: Any?,
)

@MainThread
internal class MeshGradientRenderer {

    private class MeshState(val album: MeshAlbum, var alpha: Float) {
        var targetAlpha = if (alpha == 1f) 0f else 1f
        val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            blendMode = BlendMode.SRC
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && album.runtimeShader is RuntimeShader) {
                shader = album.runtimeShader
            }
        }
        val fallbackBitmapShader = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            BitmapShader(album.bitmap, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR)
        } else null
        val fallbackMatrix = Matrix()
        val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val scaledPositions = FloatArray(album.positions.size)
        var viewportSize = IntSize.Zero

        fun resize(size: IntSize) {
            if (viewportSize == size) return
            viewportSize = size
            val width = size.width.toFloat()
            val height = size.height.toFloat()
            val aspect = width / height.coerceAtLeast(1f)
            val scaleX = if (aspect > 1f) 1f else 1f / aspect
            val scaleY = if (aspect > 1f) aspect else 1f
            for (i in album.positions.indices step 2) {
                scaledPositions[i] = (album.positions[i] * scaleX + 1f) * width * 0.5f
                scaledPositions[i + 1] = (1f - album.positions[i + 1] * scaleY) * height * 0.5f
            }
        }
    }

    private val meshStates = mutableListOf<MeshState>()
    private var currentAlbum: MeshAlbum? = null
    private var accumulatedPlayingNanos = 0L
    private var lastFrameNanos: Long? = null
    private var revision = 0L
    private var recordedRevision = -1L
    private var recordedLayer: GraphicsLayer? = null
    private var recordedSize = IntSize.Zero
    private val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var vignetteSize = IntSize.Zero

    var flowSpeed: Float = 0.24f
        set(value) {
            if (field == value) return
            field = value
            revision++
        }

    var isPlaying: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            resetFrameClock()
        }

    val hasAlbum: Boolean
        get() = meshStates.isNotEmpty()

    val needsAnimation: Boolean
        get() = hasAlbum && (isPlaying || meshStates.any { it.targetAlpha != 0f })

    fun setAlbum(album: MeshAlbum) {
        if (currentAlbum === album) return
        val firstAlbum = meshStates.isEmpty()
        val returningState = meshStates.firstOrNull { it.album === album }
        meshStates.forEach { it.targetAlpha = -1f }
        if (returningState != null) {
            returningState.targetAlpha = 1f
        } else {
            meshStates.add(0, MeshState(album, if (firstAlbum) 1f else 0f))
        }
        currentAlbum = album
        revision++
    }

    fun advanceFrame(frameNanos: Long) {
        val previousFrame = lastFrameNanos
        lastFrameNanos = frameNanos
        val elapsedNanos = if (previousFrame == null) 0L else (frameNanos - previousFrame).coerceAtLeast(0L)
        if (!needsAnimation) return
        if (isPlaying) accumulatedPlayingNanos += elapsedNanos
        val elapsedSeconds = elapsedNanos / 1e9f

        val fadeDelta = elapsedSeconds.coerceIn(0f, 0.1f) * 1.4f
        val iterator = meshStates.iterator()
        while (iterator.hasNext()) {
            val state = iterator.next()
            state.alpha += fadeDelta * state.targetAlpha
            if (state.targetAlpha > 0f && state.alpha >= 1f) {
                state.alpha = 1f
                state.targetAlpha = 0f
            }
            if (state.targetAlpha < 0f && state.alpha <= 0f) {
                iterator.remove()
            }
        }
        revision++
    }

    fun resetFrameClock() {
        lastFrameNanos = null
    }

    fun draw(scope: DrawScope, layer: GraphicsLayer, renderScale: Float = 0.72f) = with(scope) {
        if (size.width <= 0f || size.height <= 0f) return@with
        val scale = min(
            renderScale.coerceIn(0.2f, 1f),
            min(
                720f / min(size.width, size.height),
                1440f / max(size.width, size.height)
            )
        )
        val bufferSize = IntSize(
            (size.width * scale).toInt().coerceAtLeast(1),
            (size.height * scale).toInt().coerceAtLeast(1)
        )
        if (recordedLayer !== layer || recordedSize != bufferSize || recordedRevision != revision) {
            layer.clip = true
            layer.compositingStrategy = CompositingStrategy.Offscreen
            layer.record(bufferSize) {
                drawRect(Color(0xFF14151A))
                drawIntoCanvas { composeCanvas ->
                    drawMeshes(composeCanvas.nativeCanvas, bufferSize)
                }
            }
            recordedLayer = layer
            recordedSize = bufferSize
            recordedRevision = revision
        }
        withTransform({
            scale(size.width / bufferSize.width, size.height / bufferSize.height, Offset.Zero)
        }) {
            drawLayer(layer)
        }
    }

    private fun drawMeshes(canvas: Canvas, size: IntSize) {
        val time = accumulatedPlayingNanos / 1e9f * flowSpeed
        for (index in meshStates.lastIndex downTo 0) {
            val state = meshStates[index]
            val alpha = (1f - cos(state.alpha.coerceIn(0f, 1f) * Math.PI.toFloat())) * 0.5f
            if (alpha <= 0f) continue

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && state.album.runtimeShader is RuntimeShader) {
                drawMeshApi33(canvas, size, state, state.album.runtimeShader, time, alpha)
            } else {
                drawMeshFallback(canvas, size, state, time, alpha)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun drawMeshApi33(
        canvas: Canvas,
        size: IntSize,
        state: MeshState,
        shader: RuntimeShader,
        time: Float,
        alpha: Float
    ) {
        state.resize(size)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("audioResponse", 1f, 1f, 1f)
        state.compositePaint.alpha = (alpha * 255f).roundToInt().coerceIn(0, 255)
        val saveCount = canvas.saveLayer(
            0f, 0f, size.width.toFloat(), size.height.toFloat(), state.compositePaint
        )
        canvas.drawVertices(
            Canvas.VertexMode.TRIANGLES,
            state.scaledPositions.size,
            state.scaledPositions,
            0,
            state.album.textureCoordinates,
            0,
            null,
            0,
            state.album.indices,
            0,
            state.album.indices.size,
            state.meshPaint
        )
        canvas.restoreToCount(saveCount)
    }

    private fun drawMeshFallback(
        canvas: Canvas,
        size: IntSize,
        state: MeshState,
        time: Float,
        alpha: Float
    ) {
        val bmpShader = state.fallbackBitmapShader ?: return
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        val maxDim = max(w, h) * 1.45f
        val texW = state.album.bitmap.width.toFloat().coerceAtLeast(1f)
        val texH = state.album.bitmap.height.coerceAtLeast(1).toFloat()

        state.fallbackMatrix.reset()
        state.fallbackMatrix.postTranslate(-texW * 0.5f, -texH * 0.5f)
        state.fallbackMatrix.postRotate((time * 114.59f) % 360f)
        state.fallbackMatrix.postScale(maxDim / texW, maxDim / texH)
        state.fallbackMatrix.postTranslate(w * 0.5f, h * 0.45f)
        bmpShader.setLocalMatrix(state.fallbackMatrix)

        state.meshPaint.shader = bmpShader
        state.compositePaint.alpha = (alpha * 255f).roundToInt().coerceIn(0, 255)
        val saveCount = canvas.saveLayer(0f, 0f, w, h, state.compositePaint)
        canvas.drawRect(0f, 0f, w, h, state.meshPaint)

        if (vignetteSize != size) {
            vignetteSize = size
            vignettePaint.shader = RadialGradient(
                w * 0.5f,
                h * 0.5f,
                max(w, h) * 0.78f,
                intArrayOf(0x00000000, 0x59000000),
                floatArrayOf(0.32f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, vignettePaint)
        canvas.restoreToCount(saveCount)
    }

    companion object {
        private val random = java.util.Random()

        @WorkerThread
        fun prepareAlbum(bitmap: Bitmap, subdivision: Int = 26, preset: ControlPointPreset? = null): MeshAlbum {
            val source = if (bitmap.isRecycled) {
                Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(0xFF2B2D38.toInt())
                }
            } else {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
                    ?: Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(0xFF2B2D38.toInt())
                    }
            }
            val processed = try {
                AlbumTextureProcessor.process(source)
            } finally {
                if (source !== bitmap && !source.isRecycled) source.recycle()
            }

            val selectedPreset = preset ?: if (random.nextFloat() < 0.8f) {
                CONTROL_POINT_PRESETS[random.nextInt(CONTROL_POINT_PRESETS.size)]
            } else {
                generateControlPoints(random = kotlin.random.Random(random.nextLong()))
            }

            val mesh = BHPMesh(selectedPreset.width, selectedPreset.height).apply {
                resetSubdivision(subdivision)
                configureFromPreset(selectedPreset)
            }
            val vertices = checkNotNull(mesh.buffer)
            val vertexCount = vertices.capacity() / 7
            require(vertexCount <= 65536) { "Mesh exceeds Canvas index range" }

            val positions = FloatArray(vertexCount * 2)
            val textureCoordinates = FloatArray(vertexCount * 2)
            for (i in 0 until vertexCount) {
                positions[i * 2] = vertices[i * 7]
                positions[i * 2 + 1] = vertices[i * 7 + 1]
                textureCoordinates[i * 2] = vertices[i * 7 + 5]
                textureCoordinates[i * 2 + 1] = vertices[i * 7 + 6]
            }
            val indexBuffer = mesh.generateIndexBuffer()
            val indices = ShortArray(mesh.indices) { indexBuffer[it].toShort() }

            val runtimeShader: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                createRuntimeShaderApi33(processed)
            } else {
                null
            }
            return MeshAlbum(processed, positions, textureCoordinates, indices, runtimeShader)
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun createRuntimeShaderApi33(processed: Bitmap): RuntimeShader {
            return RuntimeShader(ShaderSource.MESH_SHADER).apply {
                setInputShader(
                    "albumTexture",
                    BitmapShader(processed, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR).apply {
                        setFilterMode(BitmapShader.FILTER_MODE_LINEAR)
                    }
                )
                setFloatUniform("textureSize", processed.width.toFloat(), processed.height.toFloat())
            }
        }
    }
}

