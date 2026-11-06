package com.example.seteasecloudmusic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.test.platform.app.InstrumentationRegistry
import com.example.seteasecloudmusic.feature.player.domain.model.Lyrics
import com.example.seteasecloudmusic.feature.player.domain.model.LyricContent
import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.core.model.*
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.core.ui.adaptive.*
import com.example.seteasecloudmusic.feature.main.MainActivity
import com.example.seteasecloudmusic.feature.main.AppNavigation
import com.example.seteasecloudmusic.feature.main.MusicNavigationRail
import com.example.seteasecloudmusic.feature.main.SearchMiniPlayerBar
import com.example.seteasecloudmusic.feature.main.navigation.horizontalChromeFrame
import com.example.seteasecloudmusic.feature.main.navigation.horizontalMiniPlayerFrame
import com.example.seteasecloudmusic.core.ui.components.LocalGlassBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.example.seteasecloudmusic.feature.artist.presentation.*
import com.example.seteasecloudmusic.feature.artist.domain.model.*
import com.example.seteasecloudmusic.feature.auth.presentation.*
import com.example.seteasecloudmusic.feature.discover.presentation.*
import com.example.seteasecloudmusic.feature.discover.domain.model.*
import com.example.seteasecloudmusic.feature.home.presentation.*
import com.example.seteasecloudmusic.feature.mine.presentation.*
import com.example.seteasecloudmusic.feature.mine.domain.model.*
import com.example.seteasecloudmusic.feature.player.presentation.*
import com.example.seteasecloudmusic.feature.radio.presentation.*
import com.example.seteasecloudmusic.feature.radio.domain.model.*
import com.example.seteasecloudmusic.feature.search.presentation.*
import com.example.seteasecloudmusic.feature.search.domain.*
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import java.io.File

/** Real Compose rendering with deterministic local artwork; run on outer/inner/landscape configurations. */
class AdaptiveLayoutsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Volatile private var windowInfo: AdaptiveWindowInfo? = null
    @Volatile private var currentFontScale = 1f
    private val pages = listOf("chrome", "native_music", "home", "radio", "radio_detail", "mine", "mine_detail", "local", "playlist", "daily", "artist", "search", "discover", "native", "lyrics", "queue", "quality", "account", "settings", "captcha", "qr", "account_detail", "offline")

    @Before fun keepEmulatorVisible() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.setTurnScreenOn(true)
            val keyguard = activity.getSystemService(android.app.KeyguardManager::class.java)
            if (!keyguard.isKeyguardSecure) keyguard.requestDismissKeyguard(activity, null)
        }
    }

    @Test fun allPagesRenderAndCapture() {
        val arguments = InstrumentationRegistry.getArguments()
        val label = arguments.getString("layoutLabel") ?: "current"
        val selectedPages = arguments.getString("pages")?.split(',') ?: pages
        require(selectedPages.isNotEmpty() && selectedPages.all { it in pages }) {
            "Page scope must contain known fixtures: $selectedPages"
        }
        val tracks = fixtures()
        var page by mutableStateOf(selectedPages.first())
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                ProvideAdaptiveWindow(activity) {
                    val info = LocalAdaptiveWindow.current
                    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
                    SideEffect { windowInfo = info; currentFontScale = fontScale }
                    MaterialTheme {
                        val background = rememberLayerBackdrop { drawContent() }
                        Box(Modifier.fillMaxSize().background(Color.White).testTag("fixture")) {
                            Canvas(Modifier.fillMaxSize().layerBackdrop(background)) {
                                drawRect(Color.White)
                                drawRect(androidx.compose.ui.graphics.Brush.radialGradient(
                                    listOf(Color(0xFFE7DEF4), Color.Transparent),
                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.4f),
                                    radius = size.width * 0.65f))
                            }
                            CompositionLocalProvider(LocalGlassBackdrop provides background) { FixturePage(page, tracks) }
                        }
                    }
                }
            }
        }
        selectedPages.forEach { name ->
            compose.runOnUiThread { page = name }
            compose.waitForIdle()
            compose.onNodeWithTag("fixture").assertExists()
            val expected = when (name) {
                "chrome" -> "首页"
                "native_music" -> if (windowInfo?.width?.let { it >= 600.dp } == true || windowInfo?.separatingFold != null) "日落之前" else null
                "home" -> "首页"
                "radio", "radio_detail" -> "电台"
                "mine", "mine_detail", "local" -> "我的"
                "playlist" -> "日落之后"
                "daily" -> "每日推荐"
                "artist" -> "Luna Ensemble"
                "search" -> "最佳结果"
                "discover" -> "推荐歌单"
                "native" -> "未在播放"
                "queue" -> "播放列表"
                "settings" -> "账户设置"
                else -> null
            }
            if (expected != null) {
                compose.onAllNodesWithText(expected, substring = true).onFirst().assertExists()
            }
            // Dialogs render on Android's frame clock as well as Compose's test clock.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            capture("$label-$name")
            if (name == "chrome") {
                val info = requireNotNull(windowInfo)
                if (info.useNavigationRail) {
                    val dock = compose.onNodeWithTag("expanded_glass_dock").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                    val content = compose.onNodeWithTag("chrome_content").fetchSemanticsNode().boundsInRoot
                    val root = compose.onNodeWithTag("fixture").fetchSemanticsNode().boundsInRoot
                    assertTrue("Controls must follow the outer edge", if (info.sideControlsAtStart) dock.center.x < root.center.x else dock.center.x > root.center.x)
                    assertTrue("Content must leave the side controls clear", if (info.sideControlsAtStart) content.left >= dock.right - 1f else content.right <= dock.left + 1f)
                    compose.onNodeWithTag("horizontal_glass_dock").assertDoesNotExist()
                } else {
                    compose.onNodeWithTag("horizontal_glass_dock").assertIsDisplayed()
                    compose.onNodeWithTag("expanded_glass_dock").assertDoesNotExist()
                }
                val queueInOverflow = compose.onAllNodesWithContentDescription("播放列表").fetchSemanticsNodes().isEmpty()
                val underlyingCenter = if (queueInOverflow) {
                    val center = compose.onNodeWithTag("glass_dock_overflow").fetchSemanticsNode().boundsInRoot.center
                    compose.onNodeWithTag("glass_dock_overflow").performClick()
                    center
                } else compose.onNodeWithContentDescription("播放列表").fetchSemanticsNode().boundsInRoot.center
                compose.onNodeWithContentDescription("播放列表").performClick()
                compose.onNodeWithText("播放列表").assertExists()
                capture("$label-chrome-queue")
                androidx.test.espresso.Espresso.pressBack()
                compose.onNodeWithContentDescription("打开账户").performClick()
                compose.onNodeWithText("Setease 账户").assertExists()
                assertAwayFromFold("account_glass_surface")
                capture("$label-chrome-account")
                val fixtureOrigin = compose.onNodeWithTag("fixture").fetchSemanticsNode().boundsInRoot.topLeft
                compose.onNodeWithTag("fixture").performTouchInput { click(underlyingCenter - fixtureOrigin) }
                compose.onNodeWithText("播放列表").assertDoesNotExist()
                if (queueInOverflow) compose.onNodeWithTag("glass_dock_0").assertDoesNotExist()
                // The same screen point can be outside the narrower sheet: its scrim may dismiss it.
                if (compose.onAllNodesWithText("Setease 账户").fetchSemanticsNodes().isEmpty()) {
                    compose.onNodeWithContentDescription("打开账户").performClick()
                }
                compose.onNodeWithText("Setease 账户").assertExists()
                androidx.test.espresso.Espresso.pressBack()
            }
            if (name == "native_music") {
                compose.onNode(hasContentDescription("播放") or hasContentDescription("暂停")).assertExists()
            }
            if (name == "settings") {
                compose.onNodeWithText("在线播放音质").performClick()
                compose.onNodeWithText("选择在线播放音质").assertExists()
                capture("$label-settings-quality")
                compose.onNodeWithText("完成").performClick()
            }
            if (name == "search") {
                listOf("艺人", "歌单", "歌曲").forEach { category ->
                    compose.onAllNodesWithText(category).onFirst().performClick()
                    compose.waitForIdle()
                    capture("$label-search-$category")
                }
            }
        }
    }

    @Test fun compactMiniPlayerKeepsClearOfBothButtonsAcrossWidths() {
        var width by mutableStateOf(320.dp)
        var scale by mutableFloatStateOf(1f)
        var compact by mutableStateOf(false)
        val tracks = fixtures()
        val sizingArtist = "陈奕迅 · Luna Ensemble"
        val sizingTrack = tracks[0].copy(artists = listOf(Artist(1, sizingArtist)))
        val playback = kotlinx.coroutines.flow.MutableStateFlow(PlaybackState(PlayerStatus.PAUSED, sizingTrack, durationMs = 220000))
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                ProvideAdaptiveWindow(activity) {
                    val info = LocalAdaptiveWindow.current
                    SideEffect { windowInfo = info }
                    val original = androidx.compose.ui.platform.LocalDensity.current
                    SideEffect { currentFontScale = original.fontScale }
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(original.density, scale)) {
                        MaterialTheme {
                            val background = rememberLayerBackdrop { drawContent() }
                            Box(Modifier.fillMaxSize().background(Color(0xFFF2F2F7)), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Canvas(Modifier.fillMaxSize().layerBackdrop(background)) { drawRect(Color(0xFFF2F2F7)) }
                                Box(Modifier.requiredWidth(width).height(160.dp)) {
                                    Box(Modifier.align(androidx.compose.ui.Alignment.BottomCenter).horizontalChromeFrame().height(54.dp).testTag("sizing_nav_frame")) {
                                        Box(Modifier.align(androidx.compose.ui.Alignment.CenterStart).size(54.dp).background(Color.White, androidx.compose.foundation.shape.CircleShape).testTag("sizing_left_button")) {
                                            androidx.compose.material3.Text("首页", Modifier.align(androidx.compose.ui.Alignment.Center))
                                        }
                                        Box(Modifier.align(androidx.compose.ui.Alignment.CenterEnd).size(54.dp).background(Color.White, androidx.compose.foundation.shape.CircleShape).testTag("sizing_right_button")) {
                                            androidx.compose.material3.Text("搜索", Modifier.align(androidx.compose.ui.Alignment.Center))
                                        }
                                    }
                                    val progress = if (compact) 1f else 0f
                                    SearchMiniPlayerBar(background, 27.dp, playback,
                                        contentProgress = remember(progress) { mutableStateOf(progress) },
                                        onPlayPauseClick = {}, onPlaylistClick = {}, onNextClick = {}, onPreviousClick = {},
                                        onBarClick = {}, onArtworkBoundsChanged = {}, hideArtwork = false,
                                        modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                                            .padding(bottom = if (compact) 0.dp else 66.dp)
                                            .horizontalMiniPlayerFrame(progress).height(54.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        for (font in listOf(1f, 1.5f)) for (size in listOf(280.dp, 320.dp, 443.dp, 600.dp, 851.dp, 1200.dp)) for (collapsed in listOf(false, true)) {
            compose.runOnUiThread { width = size; scale = font; compact = collapsed }
            compose.waitForIdle()
            val frame = compose.onNodeWithTag("sizing_nav_frame").fetchSemanticsNode().boundsInRoot
            val mini = compose.onNodeWithTag("bottom_mini_player_surface").fetchSemanticsNode().boundsInRoot
            val left = compose.onNodeWithTag("sizing_left_button").fetchSemanticsNode().boundsInRoot
            val right = compose.onNodeWithTag("sizing_right_button").fetchSemanticsNode().boundsInRoot
            val density = left.width / 54f
            assertEquals("Playback remains centred at $size/$font", frame.center.x, mini.center.x, 1f)
            if (collapsed) {
                assertTrue("Left button must not cover playback at $size/$font", mini.left >= left.right + 9f * density)
                assertTrue("Search must not cover playback at $size/$font", mini.right <= right.left - 9f * density)
            } else {
                assertEquals(frame.width, mini.width, 1f)
                assertTrue("Expanded playback must stay above navigation", mini.bottom <= left.top - 11f * density)
            }
            val title = compose.onNodeWithText("日落之前", useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val artist = compose.onNodeWithText(sizingArtist, useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("Large text must stay inside playback", title.top >= mini.top - 1f && artist.bottom <= mini.bottom + 1f)
            for (text in listOf("日落之前", sizingArtist)) {
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                compose.onNodeWithText(text, useUnmergedTree = true)
                    .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
                val layout = layouts.single()
                // didOverflowHeight also includes intentional maxLines/ellipsis truncation.
                if (layout.size.height + 1f < layout.multiParagraph.height) capture("miniplayer-overflow")
                assertTrue("Visible line must fit vertically at $size/$font/$collapsed: $text, allocated=${layout.size.height}, paragraph=${layout.multiParagraph.height}, mini=$mini, title=$title, artist=$artist",
                    layout.size.height + 1f >= layout.multiParagraph.height)
            }
            val play = compose.onNodeWithContentDescription("播放", useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(play.left >= mini.left && play.right <= mini.right + 1f)
            if (collapsed && font == 1.5f && size in listOf(320.dp, 851.dp, 1200.dp)) capture("miniplayer-${size.value.toInt()}-font150")
        }
    }

    @Test fun playerTransportStaysCenteredWithLargeQualityLabels() {
        var width by mutableStateOf(320.dp)
        var fontScale by mutableFloatStateOf(1f)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val density = androidx.compose.ui.platform.LocalDensity.current
                CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, fontScale)) {
                    MaterialTheme {
                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Box(Modifier.width(width).height(250.dp).testTag("player_controls_fixture")) {
                                AppleMusicPlaybackControls(
                                    currentPositionMs = 6000,
                                    durationMs = 220000,
                                    isPlaying = false,
                                    currentAudioQuality = com.example.seteasecloudmusic.core.settings.OnlineAudioQuality.HIRES,
                                    onPlayPause = {},
                                    onNext = {},
                                    onPrevious = {},
                                    onSeekTo = {}
                                )
                            }
                        }
                    }
                }
            }
        }
        listOf(1f, 1.5f).forEach { scale ->
            listOf(320.dp, 350.dp, 420.dp).forEach { size ->
                compose.runOnUiThread { width = size; fontScale = scale }
                compose.waitForIdle()
                val container = compose.onNodeWithTag("player_controls_fixture").fetchSemanticsNode().boundsInRoot
                val play = compose.onNodeWithContentDescription("播放").fetchSemanticsNode().boundsInRoot
                assertEquals("Play must stay centered at $size / font scale $scale", container.center.x, play.center.x, 1f)
                compose.onNodeWithText("Hi-Res", substring = true).assertIsDisplayed()
            }
        }
    }

    @Test fun expandedGlassDockSupportsDragAndSearch() {
        var selected by mutableIntStateOf(0)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val background = rememberLayerBackdrop { drawContent() }
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().background(Color(0xFFF0E8F5)).layerBackdrop(background))
                    MusicNavigationRail(selected, { selected = it }, background)
                }
            }
        }
        compose.onNodeWithTag("glass_dock_0").assertIsSelected()
        val start = compose.onNodeWithTag("glass_dock_0").fetchSemanticsNode().boundsInRoot.center
        val end = compose.onNodeWithTag("glass_dock_2").fetchSemanticsNode().boundsInRoot.center
        val origin = compose.onNodeWithTag("expanded_glass_dock").fetchSemanticsNode().boundsInRoot.topLeft
        compose.onNodeWithTag("expanded_glass_dock").performTouchInput { swipe(start - origin, end - origin, 450) }
        compose.onNodeWithTag("glass_dock_2").assertIsSelected()
        compose.onNodeWithTag("glass_dock_search").performClick().assertIsSelected()
        compose.onNodeWithTag("glass_dock_1").performClick().assertIsSelected()
    }

    @Test fun shortSideDockKeepsAllDestinationsReachable() {
        var selected by mutableIntStateOf(0)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                CompositionLocalProvider(LocalAdaptiveWindow provides AdaptiveWindowInfo(800.dp, 420.dp, isFoldable = true)) {
                    val background = rememberLayerBackdrop { drawContent() }
                    Box(Modifier.width(84.dp).height(350.dp)) {
                        MusicNavigationRail(selected, { selected = it }, background)
                    }
                }
            }
        }
        compose.onNodeWithTag("glass_dock_overflow").assertIsDisplayed().performClick()
        compose.onNodeWithText("我的").performClick()
        compose.runOnIdle { assertEquals(2, selected) }
        compose.onNodeWithTag("glass_dock_search").performClick().assertIsSelected()
        compose.onNodeWithTag("glass_dock_overflow").performClick()
        compose.onNodeWithText("首页").performClick()
        compose.runOnIdle { assertEquals(0, selected) }
    }

    @Test fun splitWindowKeepsControlsOnItsLeftOuterEdge() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                CompositionLocalProvider(LocalAdaptiveWindow provides AdaptiveWindowInfo(500.dp, 800.dp,
                    isFoldable = true, sideControlsAtStart = true)) {
                    AppNavigation()
                }
            }
        }
        val dock = compose.onNodeWithTag("expanded_glass_dock").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithTag("chrome_content").fetchSemanticsNode().boundsInRoot
        assertTrue("Left split controls must leave the app content clear", dock.right <= content.left + 1f)
        compose.onNodeWithTag("glass_dock_search").performClick()
        compose.onNode(hasSetTextAction()).assertExists()
    }

    @Test fun navigationAndSearchSurviveActivityRecreation() {
        compose.onAllNodes(hasText("搜索") or hasContentDescription("搜索")).onFirst().performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Luna")
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).assertTextEquals("Luna")
    }

    @Test fun feedKeepsVisibleSectionWhenColumnsChange() {
        var width by mutableStateOf(350.dp)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.width(width).fillMaxHeight()) {
                        AdaptiveSectionColumn(Modifier.fillMaxSize()) {
                            repeat(30) { index ->
                                item(key = "section_$index") {
                                    androidx.compose.material3.Text("Section $index", Modifier.fillMaxWidth().height(180.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(13)
        compose.onNodeWithText("Section 13").assertIsDisplayed()
        compose.runOnUiThread { width = 800.dp }
        compose.waitForIdle()
        compose.onNodeWithText("Section 13").assertIsDisplayed()
    }

    @Test fun secondaryFeedKeepsItsAnchorAfterFoldingEnds() {
        var folded by mutableStateOf(true)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val hinge = with(density) { Rect(390.dp.toPx(), 0f, 410.dp.toPx(), 900.dp.toPx()) }
                CompositionLocalProvider(LocalAdaptiveWindow provides AdaptiveWindowInfo(800.dp, 900.dp, if (folded) hinge else null, false)) {
                    AdaptiveSectionColumn(Modifier.fillMaxSize()) {
                        repeat(30) { index ->
                            item(key = "section_$index") {
                                androidx.compose.material3.Text("Section $index", Modifier.fillMaxWidth().height(180.dp))
                            }
                        }
                    }
                }
            }
        }
        compose.onAllNodes(hasScrollToIndexAction()).onLast().performScrollToIndex(8)
        compose.waitForIdle()
        compose.onNodeWithText("Section 17").assertIsDisplayed()
        compose.runOnUiThread { folded = false }
        compose.waitForIdle()
        compose.onNodeWithText("Section 17").assertIsDisplayed()
    }

    @Test fun separatingFoldKeepsBothPanesClearOfHinge() {
        var horizontal by mutableStateOf(false)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val density = androidx.compose.ui.platform.LocalDensity.current.density
                    val fold = if (horizontal) Rect(0f, maxHeight.value * density / 2 - 10, maxWidth.value * density, maxHeight.value * density / 2 + 10)
                        else Rect(maxWidth.value * density / 2 - 10, 0f, maxWidth.value * density / 2 + 10, maxHeight.value * density)
                    CompositionLocalProvider(LocalAdaptiveWindow provides AdaptiveWindowInfo(maxWidth, maxHeight, fold, horizontal)) {
                        AdaptiveTwoPane(Modifier.fillMaxSize(), primary = {}, secondary = {})
                    }
                }
            }
        }
        compose.waitForIdle()
        var primary = compose.onNodeWithTag("adaptive_primary").fetchSemanticsNode().boundsInRoot
        var secondary = compose.onNodeWithTag("adaptive_secondary").fetchSemanticsNode().boundsInRoot
        assertTrue("Book panes must leave the hinge clear", primary.right + 20 <= secondary.left)
        compose.runOnUiThread { horizontal = true }
        compose.waitForIdle()
        primary = compose.onNodeWithTag("adaptive_primary").fetchSemanticsNode().boundsInRoot
        secondary = compose.onNodeWithTag("adaptive_secondary").fetchSemanticsNode().boundsInRoot
        assertTrue("Tabletop panes must leave the hinge clear", primary.bottom + 20 <= secondary.top)
        assertTrue(primary.height > 0 && secondary.height > 0)
    }

    private fun assertAwayFromFold(tag: String) {
        val info = windowInfo ?: return
        val fold = info.separatingFold ?: return
        val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
        assertTrue("$tag must avoid the fold: $bounds / $fold",
            if (info.horizontalFold) bounds.bottom <= fold.top + 1f || bounds.top >= fold.bottom - 1f
            else bounds.right <= fold.left + 1f || bounds.left >= fold.right - 1f)
    }

    private fun capture(name: String) {
        // Android dialogs use Choreographer, independently of Compose's virtual clock.
        val frames = java.util.concurrent.CountDownLatch(1)
        compose.runOnUiThread {
            val choreographer = android.view.Choreographer.getInstance()
            var remaining = 18
            val callback = object : android.view.Choreographer.FrameCallback {
                override fun doFrame(time: Long) {
                    if (--remaining <= 0) frames.countDown() else choreographer.postFrameCallback(this)
                }
            }
            choreographer.postFrameCallback(callback)
        }
        assertTrue("Android rendering frames must advance", frames.await(5, java.util.concurrent.TimeUnit.SECONDS))
        val context = compose.activity
        val output = File(context.getExternalFilesDir(null), "foldable-qa").apply { mkdirs() }
        File(output, "$name-window.txt").writeText("$windowInfo\nfontScale=$currentFontScale")
        val label = InstrumentationRegistry.getArguments().getString("layoutLabel")
        val info = requireNotNull(windowInfo)
        when (label) {
            "outer", "outer-large-font" -> {
                assertTrue("Outer panel must be portrait: $info", info.width < 600.dp && info.height > info.width)
                assertTrue("Foldable cover must expose side controls on a fresh activity", info.isFoldable && info.useNavigationRail)
            }
            "outer-landscape" -> assertTrue("Outer landscape must actually be short and wide: $info", info.height < 600.dp && info.width > info.height)
            "landscape" -> assertTrue("Inner landscape must be wide: $info", info.width > info.height && info.height >= 600.dp)
            "book" -> assertTrue("Book posture must expose a vertical separating fold: $info", info.isBook)
            "tabletop" -> assertTrue("Tabletop must expose a horizontal separating fold: $info", info.isTabletop)
            "resized" -> assertTrue("Resized window must be compact: $info", info.width < 600.dp)
        }
        if (label?.endsWith("large-font") == true) assertEquals(1.5f, currentFontScale, 0.01f)
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        requireNotNull(screenshot).useBitmap { bitmap -> File(output, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }

    private inline fun Bitmap.useBitmap(block: (Bitmap) -> Unit) { try { block(this) } finally { recycle() } }

    private fun fixtures(): List<Track> {
        val directory = File(compose.activity.cacheDir, "adaptive-fixtures").apply { mkdirs() }
        val colors = listOf(0xFF7654D9.toInt(), 0xFFDA755B.toInt(), 0xFF3A9E9B.toInt(), 0xFF4770B6.toInt())
        val covers = colors.mapIndexed { index, color ->
            val bitmap = Bitmap.createBitmap(480, 480, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, 480f, 480f, color, 0xFF161725.toInt(), Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, 480f, 480f, paint)
            paint.shader = null; paint.color = 0x55FFFFFF
            canvas.drawCircle(340f, 130f, 170f, paint)
            paint.color = 0xFFFEF4D8.toInt(); paint.textSize = 42f
            canvas.drawText(listOf("AFTER HOURS", "SLOW DAYS", "IN BLOOM", "BLUE ROOM")[index], 32f, 390f, paint)
            val file = File(directory, "cover-$index.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
            file.toURI().toString()
        }
        return (0 until 24).map { index -> Track(index.toLong() + 1, listOf("日落之前", "在城市的另一边", "晚风与星光", "留给明天的歌")[index % 4],
            listOf(Artist(1, "Luna Ensemble")), Album(1, "After Hours"), coverUrl = covers[index % covers.size], durationMs = 220000) }
    }
}

@Composable
private fun FixturePage(page: String, tracks: List<Track>) {
    val backdrop = rememberLayerBackdrop { drawContent() }
    val bottom = 90.dp
    val playlist = PlaylistDetail(1, "日落之后 · 私人音乐时光", tracks[0].coverUrl, "适合独处时循环播放的声音", tracks.size, 12000, "Luna", tracks)
    val userPlaylists = (1L..8L).map { UserPlaylist(it, listOf("夜晚漫游", "微风与海", "工作时听", "周末慢生活")[(it % 4).toInt()], tracks[(it % 4).toInt()].coverUrl, 24, 1200) }
    CompositionLocalProvider(LocalContentBottomPadding provides bottom) {
        when (page) {
            "chrome" -> AppNavigation()
            "native_music" -> {
                PlayerSystemBars()
                val info = LocalAdaptiveWindow.current
                if (info.width >= 600.dp || info.separatingFold != null) {
                    val lyrics = com.example.seteasecloudmusic.feature.player.util.LyricParser.parseLyricResponse(Lyrics(lrc = LyricContent(
                        lyric = "[00:00.00]在城市的另一边\n[00:05.00]听见晚风轻轻唱\n[00:10.00]把平凡的瞬间写成旋律")))
                    val background = rememberLayerBackdrop { drawContent() }
                    var showQueue by remember { mutableStateOf(false) }
                    var showQuality by remember { mutableStateOf(false) }
                    val state = PlaybackState(PlayerStatus.PAUSED, tracks[0], currentPositionMs = 6000,
                        durationMs = 220000, queueTracks = tracks)
                    val quality = com.example.seteasecloudmusic.core.settings.OnlineAudioQuality.HIRES
                    Box(Modifier.fillMaxSize()) {
                        PlayerBackdrop(tracks[0], Modifier.layerBackdrop(background))
                        CompositionLocalProvider(LocalPlayerBackdrop provides background, LocalGlassBackdrop provides background) {
                            ExpandedNativePlayerContent(state, lyrics, quality,
                                {}, remember { mutableStateOf(1f) }, {}, {}, {}, {}, {},
                                { showQuality = true }, { showQueue = true })
                            if (showQueue) PlaylistQueueSheet(state, {}, {}, {}, {}, { showQueue = false })
                            if (showQuality) AudioQualitySelectionSheet(quality, {}, { showQuality = false })
                        }
                    }
                } else NativePlayerScreen(hiltViewModel(), {}, remember { mutableStateOf(1f) }, {})
            }
            "home" -> HomeScreenContent(HomeUiState(tracks = tracks, favoriteTracks = tracks, radarTracks = tracks), 0.dp, bottom,
                displayName = "Luna", onTrackClick = {}, onFavoriteTrackClick = {}, onCardPlayClick = {}, onRetryClick = {}, onRefreshClick = {}, onPosterWallClick = { _, _ -> })
            "radio", "radio_detail" -> RadioScreenContent(RadioUiState(
                toplists = userPlaylists.map { RadioToplistItem(it.id, it.name, coverUrl = it.coverUrl) },
                radarPlaylists = userPlaylists.map { RadioRadarItem(it.id, it.name, "为你精选", it.coverUrl) },
                recommendedSingers = userPlaylists.map { RadioArtistItem(it.id, "Luna Ensemble", it.coverUrl) },
                recommendedPlaylists = userPlaylists.map { RadioPlaylistItem(it.id, it.name, it.coverUrl) },
                activePlaylistDetail = if (page == "radio_detail") playlist else null),
                0.dp, bottom, displayName = "Luna", onOpenPlaylistDetail = { _, _, _ -> }, onClosePlaylistDetail = {},
                onPlayTrackInPlaylist = { _, _ -> }, onPlayAllTracks = {}, onPlayToplist = {}, onPlayRadar = {}, onPlayRoam = {}, onPlayPlaylist = {}, onPlayArtistHot = {}, onPlayTrend = {}, onPlayMood = {}, onPlayFollowedNewSong = {}, onRetryClick = {})
            "mine", "mine_detail", "local" -> MineScreen(MineUiState(authSession = AuthSession(1, "Luna", isLoggedIn = true),
                createdPlaylists = userPlaylists, favoritedPlaylists = userPlaylists, likedPlaylist = userPlaylists.first(), localSongs = tracks,
                selectedTab = if (page == "local") MinePlaylistTab.LOCAL else MinePlaylistTab.CREATED,
                activePlaylistDetail = if (page == "mine_detail") playlist else null),
                0.dp, bottom, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {})
            "playlist" -> PlaylistDetailScreen(playlist, backdrop, {}, {}, {})
            "daily" -> DailyRecommendDetailScreen(tracks, "每日推荐", {}, {})
            "artist" -> ArtistDetailScreen(ArtistDetailUiState(artistId = 1, artistName = "Luna Ensemble", artistCoverUrl = tracks[0].coverUrl,
                songs = tracks, albums = userPlaylists.map { ArtistAlbum(it.id, it.name, it.coverUrl, 2026, 12) },
                similarArtists = userPlaylists.map { ArtistSummary(it.id, it.name, it.coverUrl) },
                description = ArtistDescription("用音乐记录日常，让每一个平凡的时刻拥有自己的旋律。", emptyList())), {}, {}, {}, {}, {}, {})
            "search" -> SearchScreenContent(SearchUiState(query = "Luna", tracks = tracks,
                suggestions = SearchSuggestions(artists = userPlaylists.map { ArtistSuggestion(it.id, it.name, it.coverUrl) })), 0.dp, bottom, {}, { _, _, _ -> }, {}, {})
            "discover" -> DiscoverScreen(DiscoverUiState(newsongs = tracks,
                personalizedPlaylists = userPlaylists.map { DiscoverPlaylist(it.id, it.name, it.coverUrl) },
                hotPlaylists = userPlaylists.map { DiscoverPlaylist(it.id, it.name, it.coverUrl) },
                toplists = userPlaylists.map { DiscoverToplist(it.id, it.name, it.coverUrl) }), 0.dp, bottom, {}, {}, {}, {})
            "native" -> { PlayerSystemBars(); NativePlayerScreen(hiltViewModel(), {}, remember { mutableStateOf(1f) }, {}) }
            "lyrics" -> {
                PlayerSystemBars()
                val lines = listOf("在城市的另一边，听见晚风轻轻唱", "把每个平凡的瞬间，写成属于我们的旋律", "Every little moment becomes a melody", "日落之前，让音乐陪你走远一点").mapIndexed { index, text ->
                    com.example.seteasecloudmusic.feature.player.domain.model.LyricLine(
                        listOf(com.example.seteasecloudmusic.feature.player.domain.model.LyricWord(text, index * 5000, (index + 1) * 5000)),
                        startTime = index * 5000, endTime = (index + 1) * 5000)
                }
                Box(Modifier.fillMaxSize().background(Color(0xFF19191F)).systemBarsPadding(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    LyricsColumn(com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics(lines), 1, 6000, false,
                        modifier = Modifier.widthIn(max = 440.dp).fillMaxSize())
                }
            }
            "quality" -> AudioQualitySelectionSheet(com.example.seteasecloudmusic.core.settings.OnlineAudioQuality.STANDARD, {}, {})
            "queue" -> PlaylistQueueSheet(PlaybackState(PlayerStatus.PAUSED, tracks[0], queueTracks = tracks, currentQueueIndex = 0), {}, {}, {}, {}, {})
            "qr" -> AdaptiveOverlayPane(Modifier.fillMaxSize().systemBarsPadding()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Column(Modifier.widthIn(max = 560.dp).fillMaxSize().padding(20.dp)) {
                        val image = android.util.Base64.encodeToString(File(java.net.URI(tracks[0].coverUrl!!)).readBytes(), android.util.Base64.NO_WRAP)
                        QrPanel(Color(0xFFFA233B), Color.Gray, "打开网易云音乐，扫描二维码登录", image, false, {}, {})
                    }
                }
            }
            "offline" -> com.example.seteasecloudmusic.core.ui.components.Ios26NetworkOfflineDialog(true, {}, {})
            "account", "settings", "captcha", "account_detail" -> {
                val vm: AuthViewModel = hiltViewModel()
                LaunchedEffect(page) { if (page == "settings") vm.onAccountDetailsOpened() else if (page == "captcha") vm.onCaptchaPanelOpened() else if (page == "account_detail") vm.onAccountDetailsDestinationOpened(AccountDetailsDestination.SUBSCRIPTIONS) else vm.onBackToMethods() }
                AdaptiveOverlayPane(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        AccountLoginSheetContent({}, Modifier.widthIn(max = 560.dp).fillMaxSize(), viewModel = vm)
                    }
                }
            }
        }
    }
}
