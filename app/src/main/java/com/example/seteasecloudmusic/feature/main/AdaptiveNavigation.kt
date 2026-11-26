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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import coil.compose.AsyncImage
import com.kyant.shapes.RoundedRectangle
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.window.WindowListPopup
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
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.ui.components.*
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

internal data class SelectedArtist(
    val id: Long,
    val name: String,
    val coverUrl: String?,
    val pageIndex: Int = 3
)
internal data class DailyRecommendState(
    val tracks: List<Track>,
    val title: String = "每日推荐",
    val coverUrl: String? = null
)

/** 跨窗口尺寸变更保留选定艺人与每日推荐状态，避免将大列表写入 saved-instance bundle。 */
internal class AppNavigationState : ViewModel() {
    var selectedArtist by mutableStateOf<SelectedArtist?>(null)
    var dailyRecommend by mutableStateOf<DailyRecommendState?>(null)
}

/** 悬浮侧栏背后的环境光底色，随页面一并参与毛玻璃背景采样。 */
@Composable
internal fun ExpandedChromeBackground(modifier: Modifier = Modifier, atStart: Boolean = false) {
    val isDark = AppTheme.isDark
    val bgColor = AppTheme.colors.background
    // 环境光主/次光晕颜色与透明度（暗色/亮色分别调节）
    val firstGlow = if (isDark) Color(0xFF5A2038).copy(alpha = 0.24f) else Color(0xFFF5DCE8).copy(alpha = 0.36f)
    val secondGlow = if (isDark) Color(0xFF1C2E5A).copy(alpha = 0.22f) else Color(0xFFE1EAFB).copy(alpha = 0.32f)
    Canvas(modifier.fillMaxSize()) {
        drawRect(bgColor)
        // 主径向光晕半径 280.dp（控制边缘氛围扩散范围）
        drawRect(Brush.radialGradient(
            listOf(firstGlow, Color.Transparent),
            center = Offset(if (atStart) 0f else size.width, size.height * 0.52f), radius = 280.dp.toPx()))
        // 次径向光晕半径 220.dp
        drawRect(Brush.radialGradient(
            listOf(secondGlow, Color.Transparent),
            center = Offset(if (atStart) 40.dp.toPx() else size.width - 40.dp.toPx(), size.height * 0.2f), radius = 220.dp.toPx()))
    }
}

/** 宽屏/展开态侧边导航栏：外侧边缘纵向排布头像、迷你播放器、Tab 胶囊与独立搜索按钮。 */
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
    val isDark = AppTheme.isDark
    val colors = AppTheme.colors
    val window = LocalAdaptiveWindow.current
    var overflow by remember { mutableStateOf(false) }
    val activeTabIndex = selectedIndex.coerceAtMost(2)
    val segmentState = rememberLiquidSegmentState(itemCount = items.size, selectedIndex = activeTabIndex)
    val tabsBackdrop = rememberLayerBackdrop()
    val combinedTabBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)
    val railAlphaProvider: () -> Float = { if (selectedIndex == 3) 0f else 1f }
    BoxWithConstraints(modifier.width(window.sideControlWidth).fillMaxHeight()
        .systemBarsPadding().imePadding().padding(vertical = 8.dp).testTag("expanded_glass_dock")) {
        val collapsed = maxHeight < 480.dp // 窗口高度低于此阈值时侧栏 Tab 收起为溢出菜单按钮
        val controlsInOverflow = maxHeight < 360.dp // 窗口高度极小时将迷你播放器收入溢出菜单
        val showLabels = maxHeight >= 600.dp && LocalDensity.current.fontScale < 1.4f // 高度充足且系统字体未过大缩放时显示文字标签
        val extent = if (showLabels) 64.dp else 56.dp // 单个侧栏 Tab 高度（带文字 64.dp / 仅图标 56.dp）
        val groupHeight = extent * items.size
        val dockWidth = if (window.isRegularWidth) 68.dp else 56.dp // 侧边 Tab 胶囊宽度（常规宽屏 68.dp / 紧凑侧栏 56.dp）
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            header()
            Spacer(Modifier.weight(1f))
            if (!controlsInOverflow) miniPlayer(false) {}
            Spacer(Modifier.height(12.dp))
            if (collapsed) {
                Box {
                    IconButton({ overflow = true }, Modifier.size(48.dp)
                        .liquidGlass(backdrop, 24.dp, isDarkSurface = isDark)
                        .interactiveGlassHighlight(RoundedRectangle(24.dp), isDarkSurface = isDark)
                        .testTag("glass_dock_overflow")) {
                        Icon(painterResource(items[activeTabIndex].selectedIconRes), "更多导航", tint = colors.accent)
                    }
                    WindowListPopup(
                        show = overflow,
                        onDismissRequest = { overflow = false }
                    ) {
                        ListPopupColumn {
                            items.forEachIndexed { index, item ->
                                val selected = index == selectedIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(index); overflow = false }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .testTag("glass_dock_$index"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(item.iconRes),
                                        contentDescription = null,
                                        tint = if (selected) colors.accent else colors.iconPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = item.title,
                                        color = if (selected) colors.accent else colors.textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                            if (controlsInOverflow) {
                                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    miniPlayer(true) { overflow = false }
                                }
                            }
                        }
                    }
                }
            } else {
                val railCorner = dockWidth / 2
                val railShape = remember(railCorner) { RoundedRectangle(railCorner) }
                val railLayerBlock: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {
                    val p = segmentState.pressProgress.value.coerceIn(0f, 1f)
                    if (size.height > 0f) {
                        val maxScale = (size.height + 10.dp.toPx()) / size.height // 按压时纵向最多额外放大 10.dp
                        val s = lerp(1f, maxScale, p)
                        scaleX = s
                        scaleY = s
                    }
                }
                val railSelectedContent: @Composable BoxScope.() -> Unit = {
                    Column(Modifier.fillMaxSize()) {
                        items.forEach { item ->
                            Column(
                                Modifier.fillMaxWidth().height(extent),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painterResource(item.selectedIconRes),
                                    null,
                                    Modifier.size(23.dp),
                                    tint = colors.accent
                                )
                                if (showLabels) {
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        item.title,
                                        color = colors.accent,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                Box(Modifier.width(dockWidth).height(groupHeight)) {
                    Box(
                        Modifier.fillMaxSize()
                            .liquidGlass(
                                backdrop = backdrop,
                                cornerRadius = railCorner,
                                isDarkSurface = isDark,
                                bloomExtraDegrees = -45f, // 边缘高光偏转角度（度）
                                layerBlock = railLayerBlock
                            )
                            .interactiveGlassHighlight(shape = railShape, isDarkSurface = isDark)
                            .liquidSegmentDrag(
                                state = segmentState,
                                itemCount = items.size,
                                selectedIndex = activeTabIndex,
                                onSettle = onSelect,
                                orientation = Orientation.Vertical
                            )
                    ) {
                        LiquidGlassDualColorTabs(
                            state = segmentState,
                            containerWidth = dockWidth,
                            containerHeight = groupHeight,
                            cornerRadius = railCorner,
                            isDarkSurface = isDark,
                            alphaProvider = railAlphaProvider,
                            orientation = Orientation.Vertical,
                            unselectedContent = {
                                Column(Modifier.fillMaxSize()) {
                                    items.forEachIndexed { index, item ->
                                        val selected = index == selectedIndex
                                        Column(
                                            Modifier.fillMaxWidth().height(extent)
                                                .selectable(
                                                    selected,
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    role = Role.Tab,
                                                    onClick = { onSelect(index) }
                                                )
                                                .semantics { contentDescription = item.title }
                                                .testTag("glass_dock_$index"),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                painterResource(item.iconRes),
                                                null,
                                                Modifier.size(23.dp),
                                                tint = colors.iconPrimary
                                            )
                                            if (showLabels) {
                                                Spacer(Modifier.height(3.dp))
                                                Text(
                                                    item.title,
                                                    color = colors.iconPrimary,
                                                    fontSize = 11.sp,
                                                    lineHeight = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            selectedContent = railSelectedContent
                        )
                    }

                    LiquidGlassActiveTabsRecorder(
                        pageBackdrop = backdrop,
                        activeTabsBackdrop = tabsBackdrop,
                        state = segmentState,
                        containerWidth = dockWidth,
                        containerHeight = groupHeight,
                        cornerRadius = railCorner,
                        isDarkSurface = isDark,
                        layerBlock = railLayerBlock,
                        alphaProvider = railAlphaProvider,
                        modifier = Modifier.align(Alignment.TopStart),
                        selectedContent = railSelectedContent
                    )

                    LiquidGlassActiveDroplet(
                        combinedBackdrop = combinedTabBackdrop,
                        state = segmentState,
                        containerWidth = dockWidth,
                        containerHeight = groupHeight,
                        cornerRadius = railCorner,
                        maxPressScale = GlassSliderDefaults.CompactMaxPressScale,
                        isDarkSurface = isDark,
                        alphaProvider = railAlphaProvider,
                        orientation = Orientation.Vertical,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            val searchInteractions = remember { MutableInteractionSource() }
            val searchPressed by searchInteractions.collectIsPressedAsState()
            val searchScale by animateFloatAsState(if (searchPressed) 1.06f else 1f,
                // 侧栏搜索按钮按压缩放弹簧参数（阻尼比 0.8f，刚度 500f）
                spring(dampingRatio = 0.8f, stiffness = 500f), label = "dockSearchPress")
            Box(Modifier.size(48.dp)
                .liquidGlass(
                    backdrop = backdrop,
                    cornerRadius = 24.dp,
                    // 选中搜索页时加重玻璃表面不透明度
                    surfaceAlpha = if (selectedIndex == 3) (if (isDark) 0.40f else 0.58f) else GlassDefaults.SurfaceAlphaBright,
                    isDarkSurface = isDark,
                    layerBlock = { scaleX = searchScale; scaleY = searchScale }
                )
                .interactiveGlassHighlight(RoundedRectangle(24.dp), isDarkSurface = isDark)
                .selectable(selectedIndex == 3, interactionSource = searchInteractions, indication = null,
                    role = Role.Tab, onClick = { onSelect(3) })
                .semantics { contentDescription = "搜索" }.testTag("glass_dock_search"), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_nav_search), null, Modifier.size(23.dp),
                    tint = if (selectedIndex == 3) colors.accent else colors.iconSecondary)
            }
        }
    }
}

/** 宽屏侧栏内嵌迷你播放条：纵向排列封面、播放/暂停与队列按钮。 */
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
    val state by remember(stateFlow) {
        stateFlow.map { it.currentTrack to it.status }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = stateFlow.value.let { it.currentTrack to it.status })
    val track = state.first
    val status = state.second
    val isDark = AppTheme.isDark
    val colors = AppTheme.colors
    val shared = LocalPlayerArtwork.current
    val artwork = track?.coverUrl ?: track?.album?.coverUrl
    // 侧栏迷你播放条宽度 56.dp、全圆角半径 28.dp
    Column(Modifier.width(56.dp).liquidGlass(backdrop, 28.dp, isDarkSurface = isDark, enableNoiseDither = true)
        .interactiveGlassHighlight(RoundedRectangle(28.dp), isDarkSurface = isDark)
        .padding(vertical = 6.dp).onGloballyPositioned { onBounds(it.boundsInRoot()) }.testTag("side_mini_player"),
        horizontalAlignment = Alignment.CenterHorizontally) {
        // 封面槽尺寸 44.dp、圆角 12.dp
        Box(Modifier.size(44.dp).clip(RoundedRectangle(12.dp))
            .onGloballyPositioned { onArtworkBounds(it.boundsInRoot()) }
            .clickable { onOpen(artwork) }.semantics { contentDescription = "打开播放器" }, contentAlignment = Alignment.Center) {
            val painter = shared?.takeIf { it.first == normalizeHighResCoverUrl(artwork) }?.second
            if (painter != null) Image(painter, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else if (!artwork.isNullOrBlank()) AsyncImage(rememberHighResArtworkRequest(artwork), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(painterResource(R.drawable.ic_nav_radio), null, Modifier.size(22.dp), tint = colors.textSecondary)
        }
        IconButton(onPlayPause, Modifier.size(44.dp), enabled = track != null) {
            if (status == PlayerStatus.BUFFERING) {
                AppLoadingIndicator(
                    modifier = Modifier.size(18.dp),
                    color = colors.iconSecondary,
                    strokeWidth = 2.dp,
                    size = 18.dp
                )
            } else {
                Icon(
                    painter = painterResource(
                        id = if (status == PlayerStatus.PLAYING) R.drawable.ic_mini_pause else R.drawable.ic_mini_play
                    ),
                    contentDescription = if (status == PlayerStatus.PLAYING) "暂停" else "播放",
                    tint = colors.iconSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        IconButton(onQueue, Modifier.size(44.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_mini_playlist),
                contentDescription = "播放列表",
                tint = colors.iconSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
