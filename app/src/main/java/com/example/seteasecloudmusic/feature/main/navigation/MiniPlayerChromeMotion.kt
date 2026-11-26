package com.example.seteasecloudmusic.feature.main.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * 迷你播放条在常规态与滚动紧凑态之间的三通道动画进度状态：
 * 分别控制纵向位移动画、水平缩进动画以及内部次级按钮的显隐过渡。
 */
class MiniPlayerChromeMotion {
    internal val vertical = Animatable(0f)
    internal val horizontal = Animatable(0f)
    internal val content = Animatable(0f)
    val verticalProgress: State<Float> get() = vertical.asState()
    val horizontalProgress: State<Float> get() = horizontal.asState()
    val contentProgress: State<Float> get() = content.asState()
}

/**
 * 驱动 [MiniPlayerChromeMotion] 三通道弹簧动画的状态记忆函数。
 *
 * @param compact 是否处于滚动收起的紧凑态
 * @param playerVisible 全屏播放页是否可见（可见时恢复常规态进度 0f）
 * @param centerTravel 紧凑态纵向向下位移总量（dp）
 */
@Composable
fun rememberMiniPlayerChromeMotion(compact: Boolean, playerVisible: Boolean, centerTravel: Dp): MiniPlayerChromeMotion {
    val state = remember { MiniPlayerChromeMotion() }
    LaunchedEffect(compact, playerVisible, centerTravel) {
        val target = if (compact && !playerVisible) 1f else 0f
        coroutineScope {
            // 纵向位移弹簧：阻尼比 0.86f、刚度 420f（控制下沉到底栏中间的节奏）
            launch { state.vertical.animateTo(target, spring(dampingRatio = 0.86f, stiffness = 420f)) }
            // 水平收缩弹簧：阻尼比 0.9f、刚度 500f（控制两侧向内缩进速度）
            launch { state.horizontal.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 500f)) }
            // 内部控件显隐弹簧：临界阻尼 1f、刚度 500f（无过冲淡出播放列表按钮）
            launch { state.content.animateTo(target, spring(dampingRatio = 1f, stiffness = 500f)) }
        }
    }
    return state
}
