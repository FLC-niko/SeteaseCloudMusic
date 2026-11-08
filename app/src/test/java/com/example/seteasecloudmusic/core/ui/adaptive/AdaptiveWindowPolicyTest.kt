package com.example.seteasecloudmusic.core.ui.adaptive

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test

class AdaptiveWindowPolicyTest {
    @Test fun duoKeepsHorizontalBarsOnlyForInnerPortraitAndTabletop() {
        assertTrue(AdaptiveWindowInfo(443.dp, 970.dp, isFoldable = true).useNavigationRail)
        assertTrue(AdaptiveWindowInfo(883.dp, 852.dp, isFoldable = true).useNavigationRail)
        assertFalse(AdaptiveWindowInfo(851.dp, 883.dp, isFoldable = true).useNavigationRail)
        val tabletop = AdaptiveWindowInfo(883.dp, 852.dp, Rect(0f, 400f, 883f, 420f), true, true)
        assertFalse(tabletop.useNavigationRail)
        assertTrue(tabletop.useExpandedPlayer)
    }

    @Test fun ordinaryPhonesAndFreelyResizedWindowsKeepReachableLayouts() {
        assertFalse(AdaptiveWindowInfo(393.dp, 850.dp).useNavigationRail)
        assertTrue(AdaptiveWindowInfo(850.dp, 420.dp).useNavigationRail)
        assertFalse(AdaptiveWindowInfo(850.dp, 280.dp).useNavigationRail)
        assertFalse(AdaptiveWindowInfo(851.dp, 883.dp, isFoldable = true).useExpandedPlayer)
        assertTrue(AdaptiveWindowInfo(883.dp, 852.dp, isFoldable = true).useExpandedPlayer)
    }
    @Test fun detailsOnlyUseTwoPanesAfterSelectionAndWithinAvailableContent() {
        val inner = AdaptiveWindowInfo(851.dp, 883.dp, isFoldable = true)
        assertFalse(inner.showDetailAlongside(false, 851.dp, 883.dp))
        assertTrue(inner.showDetailAlongside(true, 851.dp, 883.dp))
        assertFalse(inner.showDetailAlongside(true, 640.dp, 883.dp))
        assertFalse(inner.showDetailAlongside(true, 851.dp, 280.dp))
        val tabletop = AdaptiveWindowInfo(883.dp, 852.dp, Rect(0f, 400f, 883f, 420f), true, true)
        assertFalse(tabletop.showDetailAlongside(false, 883.dp, 852.dp))
        assertTrue(tabletop.showDetailAlongside(true, 883.dp, 852.dp))
    }

}
