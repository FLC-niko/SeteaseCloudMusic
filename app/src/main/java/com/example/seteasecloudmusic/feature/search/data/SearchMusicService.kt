package com.example.seteasecloudmusic.feature.search.data

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 网易云音乐搜索、播放直链解析及听歌打卡相关 Retrofit 接口定义。
 */
interface NeteaseMusicService {
    /**
     * 根据歌曲 ID 获取可播放 URL。
     *
     * 默认使用 weapi 加密请求 `/song/url/v1`，规避部分节点默认 eapi 指纹触发 -460 风控的问题。
     */
    @GET("song/url/v1")
    suspend fun getSongUrl(
        @Query("id") id: Long,
        @Query("level") level: String = "hires", // 请求音质等级（如 standard / lossless / hires）
        @Query("crypto") crypto: String? = DEFAULT_SONG_URL_CRYPTO
    ): SongResponse

    /**
     * 降级获取可播放 URL（当 `/song/url/v1` 触发风控或不可用时使用）。
     */
    @GET("song/url")
    suspend fun getLegacySongUrl(
        @Query("id") id: Long,
        @Query("br") br: Int = DEFAULT_LEGACY_BITRATE
    ): SongResponse

    /**
     * 按关键词分页搜索歌曲（cloudsearch 接口）。
     */
    @GET("cloudsearch")
    suspend fun searchSongs(
        @Query("keywords") keywords: String,
        @Query("limit") limit: Int = 30, // 单页返回条数
        @Query("offset") offset: Int = 0, // 分页偏移量
        @Query("type") type: Int = SearchType.SONG.value
    ): SearchResultResponse

    /**
     * 按关键词分页搜索歌手（包含头像信息）。
     */
    @GET("cloudsearch")
    suspend fun searchArtists(
        @Query("keywords") keywords: String,
        @Query("limit") limit: Int = 30, // 单页返回条数
        @Query("offset") offset: Int = 0, // 分页偏移量
        @Query("type") type: Int = SearchType.ARTIST.value
    ): SearchResultResponse

    /**
     * 按关键词分页搜索歌单。
     */
    @GET("cloudsearch")
    suspend fun searchPlaylists(
        @Query("keywords") keywords: String,
        @Query("limit") limit: Int = 30, // 单页返回条数
        @Query("offset") offset: Int = 0, // 分页偏移量
        @Query("type") type: Int = SearchType.PLAYLIST.value
    ): SearchResultResponse

    /**
     * 获取搜索建议（包含单曲、歌手、歌单）。
     */
    @GET("search/suggest")
    suspend fun getSearchSuggestions(
        @Query("keywords") keywords: String,
        @Query("type") type: String? = null
    ): SearchSuggestResponse

    /**
     * 获取详细热搜列表。
     */
    @GET("search/hot/detail")
    suspend fun getHotSearchDetail(): HotSearchDetailResponse

    /**
     * 获取简略热门搜索列表。
     */
    @GET("search/hot")
    suspend fun getHotSearch(): HotSearchResponse

    /**
     * 听歌打卡与收听时长上报接口（/scrobble 对应底层 /api/feedback/weblog）
     * 写入听歌排行、累计听歌量与年度听歌报告
     */
    @GET("scrobble")
    suspend fun scrobble(
        @Query("id") id: Long,
        @Query("sourceid") sourceId: Long? = null,
        @Query("time") time: Int,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): ScrobbleResponse

    companion object {
        const val DEFAULT_SONG_URL_CRYPTO = "weapi" // /song/url/v1 默认加密模式
        const val DEFAULT_LEGACY_BITRATE = 999000 // /song/url 降级接口默认请求码率（bps，999000 表示最高可用码率）
    }
}

/**
 * 听歌打卡接口（/scrobble）响应。
 */
data class ScrobbleResponse(
    val code: Int? = null,
    val message: String? = null,
    val data: Any? = null
)

/**
 * 搜索类型映射，值与后端接口约定保持一致。
 */
enum class SearchType(val value: Int) {
    SONG(1),
    ALBUM(10),
    ARTIST(100),
    PLAYLIST(1000),
    USER(1002),
    MV(1004),
    LYRIC(1006),
    RADIO(1009),
    VIDEO(1014),
    COMPOSITE(1018),
    VOICE(2000)
}
