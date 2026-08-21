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
import com.example.seteasecloudmusic.core.database.RecentTrackRecorder
import com.example.seteasecloudmusic.core.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

enum class PlayerStatus { IDLE, BUFFERING, PLAYING, PAUSED, ENDED, ERROR }

enum class QueueRepeatMode { OFF, ALL, ONE }

data class PlaybackState(
    val status: PlayerStatus = PlayerStatus.IDLE,
    val currentTrack: Track? = null,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val errorMessage: String? = null,
    val queueTracks: List<Track> = emptyList(),
    val currentQueueIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val repeatMode: QueueRepeatMode = QueueRepeatMode.OFF,
    val volume: Float = 1f
)

@Singleton
class MusicPlayerController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val trackPlaybackPreparer: TrackPlaybackPreparer,
    private val recentTrackRecorder: RecentTrackRecorder
) {
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    // 控制器自己的协程域：用于异步连接服务、拉 URL、更新状态（生命周期与单例绑定）
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)

    // Media3 控制端（连接到 MusicService 的 MediaSession）
    @Volatile
    private var controller: MediaController? = null
    private var connectionDeferred = CompletableDeferred<MediaController>()

    // 进度轮询任务：同步 position/duration 到 UI，歌词逐字高亮需要更密的节奏。
    private var progressJob: Job? = null
    private var playJob: Job? = null
    private var prefetchJob: Job? = null
    private var prefetchedTrackId: Long? = null
    private val latestPlayRequestId = AtomicLong(0L)
    private val playedShuffleIndices = Collections.synchronizedSet(mutableSetOf<Int>())

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        // 单例创建时自动建立后台连接
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

            if (mapped == PlayerStatus.ENDED) {
                if (playNextInternal(fromCompletion = true)) {
                    return
                }
            }

            _playbackState.update {
                it.copy(
                    status = mapped,
                    currentPositionMs = c.currentPosition.toInt().coerceAtLeast(0),
                    durationMs = c.duration.takeIf { d -> d > 0 }?.toInt() ?: 0
                )
            }

            if (mapped == PlayerStatus.PLAYING) startProgressTicker() else stopProgressTicker()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update {
                it.copy(status = if (isPlaying) PlayerStatus.PLAYING else PlayerStatus.PAUSED)
            }
            if (isPlaying) startProgressTicker() else stopProgressTicker()
        }

        override fun onVolumeChanged(volume: Float) {
            _playbackState.update { it.copy(volume = volume.coerceIn(0f, 1f)) }
        }

        override fun onPlayerError(error: PlaybackException) {
            stopProgressTicker()
            _playbackState.update {
                it.copy(status = PlayerStatus.ERROR, errorMessage = error.message ?: "Playback error")
            }
        }
    }

    /** 建立到 MusicService 的连接（支持并发安全与结果重用） */
    @Synchronized
    fun connect() {
        if (controller != null) {
            if (!connectionDeferred.isCompleted) {
                connectionDeferred.complete(controller!!)
            }
            return
        }

        if (connectionDeferred.isCompleted && connectionDeferred.isCancelled) {
            connectionDeferred = CompletableDeferred()
        }

        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()

        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { c ->
                        controller = c
                        c.addListener(playerListener)
                        if (!connectionDeferred.isCompleted) {
                            connectionDeferred.complete(c)
                        }
                        _playbackState.update { it.copy(volume = c.volume.coerceIn(0f, 1f)) }
                    }
                    .onFailure { e ->
                        if (!connectionDeferred.isCompleted) {
                            connectionDeferred.completeExceptionally(e)
                        }
                        _playbackState.update {
                            it.copy(status = PlayerStatus.ERROR, errorMessage = e.message)
                        }
                    }
            },
            context.mainExecutor
        )
    }

    private suspend fun ensureController(): MediaController? {
        controller?.let { return it }
        connect()
        return runCatching { connectionDeferred.await() }.getOrNull()
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
                errorMessage = null
            )
        }
        playedShuffleIndices.clear()
        playedShuffleIndices += startIndex

        playQueueIndex(startIndex)
    }

    fun play(track: Track) {
        replaceQueueAndPlay(listOf(track), startIndex = 0)
    }

    fun playNext() {
        playNextInternal(fromCompletion = false)
    }

    fun playPrevious() {
        val state = _playbackState.value
        if (state.currentPositionMs > PREVIOUS_RESTART_THRESHOLD_MS) {
            seekTo(0)
            return
        }

        val previousIndex = when {
            state.currentQueueIndex > 0 -> state.currentQueueIndex - 1
            state.repeatMode == QueueRepeatMode.ALL && state.queueTracks.isNotEmpty() -> {
                state.queueTracks.lastIndex
            }
            else -> return
        }
        playQueueIndex(previousIndex)
    }

    fun playQueueItem(index: Int) {
        playQueueIndex(index)
    }

    fun toggleShuffle() {
        _playbackState.update { state ->
            val enabled = !state.shuffleEnabled
            playedShuffleIndices.clear()
            if (enabled && state.currentQueueIndex in state.queueTracks.indices) {
                playedShuffleIndices += state.currentQueueIndex
            }
            state.copy(shuffleEnabled = enabled)
        }
    }

    fun cycleRepeatMode() {
        _playbackState.update { state ->
            val nextMode = when (state.repeatMode) {
                QueueRepeatMode.OFF -> QueueRepeatMode.ALL
                QueueRepeatMode.ALL -> QueueRepeatMode.ONE
                QueueRepeatMode.ONE -> QueueRepeatMode.OFF
            }
            state.copy(repeatMode = nextMode)
        }
    }

    fun setVolume(volume: Float) {
        val safeVolume = volume.coerceIn(0f, 1f)
        controller?.volume = safeVolume
        _playbackState.update { it.copy(volume = safeVolume) }
    }

    fun replayCurrent() {
        val state = _playbackState.value
        when {
            state.currentQueueIndex in state.queueTracks.indices -> {
                playQueueIndex(state.currentQueueIndex)
            }

            state.currentTrack != null -> {
                play(state.currentTrack)
            }
        }
    }

    fun pause() = controller?.pause() ?: Unit
    fun resume() = controller?.play() ?: Unit
    fun stop() = controller?.stop() ?: Unit
    fun seekTo(positionMs: Int) = controller?.seekTo(positionMs.toLong()) ?: Unit

    fun release() {
        stopProgressTicker()
        playJob?.cancel()
        playJob = null
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        connectionDeferred = CompletableDeferred()
    }

    private fun playQueueIndex(index: Int) {
        val queue = _playbackState.value.queueTracks
        if (index !in queue.indices) {
            _playbackState.update {
                it.copy(status = PlayerStatus.ERROR, errorMessage = "Queue index out of bounds")
            }
            return
        }

        val track = queue[index]
        if (_playbackState.value.shuffleEnabled) {
            playedShuffleIndices += index
        }
        playJob?.cancel()
        val requestId = nextPlayRequestId()
        playJob = scope.launch {
            _playbackState.update {
                it.copy(
                    status = PlayerStatus.BUFFERING,
                    currentTrack = track,
                    currentQueueIndex = index,
                    errorMessage = null
                )
            }

            val prepared = withContext(ioDispatcher) { trackPlaybackPreparer(track) }
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

                    val targetController = ensureController()
                    if (targetController == null) {
                        _playbackState.update {
                            it.copy(status = PlayerStatus.ERROR, errorMessage = "Controller not connected")
                        }
                        return@onSuccess
                    }

                    targetController.apply {
                        setMediaItem(item)
                        prepare()
                        play()
                    }

                    // 播放器只报告成功播放，具体持久化由数据库记录器负责。
                    withContext(ioDispatcher) {
                        runCatching { recentTrackRecorder.record(t) }
                    }
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

    private fun playNextInternal(fromCompletion: Boolean): Boolean {
        val state = _playbackState.value
        if (state.queueTracks.isEmpty()) return false

        if (fromCompletion && state.repeatMode == QueueRepeatMode.ONE) {
            playQueueIndex(state.currentQueueIndex)
            return true
        }

        val nextIndex = when {
            state.shuffleEnabled -> nextShuffleIndex(state)
            state.currentQueueIndex < state.queueTracks.lastIndex -> state.currentQueueIndex + 1
            state.repeatMode == QueueRepeatMode.ALL -> 0
            else -> -1
        }
        if (nextIndex !in state.queueTracks.indices) {
            _playbackState.update {
                it.copy(status = PlayerStatus.ENDED, errorMessage = null)
            }
            return false
        }

        playQueueIndex(nextIndex)
        return true
    }

    private fun nextShuffleIndex(state: PlaybackState): Int {
        val unplayed = state.queueTracks.indices.filterNot(playedShuffleIndices::contains)
        if (unplayed.isNotEmpty()) return unplayed.random()
        if (state.repeatMode != QueueRepeatMode.ALL) return -1

        playedShuffleIndices.clear()
        playedShuffleIndices += state.currentQueueIndex
        return state.queueTracks.indices
            .filterNot { it == state.currentQueueIndex }
            .takeIf { it.isNotEmpty() }
            ?.random()
            ?: state.currentQueueIndex
    }

    private fun nextPlayRequestId(): Long = latestPlayRequestId.incrementAndGet()

    private fun isStaleRequest(requestId: Long): Boolean = requestId != latestPlayRequestId.get()

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
                        durationMs = dur
                    )
                }

                // 预加载下一首：当播放进度超过 70% 或剩余少于 20 秒时
                if (dur > 0 && (pos.toFloat() / dur > 0.7f || (dur - pos) < 20_000)) {
                    triggerPrefetchNextTrack()
                }

                delay(100L)
            }
        }
    }

    private fun triggerPrefetchNextTrack() {
        val state = _playbackState.value
        if (state.queueTracks.isEmpty() || prefetchJob?.isActive == true) return

        val nextIndex = when {
            state.shuffleEnabled -> null // 随机模式下一首未定，不预拉取
            state.currentQueueIndex < state.queueTracks.lastIndex -> state.currentQueueIndex + 1
            state.repeatMode == QueueRepeatMode.ALL -> 0
            else -> null
        } ?: return

        val nextTrack = state.queueTracks.getOrNull(nextIndex) ?: return
        if (nextTrack.id == prefetchedTrackId) return

        prefetchedTrackId = nextTrack.id
        prefetchJob = scope.launch(ioDispatcher) {
            try {
                trackPlaybackPreparer(nextTrack)
            } catch (_: Exception) {}
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    private companion object {
        const val PREVIOUS_RESTART_THRESHOLD_MS = 3_000
    }
}
