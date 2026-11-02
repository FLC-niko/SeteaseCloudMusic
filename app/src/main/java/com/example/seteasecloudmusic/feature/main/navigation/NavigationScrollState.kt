package com.example.seteasecloudmusic.feature.main.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlinx.coroutines.launch

class BottomNavigationViewport internal constructor(val list: LazyListState)
fun LazyListState.toNavigationViewport() = BottomNavigationViewport(this)

class NavigationScrollState {
    val compactProgress = Animatable(0f)
    val scrollHintProgress = Animatable(0f)
    var compactTarget by mutableStateOf(false)
        internal set
    internal var enabled = true
    internal var viewport: BottomNavigationViewport? = null
    private var travel = 0f
    fun expand() { compactTarget = false; travel = 0f }
    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (!enabled || source != NestedScrollSource.UserInput) return Offset.Zero
            if (viewport?.list?.canScrollBackward != true) { expand(); return Offset.Zero }
            if (consumed.y * travel < 0) travel = 0f
            travel += consumed.y
            if (travel < -48f) compactTarget = true
            if (travel > 24f) compactTarget = false
            return Offset.Zero
        }
    }
}

@Composable
fun rememberNavigationScrollState(viewport: BottomNavigationViewport?, enabled: Boolean): NavigationScrollState {
    val state = remember { NavigationScrollState() }
    SideEffect { state.viewport = viewport; state.enabled = enabled }
    LaunchedEffect(enabled, viewport) { state.expand() }
    LaunchedEffect(state.compactTarget, enabled) {
        state.compactProgress.animateTo(if (enabled && state.compactTarget) 1f else 0f, spring(dampingRatio = 0.86f, stiffness = 450f))
    }
    return state
}
