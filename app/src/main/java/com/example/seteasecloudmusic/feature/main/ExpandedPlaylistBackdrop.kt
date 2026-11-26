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
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.example.seteasecloudmusic.feature.home.presentation.HomeViewModel
import com.example.seteasecloudmusic.feature.mine.presentation.LOCAL_PLAYLIST_ID
import com.example.seteasecloudmusic.feature.mine.presentation.MineViewModel
import com.example.seteasecloudmusic.feature.mine.presentation.rememberCoverDominantDarkColor
import com.example.seteasecloudmusic.feature.mine.presentation.rememberPlaylistColorBackgroundEnabled
import com.example.seteasecloudmusic.feature.radio.presentation.RadioViewModel
import com.example.seteasecloudmusic.feature.search.presentation.SearchViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** 将歌单详情的主色背景延伸至宽屏侧边操作区，供悬浮玻璃组件统一采样。 */
@Composable
internal fun ExpandedPlaylistBackdrop(
    coverUrl: String?,
    modifier: Modifier = Modifier,
    playlistId: Long? = null
) {
    val enabled = rememberPlaylistColorBackgroundEnabled(playlistId)
    val color by rememberCoverDominantDarkColor(coverUrl)
    val bgColor = AppTheme.colors.background
    Canvas(modifier.fillMaxSize()) { drawRect(if (enabled) color else bgColor) }
}

/** 监听当前激活 Tab 的歌单详情封面，并在分栏模式下绘制延伸背景色。 */
@Composable
internal fun ExpandedPlaylistChromeBackground(
    selectedIndex: State<Int>, home: HomeViewModel, radio: RadioViewModel, mine: MineViewModel, search: SearchViewModel
) {
    val index = selectedIndex.value
    val window = LocalAdaptiveWindow.current
    key(index) {
        // 只订阅详情身份和封面，歌曲列表、加载进度等更新不会重组导航外壳。
        val flow = remember(index, home, radio, mine, search) {
            when (index) {
                0 -> home.uiState.map { it.activePlaylistDetail }
                1 -> radio.uiState.map { it.activePlaylistDetail }
                2 -> mine.uiState.map { it.activePlaylistDetail }
                3 -> search.uiState.map { it.activePlaylistDetail }
                else -> kotlinx.coroutines.flow.flowOf(null)
            }.map { detail ->
                detail?.let {
                    val isLikedPlaylist = it.name.contains("喜欢") ||
                        (it.id != LOCAL_PLAYLIST_ID && it.description?.contains("红心") == true)
                    val coverUrl = if (isLikedPlaylist) {
                        it.tracks.firstOrNull { t -> !t.coverUrl.isNullOrBlank() }?.coverUrl
                            ?: it.tracks.firstOrNull { t -> !t.album.coverUrl.isNullOrBlank() }?.album?.coverUrl
                    } else {
                        it.coverUrl
                    }
                    it.id to coverUrl
                }
            }.distinctUntilChanged()
        }
        val detail by flow.collectAsStateWithLifecycle(initialValue = null)
        if (window.showDetailAlongside(detail != null, window.width - window.sideControlWidth, window.height)) {
            val active = requireNotNull(detail)
            ExpandedPlaylistBackdrop(coverUrl = active.second, playlistId = active.first)
        }
    }
}
