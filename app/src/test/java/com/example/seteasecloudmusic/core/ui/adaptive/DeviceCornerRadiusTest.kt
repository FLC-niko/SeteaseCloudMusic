package com.example.seteasecloudmusic.core.ui.adaptive

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCornerRadiusTest {

    @Test
    fun testDeviceCornerRadiusProperties() {
        val corners = DeviceCornerRadius(
            topStart = 36.dp,
            topEnd = 36.dp,
            bottomStart = 28.dp,
            bottomEnd = 28.dp
        )
        assertTrue(corners.hasRoundedCorners)
        assertEquals(36.dp, corners.maxRadius)

        val shape = corners.toShape()
        val density = androidx.compose.ui.unit.Density(1f)
        val size = androidx.compose.ui.geometry.Size(200f, 200f)
        assertEquals(36f, shape.topStart.toPx(size, density), 0.01f)
        assertEquals(28f, shape.bottomEnd.toPx(size, density), 0.01f)
    }

    @Test
    fun testZeroCornerRadius() {
        val squareCorners = DeviceCornerRadius(0.dp, 0.dp, 0.dp, 0.dp)
        assertFalse(squareCorners.hasRoundedCorners)
        assertEquals(0.dp, squareCorners.maxRadius)
    }
}
