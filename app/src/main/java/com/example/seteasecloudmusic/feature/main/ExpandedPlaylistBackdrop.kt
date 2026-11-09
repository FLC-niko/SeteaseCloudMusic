package com.example.seteasecloudmusic.feature.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.seteasecloudmusic.core.ui.adaptive.LocalAdaptiveWindow
import com.example.seteasecloudmusic.feature.home.presentation.HomeViewModel
import com.example.seteasecloudmusic.feature.mine.presentation.MineViewModel
import com.example.seteasecloudmusic.feature.mine.presentation.rememberCoverDominantDarkColor
import com.example.seteasecloudmusic.feature.mine.presentation.rememberPlaylistColorBackgroundEnabled
import com.example.seteasecloudmusic.feature.radio.presentation.RadioViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** 与详情同一取色底座延伸到侧边操作区，玻璃仍由原导航组件统一采样。 */
@Composable
internal fun ExpandedPlaylistBackdrop(coverUrl: String?, modifier: Modifier = Modifier) {
    val enabled = rememberPlaylistColorBackgroundEnabled()
    val color by rememberCoverDominantDarkColor(coverUrl)
    Canvas(modifier.fillMaxSize()) { drawRect(if (enabled) color else Color.White) }
}

@Composable
internal fun ExpandedPlaylistChromeBackground(
    selectedIndex: State<Int>, home: HomeViewModel, radio: RadioViewModel, mine: MineViewModel
) {
    val index = selectedIndex.value
    val window = LocalAdaptiveWindow.current
    key(index) {
        // 只订阅详情身份和封面，歌曲列表、加载进度等更新不会重组导航外壳。
        val flow = remember(index, home, radio, mine) {
            when (index) {
                0 -> home.uiState.map { it.activePlaylistDetail }
                1 -> radio.uiState.map { it.activePlaylistDetail }
                2 -> mine.uiState.map { it.activePlaylistDetail }
                else -> kotlinx.coroutines.flow.flowOf(null)
            }.map { detail -> detail?.let { it.id to it.coverUrl } }.distinctUntilChanged()
        }
        val detail by flow.collectAsStateWithLifecycle(initialValue = null)
        if (window.showDetailAlongside(detail != null, window.width - window.sideControlWidth, window.height)) {
            ExpandedPlaylistBackdrop(requireNotNull(detail).second)
        }
    }
}
