package com.example.seteasecloudmusic.core.player

import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.seteasecloudmusic.core.cache.PlaybackCacheManager
import com.example.seteasecloudmusic.core.cache.SavedPlaybackState
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.search.domain.PrepareTrackForPlaybackUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class PlayerStatus { IDLE, BUFFERING, PLAYING, PAUSED, ENDED, ERROR }

data class PlaybackState(
    val status: PlayerStatus = PlayerStatus.IDLE,
    val currentTrack: Track? = null,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val errorMessage: String? = null,
    val queueTracks: List<Track> = emptyList(),
    val currentQueueIndex: Int = -1
)

@Singleton
class MusicPlayerController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val prepareTrackForPlaybackUseCase: PrepareTrackForPlaybackUseCase,
    private val playbackCacheManager: PlaybackCacheManager
) {
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    // 控制器自己的协程域：用于异步连接服务、拉 URL、更新状态
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)

    // Media3 控制端（连接到 MusicService 的 MediaSession）
    private var controller: MediaController? = null

    // 进度轮询任务：每 500ms 同步一次 position/duration 到 UI
    private var progressJob: Job? = null
    private var playJob: Job? = null
    private var prefetchJob: Job? = null
    private var latestPlayRequestId: Long = 0L
    private var lastPersistTimeMs: Long = 0L

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        // 冷启动自动恢复上次持久化的播放状态、播放列表与进度
        val saved = playbackCacheManager.getSavedPlaybackState()
        if (saved != null && saved.queueTracks.isNotEmpty() && saved.currentQueueIndex in saved.queueTracks.indices) {
            val track = saved.queueTracks.getOrNull(saved.currentQueueIndex)
            _playbackState.update {
                it.copy(
                    status = PlayerStatus.PAUSED,
                    currentTrack = track,
                    currentPositionMs = saved.currentPositionMs,
                    durationMs = saved.durationMs,
                    queueTracks = saved.queueTracks,
                    currentQueueIndex = saved.currentQueueIndex
                )
            }
        }
    }

    // 监听 Media3 播放器状态变化，统一映射到你的 PlaybackState
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val c = controller ?: return
            val mapped = when (playbackState) {
                Player.STATE_IDLE -> PlayerStatus.IDLE
                Player.STATE_BUFFERING -> PlayerStatus.BUFFERING
                Player.STATE_READY -> if (c.isPlaying) PlayerStatus.PLAYING else PlayerStatus.PAUSED
                Player.STATE_ENDED -> PlayerStatus.ENDED
                else -> PlayerStatus.ERROR
            }

            if (mapped == PlayerStatus.ENDED) {
                if (playNextInternal()) {
                    return
                }
            }

            _playbackState.update { current ->
                val pos = if (mapped == PlayerStatus.BUFFERING && current.currentPositionMs > 0 && c.currentPosition <= 0L) {
                    current.currentPositionMs
                } else {
                    c.currentPosition.toInt().coerceAtLeast(0)
                }
                val dur = c.duration.takeIf { d -> d > 0 }?.toInt() ?: current.durationMs
                current.copy(
                    status = mapped,
                    currentPositionMs = pos,
                    durationMs = dur
                )
            }

            if (mapped == PlayerStatus.PLAYING) startProgressTicker() else stopProgressTicker()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update {
                it.copy(status = if (isPlaying) PlayerStatus.PLAYING else PlayerStatus.PAUSED)
            }
            if (isPlaying) {
                startProgressTicker()
            } else {
                stopProgressTicker()
                persistCurrentState()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            stopProgressTicker()
            _playbackState.update {
                it.copy(status = PlayerStatus.ERROR, errorMessage = error.message ?: "Playback error")
            }
        }
    }

    /** 在 ViewModel 初始化时调用：建立到 MusicService 的连接 */
    fun connect() {
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()

        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { c ->
                        controller = c
                        c.addListener(playerListener)
                    }
                    .onFailure { e ->
                        _playbackState.update {
                            it.copy(status = PlayerStatus.ERROR, errorMessage = e.message)
                        }
                    }
            },
            context.mainExecutor
        )
    }

    fun replaceQueueAndPlay(tracks: List<Track>, startIndex: Int = 0) {
        val snapshot = tracks.toList()
        if (snapshot.isEmpty()) {
            _playbackState.update {
                it.copy(status = PlayerStatus.ERROR, errorMessage = "Queue is empty")
            }
            return
        }
        if (startIndex !in snapshot.indices) {
            _playbackState.update {
                it.copy(
                    status = PlayerStatus.ERROR,
                    errorMessage = "Queue index out of bounds"
                )
            }
            return
        }

        _playbackState.update {
            it.copy(
                queueTracks = snapshot,
                currentQueueIndex = startIndex,
                currentPositionMs = 0,
                errorMessage = null
            )
        }

        playQueueIndex(startIndex, initialSeekMs = 0)
    }

    fun play(track: Track) {
        replaceQueueAndPlay(listOf(track), startIndex = 0)
    }

    fun playNext() {
        playNextInternal()
    }

    fun playPrevious() {
        val state = _playbackState.value
        val previousIndex = state.currentQueueIndex - 1
        if (previousIndex in state.queueTracks.indices) {
            playQueueIndex(previousIndex, initialSeekMs = 0)
        } else {
            seekTo(0)
        }
    }

    fun replayCurrent() {
        val state = _playbackState.value
        when {
            state.currentQueueIndex in state.queueTracks.indices -> {
                playQueueIndex(state.currentQueueIndex, initialSeekMs = state.currentPositionMs)
            }

            state.currentTrack != null -> {
                play(state.currentTrack)
            }
        }
    }

    fun pause() {
        controller?.pause()
        persistCurrentState()
    }

    fun resume() {
        val c = controller
        val state = _playbackState.value
        if (c != null && c.currentMediaItem != null) {
            c.play()
        } else if (state.currentQueueIndex in state.queueTracks.indices) {
            // 冷启动恢复播放：使用 setMediaItem(item, startPositionMs) 精确从上次记忆的进度开始无缝续播
            playQueueIndex(state.currentQueueIndex, initialSeekMs = state.currentPositionMs)
        } else {
            c?.play()
        }
    }

    fun stop() {
        controller?.stop()
        persistCurrentState()
    }

    fun seekTo(positionMs: Int) {
        controller?.seekTo(positionMs.toLong())
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
        persistCurrentState()
    }

    fun release() {
        persistCurrentState()
        stopProgressTicker()
        playJob?.cancel()
        playJob = null
        prefetchJob?.cancel()
        prefetchJob = null
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        scope.cancel()
    }

    private fun playQueueIndex(index: Int, initialSeekMs: Int = 0) {
        val queue = _playbackState.value.queueTracks
        if (index !in queue.indices) {
            _playbackState.update {
                it.copy(status = PlayerStatus.ERROR, errorMessage = "Queue index out of bounds")
            }
            return
        }

        val track = queue[index]
        playJob?.cancel()
        val requestId = nextPlayRequestId()

        playJob = scope.launch {
            _playbackState.update {
                it.copy(
                    status = PlayerStatus.BUFFERING,
                    currentTrack = track,
                    currentQueueIndex = index,
                    currentPositionMs = initialSeekMs,
                    errorMessage = null
                )
            }

            // 1. 获取当前曲目直链（命中缓存 0ms 即刻返回）
            val prepared = withContext(ioDispatcher) { prepareTrackForPlaybackUseCase(track) }
            if (isStaleRequest(requestId)) {
                return@launch
            }

            prepared
                .onSuccess { t ->
                    if (isStaleRequest(requestId)) {
                        return@onSuccess
                    }

                    val url = t.playableUrl
                    if (url.isNullOrBlank() || !t.isPlayable) {
                        _playbackState.update {
                            it.copy(status = PlayerStatus.ERROR, errorMessage = "Track is not playable")
                        }
                        return@onSuccess
                    }

                    val item = MediaItem.Builder()
                        .setMediaId(t.id.toString())
                        .setUri(url)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(t.title)
                                .setArtist(t.artists.joinToString(" / ") { it.name })
                                .setArtworkUri(t.coverUrl?.toUri())
                                .build()
                        )
                        .build()

                    controller?.apply {
                        if (initialSeekMs > 0) {
                            setMediaItem(item, initialSeekMs.toLong())
                        } else {
                            setMediaItem(item)
                        }
                        prepare()
                        play()
                    } ?: _playbackState.update {
                        it.copy(status = PlayerStatus.ERROR, errorMessage = "Controller not connected")
                    }

                    persistCurrentState()

                    // 2. 核心秒切优化：在后台静默预加载上一首和下一首的播放直链
                    prefetchNeighbors(index)
                }
                .onFailure { e ->
                    if (isStaleRequest(requestId)) {
                        return@onFailure
                    }
                    _playbackState.update {
                        it.copy(status = PlayerStatus.ERROR, errorMessage = e.message ?: "Unknown error")
                    }
                }
        }
    }

    /**
     * 智能预加载相邻曲目播放直链到内存缓存（实现秒切 0ms 核心）
     */
    private fun prefetchNeighbors(currentIndex: Int) {
        prefetchJob?.cancel()
        prefetchJob = scope.launch(ioDispatcher) {
            val queue = _playbackState.value.queueTracks
            if (queue.isEmpty()) return@launch

            // 优先预加载下一首
            val nextIndex = currentIndex + 1
            if (nextIndex in queue.indices) {
                runCatching { prepareTrackForPlaybackUseCase(queue[nextIndex]) }
            } else if (queue.isNotEmpty()) {
                // 循环到第一首
                runCatching { prepareTrackForPlaybackUseCase(queue[0]) }
            }

            // 预加载上一首
            val prevIndex = currentIndex - 1
            if (prevIndex in queue.indices) {
                runCatching { prepareTrackForPlaybackUseCase(queue[prevIndex]) }
            }
        }
    }

    private fun persistCurrentState() {
        val state = _playbackState.value
        if (state.queueTracks.isNotEmpty() && state.currentQueueIndex in state.queueTracks.indices) {
            playbackCacheManager.savePlaybackState(
                SavedPlaybackState(
                    queueTracks = state.queueTracks,
                    currentQueueIndex = state.currentQueueIndex,
                    currentPositionMs = state.currentPositionMs,
                    durationMs = state.durationMs
                )
            )
        }
    }

    private fun playNextInternal(): Boolean {
        val state = _playbackState.value
        val nextIndex = state.currentQueueIndex + 1
        if (nextIndex in state.queueTracks.indices) {
            playQueueIndex(nextIndex, initialSeekMs = 0)
            return true
        } else if (state.queueTracks.isNotEmpty()) {
            playQueueIndex(0, initialSeekMs = 0)
            return true
        }
        _playbackState.update {
            it.copy(status = PlayerStatus.ENDED, errorMessage = null)
        }
        return false
    }

    private fun nextPlayRequestId(): Long {
        latestPlayRequestId += 1L
        return latestPlayRequestId
    }

    private fun isStaleRequest(requestId: Long): Boolean = requestId != latestPlayRequestId

    private fun startProgressTicker() {
        stopProgressTicker()
        progressJob = scope.launch {
            while (isActive) {
                val c = controller ?: break
                val pos = c.currentPosition.toInt().coerceAtLeast(0)
                val dur = c.duration.takeIf { d -> d > 0 }?.toInt() ?: 0
                _playbackState.update {
                    it.copy(
                        currentPositionMs = pos,
                        durationMs = if (dur > 0) dur else it.durationMs
                    )
                }

                // 每隔 3 秒周期性同步进度到本地持久化存储
                val now = System.currentTimeMillis()
                if (now - lastPersistTimeMs > 3000L) {
                    lastPersistTimeMs = now
                    persistCurrentState()
                }

                delay(500L)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }
}