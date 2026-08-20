package com.example.seteasecloudmusic.feature.discover.data

import retrofit2.http.GET
import retrofit2.http.Query

interface DiscoverService {
    @GET("personalized")
    suspend fun getPersonalized(
        @Query("limit") limit: Int = 8
    ): PersonalizedResponse

    @GET("personalized/newsong")
    suspend fun getPersonalizedNewsong(
        @Query("limit") limit: Int = 10
    ): PersonalizedNewsongResponse

    @GET("toplist/detail")
    suspend fun getToplistDetail(): ToplistDetailResponse

    @GET("top/playlist")
    suspend fun getTopPlaylist(
        @Query("order") order: String = "hot",
        @Query("cat") cat: String? = null,
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): HotPlaylistResponse
}
