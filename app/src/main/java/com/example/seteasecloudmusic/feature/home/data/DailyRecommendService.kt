package com.example.seteasecloudmusic.feature.home.data

import com.example.seteasecloudmusic.feature.mine.data.PlaylistDetailResponse
import com.example.seteasecloudmusic.feature.radio.data.RadioIntelligenceResponse
import com.example.seteasecloudmusic.feature.radio.data.RadioSimiSongResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 首页推荐相关网络接口（每日推荐歌曲、推荐歌单、歌单详情、私人 DJ / 私人 FM 等）
 */
interface DailyRecommendService {
    @GET("recommend/songs")
    suspend fun getDailyRecommendSongs(
        @Query("afresh") afresh: Boolean = false
    ): DailyRecommendSongsResponse

    /**
     * 获取每日推荐歌单（需要登录）
     */
    @GET("recommend/resource")
    suspend fun getRecommendResource(): RecommendResourceResponse

    /**
     * 获取指定歌单详情（含歌曲列表）
     */
    @GET("playlist/detail")
    suspend fun getPlaylistDetail(
        @Query("id") id: Long
    ): PlaylistDetailResponse

    /**
     * 获取 APP 首页模块信息（包括 HOMEPAGE_BLOCK_STYLE_RCMD 风格推荐模块、HOMEPAGE_BLOCK_MGC_PLAYLIST 雷达模块）
     */
    @GET("homepage/block/page")
    suspend fun getHomepageBlockPage(
        @Query("refresh") refresh: Boolean = false,
        @Query("cursor") cursor: String? = null
    ): HomepageBlockPageResponse

    /**
     * 私人 DJ / 私人 FM 模式接口（mode=AIDJ / DEFAULT 等）
     */
    @GET("personal/fm/mode")
    suspend fun getPersonalFmMode(
        @Query("mode") mode: String = "AIDJ",
        @Query("submode") submode: String? = null,
        @Query("limit") limit: Int = 10,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): PersonalFmResponse

    /**
     * 经典私人 FM 接口（/personal_fm -> /api/v1/radio/get）
     */
    @GET("personal_fm")
    suspend fun getPersonalFm(
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): PersonalFmResponse

    /**
     * 相似歌曲接口（用于首页相似歌曲卡片推荐）
     */
    @GET("simi/song")
    suspend fun getSimiSongs(
        @Query("id") id: Long,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): RadioSimiSongResponse

    /**
     * 心动模式 / 智能漫游接口（红心歌曲与相似推荐混排）
     */
    @GET("playmode/intelligence/list")
    suspend fun getIntelligenceList(
        @Query("id") id: Long,
        @Query("pid") pid: Long,
        @Query("sid") sid: Long? = null,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): RadioIntelligenceResponse
}
