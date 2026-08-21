package com.example.seteasecloudmusic.feature.mine.data

import com.google.gson.annotations.SerializedName

/**
 * /user/playlist 接口响应
 */
data class UserPlaylistResponse(
    @SerializedName("playlist")
    val playlist: List<UserPlaylistItemResponse>? = emptyList(),
    @SerializedName("code")
    val code: Int? = 0,
    @SerializedName("more")
    val more: Boolean? = null
)

data class UserPlaylistItemResponse(
    @SerializedName("id")
    val id: Long? = 0L,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("coverImgUrl")
    val coverImgUrl: String? = null,
    @SerializedName("trackCount")
    val trackCount: Int? = 0,
    @SerializedName("playCount")
    val playCount: Long? = 0L,
    @SerializedName("userId")
    val userId: Long? = 0L,
    @SerializedName("specialType")
    val specialType: Int? = 0,
    @SerializedName("subscribed")
    val subscribed: Boolean? = false,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("creator")
    val creator: UserPlaylistCreatorResponse? = null
)

data class UserPlaylistCreatorResponse(
    @SerializedName("userId")
    val userId: Long? = 0L,
    @SerializedName("nickname")
    val nickname: String? = null,
    @SerializedName("avatarUrl")
    val avatarUrl: String? = null
)

/**
 * /playlist/detail 接口响应
 */
data class PlaylistDetailResponse(
    @SerializedName("playlist")
    val playlist: PlaylistDetailItemResponse? = null,
    @SerializedName("code")
    val code: Int? = 0
)

data class PlaylistDetailItemResponse(
    @SerializedName("id")
    val id: Long? = 0L,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("coverImgUrl")
    val coverImgUrl: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("trackCount")
    val trackCount: Int? = 0,
    @SerializedName("playCount")
    val playCount: Long? = 0L,
    @SerializedName("creator")
    val creator: UserPlaylistCreatorResponse? = null,
    @SerializedName("tracks")
    val tracks: List<PlaylistTrackItemResponse>? = emptyList()
)

data class PlaylistTrackItemResponse(
    @SerializedName("id")
    val id: Long? = 0L,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("ar")
    val ar: List<PlaylistArtistItemResponse>? = emptyList(),
    @SerializedName("al")
    val al: PlaylistAlbumItemResponse? = null,
    @SerializedName("dt")
    val dt: Long? = 0L,
    @SerializedName("fee")
    val fee: Int? = 0
)

data class PlaylistArtistItemResponse(
    @SerializedName("id")
    val id: Long? = 0L,
    @SerializedName("name")
    val name: String? = null
)

data class PlaylistAlbumItemResponse(
    @SerializedName("id")
    val id: Long? = 0L,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("picUrl")
    val picUrl: String? = null
)
