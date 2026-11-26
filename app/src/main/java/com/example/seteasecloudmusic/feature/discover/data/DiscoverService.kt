package com.example.seteasecloudmusic.feature.discover.data

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 发现页 Retrofit 接口定义，提供推荐歌单、推荐新歌、榜单与热门歌单接口。
 */
interface DiscoverService {
    @GET("personalized")
    suspend fun getPersonalized(
        @Query("limit") limit: Int = 8 // 推荐歌单默认拉取条数
    ): PersonalizedResponse

    @GET("personalized/newsong")
    suspend fun getPersonalizedNewsong(
        @Query("limit") limit: Int = 10 // 推荐新歌默认拉取条数
    ): PersonalizedNewsongResponse

    @GET("toplist/detail")
    suspend fun getToplistDetail(): ToplistDetailResponse

    @GET("top/playlist")
    suspend fun getTopPlaylist(
        @Query("order") order: String = "hot",
        @Query("cat") cat: String? = null,
        @Query("limit") limit: Int = 10, // 热门歌单单页拉取条数
        @Query("offset") offset: Int = 0 // 热门歌单分页偏移量
    ): HotPlaylistResponse
}
