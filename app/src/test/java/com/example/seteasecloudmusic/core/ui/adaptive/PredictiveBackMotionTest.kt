package com.example.seteasecloudmusic.core.ui.adaptive

import org.junit.Assert.assertEquals
import org.junit.Test

class PredictiveBackMotionTest {

    @Test
    fun testTranslationXCalculation_clampedToPreviewFraction() {
        val fullWidthPx = 1000f
        val maxPreviewShift = fullWidthPx * PredictiveBackMotion.PREVIEW_MAX_FRACTION // 220f

        // 初始状态（手势未滑动）
        assertEquals(0f, PredictiveBackMotion.calculateTranslationX(0f, fullWidthPx = fullWidthPx), 0.001f)

        // 滑动 50%
        assertEquals(110f, PredictiveBackMotion.calculateTranslationX(0.5f, fullWidthPx = fullWidthPx), 0.001f)

        // 完全拉满 100%
        assertEquals(220f, PredictiveBackMotion.calculateTranslationX(1.0f, fullWidthPx = fullWidthPx), 0.001f)

        // 越界值 clamp 保护
        assertEquals(0f, PredictiveBackMotion.calculateTranslationX(-0.2f, fullWidthPx = fullWidthPx), 0.001f)
        assertEquals(220f, PredictiveBackMotion.calculateTranslationX(1.5f, fullWidthPx = fullWidthPx), 0.001f)
    }
}
