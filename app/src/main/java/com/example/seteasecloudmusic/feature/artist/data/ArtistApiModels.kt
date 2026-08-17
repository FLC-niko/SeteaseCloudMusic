package com.example.seteasecloudmusic.feature.artist.data

import com.google.gson.annotations.SerializedName

/**
 * /artist/songs response.
 */
data class ArtistSongsResponse(
    val code: Int = 0,
    val more: Boolean = false,
    val songs: List<ArtistSongItemResponse>? = null
)

data class ArtistSongItemResponse(
    val id: Long = 0L,
    val name: String = "",
    val fee: Int? = null,
    val dt: Long? = null,
    @SerializedName("ar")
    val artists: List<ArtistSongArtistResponse>? = null,
    @SerializedName("al")
    val album: ArtistSongAlbumResponse? = null,
    val h: ArtistSongQualityResponse? = null,
    val m: ArtistSongQualityResponse? = null,
    val l: ArtistSongQualityResponse? = null,
    val sq: ArtistSongQualityResponse? = null,
    val hr: ArtistSongQualityResponse? = null
)

data class ArtistSongArtistResponse(
    val id: Long? = null,
    val name: String? = null,
    @SerializedName("picUrl")
    val picUrl: String? = null,
    @SerializedName("img1v1Url")
    val imageUrl: String? = null
)

data class ArtistSongAlbumResponse(
    val id: Long? = null,
    val name: String? = null,
    @SerializedName("picUrl")
    val coverUrl: String? = null
)

data class ArtistSongQualityResponse(
    val br: Long? = null,
    val fid: Long? = null,
    val size: Long? = null,
    val vd: Long? = null,
    val sr: Int? = null
)

/**
 * /artist/album response.
 */
data class ArtistAlbumsResponse(
    val code: Int = 0,
    val more: Boolean = false,
    @SerializedName("hotAlbums")
    val hotAlbums: List<ArtistAlbumItemResponse>? = null
)

data class ArtistAlbumItemResponse(
    val id: Long = 0L,
    val name: String = "",
    @SerializedName("picUrl")
    val coverUrl: String? = null,
    val size: Int? = null,
    val publishTime: Long? = null
)

/**
 * /artist/desc response.
 */
data class ArtistDescriptionResponse(
    val code: Int = 0,
    val briefDesc: String? = null,
    val introduction: List<ArtistDescriptionSectionResponse>? = null
)

data class ArtistDescriptionSectionResponse(
    @SerializedName("ti")
    val title: String? = null,
    @SerializedName("txt")
    val content: String? = null
)

/**
 * /artist/detail response.
 */
data class ArtistDetailResponse(
    val code: Int = 0,
    val data: ArtistDetailDataResponse? = null
)

data class ArtistDetailDataResponse(
    val artist: ArtistProfileResponse? = null,
    val cover: String? = null
)

data class ArtistProfileResponse(
    val id: Long = 0L,
    val name: String? = null,
    @SerializedName("picUrl")
    val picUrl: String? = null,
    @SerializedName("cover")
    val coverUrl: String? = null,
    val musicSize: Int? = null,
    val albumSize: Int? = null,
    val mvSize: Int? = null
)

/**
 * /simi/artist response.
 */
data class SimilarArtistsResponse(
    val code: Int = 0,
    val artists: List<SimilarArtistItemResponse> = emptyList()
)

data class SimilarArtistItemResponse(
    val id: Long = 0L,
    val name: String? = null,
    @SerializedName("picUrl")
    val picUrl: String? = null,
    @SerializedName("img1v1Url")
    val imageUrl: String? = null
)
