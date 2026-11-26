package com.example.seteasecloudmusic.feature.artist.domain.model

import com.example.seteasecloudmusic.core.model.Track

/** 歌手基础资料与统计信息。 */
data class ArtistDetailInfo(
    val id: Long,
    val name: String,
    val coverUrl: String?,
    val musicCount: Int?,
    val albumCount: Int?,
    val mvCount: Int?,
    val userId: Long? = null,
    val alias: String? = null
)

/** 歌手百科与分段经历。 */
data class ArtistDescription(
    val brief: String,
    val sections: List<ArtistDescriptionSection>
)

data class ArtistDescriptionSection(
    val title: String,
    val content: String
)

data class ArtistAlbum(
    val id: Long,
    val title: String,
    val coverUrl: String?,
    val publishYear: Int?,
    val trackCount: Int?
)

data class ArtistSummary(
    val id: Long,
    val name: String,
    val coverUrl: String?,
    val subtitle: String? = null
)

data class ArtistFavoritePlaylist(
    val id: Long,
    val name: String,
    val coverUrl: String?,
    val trackCount: Int,
    val playCount: Long,
    val isLikedPlaylist: Boolean = false
)

data class ArtistTopSongsResult(
    val tracks: List<Track>,
    val publishYears: Map<Long, Int> = emptyMap()
)

data class ArtistAlbumDetail(
    val album: ArtistAlbum,
    val artistName: String,
    val description: String?,
    val tracks: List<Track>
)

/** 歌手歌曲列表排序方式。 */
enum class ArtistSongOrder(val value: String) {
    HOT("hot"),
    TIME("time")
}

/** 通用分页结果包装。 */
data class PagedResult<T>(
    val items: List<T>,
    val nextOffset: Int,
    val hasMore: Boolean
)
