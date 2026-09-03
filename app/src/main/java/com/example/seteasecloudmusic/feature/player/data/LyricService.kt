package com.example.seteasecloudmusic.feature.player.data

import retrofit2.http.GET
import retrofit2.http.Query

interface LyricService {

    @GET("lyric")
    suspend fun getLyrics(
        @Query("id") id: Long
    ): LyricResponseDto

    @GET("lyric/new")
    suspend fun getYrcLyrics(
        @Query("id") id: Long
    ): LyricResponseDto
}
