package com.example.seteasecloudmusic.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.seteasecloudmusic.feature.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * 后台播放服务：
 * 1. 持有 ExoPlayer 实例
 * 2. 持有 MediaSession，用于通知栏、锁屏、耳机按键控制
 * 3. 由系统在后台托管播放能力
 */
@AndroidEntryPoint
class MusicService : MediaSessionService() {

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

        // 2. 先创建一个局部非空变量 exoPlayer
        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true) // 耳机拔出时自动暂停
            .build()

        // 3. 将局部变量赋值给类成员变量 player
        this.player = exoPlayer

        // 4. 设置点击通知栏时的跳转意图
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // 5. 使用局部变量 exoPlayer 创建 MediaSession，无需使用 !!
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivity)
            .build()
    }

    /**
     * 系统会通过这个方法拿到当前可用的 MediaSession。
     * 返回 null 表示当前没有会话可用。
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        // 先释放 session，再释放播放器
        mediaSession?.release()
        player?.release()

        mediaSession = null
        player = null

        super.onDestroy()
    }
}