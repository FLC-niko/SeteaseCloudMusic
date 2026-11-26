package com.example.seteasecloudmusic.core.player

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import coil.imageLoader
import coil.request.ImageRequest
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.player.domain.GetLyricsUseCase
import com.example.seteasecloudmusic.feature.player.domain.model.LyricLine
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.hchen.superlyricapi.SuperLyricData
import com.hchen.superlyricapi.SuperLyricHelper
import com.hchen.superlyricapi.SuperLyricLine
import com.hchen.superlyricapi.SuperLyricWord
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 基于 Coil 的 Media3 [BitmapLoader]。
 * 复用 Coil 内存/磁盘缓存加载封面，并强制转换为 CPU 内存 ARGB_8888 软件位图（禁用 GPU HARDWARE 纹理），
 * 以支持通过 Binder 跨进程传递至通知栏、锁屏及副屏。
 */
@OptIn(UnstableApi::class)
class CoilBitmapLoader(private val context: Context) : BitmapLoader {
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
            if (bitmap != null) {
                val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    bitmap
                }
                future.set(softwareBitmap)
            } else {
                future.setException(IllegalArgumentException("Failed to decode bitmap"))
            }
        } catch (e: Exception) {
            future.setException(e)
        }
        return future
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        val request = ImageRequest.Builder(context)
            .data(uri.toString())
            .size(512, 512)        // 媒体会话封面最大采样尺寸（px，限制跨进程 Binder 传输体积）
            .allowHardware(false)  // 禁用 HARDWARE 纹理位图，确保 Binder 跨进程安全传输至锁屏与副屏
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .target(
                onSuccess = { drawable ->
                    val bitmap = drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
                    future.set(bitmap)
                },
                onError = {
                    future.setException(IllegalStateException("Failed to load image from $uri"))
                }
            )
            .build()
        context.imageLoader.enqueue(request)
        return future
    }

    override fun loadBitmapFromMetadata(metadata: androidx.media3.common.MediaMetadata): ListenableFuture<Bitmap>? {
        val data = metadata.artworkData
        if (data != null) {
            return decodeBitmap(data)
        }
        val uri = metadata.artworkUri
        if (uri != null) {
            return loadBitmap(uri)
        }
        return null
    }

    override fun supportsMimeType(mimeType: String): Boolean = true
}

/**
 * 后台音乐播放服务，基于 Media3 [MediaSessionService] 实现。
 * 持有 [ExoPlayer] 与 [MediaSession]，接管前台服务与通知生命周期，
 * 并协调系统媒体按键路由、蓝牙歌词、SuperLyric 联动及平台媒体元数据同步。
 */
@AndroidEntryPoint
@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {

    @Inject
    lateinit var musicPlayerController: MusicPlayerController

    @Inject
    lateinit var getLyricsUseCase: GetLyricsUseCase

    @Inject
    lateinit var playerSettingsManager: PlayerSettingsManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 播放器实例：真正负责音频播放
    private var player: ExoPlayer? = null

    // 媒体会话：向系统暴露播放控制能力
    private var mediaSession: MediaSession? = null

    // 实时歌词同步引擎状态
    private var currentLyricLines: List<LyricLine> = emptyList()
    private var currentLyricsHasWordTiming: Boolean = false
    private var rawLrcContent: String = ""
    private var currentLyricJob: Job? = null
    private var lyricTickerJob: Job? = null
    private var lastBroadcastLyric: String = ""
    private var currentTrackId: Long? = null

    // 小米 17 Pro 妙享背屏与平台媒体会话同步支持
    private var musicForwardingPlayer: MusicForwardingPlayer? = null
    private var currentArtworkBitmap: Bitmap? = null
    private var cachedSessionCompat: androidx.media3.session.legacy.MediaSessionCompat? = null
    private var sessionCompatReflectionFailed: Boolean = false
    private var lastNotificationRenderKey: NotificationRenderKey? = null
    private var lastPlatformMetadataKey: PlatformMetadataKey? = null
    private var lastObservedStatus: PlayerStatus? = null
    private var lastObservedTitle: String? = null
    private var lastObservedCoverUrl: String? = null

    // 跨切歌空窗期的唤醒锁与 Wi-Fi 锁，防止切歌等待期间系统休眠或断网
    private var serviceWakeLock: android.os.PowerManager.WakeLock? = null
    private var serviceWifiLock: android.net.wifi.WifiManager.WifiLock? = null

    private fun acquirePlaybackLocks() {
        try {
            if (serviceWakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                serviceWakeLock = pm?.newWakeLock(
                    android.os.PowerManager.PARTIAL_WAKE_LOCK,
                    "SeteaseCloudMusic:MusicServiceWakeLock"
                )?.apply { setReferenceCounted(false) }
            }
            if (serviceWakeLock?.isHeld == false) {
                serviceWakeLock?.acquire()
            }

            if (serviceWifiLock == null) {
                val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    @Suppress("DEPRECATION")
                    android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                serviceWifiLock = wm?.createWifiLock(lockMode, "SeteaseCloudMusic:MusicServiceWifiLock")?.apply {
                    setReferenceCounted(false)
                }
            }
            if (serviceWifiLock?.isHeld == false) {
                serviceWifiLock?.acquire()
            }
        } catch (e: Exception) {
            android.util.Log.w("MusicService", "Failed to acquire playback locks", e)
        }
    }

    private fun releasePlaybackLocks() {
        try {
            if (serviceWakeLock?.isHeld == true) serviceWakeLock?.release()
            if (serviceWifiLock?.isHeld == true) serviceWifiLock?.release()
        } catch (e: Exception) {
            android.util.Log.w("MusicService", "Failed to release playback locks", e)
        }
    }

    inner class MusicForwardingPlayer(player: Player) : ForwardingPlayer(player) {
        private val playerListeners = java.util.concurrent.CopyOnWriteArraySet<Player.Listener>()
        private val wrappedListeners = java.util.concurrent.ConcurrentHashMap<Player.Listener, Player.Listener>()
        private var dynamicMetadata: MediaMetadata? = null

        override fun addListener(listener: Player.Listener) {
            val wrapped = object : Player.Listener by listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val mapped = mapExposedPlaybackState(
                        rawState = playbackState,
                        controllerStatus = musicPlayerController.playbackState.value.status
                    )
                    listener.onPlaybackStateChanged(mapped)
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    val mapped = mapExposedPlayWhenReady(
                        rawPlayWhenReady = playWhenReady,
                        controllerStatus = musicPlayerController.playbackState.value.status
                    )
                    listener.onPlayWhenReadyChanged(mapped, reason)
                }

                @Deprecated("Deprecated in Java")
                @Suppress("DEPRECATION")
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    val status = musicPlayerController.playbackState.value.status
                    listener.onPlayerStateChanged(
                        mapExposedPlayWhenReady(playWhenReady, status),
                        mapExposedPlaybackState(playbackState, status)
                    )
                }
            }
            wrappedListeners[listener] = wrapped
            super.addListener(wrapped)
            playerListeners.add(listener)
        }

        override fun removeListener(listener: Player.Listener) {
            val wrapped = wrappedListeners.remove(listener) ?: listener
            super.removeListener(wrapped)
            playerListeners.remove(listener)
        }

        override fun getMediaMetadata(): MediaMetadata {
            return dynamicMetadata ?: super.getMediaMetadata()
        }

        fun updateDynamicMetadata(metadata: MediaMetadata) {
            dynamicMetadata = metadata
            for (listener in playerListeners) {
                try {
                    listener.onMediaMetadataChanged(metadata)
                } catch (e: Exception) {
                    android.util.Log.w("MusicService", "Error notifying listener", e)
                }
            }
        }

        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands().buildUpon()
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()
        }

        override fun isCommandAvailable(command: Int): Boolean {
            return when (command) {
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true

                else -> super.isCommandAvailable(command)
            }
        }

        override fun getPlaybackState(): Int {
            return mapExposedPlaybackState(
                rawState = super.getPlaybackState(),
                controllerStatus = musicPlayerController.playbackState.value.status
            )
        }

        override fun getPlayWhenReady(): Boolean {
            return mapExposedPlayWhenReady(
                rawPlayWhenReady = super.getPlayWhenReady(),
                controllerStatus = musicPlayerController.playbackState.value.status
            )
        }

        override fun play() {
            val p = this@MusicService.player
            val currentTrackId = musicPlayerController.playbackState.value.currentTrack?.id
            if (p == null || p.mediaItemCount == 0 ||
                !MusicPlayerController.shouldReusePlayerMediaItem(p.currentMediaItem?.mediaId, currentTrackId)
            ) {
                musicPlayerController.resume()
            } else {
                super.play()
            }
        }

        override fun pause() {
            super.pause()
        }

        override fun seekToNext() {
            musicPlayerController.playNext()
        }

        override fun seekToNextMediaItem() {
            musicPlayerController.playNext()
        }

        override fun seekToPrevious() {
            musicPlayerController.playPrevious()
        }

        override fun seekToPreviousMediaItem() {
            musicPlayerController.playPrevious()
        }
    }

    companion object {
        const val CHANNEL_ID = "playback_channel_id"
        const val NOTIFICATION_ID = 1001
        private const val MEDIA_NOTIFICATION_GROUP_KEY = "media3_group_key"

        internal data class DisplayedMediaText(
            val title: String,
            val artist: String,
            val subtitle: String
        )

        internal fun resolveDisplayedMediaText(
            trackTitle: String,
            artist: String,
            lyric: String,
            isPlaying: Boolean,
            bluetoothLyricsEnabled: Boolean
        ): DisplayedMediaText {
            val hasLyric = lyric.isNotBlank() && isPlaying
            return if (hasLyric && bluetoothLyricsEnabled) {
                // 车载蓝牙歌词模式：第一行实时歌词，第二行歌曲名
                DisplayedMediaText(
                    title = lyric,
                    artist = trackTitle,
                    subtitle = lyric
                )
            } else {
                // 标准模式：保持曲目标题与歌手名，歌词由 SuperLyric 或广播独立推送
                DisplayedMediaText(
                    title = trackTitle,
                    artist = artist,
                    subtitle = artist
                )
            }
        }

        internal data class NotificationRenderKey(
            val trackId: Long?,
            val title: String,
            val text: String,
            val isPlaying: Boolean,
            val artworkIdentity: Int
        )

        internal data class PlatformMetadataKey(
            val trackId: Long?,
            val displayTitle: String,
            val displayArtist: String,
            val albumTitle: String,
            val rawLrc: String,
            val durationMs: Long,
            val artworkIdentity: Int
        )

        internal fun mapExposedPlaybackState(
            rawState: Int,
            controllerStatus: PlayerStatus
        ): Int {
            // 切歌间隙底层 ExoPlayer 处于 STATE_ENDED/STATE_IDLE 且控制器处于 BUFFERING/PLAYING 时，
            // 对外映射为 STATE_BUFFERING 以保持系统媒体状态连续
            if ((rawState == Player.STATE_ENDED || rawState == Player.STATE_IDLE) &&
                (controllerStatus == PlayerStatus.BUFFERING || controllerStatus == PlayerStatus.PLAYING)
            ) {
                return Player.STATE_BUFFERING
            }
            return rawState
        }

        internal fun mapExposedPlayWhenReady(
            rawPlayWhenReady: Boolean,
            controllerStatus: PlayerStatus
        ): Boolean {
            if (controllerStatus == PlayerStatus.BUFFERING) {
                return true
            }
            return rawPlayWhenReady
        }

        internal fun shouldShowPauseAction(
            playerPlayWhenReady: Boolean,
            controllerStatus: PlayerStatus
        ): Boolean {
            return playerPlayWhenReady || controllerStatus == PlayerStatus.BUFFERING
        }

        internal fun shouldReleasePlaybackLocks(
            playerPlayWhenReady: Boolean,
            controllerStatus: PlayerStatus
        ): Boolean {
            return !playerPlayWhenReady && controllerStatus != PlayerStatus.BUFFERING
        }

        internal fun shouldKeepServiceAliveOnTaskRemoved(
            hasPlayer: Boolean,
            playerPlayWhenReady: Boolean,
            playerMediaItemCount: Int,
            controllerStatus: PlayerStatus,
            hasCurrentTrack: Boolean
        ): Boolean {
            return hasPlayer &&
                (playerPlayWhenReady ||
                    controllerStatus == PlayerStatus.PLAYING ||
                    controllerStatus == PlayerStatus.BUFFERING) &&
                (playerMediaItemCount > 0 || hasCurrentTrack)
        }

        internal fun shouldExitForegroundForIdleEmptyQueue(
            state: PlaybackState,
            isPlayerPlaying: Boolean
        ): Boolean {
            return state.currentTrack == null &&
                state.queueTracks.isEmpty() &&
                state.status == PlayerStatus.IDLE &&
                !isPlayerPlaying
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.playback_notification_channel_name)
            val descriptionText = getString(R.string.playback_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC // 关键：公开锁屏与副屏可见性，避免小米系统隐私拦截
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onCreate() {
        super.onCreate()

        // 1. 定义音频属性
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // 2. 创建 ExoPlayer 并配置流媒体缓冲参数与网络唤醒锁
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,                  // 最低维持缓冲时长（ms，低于此值触发继续拉流）
                /* maxBufferMs = */ 50_000,                  // 最大缓冲上限（ms，达到后暂停拉流以控制内存与流量）
                /* bufferForPlaybackMs = */ 500,             // 首次起播所需缓冲阈值（ms，越小起播越快，过小增加卡顿风险）
                /* bufferForPlaybackAfterRebufferMs = */ 1_500 // 卡顿重缓冲后恢复播放所需缓冲阈值（ms）
            )
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true) // 音频输出切换（如拔出耳机）时自动暂停
            .setWakeMode(C.WAKE_MODE_NETWORK)  // 播放期间持有 CPU 与网络锁，防止后台网络断流
            .build()

        // 注册 SuperLyric 歌词发行商
        SuperLyricHelper.registerPublisher()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    acquirePlaybackLocks()
                    musicPlayerController.onServicePlayerEnded()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    acquirePlaybackLocks()
                    SuperLyricHelper.registerPublisher()
                    lastBroadcastLyric = ""
                    startLyricTicker()
                    mediaSession?.let { postOrUpdateForegroundNotification(it, allowStartForeground = true) }
                    val track = musicPlayerController.playbackState.value.currentTrack
                    if (track != null) {
                        updateMetadataAndNotification(track, "")
                    }
                } else {
                    stopLyricTicker()
                    SuperLyricHelper.sendStop()
                    val state = musicPlayerController.playbackState.value
                    if (shouldReleasePlaybackLocks(exoPlayer.playWhenReady, state.status)) {
                        releasePlaybackLocks()
                    }
                    val track = state.currentTrack
                    if (track != null) {
                        updateMetadataAndNotification(track, "")
                    }
                }
            }
        })

        this.player = exoPlayer
        musicPlayerController.bindServicePlayer(exoPlayer)

        // 3. 设置点击通知栏时的跳转意图
        val launchIntent = requireNotNull(packageManager.getLaunchIntentForPackage(packageName)).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 4. 使用 MusicForwardingPlayer 将系统通知栏、锁屏及副屏的播控操作路由到播放器与队列，并分发动态歌词元数据
        val forwardingPlayer = MusicForwardingPlayer(exoPlayer)
        this.musicForwardingPlayer = forwardingPlayer

        // 5. 创建公开通知渠道与 BitmapLoader（通知与前台服务生命周期由 MusicService 直接接管）
        createNotificationChannel()
        val bitmapLoader = CoilBitmapLoader(this)

        // 6. 创建 MediaSession 并配置副屏/系统播控可用指令集与 BitmapLoader
        val session = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(sessionActivity)
            .setBitmapLoader(bitmapLoader)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().build()
                    val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                        .add(Player.COMMAND_PLAY_PAUSE)
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .add(Player.COMMAND_SET_MEDIA_ITEM)
                        .add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
                        .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                        .add(Player.COMMAND_GET_TIMELINE)
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .setAvailablePlayerCommands(playerCommands)
                        .build()
                }
            })
            .build()
        mediaSession = session
        addSession(session)

        // 7. 监听曲目、状态与基础元数据变化，按需同步歌词、平台会话与前台通知（忽略纯进度变化）
        serviceScope.launch {
            musicPlayerController.playbackState.collect { state ->
                if (shouldExitForegroundForIdleEmptyQueue(state, player?.isPlaying == true)) {
                    currentTrackId = null
                    lastObservedStatus = state.status
                    lastObservedTitle = null
                    lastObservedCoverUrl = null
                    currentLyricJob?.cancel()
                    stopLyricTicker()
                    SuperLyricHelper.sendStop()
                    currentLyricLines = emptyList()
                    currentLyricsHasWordTiming = false
                    rawLrcContent = ""
                    lastBroadcastLyric = ""
                    currentArtworkBitmap = null
                    lastPlatformMetadataKey = null
                    releasePlaybackLocks()
                    exitForegroundAndRemoveNotification()
                    return@collect
                }

                val track = state.currentTrack
                if (track != null) {
                    val trackChanged = track.id != currentTrackId
                    val statusChanged = state.status != lastObservedStatus
                    val metaChanged = track.title != lastObservedTitle || track.coverUrl != lastObservedCoverUrl
                    currentTrackId = track.id
                    lastObservedStatus = state.status
                    lastObservedTitle = track.title
                    lastObservedCoverUrl = track.coverUrl

                    if (!isForegroundServiceStarted &&
                        (state.status == PlayerStatus.PLAYING || state.status == PlayerStatus.BUFFERING)
                    ) {
                        acquirePlaybackLocks()
                        mediaSession?.let {
                            postOrUpdateForegroundNotification(it, allowStartForeground = true)
                        }
                    } else if (state.status == PlayerStatus.PAUSED && player?.playWhenReady != true) {
                        releasePlaybackLocks()
                    }

                    if (trackChanged) {
                        currentLyricJob?.cancel()
                        stopLyricTicker()
                        SuperLyricHelper.sendStop()
                        currentLyricLines = emptyList()
                        currentLyricsHasWordTiming = false
                        rawLrcContent = ""
                        lastBroadcastLyric = ""
                        currentArtworkBitmap = null
                        loadArtworkBitmap(track.coverUrl)
                        fetchLyricsForTrack(track.id)
                    }

                    if (trackChanged || statusChanged || metaChanged) {
                        updateMetadataAndNotification(track, lastBroadcastLyric)
                    }
                }
            }
        }

        // 8. 监听歌词输出开关变更（蓝牙歌词 / SuperLyric API）
        serviceScope.launch {
            playerSettingsManager.bluetoothLyricsEnabled.drop(1).collect {
                val track = musicPlayerController.playbackState.value.currentTrack ?: return@collect
                updateMetadataAndNotification(track, lastBroadcastLyric)
            }
        }
        serviceScope.launch {
            // SuperLyric 开关热切换：关闭时立即发送停止信号，开启时注册发行商并补推当前行歌词
            playerSettingsManager.superLyricApiEnabled.drop(1).collect { enabled ->
                if (!enabled) {
                    SuperLyricHelper.sendStop()
                } else {
                    SuperLyricHelper.registerPublisher()
                    val p = player
                    val track = musicPlayerController.playbackState.value.currentTrack
                    if (p != null && p.isPlaying && track != null && currentLyricLines.isNotEmpty()) {
                        val pos = p.currentPosition.toInt()
                        // 歌词行末尾宽容窗口（ms，无下一行覆盖时在 endTime 后最多延续 1500ms）
                        val currentLine = currentLyricLines.lastOrNull { line ->
                            line.startTime <= pos && (line.endTime <= 0 || pos <= line.endTime + 1500)
                        }
                        sendSuperLyric(track, currentLine)
                    }
                }
            }
        }
    }

    private fun loadArtworkBitmap(coverUrl: String?) {
        if (coverUrl.isNullOrBlank()) {
            currentArtworkBitmap = null
            return
        }
        val request = ImageRequest.Builder(this)
            .data(coverUrl)
            .size(320, 320) // 通知栏封面采样尺寸（px，平衡清晰度与 Binder 传输体积）
            .memoryCacheKey("$coverUrl#notif_320")
            .allowHardware(false) // 禁用 HARDWARE 位图，确保 Binder 跨进程安全传输至通知与副屏
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .target(
                onSuccess = { drawable ->
                    val bitmap = drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
                    currentArtworkBitmap = bitmap
                    val track = musicPlayerController.playbackState.value.currentTrack
                    if (track != null) {
                        updateMetadataAndNotification(track, lastBroadcastLyric)
                    }
                },
                onError = {
                    currentArtworkBitmap = null
                }
            )
            .build()
        imageLoader.enqueue(request)
    }

    private fun getMediaSessionCompat(): androidx.media3.session.legacy.MediaSessionCompat? {
        if (cachedSessionCompat != null) return cachedSessionCompat
        if (sessionCompatReflectionFailed) return null
        return try {
            val session = mediaSession ?: return null
            val implField = session.javaClass.getDeclaredField("impl").apply { isAccessible = true }
            val impl = implField.get(session) ?: return null
            val stubField = impl.javaClass.getDeclaredField("sessionLegacyStub").apply { isAccessible = true }
            val stub = stubField.get(impl) ?: return null
            val method = stub.javaClass.getMethod("getSessionCompat")
            val result = method.invoke(stub) as? androidx.media3.session.legacy.MediaSessionCompat
            cachedSessionCompat = result
            result
        } catch (e: Throwable) {
            sessionCompatReflectionFailed = true
            android.util.Log.w("MusicService", "Failed to retrieve MediaSessionCompat via reflection", e)
            null
        }
    }

    private fun updatePlatformMediaSession(
        displayTitle: String,
        displayArtist: String,
        trackTitle: String,
        albumTitle: String,
        artist: String,
        lyricLrc: String,
        duration: Long,
        artworkBitmap: Bitmap?,
        artworkUri: String?
    ) {
        val metadataKey = PlatformMetadataKey(
            trackId = currentTrackId,
            displayTitle = displayTitle,
            displayArtist = displayArtist,
            albumTitle = albumTitle,
            rawLrc = lyricLrc,
            durationMs = duration,
            artworkIdentity = System.identityHashCode(artworkBitmap)
        )
        if (metadataKey == lastPlatformMetadataKey) return

        try {
            val compat = getMediaSessionCompat() ?: return
            val compatBuilder = androidx.media3.session.legacy.MediaMetadataCompat.Builder()
                .putString(androidx.media3.session.legacy.MediaMetadataCompat.METADATA_KEY_TITLE, displayTitle)
                .putString(androidx.media3.session.legacy.MediaMetadataCompat.METADATA_KEY_ARTIST, displayArtist)
                .putString(androidx.media3.session.legacy.MediaMetadataCompat.METADATA_KEY_ALBUM, albumTitle)
                .putString(androidx.media3.session.legacy.MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, artist)
                .putString(androidx.media3.session.legacy.MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, displayTitle)
                .putString(androidx.media3.session.legacy.MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, displayArtist)
                .putString(androidx.media3.session.legacy.MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, displayArtist)

            currentTrackId?.let {
                compatBuilder.putString(
                    androidx.media3.session.legacy.MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                    it.toString()
                )
            }

            if (lyricLrc.isNotBlank()) {
                compatBuilder.putString("android.media.metadata.LYRIC", lyricLrc)
                compatBuilder.putString("METADATA_KEY_LYRIC", lyricLrc)
            }
            if (duration > 0) {
                compatBuilder.putLong(androidx.media3.session.legacy.MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            }
            if (artworkBitmap != null) {
                compatBuilder.putBitmap(androidx.media3.session.legacy.MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artworkBitmap)
                compatBuilder.putBitmap(androidx.media3.session.legacy.MediaMetadataCompat.METADATA_KEY_ART, artworkBitmap)
            }
            if (!artworkUri.isNullOrBlank()) {
                compatBuilder.putString(androidx.media3.session.legacy.MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artworkUri)
                compatBuilder.putString(androidx.media3.session.legacy.MediaMetadataCompat.METADATA_KEY_ART_URI, artworkUri)
            }
            compat.setMetadata(compatBuilder.build())
            lastPlatformMetadataKey = metadataKey
        } catch (e: Throwable) {
            android.util.Log.e("MusicService", "updatePlatformMediaSession failed", e)
        }
    }

    private fun fetchLyricsForTrack(trackId: Long) {
        currentLyricJob = serviceScope.launch {
            val result = runCatching { getLyricsUseCase(trackId) }.getOrNull()
            val parsed = result?.getOrNull()
            if (isActive && parsed != null && currentTrackId == trackId) {
                currentLyricLines = parsed.lines
                currentLyricsHasWordTiming = parsed.hasWordTiming
                rawLrcContent = buildRawLrc(parsed.lines)
                val track = musicPlayerController.playbackState.value.currentTrack
                if (track != null) {
                    updateMetadataAndNotification(track, lastBroadcastLyric)
                }
                if (player?.isPlaying == true) {
                    startLyricTicker()
                }
            }
        }
    }

    private fun buildRawLrc(lines: List<LyricLine>): String {
        if (lines.isEmpty()) return ""
        val sb = StringBuilder()
        for (line in lines) {
            val m = line.startTime / 60000
            val s = (line.startTime % 60000) / 1000
            val ms = (line.startTime % 1000) / 10
            val text = line.words.joinToString("") { it.word }
            sb.append(String.format("[%02d:%02d.%02d]%s\n", m, s, ms, text))
        }
        return sb.toString()
    }

    /** 启动歌词轮询任务，按当前播放进度匹配歌词行并同步广播与 SuperLyric */
    private fun startLyricTicker() {
        if (lyricTickerJob?.isActive == true) return
        lyricTickerJob = serviceScope.launch {
            while (isActive) {
                val p = player
                val track = musicPlayerController.playbackState.value.currentTrack
                if (p != null && p.isPlaying && track != null && currentLyricLines.isNotEmpty()) {
                    val pos = p.currentPosition.toInt()
                    // 歌词行末尾宽容窗口（ms，无下一行覆盖时在 endTime 后最多延续 1500ms）
                    val currentLine = currentLyricLines.lastOrNull { line ->
                        line.startTime <= pos && (line.endTime <= 0 || pos <= line.endTime + 1500)
                    }
                    val text = currentLine?.words?.joinToString("") { it.word }?.trim().orEmpty()
                    if (text != lastBroadcastLyric) {
                        lastBroadcastLyric = text
                        if (playerSettingsManager.bluetoothLyricsEnabled.value) {
                            updateMetadataAndNotification(track, text)
                        }
                        broadcastLyric(track, text, pos.toLong(), p.duration)
                        sendSuperLyric(track, currentLine)
                    }
                }
                // 歌词匹配轮询间隔（ms，越小行切换越及时但唤醒更频繁）
                delay(250)
            }
        }
    }

    private fun stopLyricTicker() {
        lyricTickerJob?.cancel()
        lyricTickerJob = null
    }

    private fun updateMetadataAndNotification(track: com.example.seteasecloudmusic.core.model.Track, lyric: String) {
        val artist = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" }
        val displayed = resolveDisplayedMediaText(
            trackTitle = track.title,
            artist = artist,
            lyric = lyric,
            isPlaying = player?.isPlaying == true,
            bluetoothLyricsEnabled = playerSettingsManager.bluetoothLyricsEnabled.value
        )
        val displayTitle = displayed.title
        val displayArtist = displayed.artist
        val displaySubtitle = displayed.subtitle

        val extras = Bundle().apply {
            if (rawLrcContent.isNotBlank()) {
                putString("android.media.metadata.LYRIC", rawLrcContent)
                putString("METADATA_KEY_LYRIC", rawLrcContent)
            }
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(displayTitle)
            .setDisplayTitle(displayTitle)
            .setArtist(displayArtist)
            .setSubtitle(displaySubtitle)
            .setDescription(if (rawLrcContent.isNotBlank()) rawLrcContent else displaySubtitle)
            .setAlbumTitle(track.album.title.ifBlank { track.title })
            .setArtworkUri(track.coverUrl?.toUri())
            .setExtras(extras)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .build()

        // 1. 同步 Media3 ForwardingPlayer 内部元数据并分发给所有 Player.Listener
        musicForwardingPlayer?.updateDynamicMetadata(metadata)
        player?.playlistMetadata = metadata

        // 2. 更新底层 MediaSessionCompat 元数据，触发系统及外部设备（如副屏）刷新显示
        updatePlatformMediaSession(
            displayTitle = displayTitle,
            displayArtist = displayArtist,
            trackTitle = track.title,
            albumTitle = track.album.title.ifBlank { track.title },
            artist = artist,
            lyricLrc = rawLrcContent,
            duration = player?.duration ?: 0L,
            artworkBitmap = currentArtworkBitmap,
            artworkUri = track.coverUrl
        )

        // 3. 按渲染去重键同步前台媒体通知
        val controllerStatus = musicPlayerController.playbackState.value.status
        val allowStartFgs = isForegroundServiceStarted ||
            player?.isPlaying == true ||
            player?.playWhenReady == true ||
            controllerStatus == PlayerStatus.PLAYING ||
            controllerStatus == PlayerStatus.BUFFERING
        mediaSession?.let { postOrUpdateForegroundNotification(it, allowStartForeground = allowStartFgs) }
    }

    /** 向 SuperLyric 推送当前歌词行与逐字时间轴（受 superLyricApiEnabled 开关控制） */
    private fun sendSuperLyric(
        track: com.example.seteasecloudmusic.core.model.Track,
        line: LyricLine?
    ) {
        if (!playerSettingsManager.superLyricApiEnabled.value) return
        if (line == null) return

        val text = line.words.joinToString("") { it.word }.trim()
        if (text.isBlank()) return

        val artist = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" }
        val startTimeMs = line.startTime.toLong().coerceAtLeast(0L)
        // 未提供有效结束时间时，使用默认单行持续时长兜底（ms）
        val endTimeMs = if (line.endTime > line.startTime) line.endTime.toLong() else startTimeMs + 4000L
        val words = if (currentLyricsHasWordTiming && line.words.size > 1) {
            line.words.map {
                val wordStart = it.startTime.toLong().coerceAtLeast(0L)
                val wordEnd = it.endTime.toLong().coerceAtLeast(wordStart)
                SuperLyricWord(it.word, wordStart, wordEnd)
            }.toTypedArray()
        } else null

        val lyricLine = SuperLyricLine(text, words, startTimeMs, endTimeMs)
        val data = SuperLyricData()
            .setTitle(track.title)
            .setArtist(artist)
            .setAlbum(track.album.title.ifBlank { track.title })
            .setLyric(lyricLine)

        if (line.translatedLyric.isNotBlank()) {
            data.translation = SuperLyricLine(
                line.translatedLyric.trim(),
                startTimeMs,
                endTimeMs
            )
        }

        SuperLyricHelper.sendLyric(data)
    }

    private fun broadcastLyric(track: com.example.seteasecloudmusic.core.model.Track, lyric: String, position: Long, duration: Long) {
        val artist = track.artists.joinToString(" / ") { it.name }
        // 1. 发送网易云音乐与通用音乐歌词广播（供第三方歌词插件/状态栏抓取）
        val intent = Intent("com.netease.cloudmusic.lyrics").apply {
            putExtra("track", track.title)
            putExtra("artist", artist)
            putExtra("lyric", lyric)
            putExtra("position", position)
            putExtra("duration", duration)
            putExtra("is_playing", true)
        }
        sendBroadcast(intent)

        // 2. 兼容系统及常见状态栏歌词模块广播 (com.android.music.metachanged)
        val metaIntent = Intent("com.android.music.metachanged").apply {
            putExtra("track", track.title)
            putExtra("artist", artist)
            putExtra("album", track.album.title.ifBlank { track.title })
            putExtra("lyric", lyric)
            putExtra("position", position)
            putExtra("duration", duration)
            putExtra("playing", true)
        }
        sendBroadcast(metaIntent)
    }

    private var isForegroundServiceStarted = false

    private fun createMediaButtonPendingIntent(keyCode: Int, useForegroundService: Boolean): PendingIntent {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            component = ComponentName(this@MusicService, MusicService::class.java)
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return if (useForegroundService) {
            PendingIntent.getForegroundService(this, keyCode, intent, flags)
        } else {
            PendingIntent.getService(this, keyCode, intent, flags)
        }
    }

    private fun buildPlaybackNotification(
        session: MediaSession,
        title: String,
        text: String,
        isPlaying: Boolean
    ): Notification {
        val prevIntent = createMediaButtonPendingIntent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, useForegroundService = false)
        val playPauseIntent = createMediaButtonPendingIntent(
            keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            useForegroundService = !isPlaying && !isForegroundServiceStarted
        )
        val nextIntent = createMediaButtonPendingIntent(KeyEvent.KEYCODE_MEDIA_NEXT, useForegroundService = false)

        val playPauseIcon = if (isPlaying) {
            androidx.media3.session.R.drawable.media3_icon_pause
        } else {
            androidx.media3.session.R.drawable.media3_icon_play
        }
        val playPauseLabel = if (isPlaying) "暂停" else "播放"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setContentTitle(title.ifBlank { getString(R.string.app_name) })
            .setContentText(text)
            .setContentIntent(session.sessionActivity)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(isPlaying)
            .setGroup(MEDIA_NOTIFICATION_GROUP_KEY)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(androidx.media3.session.R.drawable.media3_icon_previous, "上一首", prevIntent)
            .addAction(playPauseIcon, playPauseLabel, playPauseIntent)
            .addAction(androidx.media3.session.R.drawable.media3_icon_next, "下一首", nextIntent)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(session).setShowActionsInCompactView(0, 1, 2))

        currentArtworkBitmap?.let { builder.setLargeIcon(it) }

        val notification = builder.build()
        notification.extras.putParcelable(Notification.EXTRA_MEDIA_SESSION, session.platformToken)
        return notification
    }

    @SuppressLint("MissingPermission")
    private fun postOrUpdateForegroundNotification(
        session: MediaSession,
        allowStartForeground: Boolean,
        forcePost: Boolean = false
    ) {
        val state = musicPlayerController.playbackState.value
        val track = state.currentTrack
        val dynamicMetadata = musicForwardingPlayer?.mediaMetadata
        val artistFallback = track?.artists?.joinToString(" / ") { it.name }?.ifBlank { "未知歌手" }.orEmpty()
        val title = dynamicMetadata?.displayTitle?.toString()?.takeIf { it.isNotBlank() }
            ?: dynamicMetadata?.title?.toString()?.takeIf { it.isNotBlank() }
            ?: track?.title?.takeIf { it.isNotBlank() }
            ?: getString(R.string.app_name)
        val text = dynamicMetadata?.artist?.toString()?.takeIf { it.isNotBlank() }
            ?: dynamicMetadata?.subtitle?.toString()?.takeIf { it.isNotBlank() }
            ?: artistFallback
        val showPauseIcon = shouldShowPauseAction(
            playerPlayWhenReady = player?.playWhenReady == true,
            controllerStatus = state.status
        )

        val renderKey = NotificationRenderKey(
            trackId = track?.id,
            title = title,
            text = text,
            isPlaying = showPauseIcon,
            artworkIdentity = System.identityHashCode(currentArtworkBitmap)
        )

        if (!isForegroundServiceStarted) {
            if (!allowStartForeground) return
            val notification = buildPlaybackNotification(session, title, text, showPauseIcon)
            try {
                ContextCompat.startForegroundService(this, Intent(this, MusicService::class.java))
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
                isForegroundServiceStarted = true
                lastNotificationRenderKey = renderKey
            } catch (e: Exception) {
                android.util.Log.e("MusicService", "Failed to start foreground service", e)
                runCatching {
                    NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
                }
            }
            return
        }

        if (!forcePost && renderKey == lastNotificationRenderKey) {
            return
        }

        val notification = buildPlaybackNotification(session, title, text, showPauseIcon)
        runCatching {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
            lastNotificationRenderKey = renderKey
        }.onFailure { e ->
            android.util.Log.w("MusicService", "Failed to update playback notification", e)
        }
    }

    private fun exitForegroundAndRemoveNotification() {
        if (isForegroundServiceStarted) {
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            isForegroundServiceStarted = false
        }
        lastNotificationRenderKey = null
        runCatching { NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID) }
    }

    /**
     * 接管系统媒体通知与前台服务生命周期：
     * Media3 默认的 MediaNotificationManager 在切歌或清空瞬间的 STATE_IDLE / 空 Timeline 状态下
     * 会调用 stopForeground(STOP_FOREGROUND_REMOVE)，导致 Android 14+ 后台拉起前台服务受限。
     * 本方法由 MusicService 统一调度 startForeground / notify / stopForeground。
     */
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        val controllerStatus = musicPlayerController.playbackState.value.status
        val shouldStartFgs = startInForegroundRequired ||
            player?.playWhenReady == true ||
            controllerStatus == PlayerStatus.PLAYING ||
            controllerStatus == PlayerStatus.BUFFERING
        postOrUpdateForegroundNotification(session, allowStartForeground = shouldStartFgs)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON && !isForegroundServiceStarted) {
            mediaSession?.let { postOrUpdateForegroundNotification(it, allowStartForeground = true, forcePost = true) }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun isPlaybackOngoing(): Boolean = isForegroundServiceStarted

    /**
     * 用户在系统多任务列表（Recents）划掉应用时的处理：
     * 1. 若当前正在播放或切歌缓冲中，保留前台播放服务，不调用 super.onTaskRemoved；
     * 2. 若当前已暂停或没有播放内容，主动退出前台服务并移除通知栏。
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        val state = musicPlayerController.playbackState.value
        val keepAlive = shouldKeepServiceAliveOnTaskRemoved(
            hasPlayer = p != null,
            playerPlayWhenReady = p?.playWhenReady == true,
            playerMediaItemCount = p?.mediaItemCount ?: 0,
            controllerStatus = state.status,
            hasCurrentTrack = state.currentTrack != null
        )
        if (!keepAlive) {
            exitForegroundAndRemoveNotification()
            stopSelf()
            super.onTaskRemoved(rootIntent)
        }
    }

    /** 返回当前可用的 MediaSession 供系统及外部控制器连接 */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        val shouldReconnect = player?.playWhenReady == true && player?.mediaItemCount != 0
        musicPlayerController.onServiceDestroyed(shouldReconnect)
        musicPlayerController.unbindServicePlayer(player)
        releasePlaybackLocks()
        exitForegroundAndRemoveNotification()
        stopLyricTicker()
        SuperLyricHelper.sendStop()
        SuperLyricHelper.unregisterPublisher()
        currentLyricJob?.cancel()
        currentLyricLines = emptyList()
        currentLyricsHasWordTiming = false
        rawLrcContent = ""
        cachedSessionCompat = null
        lastPlatformMetadataKey = null
        currentArtworkBitmap = null
        musicForwardingPlayer = null
        serviceScope.cancel()
        mediaSession?.release()
        player?.release()

        mediaSession = null
        player = null

        super.onDestroy()
    }
}
