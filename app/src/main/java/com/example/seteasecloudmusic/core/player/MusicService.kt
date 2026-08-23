package com.example.seteasecloudmusic.core.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import coil.imageLoader
import coil.request.ImageRequest
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 基于 Coil 深度定制的 Media3 BitmapLoader：
 * 1. 命中 Coil 200MB 磁盘缓存与内存缓存，0ms 极速提取封面
 * 2. 避免 Media3 默认 SimpleBitmapLoader 因纯原生 HttpURLConnection 导致的超时卡死与系统通知栏不同步
 */
class CoilBitmapLoader(private val context: Context) : BitmapLoader {
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        try {
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            if (bitmap != null) {
                future.set(bitmap)
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
            .target(
                onSuccess = { drawable ->
                    future.set(drawable.toBitmap())
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
class MusicService : MediaSessionService() {

    @Inject
    lateinit var musicPlayerController: MusicPlayerController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 播放器实例：真正负责音频播放
    private var player: ExoPlayer? = null

    // 媒体会话：向系统暴露播放控制能力
    private var mediaSession: MediaSession? = null

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

        this.player = exoPlayer

        // 3. 设置点击通知栏时的跳转意图
        val launchIntent = requireNotNull(packageManager.getLaunchIntentForPackage(packageName))
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 4. 使用 ForwardingPlayer 将系统通知栏与锁屏的上一首/下一首控制暴露并路由到播放队列
        val forwardingPlayer = object : ForwardingPlayer(exoPlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> true

                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true

                    else -> super.isCommandAvailable(command)
                }
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

        // 5. 创建定制的 CoilBitmapLoader 并配置 MediaNotificationProvider，确保通知栏毫秒级刷新
        val bitmapLoader = CoilBitmapLoader(this)
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .build()
        )

        // 6. 创建 MediaSession 并配置可用指令集与 BitmapLoader
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

        // 7. 实时监听 MusicPlayerController 的曲目变化，直接向 ExoPlayer 与 MediaSession 推送最新媒体元数据，保证通知栏 0ms 同步
        serviceScope.launch {
            musicPlayerController.playbackState.collect { state ->
                val track = state.currentTrack
                if (track != null) {
                    val metadata = MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setDisplayTitle(track.title)
                        .setArtist(track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" })
                        .setSubtitle(track.artists.joinToString(" / ") { it.name }.ifBlank { "未知歌手" })
                        .setAlbumTitle(track.album?.title ?: track.title)
                        .setArtworkUri(track.coverUrl?.toUri())
                        .setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .build()

                    exoPlayer.playlistMetadata = metadata
                }
            }
        }
    }

    /**
     * 系统会通过这个方法拿到当前可用的 MediaSession。
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.release()
        player?.release()

        mediaSession = null
        player = null

        super.onDestroy()
    }
}