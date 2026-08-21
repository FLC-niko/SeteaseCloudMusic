package com.example.seteasecloudmusic.feature.search.data

import retrofit2.http.GET
import retrofit2.http.Query
import com.example.seteasecloudmusic.feature.player.data.LyricResponse

/**
 * `data.api` 模块说明：
 *
 * 这一层直接描述“怎么和后端接口通信”，只关心请求路径、参数和响应模型。
 * 它不处理业务判断，也不决定界面怎么展示。
 *
 * `NeteaseMusicService` 是 Retrofit 接口声明，
 * 负责把网易云相关 HTTP API 映射成可直接调用的 Kotlin suspend 函数。
 */
interface NeteaseMusicService {
    /**
     * 根据歌曲 id 获取可播放 URL。
     */
    @GET("song/url/v1")
    suspend fun getSongUrl(
        @Query("id") id: Long,
        @Query("level") level: String = "hires"
    ): SongResponse

    /**
     * 使用 cloudsearch 获取更完整的搜索结果。
     */
    @GET("cloudsearch")
    suspend fun searchSongs(
        @Query("keywords") keywords: String,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0,
        @Query("type") type: Int = SearchType.SONG.value
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
     * 获取歌词
     */
    @GET("/lyric/new")
    suspend fun getNewLyric(@Query("id") songId: Long): LyricResponse
}

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
