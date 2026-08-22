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

enum class PlaybackMode {
    SEQUENTIAL,  // 顺序播放 / 列表循环
    SHUFFLE      // 随机播放
}

data class PlaybackState(
    val status: PlayerStatus = PlayerStatus.IDLE,
    val currentTrack: Track? = null,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val errorMessage: String? = null,
    val queueTracks: List<Track> = emptyList(),
    val currentQueueIndex: Int = -1,
    val playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL
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

    // 历史播放足迹栈：记录实际听过的曲目顺序，切上一首时按真实顺序依次倒序返回
    private val playbackHistory = ArrayDeque<Int>()
    private var pendingPlayItem: Pair<MediaItem, Int>? = null
    private var isConnecting: Boolean = false

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        // 1. 冷启动自动恢复上次持久化的播放状态、播放列表与进度
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
                    currentQueueIndex = saved.currentQueueIndex,
                    playbackMode = saved.playbackMode
                )
            }
        }

        // 2. 启动时后台即刻建立到 MusicService 的 MediaController 管道，保证冷启动起播 0ms 秒响应
        connect()
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

            if (playbackState == Player.STATE_ENDED) {
                playNextInternal()
                return
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
            _playbackState.update { current ->
                if (current.status == PlayerStatus.BUFFERING && !isPlaying) {
                    current // 正在缓冲新曲目时不要被旧播放器的 isPlaying=false 冲刷成 PAUSED
                } else {
                    current.copy(status = if (isPlaying) PlayerStatus.PLAYING else PlayerStatus.PAUSED)
                }
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

    /** 建立到 MusicService 的连接 */
    fun connect() {
        if (controller != null || isConnecting) return
        isConnecting = true
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()

        future.addListener(
            {
                isConnecting = false
                runCatching { future.get() }
                    .onSuccess { c ->
                        controller = c
                        c.addListener(playerListener)
                        // 若有在连接建立前触发的待播放项目，连接就绪瞬间立即起播！
                        pendingPlayItem?.let { (item, seekMs) ->
                            if (seekMs > 0) {
                                c.setMediaItem(item, seekMs.toLong())
                            } else {
                                c.setMediaItem(item)
                            }
                            c.prepare()
                            c.play()
                            pendingPlayItem = null
                        }
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

        playbackHistory.clear()

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
        val queue = state.queueTracks
        if (queue.isEmpty()) return

        // 1. 如果当前歌曲已播放超过 3 秒，点击上一首优先从头播放当前歌曲（符合主流音乐 App 习惯）
        if (state.currentPositionMs > 3000) {
            seekTo(0)
            return
        }

        // 2. 优先从历史足迹栈中弹出最近听过的曲目返回（实现随机模式下上一首按听歌顺序逐首回退）
        if (playbackHistory.isNotEmpty()) {
            val prevIndex = playbackHistory.removeLast()
            if (prevIndex in queue.indices && prevIndex != state.currentQueueIndex) {
                playQueueIndex(prevIndex, initialSeekMs = 0)
                return
            }
        }

        // 3. 若无历史记录，顺序模式下取前一首，随机模式下回到当前歌曲开头
        val prevIndex = when (state.playbackMode) {
            PlaybackMode.SHUFFLE -> {
                seekTo(0)
                return
            }
            PlaybackMode.SEQUENTIAL -> {
                val idx = state.currentQueueIndex - 1
                if (idx < 0) queue.size - 1 else idx
            }
        }
        playQueueIndex(prevIndex, initialSeekMs = 0)
    }

    fun togglePlaybackMode() {
        val nextMode = when (_playbackState.value.playbackMode) {
            PlaybackMode.SEQUENTIAL -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.SEQUENTIAL
        }
        _playbackState.update { it.copy(playbackMode = nextMode) }
        prefetchNeighbors(_playbackState.value.currentQueueIndex)
        persistCurrentState()
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        _playbackState.update { it.copy(playbackMode = mode) }
        prefetchNeighbors(_playbackState.value.currentQueueIndex)
        persistCurrentState()
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

    private fun buildMediaItem(track: Track, url: String): MediaItem {
        val artistName = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" }
        val albumName = track.album?.title ?: track.title
        val coverUri = track.coverUrl?.toUri()

        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setDisplayTitle(track.title)
                    .setArtist(artistName)
                    .setSubtitle(artistName)
                    .setDescription(artistName)
                    .setAlbumTitle(albumName)
                    .setArtworkUri(coverUri)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()
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

            // 0. 若曲目已带有可用直链（预加载命中），立即 0ms 起播，无需等待任何网络！
            val directUrl = track.playableUrl
            if (!directUrl.isNullOrBlank() && track.isPlayable) {
                val item = buildMediaItem(track, directUrl)

                val c = controller
                if (c != null) {
                    if (initialSeekMs > 0) {
                        c.setMediaItem(item, initialSeekMs.toLong())
                    } else {
                        c.setMediaItem(item)
                    }
                    c.prepare()
                    c.play()
                } else {
                    pendingPlayItem = item to initialSeekMs
                    connect()
                }

                persistCurrentState()
                prefetchNeighbors(index)
                return@launch
            }

            // 1. 获取当前曲目直链（命中内存缓存 0ms 即刻返回）
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

                    val item = buildMediaItem(t, url)

                    val ctrl = controller
                    if (ctrl != null) {
                        if (initialSeekMs > 0) {
                            ctrl.setMediaItem(item, initialSeekMs.toLong())
                        } else {
                            ctrl.setMediaItem(item)
                        }
                        ctrl.prepare()
                        ctrl.play()
                    } else {
                        pendingPlayItem = item to initialSeekMs
                        connect()
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
            val state = _playbackState.value
            val queue = state.queueTracks
            if (queue.isEmpty()) return@launch

            when (state.playbackMode) {
                PlaybackMode.SEQUENTIAL -> {
                    // 优先预加载下一首
                    val nextIndex = (currentIndex + 1) % queue.size
                    runCatching { prepareTrackForPlaybackUseCase(queue[nextIndex]) }

                    // 预加载上一首
                    val prevIndex = if (currentIndex - 1 < 0) queue.size - 1 else currentIndex - 1
                    runCatching { prepareTrackForPlaybackUseCase(queue[prevIndex]) }
                }
                PlaybackMode.SHUFFLE -> {
                    if (queue.size > 1) {
                        val candidates = queue.indices.filter { it != currentIndex }
                        val nextRandom = candidates.random()
                        runCatching { prepareTrackForPlaybackUseCase(queue[nextRandom]) }
                    }
                    if (playbackHistory.isNotEmpty()) {
                        val lastHistoryIndex = playbackHistory.last()
                        if (lastHistoryIndex in queue.indices) {
                            runCatching { prepareTrackForPlaybackUseCase(queue[lastHistoryIndex]) }
                        }
                    }
                }
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
                    durationMs = state.durationMs,
                    playbackMode = state.playbackMode
                )
            )
        }
    }

    private fun playNextInternal(): Boolean {
        val state = _playbackState.value
        val queue = state.queueTracks
        if (queue.isEmpty()) {
            _playbackState.update {
                it.copy(status = PlayerStatus.ENDED, errorMessage = null)
            }
            return false
        }

        // 记录当前曲目到历史足迹栈，供上一首逐一回溯
        if (state.currentQueueIndex in queue.indices) {
            playbackHistory.addLast(state.currentQueueIndex)
            if (playbackHistory.size > 50) {
                playbackHistory.removeFirst()
            }
        }

        val nextIndex = when (state.playbackMode) {
            PlaybackMode.SHUFFLE -> {
                if (queue.size > 1) {
                    val candidates = queue.indices.filter { it != state.currentQueueIndex }
                    // 优先选择不在最近历史中的候选曲目，避免短时间内重复
                    val recentHistory = playbackHistory.takeLast(queue.size.coerceAtMost(8)).toSet()
                    val unplayedCandidates = candidates.filter { it !in recentHistory }
                    if (unplayedCandidates.isNotEmpty()) {
                        unplayedCandidates.random()
                    } else {
                        candidates.random()
                    }
                } else {
                    0
                }
            }
            PlaybackMode.SEQUENTIAL -> {
                (state.currentQueueIndex + 1) % queue.size
            }
        }

        playQueueIndex(nextIndex, initialSeekMs = 0)
        return true
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