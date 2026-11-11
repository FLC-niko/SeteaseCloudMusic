package com.example.seteasecloudmusic.feature.search.domain

import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.Track

interface SearchRepository {
    // 搜索歌曲，返回匹配的曲目列表。
    suspend fun searchTracks(
        query: String,
        limit: Int = 30,
        offset: Int = 0
    ): Result<List<Track>>

    // 搜索歌手，返回包含真实头像的歌手列表。
    suspend fun searchArtists(
        query: String,
        limit: Int = 30,
        offset: Int = 0
    ): Result<List<Artist>>

    // 搜索歌单，返回匹配的歌单列表。
    suspend fun searchPlaylists(
        query: String,
        limit: Int = 30,
        offset: Int = 0
    ): Result<List<PlaylistSuggestion>>

    // 获取指定歌曲的可播放 URL。
    suspend fun getTrackUrl(
        trackId: Long,
        level: String = "hires"
    ): Result<String>

    // 获取搜索建议，返回单曲、歌手、歌单的综合建议。
    suspend fun getSearchSuggestions(
        query: String,
        type: String? = null
    ): Result<SearchSuggestions>

    // 获取热门搜索列表。
    suspend fun getHotSearches(): Result<List<HotSearchItem>>

    // 听歌打卡与时长上报接口（NCBL + Weblog）
    suspend fun scrobble(
        trackId: Long,
        durationSeconds: Int,
        sourceId: Long?,
        title: String,
        artist: String,
        totalDurationSeconds: Int
    ): Result<Unit>

    // 歌曲起播时上报 PLV（建立收听会话）
    suspend fun scrobbleStart(
        trackId: Long,
        sourceId: Long?,
        title: String,
        artist: String,
        totalDurationSeconds: Int
    ): Result<Unit>

    // 解析本地歌曲对应的网易云在线单曲 ID
    suspend fun resolveOnlineTrackId(
        title: String,
        artist: String
    ): Long?
}
