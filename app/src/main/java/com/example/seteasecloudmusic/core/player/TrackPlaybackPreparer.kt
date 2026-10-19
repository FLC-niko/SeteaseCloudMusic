package com.example.seteasecloudmusic.core.player

import com.example.seteasecloudmusic.core.model.Track

interface TrackPlaybackPreparer {
    suspend operator fun invoke(track: Track): Result<Track>

    /**
     * 听歌打卡与时长上报（PLD 播放结束/里程碑上报）
     */
    suspend fun scrobble(
        trackId: Long,
        durationSeconds: Int,
        sourceId: Long?,
        title: String,
        artist: String,
        totalDurationSeconds: Int
    ): Result<Unit>

    /**
     * 歌曲起播时上报 PLV（建立收听会话）
     */
    suspend fun scrobbleStart(
        trackId: Long,
        sourceId: Long?,
        title: String,
        artist: String,
        totalDurationSeconds: Int
    ): Result<Unit>

    /**
     * 将本地歌曲标题与歌手匹配为网易云在线曲目 ID（支持本地音乐上报打卡）
     */
    suspend fun resolveOnlineTrackId(title: String, artist: String): Long?

    /**
     * 校验在线播放直链是否已过期或即将过期
     */
    fun isStreamUrlExpired(url: String): Boolean = false

    /**
     * 主动使指定曲目的内存直链缓存失效（用于 HTTP 403 / 播放源异常时强制重新拉取）
     */
    fun invalidateTrackUrl(trackId: Long) {}
}
