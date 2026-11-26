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

/** 热门歌手条目 DTO */
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

/** 通用歌曲条目 DTO（dt/ar/al 与 duration/artists/album 适配不同接口字段名） */
data class RadioSongItemResponse(
    val id: Long = 0L,
    val name: String = "",
    @SerializedName("dt", alternate = ["duration"])
    val dt: Long? = null,
    val fee: Int? = null,
    @SerializedName("ar", alternate = ["artists"])
    val artists: List<RadioArtistReferenceResponse> = emptyList(),
    @SerializedName("al", alternate = ["album"])
    val album: RadioAlbumReferenceResponse? = null
)

/** 歌曲关联歌手引用 DTO */
data class RadioArtistReferenceResponse(
    val id: Long? = null,
    val name: String? = null,
    val picUrl: String? = null
)

/** 歌曲关联专辑引用 DTO */
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

/** 智能漫游推荐条目 DTO */
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

/** 关注歌手新歌条目 DTO */
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


