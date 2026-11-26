package com.example.seteasecloudmusic.feature.artist.data

import com.google.gson.annotations.SerializedName

/**
 * `/artist/songs` 与 `/artist/top/song` 接口响应模型。
 */
data class ArtistSongsResponse(
    val code: Int = 0,
    val more: Boolean = false,
    val songs: List<ArtistSongItemResponse> = emptyList()
)

data class ArtistSongItemResponse(
    val id: Long = 0L,
    val name: String = "",
    val fee: Int? = null,
    val dt: Long? = null,
    val publishTime: Long? = null,
    @SerializedName("ar")
    val artists: List<ArtistSongArtistResponse> = emptyList(),
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
 * `/artist/album` 接口响应模型。
 */
data class ArtistAlbumsResponse(
    val code: Int = 0,
    val more: Boolean = false,
    @SerializedName("hotAlbums")
    val hotAlbums: List<ArtistAlbumItemResponse> = emptyList()
)

data class ArtistAlbumItemResponse(
    val id: Long = 0L,
    val name: String = "",
    @SerializedName("picUrl")
    val coverUrl: String? = null,
    val size: Int? = null,
    val publishTime: Long? = null,
    val description: String? = null,
    val artist: ArtistSongArtistResponse? = null
)

/**
 * `/album` 专辑详情接口响应模型。
 */
data class ArtistAlbumDetailResponse(
    val code: Int = 0,
    val album: ArtistAlbumItemResponse? = null,
    val songs: List<ArtistSongItemResponse> = emptyList()
)

/**
 * `/artist/desc` 歌手简介与百科分段响应模型。
 */
data class ArtistDescriptionResponse(
    val code: Int = 0,
    val briefDesc: String? = null,
    val introduction: List<ArtistDescriptionSectionResponse> = emptyList()
)

data class ArtistDescriptionSectionResponse(
    @SerializedName("ti")
    val title: String? = null,
    @SerializedName("txt")
    val content: String? = null
)

/**
 * `/artist/detail` 歌手基础资料响应模型。
 */
data class ArtistDetailResponse(
    val code: Int = 0,
    val data: ArtistDetailDataResponse? = null
)

data class ArtistDetailDataResponse(
    val artist: ArtistProfileResponse? = null,
    val cover: String? = null,
    val user: ArtistUserProfileResponse? = null
)

data class ArtistUserProfileResponse(
    val userId: Long? = null,
    val nickname: String? = null,
    val avatarUrl: String? = null
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
    val mvSize: Int? = null,
    val alias: List<String> = emptyList()
)

/**
 * `/simi/artist` 与 `/top/artists` 相似/热门歌手列表响应模型。
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
    val imageUrl: String? = null,
    val alias: List<String> = emptyList(),
    val trans: String? = null,
    val musicSize: Int? = null
)

/**
 * `/user/playlist` 艺人关联账号爱听歌单响应模型。
 */
data class ArtistUserPlaylistsResponse(
    val code: Int = 0,
    val playlist: List<ArtistUserPlaylistItemResponse> = emptyList()
)

data class ArtistUserPlaylistItemResponse(
    val id: Long = 0L,
    val name: String? = null,
    val coverImgUrl: String? = null,
    val trackCount: Int = 0,
    val playCount: Long = 0L,
    val specialType: Int = 0
)
