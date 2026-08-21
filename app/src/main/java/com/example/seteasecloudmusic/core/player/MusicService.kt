package com.example.seteasecloudmusic.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.seteasecloudmusic.feature.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

        // 2. 创建 ExoPlayer
        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true) // 耳机拔出时自动暂停
            .build()

        this.player = exoPlayer

        // 3. 设置点击通知栏时的跳转意图
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
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

        // 5. 创建 MediaSession 并配置可用指令集
        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(sessionActivity)
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
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .setAvailablePlayerCommands(playerCommands)
                        .build()
                }
            })
            .build()
    }

    /**
     * 系统会通过这个方法拿到当前可用的 MediaSession。
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.release()
        player?.release()

        mediaSession = null
        player = null

        super.onDestroy()
    }
}