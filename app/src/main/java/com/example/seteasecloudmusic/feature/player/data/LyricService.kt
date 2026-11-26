package com.example.seteasecloudmusic.feature.player.data

import retrofit2.http.GET
import retrofit2.http.Query

/** 网易云音乐歌词 Retrofit 接口 */
interface LyricService {

    /** 获取标准逐行歌词及翻译/音译（/lyric） */
    @GET("lyric")
    suspend fun getLyrics(
        @Query("id") id: Long
    ): LyricResponseDto

    /** 获取逐字歌词 YRC 及翻译/音译（/lyric/new） */
    @GET("lyric/new")
    suspend fun getYrcLyrics(
        @Query("id") id: Long
    ): LyricResponseDto
}
