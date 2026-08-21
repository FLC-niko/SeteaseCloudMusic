package com.example.seteasecloudmusic.feature.mine.data

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 我的页面 API 接口声明
 */
interface MineService {

    /**
     * 获取用户歌单（包括我喜欢的音乐、创建的歌单、收藏的歌单）
     */
    @GET("user/playlist")
    suspend fun getUserPlaylists(
        @Query("uid") uid: Long,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): UserPlaylistResponse

    /**
     * 获取歌单详情（含歌曲列表）
     */
    @GET("playlist/detail")
    suspend fun getPlaylistDetail(
        @Query("id") id: Long
    ): PlaylistDetailResponse
}
