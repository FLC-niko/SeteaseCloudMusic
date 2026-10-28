package com.example.seteasecloudmusic.feature.radio.data

import com.example.seteasecloudmusic.feature.discover.data.HotPlaylistResponse
import com.example.seteasecloudmusic.feature.discover.data.PersonalizedNewsongResponse
import com.example.seteasecloudmusic.feature.discover.data.PersonalizedResponse
import com.example.seteasecloudmusic.feature.discover.data.ToplistDetailResponse
import com.example.seteasecloudmusic.feature.home.data.DailyRecommendSongsResponse
import com.example.seteasecloudmusic.feature.home.data.HomepageBlockPageResponse
import com.example.seteasecloudmusic.feature.home.data.RecommendResourceResponse
import com.example.seteasecloudmusic.feature.mine.data.PlaylistDetailResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RadioService {

    /**
     * 1. 获取排行榜摘要及前三首歌曲
     */
    @GET("toplist/detail")
    suspend fun getToplistDetail(): ToplistDetailResponse

    /**
     * 2. 获取热门歌手列表
     */
    @GET("top/artists")
    suspend fun getTopArtists(
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): RadioTopArtistsResponse

    /**
     * 3. 获取歌手热门歌曲
     */
    @GET("artist/top/song")
    suspend fun getArtistTopSongs(
        @Query("id") id: Long
    ): RadioArtistTopSongResponse

    /**
     * 4. 推荐歌单（无需登录）
     */
    @GET("personalized")
    suspend fun getPersonalized(
        @Query("limit") limit: Int = 10
    ): PersonalizedResponse

    /**
     * 5. 每日推荐歌单（需登录，含个性化雷达歌单）
     */
    @GET("recommend/resource")
    suspend fun getRecommendResource(): RecommendResourceResponse

    /**
     * 6. 分类歌单（用于每周新热趋势、心情氛围等）
     */
    @GET("top/playlist")
    suspend fun getTopPlaylist(
        @Query("cat") cat: String? = null,
        @Query("order") order: String = "hot",
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): HotPlaylistResponse

    /**
     * 7. 歌单详情（含曲目）
     */
    @GET("playlist/detail")
    suspend fun getPlaylistDetail(
        @Query("id") id: Long
    ): PlaylistDetailResponse

    /**
     * 8. 每日推荐歌曲（需登录，用于漫游歌曲起点）
     */
    @GET("recommend/songs")
    suspend fun getDailyRecommendSongs(): DailyRecommendSongsResponse

    /**
     * 9. 心动模式 / 智能漫游
     */
    @GET("playmode/intelligence/list")
    suspend fun getIntelligenceList(
        @Query("id") id: Long,
        @Query("pid") pid: Long
    ): RadioIntelligenceResponse

    /**
     * 10. 关注的歌手列表（需登录）
     */
    @GET("artist/sublist")
    suspend fun getArtistSublist(
        @Query("limit") limit: Int = 10
    ): RadioArtistSublistResponse

    /**
     * 11. 关注歌手的新歌（需登录）
     */
    @GET("artist/new/song")
    suspend fun getArtistNewSong(
        @Query("limit") limit: Int = 10
    ): RadioArtistNewSongResponse

    /**
     * 12. 新歌速递（通用新歌兜底）
     */
    @GET("top/song")
    suspend fun getTopSongs(
        @Query("type") type: Int = 0
    ): RadioTopSongResponse

    /**
     * 13. 推荐新歌
     */
    @GET("personalized/newsong")
    suspend fun getPersonalizedNewsong(
        @Query("limit") limit: Int = 10
    ): PersonalizedNewsongResponse

    /**
     * 14. 相似歌曲（用于漫游生成）
     */
    @GET("simi/song")
    suspend fun getSimiSongs(
        @Query("id") id: Long
    ): RadioSimiSongResponse

    /**
     * 15. App 首页发现聚合模块（包含真实雷达歌单 MGC_PLAYLIST 等）
     */
    @GET("homepage/block/page")
    suspend fun getHomepageBlockPage(
        @Query("refresh") refresh: Boolean = false,
        @Query("cursor") cursor: String? = null
    ): HomepageBlockPageResponse
}

