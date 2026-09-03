package com.example.seteasecloudmusic.feature.player.presentation

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject

private class WebPlayerSession {
    val webReady = MutableStateFlow(false)

    @Volatile
    var webView: WebView? = null

    @Volatile
    var disposed: Boolean = false
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPlayerScreen(
    musicPlayerController: MusicPlayerController,
    ttmlProvider: (suspend (songId: String) -> String?)? = null
) {
    val session = remember { WebPlayerSession() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentTtmlProvider by rememberUpdatedState(ttmlProvider)

    fun dispatchToWeb(type: String, payload: JSONObject) {
        if (session.disposed) return

        val webView = session.webView ?: return
        val message = JSONObject().apply {
            put("type", type)
            put("payload", payload)
        }
        // 使用 URLEncoder 传递 UTF-8 字符串，解决 Base64 在 JS atob 解码时破坏多字节中文字符的问题
        val encoded = java.net.URLEncoder.encode(message.toString(), "UTF-8").replace("+", "%20")
        val js = "window.dispatchEvent(new CustomEvent('scm-native-message',{detail: JSON.parse(decodeURIComponent('$encoded'))}));"

        // WebView 的页面可能在排队期间被销毁或替换，执行前再次校验实例归属。
        webView.post {
            if (session.disposed || session.webView !== webView) return@post
            runCatching { webView.evaluateJavascript(js, null) }
        }
    }

    fun dispatchPlaybackNow() {
        if (!session.webReady.value) return

        val state = musicPlayerController.playbackState.value
        dispatchToWeb(
            "SET_PLAYBACK",
            JSONObject().apply {
                put("currentTimeMs", state.currentPositionMs)
                put("durationMs", state.durationMs)
                put("playing", state.status.name == "PLAYING")
            }
        )
    }

    suspend fun dispatchTrackToWeb(track: Track) {
        val lyric = currentTtmlProvider?.invoke(track.id.toString()).orEmpty()
        if (session.disposed || !session.webReady.value) return

        dispatchToWeb(
            "SET_TRACK",
            JSONObject().apply {
                put("id", track.id.toString())
                put("title", track.title)
                put("artist", track.artists.joinToString(" / ") { it.name })
                put("coverUrl", track.coverUrl ?: "")
                put("lrc", lyric)
            }
        )
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView.setWebContentsDebuggingEnabled(true)
            WebView(context).apply {
                session.webView = this
                session.webReady.value = false
                setBackgroundColor(Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                // 强制 WebView 读取前端的 Viewport 视口配置
                settings.useWideViewPort = false
                settings.loadWithOverviewMode = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(
                        view: WebView?,
                        url: String?,
                        favicon: Bitmap?
                    ) {
                        // 页面刷新后必须重新等待 WEB_READY，避免把旧页面当成已就绪。
                        session.webReady.value = false
                        super.onPageStarted(view, url, favicon)
                    }
                }

                addJavascriptInterface(
                    AmlWebBridge { type, payload ->
                        // JavascriptInterface 回调不保证运行在主线程；所有 Controller/WebView 操作统一切回主线程。
                        scope.launch {
                            if (session.disposed) return@launch

                            when (type) {
                                "TOGGLE_PLAY" -> {
                                    val state = musicPlayerController.playbackState.value.status
                                    if (state.name == "PLAYING") {
                                        musicPlayerController.pause()
                                    } else {
                                        musicPlayerController.resume()
                                    }
                                }

                                "NEXT_TRACK" -> musicPlayerController.playNext()
                                "PREV_TRACK" -> musicPlayerController.playPrevious()
                                "SEEK_TO" -> {
                                    val timeMs = payload?.optLong("timeMs", 0L)?.toInt() ?: 0
                                    musicPlayerController.seekTo(timeMs)
                                }

                                "WEB_READY" -> {
                                    session.webReady.value = true
                                    // ready 后先把当前播放态推过去；当前曲目由下面的 combine 重新推送。
                                    dispatchPlaybackNow()
                                }
                            }
                        }
                    },
                    "SCMBridge"
                )

                // 开发调试：新电脑当前局域网 IP (亦可结合 adb reverse 使用 http://localhost:5173)
                loadUrl("http://192.168.1.113:5173")
            }
        },
        update = { webView ->
            // AndroidView 重组时保持当前实例引用，销毁流程仍能准确释放它。
            session.webView = webView
        }
    )

    DisposableEffect(musicPlayerController, lifecycleOwner) {
        val playbackJob: Job = scope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                musicPlayerController.playbackState.collectLatest { state ->
                    if (!session.webReady.value || session.disposed) return@collectLatest
                    dispatchToWeb(
                        "SET_PLAYBACK",
                        JSONObject().apply {
                            put("currentTimeMs", state.currentPositionMs)
                            put("durationMs", state.durationMs)
                            put("playing", state.status.name == "PLAYING")
                        }
                    )
                }
            }
        }

        val trackJob: Job = scope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    musicPlayerController.playbackState
                        .map { it.currentTrack }
                        .distinctUntilChanged(),
                    session.webReady
                ) { track, ready -> track to ready }
                    .collectLatest { (track, ready) ->
                        if (ready && track != null && !session.disposed) {
                            // track 变化或 WebView 再次 ready 都会触发，防止页面刷新后停留在空白歌词。
                            dispatchTrackToWeb(track)
                        }
                    }
            }
        }

        onDispose {
            session.disposed = true
            session.webReady.value = false
            playbackJob.cancel()
            trackJob.cancel()

            // 先断开引用和 JS bridge，再停止加载并销毁，避免排队回调重新触碰旧 WebView。
            session.webView?.let { webView ->
                session.webView = null
                webView.removeJavascriptInterface("SCMBridge")
                webView.stopLoading()
                webView.destroy()
            }
        }
    }
}
