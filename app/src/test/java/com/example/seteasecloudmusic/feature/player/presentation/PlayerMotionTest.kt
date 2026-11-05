package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class PlayerMotionTest {
    @Test fun pausedArtworkKeepsItsCenterAndEndsAtTheLyricHeader() {
        val album = Rect(20f, 100f, 420f, 500f)
        val header = Rect(28f, 40f, 82f, 94f)
        val paused = appleMusicArtworkFrame(album, header, 14f, 8f, 26f, 0.78f, 0f)
        assertEquals(album.center, paused.bounds.center)
        assertEquals(312f, paused.bounds.width, 0.001f)
        val end = appleMusicArtworkFrame(album, header, 14f, 8f, 26f, 0.78f, 1f)
        assertEquals(header, end.bounds)
        assertEquals(8f, end.cornerRadius, 0f)
        assertEquals(0f, end.shadowElevation, 0f)
    }

    @Test fun transitionsStayValidAcrossWindowChangesAndSpringOvershoot() {
        val mini = Rect(20f, 1800f, 1000f, 1940f)
        val full = Rect(0f, 0f, 2076f, 2152f)
        val sourceArt = Rect(30f, 1810f, 150f, 1930f)
        val targetArt = Rect(40f, 60f, 160f, 180f)
        for (step in -5..105) {
            val p = step / 100f
            assertTrue(playerContainerRect(mini, full, p).isUsable())
            assertTrue(playerTopAnchoredArtworkRect(sourceArt, targetArt, mini, full, p).isUsable())
            assertTrue(playerContentAlpha(p) in 0f..1f)
            assertTrue(playerMiniContentAlpha(p) in 0f..1f)
            assertTrue(playerBackgroundAlpha(p) in 0f..1f)
        }
        assertEquals(mini, playerContainerRect(mini, full, -0.1f))
        assertEquals(full, playerContainerRect(mini, full, 1.1f))
        assertEquals(sourceArt, playerTopAnchoredArtworkRect(sourceArt, targetArt, mini, full, 0f))
        assertEquals(targetArt, playerTopAnchoredArtworkRect(sourceArt, targetArt, mini, full, 1f))
        assertFalse(Rect(0f, 0f, Float.NaN, 40f).isUsable())
    }
}
