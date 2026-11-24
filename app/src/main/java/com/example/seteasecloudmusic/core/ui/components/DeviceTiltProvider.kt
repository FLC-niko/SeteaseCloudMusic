package com.example.seteasecloudmusic.core.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal const val DEFAULT_TILT_ANGLE_RAD: Float = (-PI / 2.0).toFloat()
internal const val TILT_QUANTIZATION_STEP_RAD: Float = (PI / 60.0).toFloat() // 3°
internal const val TILT_LOW_PASS_ALPHA: Float = 0.15f
internal const val TILT_FLAT_THRESHOLD_SQ: Float = 0.01f

/**
 * 将屏幕平面重力分量 (gravityX, gravityY) 转换为 3° 步长量化的弧度角。
 *
 * 当设备近乎水平放置时（投影模长平方 <= 0.01f），返回固定的默认上方倾角 (-PI / 2)，
 * 避免平放桌面时方向角剧烈跳变导致玻璃边缘高光闪烁。
 */
internal fun computeQuantizedGravityAngle(gravityX: Float, gravityY: Float): Float {
    val magSq = gravityX * gravityX + gravityY * gravityY
    if (magSq <= TILT_FLAT_THRESHOLD_SQ) {
        return DEFAULT_TILT_ANGLE_RAD
    }
    val rawAngle = atan2(gravityY, gravityX)
    val stepIndex = (rawAngle / TILT_QUANTIZATION_STEP_RAD).roundToInt()
    return stepIndex * TILT_QUANTIZATION_STEP_RAD
}

/**
 * 将设备重力方向角与角度偏移、Z 分量组合成单位 3D 光源方向向量。
 */
internal fun rotateLightDirection(
    gravityAngleRad: Float,
    extraDegrees: Float,
    z: Float
): Triple<Float, Float, Float> {
    val rad = gravityAngleRad + Math.toRadians(extraDegrees.toDouble()).toFloat()
    val dx = cos(rad)
    val dy = sin(rad)
    val len = sqrt(dx * dx + dy * dy + z * z)
    return if (len > 1e-6f) {
        Triple(dx / len, dy / len, z / len)
    } else {
        Triple(0f, -1f, 0f)
    }
}

/**
 * 全局共享的设备倾斜倾角状态，仅在 Draw 阶段读取以实现零重组重力扫光。
 */
@Stable
class DeviceTiltState {
    var quantizedAngleRad: Float by mutableFloatStateOf(DEFAULT_TILT_ANGLE_RAD)
        internal set
}

val LocalDeviceTiltState = staticCompositionLocalOf { DeviceTiltState() }

/**
 * 在应用根部提供生命周期感知的设备倾斜监听（`ON_RESUME` 注册、`ON_PAUSE` 注销），
 * 通过低通滤波 + 3° 量化向全项目所有液态玻璃组件提供稳定的重力扫光方向。
 */
@Composable
fun ProvideDeviceTilt(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val tiltState = remember { DeviceTiltState() }

    DisposableEffect(context, lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensorInfo = selectTiltSensor(sensorManager)

        if (sensorManager == null || sensorInfo == null) {
            tiltState.quantizedAngleRad = DEFAULT_TILT_ANGLE_RAD
            return@DisposableEffect onDispose {}
        }

        val (sensor, isRotationVector) = sensorInfo
        val rotMatrix = FloatArray(9)
        var initialized = false
        var smoothedGx = 0f
        var smoothedGy = 0f
        var registered = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val values = event.values
                if (values.size < 2) return

                val rawGx: Float
                val rawGy: Float
                if (isRotationVector) {
                    if (values.size < 3) return
                    SensorManager.getRotationMatrixFromVector(rotMatrix, values)
                    rawGx = -rotMatrix[6]
                    rawGy = -rotMatrix[7]
                } else {
                    rawGx = -values[0] / SensorManager.GRAVITY_EARTH
                    rawGy = -values[1] / SensorManager.GRAVITY_EARTH
                }

                if (!initialized) {
                    smoothedGx = rawGx
                    smoothedGy = rawGy
                    initialized = true
                } else {
                    smoothedGx += (rawGx - smoothedGx) * TILT_LOW_PASS_ALPHA
                    smoothedGy += (rawGy - smoothedGy) * TILT_LOW_PASS_ALPHA
                }

                val quantized = computeQuantizedGravityAngle(smoothedGx, smoothedGy)
                if (kotlin.math.abs(quantized - tiltState.quantizedAngleRad) > 1e-6f) {
                    tiltState.quantizedAngleRad = quantized
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun registerSensor() {
            if (!registered) {
                registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            }
        }

        fun unregisterSensor() {
            if (registered) {
                sensorManager.unregisterListener(listener)
                registered = false
            }
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> registerSensor()
                Lifecycle.Event.ON_PAUSE -> unregisterSensor()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            registerSensor()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            unregisterSensor()
        }
    }

    CompositionLocalProvider(LocalDeviceTiltState provides tiltState) {
        content()
    }
}

private fun selectTiltSensor(sensorManager: SensorManager?): Pair<Sensor, Boolean>? {
    if (sensorManager == null) return null
    sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)?.let { return it to true }
    sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let { return it to true }
    sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)?.let { return it to false }
    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { return it to false }
    return null
}
