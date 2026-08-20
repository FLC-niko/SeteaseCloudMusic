package com.example.seteasecloudmusic.feature.discover.data

import com.google.gson.annotations.SerializedName

data class PersonalizedResponse(
    val code: Int = 0,
    val result: List<PersonalizedItemResponse> = emptyList()
)

data class PersonalizedItemResponse(
    val id: Long = 0L,
    val name: String = "",
    val copywriter: String? = null,
    val picUrl: String? = null,
    val playCount: Long? = null,
    val trackCount: Int? = null
)

data class PersonalizedNewsongResponse(
    val code: Int = 0,
    val result: List<PersonalizedNewsongItemResponse> = emptyList()
)

data class PersonalizedNewsongItemResponse(
    val id: Long = 0L,
    val name: String? = null,
    val picUrl: String? = null,
    val song: NewsongItemResponse? = null
)

data class NewsongItemResponse(
    val id: Long = 0L,
    val name: String? = null,
    @SerializedName("artists")
    val artists: List<NewsongArtistResponse> = emptyList(),
    @SerializedName("album")
    val album: NewsongAlbumResponse? = null,
    val fee: Int? = null,
    val privilege: NewsongPrivilegeResponse? = null
)

data class NewsongArtistResponse(
    val id: Long = 0L,
    val name: String? = null
)

data class NewsongAlbumResponse(
    val id: Long = 0L,
    val name: String? = null,
    val picUrl: String? = null
)

data class NewsongPrivilegeResponse(
    val st: Int? = null,
    val pl: Long? = null
)

data class ToplistDetailResponse(
    val code: Int = 0,
    val list: List<ToplistItemResponse> = emptyList()
)

data class ToplistItemResponse(
    val id: Long = 0L,
    val name: String? = null,
    val coverImgUrl: String? = null,
    val updateFrequency: String? = null,
    val tracks: List<ToplistTrackPreviewResponse> = emptyList()
)

data class ToplistTrackPreviewResponse(
    val first: String? = null,
    val second: String? = null
)

data class HotPlaylistResponse(
    val code: Int = 0,
    val playlists: List<HotPlaylistItemResponse> = emptyList()
)

data class HotPlaylistItemResponse(
    val id: Long = 0L,
    val name: String = "",
    val coverImgUrl: String? = null,
    val playCount: Long? = null,
    val trackCount: Int? = null,
    val creator: HotPlaylistCreatorResponse? = null
)

data class HotPlaylistCreatorResponse(
    val nickname: String? = null
)
