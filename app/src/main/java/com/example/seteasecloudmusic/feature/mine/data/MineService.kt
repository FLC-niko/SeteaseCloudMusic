package com.example.seteasecloudmusic.feature.mine.data

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
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
        @Query("offset") offset: Int = 0,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): UserPlaylistResponse

    /**
     * 获取歌单详情（含歌曲列表）
     */
    @GET("playlist/detail")
    suspend fun getPlaylistDetail(
        @Query("id") id: Long,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): PlaylistDetailResponse

    /**
     * 获取用户喜欢的音乐 ID 列表
     */
    @GET("likelist")
    suspend fun getLikelist(
        @Query("uid") uid: Long,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): LikelistResponse

    /**
     * 喜欢 / 取消喜欢音乐
     * @param id 歌曲 id
     * @param like true 为喜欢，false 为取消喜欢
     */
    @GET("like")
    suspend fun likeTrack(
        @Query("id") id: Long,
        @Query("like") like: Boolean,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): LikeTrackResponse

    /**
     * 歌单添加或删除歌曲
     * @param op 操作类型: "add" 添加, "del" 删除
     * @param pid 歌单 id
     * @param tracks 歌曲 id 列表，多个用逗号隔开，如 "12345"
     */
    @GET("playlist/tracks")
    suspend fun updatePlaylistTracks(
        @Query("op") op: String,
        @Query("pid") pid: Long,
        @Query("tracks") tracks: String,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): PlaylistTracksResponse

    /**
     * 新建歌单
     * @param name 歌单名称
     * @param privacy 0 为普通歌单，10 为隐私歌单
     */
    @GET("playlist/create")
    suspend fun createPlaylist(
        @Query("name") name: String,
        @Query("privacy") privacy: Int = 0,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): CreatePlaylistResponse

    /**
     * 调整歌单内歌曲顺序（同步至网易云服务端）
     * @param pid 歌单 id
     * @param ids 调整顺序后的歌曲 id 列表 JSON 数组字符串，如 "[5268328,1219871]"
     */
    @FormUrlEncoded
    @POST("song/order/update")
    suspend fun updatePlaylistTrackOrder(
        @Field("pid") pid: Long,
        @Field("ids") ids: String,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): SongOrderUpdateResponse
}
