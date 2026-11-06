package com.example.seteasecloudmusic.feature.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import com.example.seteasecloudmusic.core.ui.adaptive.LocalAdaptiveWindow
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.feature.player.presentation.LocalPlayerArtwork
import com.example.seteasecloudmusic.feature.player.presentation.normalizeHighResCoverUrl
import com.example.seteasecloudmusic.feature.player.presentation.rememberHighResArtworkRequest
import kotlinx.coroutines.flow.StateFlow
import coil.compose.AsyncImage
import com.kyant.shapes.RoundedRectangle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.*
import com.kyant.backdrop.Backdrop

internal data class SelectedArtist(val id: Long, val name: String, val coverUrl: String?)
internal data class DailyRecommendState(
    val tracks: List<Track>,
    val title: String = "每日推荐",
    val coverUrl: String? = null
)

/** Keep large recommendation lists out of saved-instance bundles, but retain them across resizing. */
internal class AppNavigationState : ViewModel() {
    var selectedArtist by mutableStateOf<SelectedArtist?>(null)
    var dailyRecommend by mutableStateOf<DailyRecommendState?>(null)
}

/** Quiet light behind the floating dock, sampled together with the page rather than the dock itself. */
@Composable
internal fun ExpandedChromeBackground(modifier: Modifier = Modifier, atStart: Boolean = false) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(Color.White)
        drawRect(Brush.radialGradient(
            listOf(Color(0xFFF5DCE8).copy(alpha = 0.36f), Color.Transparent),
            center = Offset(if (atStart) 0f else size.width, size.height * 0.52f), radius = 280.dp.toPx()))
        drawRect(Brush.radialGradient(
            listOf(Color(0xFFE1EAFB).copy(alpha = 0.32f), Color.Transparent),
            center = Offset(if (atStart) 40.dp.toPx() else size.width - 40.dp.toPx(), size.height * 0.2f), radius = 220.dp.toPx()))
    }
}

/** Side controls follow the outer edge; tabs stay bottom aligned as on Duo. */
@Composable
internal fun MusicNavigationRail(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    miniPlayer: @Composable (Boolean, () -> Unit) -> Unit = { _, _ -> }
) {
    val items = remember { listOf(
        BottomNavItem("首页", R.drawable.ic_nav_home, R.drawable.ic_nav_home_filled),
        BottomNavItem("电台", R.drawable.ic_nav_radio, R.drawable.ic_nav_radio_filled),
        BottomNavItem("我的", R.drawable.ic_nav_mine, R.drawable.ic_nav_mine_filled)
    ) }
    val window = LocalAdaptiveWindow.current
    var dragOffset by remember { mutableStateOf<Float?>(null) }
    var pressed by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(if (pressed) 1.035f else 1f,
        spring(dampingRatio = 0.8f, stiffness = 500f), label = "dockPress")
    BoxWithConstraints(modifier.width(window.sideControlWidth).fillMaxHeight()
        .systemBarsPadding().imePadding().padding(vertical = 8.dp).testTag("expanded_glass_dock")) {
        val collapsed = maxHeight < 480.dp
        val controlsInOverflow = maxHeight < 360.dp
        val showLabels = maxHeight >= 600.dp && LocalDensity.current.fontScale < 1.4f
        val extent = if (showLabels) 64.dp else 56.dp
        val groupHeight = extent * items.size
        val dockWidth = if (window.isRegularWidth) 68.dp else 56.dp
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            header()
            Spacer(Modifier.weight(1f))
            if (!controlsInOverflow) miniPlayer(false) {}
            Spacer(Modifier.height(12.dp))
            if (collapsed) {
                Box {
                    IconButton({ overflow = true }, Modifier.size(48.dp).liquidGlass(backdrop, 24.dp)
                        .testTag("glass_dock_overflow")) {
                        Icon(painterResource(items[selectedIndex.coerceAtMost(2)].selectedIconRes), "更多导航", tint = Color(0xFFFA233B))
                    }
                    DropdownMenu(overflow, { overflow = false }) {
                        items.forEachIndexed { index, item ->
                            DropdownMenuItem(text = { Text(item.title) }, onClick = { onSelect(index); overflow = false },
                                modifier = Modifier.testTag("glass_dock_$index"),
                                leadingIcon = { Icon(painterResource(item.iconRes), null) })
                        }
                        if (controlsInOverflow) Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { miniPlayer(true) { overflow = false } }
                    }
                }
            } else {
                Box(Modifier.width(dockWidth).height(groupHeight)
                    .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                    .liquidGlass(backdrop, dockWidth / 2).glassBorder(dockWidth / 2, width = 0.5.dp)
                    .glassSegmentDrag(items.size, { dragOffset = it }, onSelect, { pressed = it }, orientation = Orientation.Vertical)) {
                    val geometry = rememberGlassThumbGeometry(groupHeight, items.size,
                        selectedIndex.coerceAtMost(2), dragOffset, dockWidth, dockWidth / 2)
                    GlassThumb(backdrop, geometry, alpha = if (selectedIndex == 3) 0f else 1f, orientation = Orientation.Vertical)
                    Column(Modifier.fillMaxSize()) {
                        items.forEachIndexed { index, item ->
                            val selected = index == selectedIndex
                            val tint by animateColorAsState(if (selected) Color(0xFFFA233B) else Color(0xFF494950),
                                spring(stiffness = 500f), label = "dockTint")
                            Column(Modifier.fillMaxWidth().height(extent)
                                .selectable(selected, indication = null, interactionSource = remember { MutableInteractionSource() },
                                    role = Role.Tab, onClick = { onSelect(index) })
                                .semantics { contentDescription = item.title }.testTag("glass_dock_$index"),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(painterResource(if (selected) item.selectedIconRes else item.iconRes), null, Modifier.size(23.dp), tint = tint)
                                if (showLabels) {
                                    Spacer(Modifier.height(3.dp))
                                    Text(item.title, color = tint, fontSize = 11.sp, lineHeight = 14.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            val searchInteractions = remember { MutableInteractionSource() }
            val searchPressed by searchInteractions.collectIsPressedAsState()
            val searchScale by animateFloatAsState(if (searchPressed) 1.06f else 1f,
                spring(dampingRatio = 0.8f, stiffness = 500f), label = "dockSearchPress")
            Box(Modifier.size(48.dp).graphicsLayer { scaleX = searchScale; scaleY = searchScale }
                .liquidGlass(backdrop, 24.dp, surfaceAlpha = if (selectedIndex == 3) 0.58f else GlassDefaults.SurfaceAlphaBright)
                .glassBorder(24.dp, width = 0.5.dp)
                .selectable(selectedIndex == 3, interactionSource = searchInteractions, indication = null,
                    role = Role.Tab, onClick = { onSelect(3) })
                .semantics { contentDescription = "搜索" }.testTag("glass_dock_search"), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_nav_search), null, Modifier.size(23.dp),
                    tint = if (selectedIndex == 3) Color(0xFFFA233B) else Color(0xFF494950))
            }
        }
    }
}

@Composable
internal fun SideMiniPlayer(
    stateFlow: StateFlow<PlaybackState>,
    backdrop: Backdrop,
    onOpen: (String?) -> Unit,
    onPlayPause: () -> Unit,
    onQueue: () -> Unit,
    onBounds: (Rect) -> Unit,
    onArtworkBounds: (Rect) -> Unit
) {
    val state by stateFlow.collectAsStateWithLifecycle()
    val track = state.currentTrack
    val shared = LocalPlayerArtwork.current
    val artwork = track?.coverUrl ?: track?.album?.coverUrl
    Column(Modifier.width(56.dp).liquidGlass(backdrop, 28.dp).glassBorder(28.dp, width = 0.5.dp)
        .padding(vertical = 6.dp).onGloballyPositioned { onBounds(it.boundsInRoot()) }.testTag("side_mini_player"),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(44.dp).clip(RoundedRectangle(12.dp))
            .onGloballyPositioned { onArtworkBounds(it.boundsInRoot()) }
            .clickable { onOpen(artwork) }.semantics { contentDescription = "打开播放器" }, contentAlignment = Alignment.Center) {
            val painter = shared?.takeIf { it.first == normalizeHighResCoverUrl(artwork) }?.second
            if (painter != null) Image(painter, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else if (!artwork.isNullOrBlank()) AsyncImage(rememberHighResArtworkRequest(artwork), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(painterResource(R.drawable.ic_nav_radio), null, Modifier.size(22.dp), tint = Color.Gray)
        }
        IconButton(onPlayPause, Modifier.size(44.dp), enabled = track != null) {
            Icon(if (state.status == PlayerStatus.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (state.status == PlayerStatus.PLAYING) "暂停" else "播放", tint = Color(0xFF494950))
        }
        IconButton(onQueue, Modifier.size(44.dp)) { Icon(Icons.AutoMirrored.Filled.QueueMusic, "播放列表", tint = Color(0xFF494950)) }
    }
}
