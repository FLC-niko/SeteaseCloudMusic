package com.example.seteasecloudmusic.core.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import coil.imageLoader
import coil.request.ImageRequest
import com.example.seteasecloudmusic.R
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 播放器实例：真正负责音频播放
    private var player: ExoPlayer? = null

    // 媒体会话：向系统暴露播放控制能力
    private var mediaSession: MediaSession? = null

    // 实时歌词同步引擎状态
    private var currentLyricLines: List<LyricLine> = emptyList()
    private var currentLyricJob: Job? = null
    private var lyricTickerJob: Job? = null
    private var lastBroadcastLyric: String = ""
    private var currentTrackId: Long? = null

    companion object {
        const val CHANNEL_ID = "playback_channel_id"
        const val NOTIFICATION_ID = 1001
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

        // 2. 创建 ExoPlayer 并配置后台网络保活（WAKE_MODE_NETWORK）
        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true) // 耳机拔出时自动暂停
            .setWakeMode(C.WAKE_MODE_NETWORK)  // 保持 CPU 和网络锁，防止挂后台被系统挂起冻结
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startLyricTicker()
                } else {
                    stopLyricTicker()
                }
            }
        })

        this.player = exoPlayer

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

        // 4. 使用 ForwardingPlayer 将系统通知栏、锁屏及副屏的播控操作（播放/暂停/上一曲/下一曲/拖拽）路由到播放器与队列
        val forwardingPlayer = object : ForwardingPlayer(exoPlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> true

                    else -> super.isCommandAvailable(command)
                }
            }

            override fun play() {
                if (exoPlayer.mediaItemCount == 0) {
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

        // 5. 创建公开通知渠道，配置 CoilBitmapLoader 与 DefaultMediaNotificationProvider，并绑定纯白单色通知图标
        createNotificationChannel()
        val bitmapLoader = CoilBitmapLoader(this)
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setNotificationId(NOTIFICATION_ID)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.playback_notification_channel_name)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_notification_music)
        setMediaNotificationProvider(notificationProvider)

        // 6. 创建 MediaSession 并配置副屏/系统播控可用指令集与 BitmapLoader
        mediaSession = MediaSession.Builder(this, forwardingPlayer)
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
                        .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
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

        // 7. 实时监听 MusicPlayerController 曲目变化，加载歌词并触发元数据与通知同步
        serviceScope.launch {
            musicPlayerController.playbackState.collect { state ->
                val track = state.currentTrack
                if (track != null) {
                    val trackChanged = track.id != currentTrackId
                    currentTrackId = track.id

                    if (trackChanged) {
                        currentLyricJob?.cancel()
                        stopLyricTicker()
                        currentLyricLines = emptyList()
                        lastBroadcastLyric = ""
                        fetchLyricsForTrack(track.id)
                    }

                    updateMetadataAndNotification(track, lastBroadcastLyric)
                }
            }
        }
    }

    private fun fetchLyricsForTrack(trackId: Long) {
        currentLyricJob = serviceScope.launch {
            val result = runCatching { getLyricsUseCase(trackId) }.getOrNull()
            val parsed = result?.getOrNull()
            if (isActive && parsed != null && currentTrackId == trackId) {
                currentLyricLines = parsed.lines
                if (player?.isPlaying == true) {
                    startLyricTicker()
                }
            }
        }
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
        val displaySubtitle = lyric.ifBlank { artist }
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setDisplayTitle(track.title)
            .setArtist(artist)
            .setSubtitle(displaySubtitle)
            .setDescription(displaySubtitle)
            .setAlbumTitle(track.album?.title ?: track.title)
            .setArtworkUri(track.coverUrl?.toUri())
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .build()

        player?.playlistMetadata = metadata
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
            putExtra("album", track.album?.title ?: track.title)
            putExtra("lyric", lyric)
            putExtra("position", position)
            putExtra("duration", duration)
            putExtra("playing", true)
        }
        sendBroadcast(metaIntent)
    }

    private var isForegroundServiceStarted = false

    /**
     * 系统媒体通知与前台服务生命周期维护：
     *
     * 核心防护机制：
     * 1. 正常起播时，Media3 传入 startInForegroundRequired = true，将服务升级为 Foreground Service。
     * 2. 在切歌间隙（单曲播放结束进入 Player.STATE_ENDED，等待下一首起播）、缓冲或暂停时，Media3 默认会传入 false 并尝试 stopForeground() 将服务降级为后台普通服务。
     * 3. 在 Android 14+ / 16 (targetSdkVersion 36) 下，后台进程严禁再次启动前台服务（BFGS 限制）。一旦在后台切歌间隙丢掉前台身份，后续调用 startForeground() 将被系统直接拒绝（报错：Background started FGS: Disallowed / Service.startForeground() not allowed）。
     * 4. 失去前台身份后，进程优先级骤降为 CACHED（adj 710，procState 16），持有的一切 CPU WakeLock 被系统停用（PowerManager disabled wakeLock reason: Process Priority），最终在几分钟内被小米澎湃 OS/MIUI 的内存回收器（ProcessKillerForUMMS umms_selfcheck）直接杀掉！
     *
     * 解决策略：
     * 一旦前台服务成功启动，强制保持 startInForegroundRequired = true，杜绝中途降级，确保连续后台播放几十首歌也能坚挺存活！
     */
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        if (startInForegroundRequired) {
            isForegroundServiceStarted = true
        }
        val shouldStayInForeground = startInForegroundRequired || isForegroundServiceStarted
        try {
            super.onUpdateNotification(session, shouldStayInForeground)
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Failed to maintain foreground notification", e)
        }
    }

    /**
     * 用户在系统多任务列表（Recents）划掉应用时的处理：
     * 1. 若当前正在播放音乐（playWhenReady == true），保留前台播放服务，音乐绝不中断；
     * 2. 若当前已暂停或没有播放内容，立即释放前台服务与通知栏，避免无意义常驻耗电。
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            isForegroundServiceStarted = false
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
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
        isForegroundServiceStarted = false
        stopLyricTicker()
        currentLyricJob?.cancel()
        currentLyricLines = emptyList()
        serviceScope.cancel()
        mediaSession?.release()
        player?.release()

        mediaSession = null
        player = null

        super.onDestroy()
    }
}
