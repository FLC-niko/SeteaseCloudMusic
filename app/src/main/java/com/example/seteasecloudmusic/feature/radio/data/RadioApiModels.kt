package com.example.seteasecloudmusic.feature.radio.data

import com.google.gson.annotations.SerializedName

/**
 * /top/artists 热门歌手响应
 */
data class RadioTopArtistsResponse(
    val code: Int = 0,
    val more: Boolean = false,
    val artists: List<RadioTopArtistItemResponse> = emptyList()
)

data class RadioTopArtistItemResponse(
    val id: Long = 0L,
    val name: String = "",
    val picUrl: String? = null,
    val img1v1Url: String? = null
)

/**
 * /artist/top/song 歌手热门歌曲响应
 */
data class RadioArtistTopSongResponse(
    val code: Int = 0,
    val songs: List<RadioSongItemResponse> = emptyList()
)

data class RadioSongItemResponse(
    val id: Long = 0L,
    val name: String = "",
    val dt: Long? = null,
    val fee: Int? = null,
    @SerializedName("ar")
    val artists: List<RadioArtistReferenceResponse> = emptyList(),
    @SerializedName("al")
    val album: RadioAlbumReferenceResponse? = null
)

data class RadioArtistReferenceResponse(
    val id: Long? = null,
    val name: String? = null,
    val picUrl: String? = null
)

data class RadioAlbumReferenceResponse(
    val id: Long? = null,
    val name: String? = null,
    val picUrl: String? = null
)

/**
 * /playmode/intelligence/list 心动模式/智能漫游响应
 */
data class RadioIntelligenceResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: List<RadioIntelligenceItemResponse> = emptyList()
)

data class RadioIntelligenceItemResponse(
    val id: Long = 0L,
    val songInfo: RadioSongItemResponse? = null
)

/**
 * /artist/sublist 关注的歌手响应
 */
data class RadioArtistSublistResponse(
    val code: Int = 0,
    val count: Int = 0,
    val data: List<RadioTopArtistItemResponse> = emptyList()
)

/**
 * /artist/new/song 关注的歌手新歌响应
 */
data class RadioArtistNewSongResponse(
    val code: Int = 0,
    val data: List<RadioArtistNewSongItemResponse> = emptyList()
)

data class RadioArtistNewSongItemResponse(
    val id: Long = 0L,
    val song: RadioSongItemResponse? = null
)

/**
 * /top/song 新歌速递响应
 */
data class RadioTopSongResponse(
    val code: Int = 0,
    val data: List<RadioSongItemResponse> = emptyList()
)

/**
 * /simi/song 相似音乐/漫游推荐响应
 */
data class RadioSimiSongResponse(
    val code: Int = 0,
    val songs: List<RadioSongItemResponse> = emptyList()
)


