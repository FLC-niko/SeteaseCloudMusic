package com.example.seteasecloudmusic

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import java.util.concurrent.atomic.AtomicBoolean
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.activity.compose.setContent
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
import com.example.seteasecloudmusic.core.auth.AuthSession
import com.example.seteasecloudmusic.core.model.*
import com.example.seteasecloudmusic.core.player.PlaybackState
import com.example.seteasecloudmusic.core.player.PlayerStatus
import com.example.seteasecloudmusic.core.ui.adaptive.*
import com.example.seteasecloudmusic.feature.main.MainActivity
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
    private val pages = listOf("home", "radio", "radio_detail", "mine", "mine_detail", "local", "playlist", "daily", "artist", "search", "discover", "native", "lyrics", "web", "queue", "quality", "account", "settings", "captcha", "qr", "account_detail", "offline")

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
        val tracks = fixtures()
        var page by mutableStateOf(selectedPages.first())
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                ProvideAdaptiveWindow(activity) {
                    val info = LocalAdaptiveWindow.current
                    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
                    SideEffect { windowInfo = info; currentFontScale = fontScale }
                    MaterialTheme {
                        Box(Modifier.fillMaxSize().background(Color.White).testTag("fixture")) {
                            FixturePage(page, tracks)
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
            // Dialogs and WebView render on Android's frame clock as well as Compose's test clock.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            if (name == "web") waitForWebPlayer()
            capture("$label-$name")
            if (name == "settings") {
                compose.onNodeWithText("播放页风格").performClick()
                compose.onNodeWithText("选择播放页风格").assertExists()
                capture("$label-settings-style")
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

    private fun waitForWebPlayer() {
        val ready = AtomicBoolean(false)
        val lastProbe = java.util.concurrent.atomic.AtomicReference("not found")
        fun findWeb(view: View): WebView? {
            if (view is WebView && view.tag == "amll_player") return view
            if (view is ViewGroup) for (index in 0 until view.childCount) {
                findWeb(view.getChildAt(index))?.let { return it }
            }
            return null
        }
        compose.waitUntil(timeoutMillis = 20000) {
            compose.runOnUiThread {
                findWeb(compose.activity.window.decorView)?.let { web ->
                    web.evaluateJavascript("JSON.stringify({ready:document.readyState,url:location.href,width:innerWidth,height:innerHeight,body:document.body?.innerHTML?.slice(0,2000),playerHeight:document.querySelector('.player')?.clientHeight})") { lastProbe.set(it) }
                    web.evaluateJavascript("Boolean(window.__scmBridgeReady && document.querySelector('.player')?.clientHeight > 0)") { value ->
                        if (value == "true") {
                            web.postInvalidateOnAnimation()
                            ready.set(true)
                        }
                    }
                }
            }
            File(compose.activity.getExternalFilesDir(null), "web-probe.json").writeText(lastProbe.get())
            ready.get()
        }
        val injected = AtomicBoolean(false)
        var fixtureMessages = ""
        compose.runOnUiThread {
            findWeb(compose.activity.window.decorView)?.let { web ->
                val track = org.json.JSONObject().put("id", "fixture")
                    .put("title", "日落之前").put("artist", "Luna Ensemble")
                    .put("coverUrl", "data:image/svg+xml," + java.net.URLEncoder.encode("<svg xmlns='http://www.w3.org/2000/svg' width='480' height='480'><rect width='480' height='480' fill='#7654d9'/><circle cx='360' cy='140' r='170' fill='#ffffff55'/><text x='32' y='390' fill='white' font-size='38'>AFTER HOURS</text></svg>", "UTF-8").replace("+", "%20"))
                    .put("lrc", "[00:00.00]在城市的另一边\n[00:05.00]听见晚风轻轻唱\n[00:10.00]把平凡的瞬间写成旋律\n[00:15.00]Every moment becomes a melody\n[00:20.00]日落之前，让音乐陪你走远")
                val message = org.json.JSONObject().put("type", "SET_TRACK").put("payload", track)
                val playback = org.json.JSONObject().put("type", "SET_PLAYBACK").put("payload", org.json.JSONObject().put("playing", false).put("currentTimeMs", 6000).put("durationMs", 220000))
                fixtureMessages = "window.dispatchEvent(new CustomEvent('scm-native-message',{detail:$message}));window.dispatchEvent(new CustomEvent('scm-native-message',{detail:$playback}));"
                web.evaluateJavascript(fixtureMessages) { injected.set(true) }
            }
        }
        compose.waitUntil(10000) { injected.get() }
        val rendered = AtomicBoolean(false)
        compose.waitUntil(10000) {
            compose.runOnUiThread {
                findWeb(compose.activity.window.decorView)?.let { web ->
                    web.evaluateJavascript("if(document.querySelector('h1')?.textContent !== '日落之前'){" + fixtureMessages + "}", null)
                    web.evaluateJavascript("JSON.stringify({body:document.body.innerHTML.slice(0,4000),errors:window.__playerErrors})") { File(compose.activity.getExternalFilesDir(null), "web-render-probe.json").writeText(it) }
                    web.evaluateJavascript("Boolean(document.querySelector('.lyrics-viewport')?.textContent.includes('听见晚风轻轻唱') && document.querySelector('h1')?.textContent === '日落之前')") { rendered.set(it == "true") }
                }
            }
            rendered.get()
        }
        compose.runOnUiThread {
            findWeb(compose.activity.window.decorView)?.let { web ->
                val probe = "JSON.stringify({layout:document.querySelector('.player').dataset.layout,rects:[...document.querySelectorAll('.player,.lyrics-pane,.controls,.lyrics-viewport,.amll-lyric-player')].map(e=>({class:e.className,rect:e.getBoundingClientRect().toJSON(),height:getComputedStyle(e).height,flex:getComputedStyle(e).flex})),lines:[...document.querySelectorAll('.amll-lyric-player *')].slice(0,15).map(e=>({text:e.textContent,rect:e.getBoundingClientRect().toJSON(),style:e.getAttribute('style')}))})"
                val label = InstrumentationRegistry.getArguments().getString("layoutLabel") ?: "current"
                web.evaluateJavascript(probe) { File(compose.activity.getExternalFilesDir(null), "foldable-qa/$label-web-layout.json").writeText(it) }
            }
        }
    }

    private fun capture(name: String) {
        // Android dialogs and WebView use Choreographer, independently of Compose's virtual clock.
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
            "outer", "outer-large-font" -> assertTrue("Outer panel must be portrait: $info", info.width < 600.dp && info.height > info.width)
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
            "web" -> { PlayerSystemBars(); val vm: PlayerViewModel = hiltViewModel(); Box(Modifier.fillMaxSize().background(Color(0xFF141419)).systemBarsPadding()) { WebPlayerScreen(vm.controller) } }
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
