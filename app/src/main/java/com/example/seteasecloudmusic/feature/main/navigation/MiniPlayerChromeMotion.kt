package com.example.seteasecloudmusic.feature.main.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MiniPlayerChromeMotion {
    internal val vertical = Animatable(0f)
    internal val horizontal = Animatable(0f)
    internal val content = Animatable(0f)
    val verticalProgress: State<Float> get() = vertical.asState()
    val horizontalProgress: State<Float> get() = horizontal.asState()
    val contentProgress: State<Float> get() = content.asState()
}

@Composable
fun rememberMiniPlayerChromeMotion(compact: Boolean, playerVisible: Boolean, centerTravel: Dp): MiniPlayerChromeMotion {
    val state = remember { MiniPlayerChromeMotion() }
    LaunchedEffect(compact, playerVisible, centerTravel) {
        val target = if (compact && !playerVisible) 1f else 0f
        coroutineScope {
            launch { state.vertical.animateTo(target, spring(dampingRatio = 0.86f, stiffness = 420f)) }
            launch { state.horizontal.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 500f)) }
            launch { state.content.animateTo(target, spring(dampingRatio = 1f, stiffness = 500f)) }
        }
    }
    return state
}
