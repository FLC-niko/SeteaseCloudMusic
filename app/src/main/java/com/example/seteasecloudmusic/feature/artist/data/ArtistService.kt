package com.example.seteasecloudmusic.feature.artist.data

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 歌手模块 Retrofit HTTP 接口定义。
 */
interface ArtistService {
    @GET("artist/top/song")
    suspend fun getArtistTopSongs(
        @Query("id") artistId: Long
    ): ArtistSongsResponse

    @GET("artist/songs")
    suspend fun getArtistSongs(
        @Query("id") artistId: Long,
        @Query("order") order: String = "hot",
        @Query("limit") limit: Int = 50, // 默认单页拉取歌曲数
        @Query("offset") offset: Int = 0
    ): ArtistSongsResponse

    @GET("artist/album")
    suspend fun getArtistAlbums(
        @Query("id") artistId: Long,
        @Query("limit") limit: Int = 30, // 默认单页拉取专辑数
        @Query("offset") offset: Int = 0
    ): ArtistAlbumsResponse

    @GET("album")
    suspend fun getAlbumDetail(
        @Query("id") albumId: Long
    ): ArtistAlbumDetailResponse

    @GET("artist/desc")
    suspend fun getArtistDescription(
        @Query("id") artistId: Long
    ): ArtistDescriptionResponse

    @GET("artist/detail")
    suspend fun getArtistDetail(
        @Query("id") artistId: Long
    ): ArtistDetailResponse

    @GET("user/playlist")
    suspend fun getArtistUserPlaylists(
        @Query("uid") uid: Long,
        @Query("limit") limit: Int = 20, // 默认单页拉取关联用户歌单数
        @Query("offset") offset: Int = 0
    ): ArtistUserPlaylistsResponse

    @GET("simi/artist")
    suspend fun getSimilarArtists(
        @Query("id") artistId: Long
    ): SimilarArtistsResponse

    @GET("top/artists")
    suspend fun getTopArtistsFallback(
        @Query("limit") limit: Int = 16, // 相似歌手兜底列表单次拉取人数
        @Query("offset") offset: Int = 0
    ): SimilarArtistsResponse
}
