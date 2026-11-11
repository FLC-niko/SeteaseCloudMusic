package com.example.seteasecloudmusic.feature.search.data

import com.google.gson.annotations.SerializedName

/**
 * /search/suggest 接口的响应。
 */
data class SearchSuggestResponse(
    val result: SearchSuggestResultResponse? = null,
    val code: Int = 0
)

/**
 * 搜索建议结果主体。
 */
data class SearchSuggestResultResponse(
    @SerializedName("allMatch")
    val allMatch: List<SearchSuggestAllMatchResponse>? = null,
    @SerializedName("songs")
    val songs: List<SearchSuggestSongResponse>? = null,
    @SerializedName("artists")
    val artists: List<SearchSuggestArtistResponse>? = null,
    @SerializedName("playlists")
    val playlists: List<SearchSuggestPlaylistResponse>? = null
)

/**
 * 关键词完全匹配建议。
 */
data class SearchSuggestAllMatchResponse(
    val keyword: String? = null,
    val type: Int? = null
)

/**
 * 搜索建议中的单曲条目。
 */
data class SearchSuggestSongResponse(
    val id: Long = 0L,
    val name: String? = null,
    @SerializedName("ar")
    val artists: List<SearchSuggestArtistBriefResponse>? = null,
    @SerializedName("al")
    val album: SearchSuggestAlbumBriefResponse? = null,
    val duration: Long? = null,
    @SerializedName("mv")
    val mvId: Long? = null
)

/**
 * 搜索建议中的歌手条目。
 */
data class SearchSuggestArtistResponse(
    val id: Long = 0L,
    val name: String? = null,
    @SerializedName("picUrl")
    val picUrl: String? = null,
    @SerializedName("img1v1Url")
    val img1v1Url: String? = null,
    val alias: List<String>? = null
) {
    val coverUrl: String?
        get() {
            val pic = picUrl?.trim()?.takeIf { it.isNotBlank() && !it.contains("5639395138885805") }
            val img = img1v1Url?.trim()?.takeIf { it.isNotBlank() && !it.contains("5639395138885805") }
            val chosen = pic ?: img ?: picUrl?.trim()?.takeIf { it.isNotBlank() } ?: img1v1Url?.trim()?.takeIf { it.isNotBlank() }
            return chosen?.let { if (it.startsWith("http://")) it.replaceFirst("http://", "https://") else it }
        }
}

/**
 * 搜索建议中的歌单条目。
 */
data class SearchSuggestPlaylistResponse(
    val id: Long = 0L,
    val name: String? = null,
    @SerializedName("coverImgUrl")
    val coverUrl: String? = null,
    val trackCount: Int? = null,
    val creator: SearchSuggestUserBriefResponse? = null
)

/**
 * 歌手简要信息。
 */
data class SearchSuggestArtistBriefResponse(
    val id: Long? = null,
    val name: String? = null
)

/**
 * 专辑简要信息。
 */
data class SearchSuggestAlbumBriefResponse(
    val id: Long? = null,
    val name: String? = null,
    @SerializedName("picUrl")
    val coverUrl: String? = null
)

/**
 * 用户信息简要。
 */
data class SearchSuggestUserBriefResponse(
    val userId: Long? = null,
    val nickname: String? = null
)