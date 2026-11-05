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
import androidx.lifecycle.ViewModel
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.*
import com.kyant.backdrop.Backdrop

internal data class SelectedArtist(val id: Long, val name: String, val coverUrl: String?)
internal data class DailyRecommendState(val tracks: List<Track>, val title: String = "每日推荐")

/** Keep large recommendation lists out of saved-instance bundles, but retain them across resizing. */
internal class AppNavigationState : ViewModel() {
    var selectedArtist by mutableStateOf<SelectedArtist?>(null)
    var dailyRecommend by mutableStateOf<DailyRecommendState?>(null)
}

/** Quiet light behind the floating dock, sampled together with the page rather than the dock itself. */
@Composable
internal fun ExpandedChromeBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(Color.White)
        drawRect(Brush.radialGradient(
            listOf(Color(0xFFF5DCE8).copy(alpha = 0.36f), Color.Transparent),
            center = Offset(0f, size.height * 0.52f), radius = 280.dp.toPx()))
        drawRect(Brush.radialGradient(
            listOf(Color(0xFFE1EAFB).copy(alpha = 0.32f), Color.Transparent),
            center = Offset(40.dp.toPx(), size.height * 0.2f), radius = 220.dp.toPx()))
    }
}

/** The compact three-tab glass capsule and separate search lens, arranged vertically on wide windows. */
@Composable
internal fun MusicNavigationRail(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val items = remember { listOf(
        BottomNavItem("首页", R.drawable.ic_nav_home, R.drawable.ic_nav_home_filled),
        BottomNavItem("电台", R.drawable.ic_nav_radio, R.drawable.ic_nav_radio_filled),
        BottomNavItem("我的", R.drawable.ic_nav_mine, R.drawable.ic_nav_mine_filled)
    ) }
    var dragOffset by remember { mutableStateOf<Float?>(null) }
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(if (pressed) 1.035f else 1f,
        spring(dampingRatio = 0.8f, stiffness = 500f), label = "dockPress")
    BoxWithConstraints(modifier.width(104.dp).fillMaxHeight().systemBarsPadding().padding(vertical = 12.dp)
        .testTag("expanded_glass_dock")) {
        val showLabels = maxHeight >= 340.dp
        val extent = if (showLabels) 68.dp else 56.dp
        val groupHeight = extent * items.size
        Column(Modifier.align(Alignment.Center).width(68.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.width(68.dp).height(groupHeight)
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .liquidGlass(backdrop, 34.dp)
                .glassBorder(34.dp, width = 0.5.dp)
                .glassSegmentDrag(items.size, { dragOffset = it }, onSelect, { pressed = it },
                    orientation = Orientation.Vertical)) {
                val geometry = rememberGlassThumbGeometry(groupHeight, items.size,
                    selectedIndex.coerceAtMost(2), dragOffset, 68.dp, 34.dp)
                GlassThumb(backdrop, geometry, alpha = if (selectedIndex == 3) 0f else 1f,
                    orientation = Orientation.Vertical)
                Column(Modifier.fillMaxSize()) {
                    items.forEachIndexed { index, item ->
                        val selected = index == selectedIndex
                        val tint by animateColorAsState(if (selected) Color(0xFFFA233B) else Color(0xFF494950),
                            spring(stiffness = 500f), label = "dockTint")
                        Column(Modifier.fillMaxWidth().height(extent)
                            .selectable(selected, indication = null, interactionSource = remember { MutableInteractionSource() },
                                role = Role.Tab, onClick = { onSelect(index) })
                            .semantics { contentDescription = item.title }
                            .testTag("glass_dock_$index"),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(painterResource(if (selected) item.selectedIconRes else item.iconRes), null,
                                Modifier.size(23.dp), tint = tint)
                            if (showLabels) {
                                Spacer(Modifier.height(3.dp))
                                Text(item.title, color = tint, fontSize = 11.sp, lineHeight = 14.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            val searchInteractions = remember { MutableInteractionSource() }
            val searchPressed by searchInteractions.collectIsPressedAsState()
            val searchScale by animateFloatAsState(if (searchPressed) 1.06f else 1f,
                spring(dampingRatio = 0.8f, stiffness = 500f), label = "dockSearchPress")
            Box(Modifier.size(56.dp).graphicsLayer { scaleX = searchScale; scaleY = searchScale }
                .liquidGlass(backdrop, 28.dp, surfaceAlpha = if (selectedIndex == 3) 0.58f else GlassDefaults.SurfaceAlphaBright)
                .glassBorder(28.dp, width = 0.5.dp)
                .selectable(selectedIndex == 3, interactionSource = searchInteractions, indication = null,
                    role = Role.Tab, onClick = { onSelect(3) })
                .semantics { contentDescription = "搜索" }.testTag("glass_dock_search"), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_nav_search), null, Modifier.size(23.dp),
                    tint = if (selectedIndex == 3) Color(0xFFFA233B) else Color(0xFF494950))
            }
        }
    }
}
