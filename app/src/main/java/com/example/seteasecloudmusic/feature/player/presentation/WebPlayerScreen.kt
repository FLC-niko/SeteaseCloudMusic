package com.example.seteasecloudmusic.feature.player.presentation

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.seteasecloudmusic.core.player.MusicPlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPlayerScreen(
    musicPlayerController: MusicPlayerController,
    ttmlProvider: (suspend (songId: String) -> String?)? = null
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Web 是否已 ready，避免页面没加载完就注入失败
    val bridgeState = remember { object { var webReady: Boolean = false } }

    fun dispatchToWeb(type: String, payload: JSONObject) {
        val webView = webViewRef ?: return
        val message = JSONObject().apply {
            put("type", type)
            put("payload", payload)
        }
        // 使用 URLEncoder 传递 UTF-8 字符串，解决 Base64 在 JS atob 解码时破坏多字节中文字符的问题
        val encoded = java.net.URLEncoder.encode(message.toString(), "UTF-8").replace("+", "%20")
        val js = "window.dispatchEvent(new CustomEvent('scm-native-message',{detail: JSON.parse(decodeURIComponent('$encoded'))}));"
        webView.post {
            webView.evaluateJavascript(js, null)
        }
    }

    fun dispatchPlaybackNow() {
        val st = musicPlayerController.playbackState.value
        dispatchToWeb(
            "SET_PLAYBACK",
            JSONObject().apply {
                put("currentTimeMs", st.currentPositionMs)
                put("durationMs", st.durationMs)
                put("playing", st.status.name == "PLAYING")
            }
        )
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView.setWebContentsDebuggingEnabled(true)
            WebView(context).apply {
            webViewRef = this
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            // 👇👇👇 加上这两行！强制 WebView 读取前端的 Viewport 视口配置
            settings.useWideViewPort = false
            settings.loadWithOverviewMode = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()

            addJavascriptInterface(
                AmlWebBridge { type, payload ->
                    when (type) {
                        "TOGGLE_PLAY" -> {
                            val state = musicPlayerController.playbackState.value.status.name
                            if (state == "PLAYING") musicPlayerController.pause() else musicPlayerController.resume()
                        }
                        "NEXT_TRACK" -> musicPlayerController.playNext()
                        "PREV_TRACK" -> musicPlayerController.playPrevious()
                        "SEEK_TO" -> {
                            val timeMs = payload?.optLong("timeMs")?.toInt() ?: 0
                            musicPlayerController.seekTo(timeMs)
                        }
                        "WEB_READY" -> {
                            bridgeState.webReady = true
                            // ready 后先把当前播放态推过去
                            dispatchPlaybackNow()
                            // track 推送交由底下的 flow 循环自动解除挂起去发送
                        }
                    }
                },
                "SCMBridge"
            )

            loadUrl("http://192.168.1.184:5173")
        }
        }
    )

    DisposableEffect(Unit) {
        val scope = CoroutineScope(Dispatchers.Main)
        var trackJob: Job? = null

        // 持续推播放状态（只在 webReady 后推）
        val playbackJob = scope.launch {
            musicPlayerController.playbackState.collectLatest { st ->
                if (!bridgeState.webReady) return@collectLatest
                dispatchToWeb(
                    "SET_PLAYBACK",
                    JSONObject().apply {
                        put("currentTimeMs", st.currentPositionMs)
                        put("durationMs", st.durationMs)
                        put("playing", st.status.name == "PLAYING")
                    }
                )
            }
        }

        // 监听 currentTrack 变化，变化时推 SET_TRACK (解决因为播放进度更新导致的 collectLatest 被反复取消的问题)
        trackJob = scope.launch {
            musicPlayerController.playbackState
                .map { it.currentTrack }
                .distinctUntilChanged() // 只有当 track 本身变化时才触发
                .collectLatest { track ->
                    if (track == null) return@collectLatest

                    // 一直挂起等待，直到 WebView 页面发来 WEB_READY
                    while(!bridgeState.webReady) {
                        kotlinx.coroutines.delay(100)
                    }

                    val trackId = track.id.toString()
                    val lrcOrTtml = ttmlProvider?.invoke(trackId) ?: ""
                    dispatchToWeb(
                        "SET_TRACK",
                        JSONObject().apply {
                            put("id", trackId)
                            put("title", track.title)
                            put("artist", track.artists.joinToString(" / ") { it.name })
                            put("coverUrl", track.coverUrl ?: "")
                            put("lrc", lrcOrTtml)
                        }
                    )
                }
        }

        onDispose {
            playbackJob.cancel()
            trackJob?.cancel()
            webViewRef?.removeJavascriptInterface("SCMBridge")
            webViewRef?.destroy()
        }
    }
}