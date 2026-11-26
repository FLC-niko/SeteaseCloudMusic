package com.example.seteasecloudmusic.feature.main.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlinx.coroutines.launch

/** 绑定主列表状态以供 [NavigationScrollState] 监听滚动方向与边界。 */
class BottomNavigationViewport internal constructor(val list: LazyListState)
fun LazyListState.toNavigationViewport() = BottomNavigationViewport(this)

val LocalNavigationScrollState = staticCompositionLocalOf<NavigationScrollState?> { null }

/**
 * 监听一级列表与歌单详情页的滚动累计距离，控制底部导航栏与迷你播放条在常规态与紧凑态间切换。
 */
class NavigationScrollState {
    val compactProgress = Animatable(0f)
    val scrollHintProgress = Animatable(0f)
    var compactTarget by mutableStateOf(false)
        internal set
    internal var enabled = true
    internal var viewport: BottomNavigationViewport? = null
    internal var detailViewport: BottomNavigationViewport? = null
    private var travel = 0f

    fun expand() { compactTarget = false; travel = 0f }

    fun attachDetailViewport(detail: BottomNavigationViewport) {
        detailViewport = detail
        travel = 0f
    }

    fun detachDetailViewport(detail: BottomNavigationViewport) {
        if (detailViewport === detail) {
            detailViewport = null
            travel = 0f
        }
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (!enabled || source != NestedScrollSource.UserInput || consumed.y == 0f) return Offset.Zero
            val activeList = (detailViewport ?: viewport)?.list
            if (consumed.y > 0f && activeList?.canScrollBackward != true) { expand(); return Offset.Zero }
            if (consumed.y * travel < 0) travel = 0f
            travel += consumed.y
            // 向上累计滚动超过 48px 时触发底栏收起为紧凑态
            if (travel < -48f) compactTarget = true
            // 向下累计回滚超过 24px 时恢复底栏展开态
            if (travel > 24f) compactTarget = false
            return Offset.Zero
        }
    }
}

/** 创建并驱动 [NavigationScrollState] 的紧凑态弹簧过渡。 */
@Composable
fun rememberNavigationScrollState(viewport: BottomNavigationViewport?, enabled: Boolean): NavigationScrollState {
    val state = remember { NavigationScrollState() }
    SideEffect { state.viewport = viewport; state.enabled = enabled }
    LaunchedEffect(enabled, viewport) { state.expand() }
    LaunchedEffect(state.compactTarget, enabled) {
        // 底栏紧凑/展开过渡弹簧参数（阻尼比 0.86f，刚度 450f）
        state.compactProgress.animateTo(if (enabled && state.compactTarget) 1f else 0f, spring(dampingRatio = 0.86f, stiffness = 450f))
    }
    return state
}
