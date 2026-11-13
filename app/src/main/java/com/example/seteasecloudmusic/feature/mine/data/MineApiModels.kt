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
    val tracks: List<PlaylistTrackItemResponse>? = emptyList(),
    @SerializedName("trackIds")
    val trackIds: List<PlaylistTrackIdItemResponse>? = emptyList()
)

data class PlaylistTrackIdItemResponse(
    @SerializedName("id")
    val id: Long? = 0L
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

/**
 * /likelist 接口响应：用户已喜欢歌曲 ID 列表
 */
data class LikelistResponse(
    @SerializedName("ids")
    val ids: List<Long>? = emptyList(),
    @SerializedName("code")
    val code: Int? = 0,
    @SerializedName("checkPoint")
    val checkPoint: Long? = null
)

/**
 * /like 接口响应：喜欢或取消喜欢
 */
data class LikeTrackResponse(
    @SerializedName("code")
    val code: Int? = 0,
    @SerializedName("playlistId")
    val playlistId: Long? = null
)

/**
 * /playlist/tracks 接口响应：歌单添加或删除歌曲
 */
data class PlaylistTracksResponse(
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("body")
    val body: PlaylistTracksBodyResponse? = null
) {
    val isAlreadyExists: Boolean
        get() {
            val msg = (message ?: body?.message).orEmpty()
            val c = body?.code ?: code ?: status ?: 0
            if (c == 502 || msg.contains("存在") || msg.contains("重复")) return true
            // 网易云 API 特性：如果成功添加，body.count 通常为 1；如果歌曲已存在，body.count 为 0
            if (body != null && body.count == 0) return true
            return false
        }

    val isSuccess: Boolean
        get() {
            if (isAlreadyExists) return false
            if (body != null && body.code != null && body.code != 200) return false
            if (code != null && code != 200) return false
            if (status != null && status != 200) return false
            return true
        }

    val errorMessage: String?
        get() = message ?: body?.message
}

data class PlaylistTracksBodyResponse(
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("count")
    val count: Int? = null,
    @SerializedName("message")
    val message: String? = null
)

/**
 * /playlist/create 接口响应：新建歌单
 */
data class CreatePlaylistResponse(
    @SerializedName("id")
    val id: Long? = 0L,
    @SerializedName("code")
    val code: Int? = 0,
    @SerializedName("playlist")
    val playlist: UserPlaylistItemResponse? = null
)

/**
 * /song/order/update 接口响应：更新歌单内歌曲顺序
 */
data class SongOrderUpdateResponse(
    @SerializedName("code")
    val code: Int? = 0,
    @SerializedName("message")
    val message: String? = null
)
