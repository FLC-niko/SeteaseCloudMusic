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
 * 基于 Coil 深度定制的 Media3 BitmapLoader：
 * 1. 命中 Coil 200MB 磁盘缓存与内存缓存，0ms 极速提取封面
 * 2. 强制使用 CPU 内存 ARGB_8888 软件位图（禁用 GPU HARDWARE 纹理），确保 Binder 跨进程安全传输至小米副屏与锁屏，杜绝崩溃
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
            .size(512, 512)
            .allowHardware(false) // 关键：禁止硬件加速纹理位图，必须使用 CPU 内存位图以支持 Binder 跨进程传输至副屏与锁屏
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
 * 后台播放服务：
 * 1. 持有 ExoPlayer 实例
 * 2. 持有 MediaSession，用于通知栏、锁屏、耳机按键控制
 * 3. 接入 ForwardingPlayer 将上一曲、下一曲等系统媒体命令路由至 MusicPlayerController 播放队列
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

    // 服务级持久唤醒锁与网络锁：跨越单曲 STATE_ENDED 到下一首 URL 解析起播的空窗期，防止切歌瞬间被系统挂起或断网
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
            // 关键防护：切歌间隙底层 ExoPlayer 处于 STATE_ENDED/STATE_IDLE 时，若控制器正在拉取下一首直链或处于播放态，
            // 强制向 MediaSession 暴露 STATE_BUFFERING，保证系统媒体状态连续。
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

        // 2. 创建 ExoPlayer 并配置快速起播缓冲与后台网络保活（WAKE_MODE_NETWORK）
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_500
            )
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true) // 耳机拔出时自动暂停
            .setWakeMode(C.WAKE_MODE_NETWORK)  // 保持 CPU 和网络锁，防止挂后台被系统挂起冻结
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

        // 7. 实时监听 MusicPlayerController 曲目与状态变化，按需同步歌词、元数据与前台通知（跳过 500ms 进度空转）
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
    }

    private fun loadArtworkBitmap(coverUrl: String?) {
        if (coverUrl.isNullOrBlank()) {
            currentArtworkBitmap = null
            return
        }
        val request = ImageRequest.Builder(this)
            .data(coverUrl)
            .size(320, 320)
            .memoryCacheKey("$coverUrl#notif_320")
            .allowHardware(false) // 禁用 GPU 硬件加速位图，确保通过 Binder 跨进程安全传输至背屏
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

    private fun startLyricTicker() {
        if (lyricTickerJob?.isActive == true) return
        lyricTickerJob = serviceScope.launch {
            while (isActive) {
                val p = player
                val track = musicPlayerController.playbackState.value.currentTrack
                if (p != null && p.isPlaying && track != null && currentLyricLines.isNotEmpty()) {
                    val pos = p.currentPosition.toInt()
                    val currentLine = currentLyricLines.lastOrNull { line ->
                        line.startTime <= pos && (line.endTime <= 0 || pos <= line.endTime + 1500)
                    }
                    val text = currentLine?.words?.joinToString("") { it.word }?.trim().orEmpty()
                    if (text != lastBroadcastLyric) {
                        lastBroadcastLyric = text
                        updateMetadataAndNotification(track, text)
                        broadcastLyric(track, text, pos.toLong(), p.duration)
                        sendSuperLyric(track, currentLine)
                    }
                }
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
        val hasLyric = lyric.isNotBlank() && (player?.isPlaying == true)
        val isBluetoothLyrics = playerSettingsManager.bluetoothLyricsEnabled.value

        val displayTitle: String
        val displayArtist: String

        if (hasLyric) {
            if (isBluetoothLyrics) {
                // 小米 17 Pro 妙享背屏与网易云音乐车载模式：第一行实时歌词，第二行歌曲名
                displayTitle = lyric
                displayArtist = track.title
            } else {
                displayTitle = track.title
                displayArtist = lyric
            }
        } else {
            // 无歌词或暂停播放：第一行歌曲名，第二行歌手名
            displayTitle = track.title
            displayArtist = artist
        }

        val displaySubtitle = if (hasLyric) lyric else artist

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

        // 2. 直接注入底层 Android MediaSessionCompat，立即触发系统 MediaController onMetadataChanged（小米副屏核心通道）
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

        // 3. 主动同步前台媒体通知，确保封面加载、歌词变化与状态切换实时且去重刷新
        val controllerStatus = musicPlayerController.playbackState.value.status
        val allowStartFgs = isForegroundServiceStarted ||
            player?.isPlaying == true ||
            player?.playWhenReady == true ||
            controllerStatus == PlayerStatus.PLAYING ||
            controllerStatus == PlayerStatus.BUFFERING
        mediaSession?.let { postOrUpdateForegroundNotification(it, allowStartForeground = allowStartFgs) }
    }

    private fun sendSuperLyric(
        track: com.example.seteasecloudmusic.core.model.Track,
        line: LyricLine?
    ) {
        if (!playerSettingsManager.superLyricApiEnabled.value) return
        if (line == null) return

        val text = line.words.joinToString("") { it.word }.trim()
        if (text.isBlank()) return

        val artist = track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" }
        val words = if (line.words.isNotEmpty()) {
            line.words.map {
                SuperLyricWord(it.word, it.startTime.toLong(), it.endTime.toLong())
            }.toTypedArray()
        } else null

        val lyricLine = SuperLyricLine(text, words, line.startTime.toLong(), line.endTime.toLong())
        val data = SuperLyricData()
            .setTitle(track.title)
            .setArtist(artist)
            .setAlbum(track.album.title.ifBlank { track.title })
            .setLyric(lyricLine)

        if (line.translatedLyric.isNotBlank()) {
            data.translation = SuperLyricLine(
                line.translatedLyric.trim(),
                line.startTime.toLong(),
                line.endTime.toLong()
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
     * 系统媒体通知与前台服务生命周期接管：
     *
     * Media3 内部的 MediaNotificationManager 只要在切歌/清空瞬间检测到 STATE_IDLE 或空 Timeline，
     * 就会直接调用 stopForeground(STOP_FOREGROUND_REMOVE) 并取消通知，导致 Android 14+/16+ 后台再次拉起
     * 前台服务触发 Background started FGS: Disallowed 并在随后被系统杀后台。
     * 因此本方法不再转发给 super.onUpdateNotification，而是由 MusicService 全权管理 startForeground / notify / stopForeground。
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

    /**
     * 系统会通过这个方法拿到当前可用的 MediaSession。
     */
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
