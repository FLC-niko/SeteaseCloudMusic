package com.example.seteasecloudmusic.feature.search.domain

import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.Track

/**
 * 搜索域仓库接口：定义歌曲/歌手/歌单搜索、热搜与联想、播放直链获取及听歌打卡能力。
 */
interface SearchRepository {
    /** 搜索歌曲，返回匹配的曲目列表。 */
    suspend fun searchTracks(
        query: String,
        limit: Int = 30, // 单页返回条数
        offset: Int = 0 // 分页偏移量
    ): Result<List<Track>>

    /** 搜索歌手，返回包含头像的歌手列表。 */
    suspend fun searchArtists(
        query: String,
        limit: Int = 30, // 单页返回条数
        offset: Int = 0 // 分页偏移量
    ): Result<List<Artist>>

    /** 搜索歌单，返回匹配的歌单列表。 */
    suspend fun searchPlaylists(
        query: String,
        limit: Int = 30, // 单页返回条数
        offset: Int = 0 // 分页偏移量
    ): Result<List<PlaylistSuggestion>>

    /** 获取指定歌曲的可播放直链 URL（[level] 为期望音质等级）。 */
    suspend fun getTrackUrl(
        trackId: Long,
        level: String = "hires"
    ): Result<String>

    /** 获取搜索建议，返回单曲、歌手、歌单与匹配词建议。 */
    suspend fun getSearchSuggestions(
        query: String,
        type: String? = null
    ): Result<SearchSuggestions>

    /** 获取热门搜索列表。 */
    suspend fun getHotSearches(): Result<List<HotSearchItem>>

    /** 听歌打卡与播放时长上报（NCBL PLD + Weblog）。 */
    suspend fun scrobble(
        trackId: Long,
        durationSeconds: Int,
        sourceId: Long?,
        title: String,
        artist: String,
        totalDurationSeconds: Int
    ): Result<Unit>

    /** 歌曲起播时上报 PLV，建立收听会话。 */
    suspend fun scrobbleStart(
        trackId: Long,
        sourceId: Long?,
        title: String,
        artist: String,
        totalDurationSeconds: Int
    ): Result<Unit>

    /** 根据本地曲目标题与歌手反查对应的网易云在线单曲 ID。 */
    suspend fun resolveOnlineTrackId(
        title: String,
        artist: String
    ): Long?
}
