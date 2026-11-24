package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.foundation.gestures.Orientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class LiquidGlassPhysicsTest {

    private companion object {
        const val EPSILON = 1e-4f
    }

    @Test
    fun computeQuantizedGravityAngle_returnsDefaultWhenDeviceIsFlat() {
        assertEquals(DEFAULT_TILT_ANGLE_RAD, computeQuantizedGravityAngle(0f, 0f), EPSILON)
        assertEquals(DEFAULT_TILT_ANGLE_RAD, computeQuantizedGravityAngle(0.05f, -0.05f), EPSILON)
        assertEquals(DEFAULT_TILT_ANGLE_RAD, computeQuantizedGravityAngle(-0.08f, 0.05f), EPSILON)
    }

    @Test
    fun computeQuantizedGravityAngle_quantizesToThreeDegreeStepsAcrossQuadrants() {
        // 0° -> 0 rad
        assertEquals(0f, computeQuantizedGravityAngle(1f, 0f), EPSILON)

        // +90° (Quadrant I/II boundary)
        assertEquals((PI / 2.0).toFloat(), computeQuantizedGravityAngle(0f, 1f), EPSILON)

        // +180° (Quadrant II/III boundary)
        assertEquals(PI.toFloat(), computeQuantizedGravityAngle(-1f, 0f), EPSILON)

        // -90° (Quadrant III/IV boundary)
        assertEquals((-PI / 2.0).toFloat(), computeQuantizedGravityAngle(0f, -1f), EPSILON)

        // Test 3° rounding across every quadrant (2.1° -> 3°, 136.0° -> 135°, -134.0° -> -135°, -46.0° -> -45°)
        val testAnglesDeg = listOf(2.1, 1.1, 44.0, 136.0, -134.0, -46.0)
        for (deg in testAnglesDeg) {
            val rad = Math.toRadians(deg)
            val quantized = computeQuantizedGravityAngle(cos(rad).toFloat(), sin(rad).toFloat())
            val expectedStep = (deg / 3.0).roundToInt() * TILT_QUANTIZATION_STEP_RAD
            assertEquals("Mismatch for $deg°", expectedStep, quantized, EPSILON)
        }
    }

    @Test
    fun rotateLightDirection_returnsUnitVectorAndAppliesExtraDegrees() {
        val dirZero = rotateLightDirection(
            gravityAngleRad = 0f,
            extraDegrees = -45f,
            z = 0f
        )
        val expectedComponent = (sqrt(2.0) / 2.0).toFloat()
        assertEquals(expectedComponent, dirZero.first, EPSILON)
        assertEquals(-expectedComponent, dirZero.second, EPSILON)
        assertEquals(0f, dirZero.third, EPSILON)

        val dirTilted = rotateLightDirection(
            gravityAngleRad = (-PI / 2.0).toFloat(),
            extraDegrees = 90f,
            z = -0.05f
        )
        val norm = sqrt(
            dirTilted.first * dirTilted.first +
                dirTilted.second * dirTilted.second +
                dirTilted.third * dirTilted.third
        )
        assertEquals(1f, norm, EPSILON)
        assertTrue(dirTilted.first > 0.99f)
        assertEquals(0f, dirTilted.second, EPSILON)
        assertTrue(dirTilted.third < 0f)
    }

    @Test
    fun computeJellyScale_atRestReturnsIdentity() {
        assertEquals(1f, computeJellyScale(0f, 0f, 1.35f, Orientation.Horizontal).first, EPSILON)
        assertEquals(1f, computeJellyScale(0f, 0f, 1.35f, Orientation.Horizontal).second, EPSILON)
        assertEquals(1f, computeJellyScale(0f, 0f, 1.35f, Orientation.Vertical).first, EPSILON)
        assertEquals(1f, computeJellyScale(0f, 0f, 1.35f, Orientation.Vertical).second, EPSILON)
        assertEquals(1f, computeJellyScale(-0.5f, 0f, 1.35f, Orientation.Horizontal).first, EPSILON)
    }

    @Test
    fun computeJellyScale_staticPressScalesUniformlyToMaxPressScale() {
        val (hx, hy) = computeJellyScale(1f, 0f, 1.35f, Orientation.Horizontal)
        assertEquals(1.35f, hx, EPSILON)
        assertEquals(1.35f, hy, EPSILON)

        val (vx, vy) = computeJellyScale(1f, 0f, 1.22f, Orientation.Vertical)
        assertEquals(1.22f, vx, EPSILON)
        assertEquals(1.22f, vy, EPSILON)

        val (clampedX, clampedY) = computeJellyScale(2f, 0f, 1.35f, Orientation.Horizontal)
        assertEquals(1.35f, clampedX, EPSILON)
        assertEquals(1.35f, clampedY, EPSILON)
    }

    @Test
    fun computeJellyScale_stretchesPrimaryAxisAndSquishesCrossAxisWithinClamps() {
        val (hx, hy) = computeJellyScale(1f, 2f, 1.35f, Orientation.Horizontal)
        assertEquals(1.35f / 0.85f, hx, EPSILON)
        assertEquals(1.35f * 0.95f, hy, EPSILON)
        assertTrue(hx > hy)

        val (vx, vy) = computeJellyScale(1f, -2f, 1.35f, Orientation.Vertical)
        assertEquals(1.35f * 0.95f, vx, EPSILON)
        assertEquals(1.35f / 0.85f, vy, EPSILON)
        assertTrue(vy > vx)

        val (extremeX, extremeY) = computeJellyScale(1f, 100f, 1.35f, Orientation.Horizontal)
        assertEquals(1.35f / 0.80f, extremeX, EPSILON)
        assertEquals(1.35f * 0.80f, extremeY, EPSILON)

        val (negExtremeX, negExtremeY) = computeJellyScale(1f, -100f, 1.35f, Orientation.Vertical)
        assertEquals(1.35f * 0.80f, negExtremeX, EPSILON)
        assertEquals(1.35f / 0.80f, negExtremeY, EPSILON)
    }

    @Test
    fun computeSegmentPillBounds_tracksHorizontalAndVerticalSlots() {
        // Horizontal 3-tab bar: 300px wide, 60px high, 4px padding => slotW = 100px, thumbW = 92px, thumbH = 52px
        val h0 = computeSegmentPillBounds(0f, 3, 300f, 60f, 4f, Orientation.Horizontal)
        assertEquals(4f, h0.left, EPSILON)
        assertEquals(4f, h0.top, EPSILON)
        assertEquals(96f, h0.right, EPSILON)
        assertEquals(56f, h0.bottom, EPSILON)

        val hMid = computeSegmentPillBounds(0.5f, 3, 300f, 60f, 4f, Orientation.Horizontal)
        assertEquals(54f, hMid.left, EPSILON)
        assertEquals(146f, hMid.right, EPSILON)

        val hEnd = computeSegmentPillBounds(2f, 3, 300f, 60f, 4f, Orientation.Horizontal)
        assertEquals(204f, hEnd.left, EPSILON)
        assertEquals(296f, hEnd.right, EPSILON)

        // Vertical 4-slot rail: 60px wide, 240px high, 4px padding => slotH = 60px, thumbW = 52px, thumbH = 52px
        val v0 = computeSegmentPillBounds(0f, 4, 60f, 240f, 4f, Orientation.Vertical)
        assertEquals(4f, v0.left, EPSILON)
        assertEquals(4f, v0.top, EPSILON)
        assertEquals(56f, v0.right, EPSILON)
        assertEquals(56f, v0.bottom, EPSILON)

        val vMid = computeSegmentPillBounds(1.5f, 4, 60f, 240f, 4f, Orientation.Vertical)
        assertEquals(94f, vMid.top, EPSILON)
        assertEquals(146f, vMid.bottom, EPSILON)
    }

    @Test
    fun computeSegmentPillBounds_guardsInvalidItemCountAndZeroContainer() {
        val zeroItems = computeSegmentPillBounds(0f, 0, 300f, 60f, 4f, Orientation.Horizontal)
        assertEquals(SegmentPillBounds(0f, 0f, 0f, 0f), zeroItems)

        val zeroSize = computeSegmentPillBounds(1f, 3, 0f, 60f, 4f, Orientation.Horizontal)
        assertEquals(SegmentPillBounds(0f, 0f, 0f, 0f), zeroSize)
    }

    @Test
    fun clampLensRefractionHeightPx_capsCompactBarsBelowHalfHeightAndPreservesLargerOnes() {
        // 44dp compact bar @3x => minDimension=132px, halfHeight=66px, desired 24dp=72px
        val compact = clampLensRefractionHeightPx(desiredPx = 72f, minDimensionPx = 132f)
        assertEquals(52.8f, compact, EPSILON)
        assertTrue(compact < 66f)

        // Large card/bar => minDimension=300px, 40%=120px > 72px -> preserves full 72px
        val full = clampLensRefractionHeightPx(desiredPx = 72f, minDimensionPx = 300f)
        assertEquals(72f, full, EPSILON)

        // Guard invalid/non-positive dimensions
        assertEquals(0f, clampLensRefractionHeightPx(0f, 132f), EPSILON)
        assertEquals(0f, clampLensRefractionHeightPx(72f, 0f), EPSILON)
        assertEquals(0f, clampLensRefractionHeightPx(-10f, 132f), EPSILON)
    }
}
