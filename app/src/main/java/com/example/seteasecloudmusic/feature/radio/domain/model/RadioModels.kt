package com.example.seteasecloudmusic.feature.radio.domain.model

import androidx.compose.ui.graphics.Color
import com.example.seteasecloudmusic.core.model.Track

/**
 * 1. 排行榜卡片数据模型，包含榜单基本信息、前几首预览曲目名及渐变底色。
 */
data class RadioToplistItem(
    val id: Long,
    val name: String,
    val playCount: Long? = null,
    val coverUrl: String? = null,
    val topSongs: List<String> = emptyList(),
    val gradientColors: List<Color> = emptyList()
)

/**
 * 2. 雷达歌单卡片数据模型。
 */
data class RadioRadarItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val coverUrl: String? = null,
    val playCount: Long? = null
)

/**
 * 3. 喜欢歌曲漫游卡片数据模型，聚合起点曲目与关联漫游曲目列表。
 */
data class RadioRoamItem(
    val id: Long,
    val songTitle: String,
    val artistName: String,
    val coverUrl: String? = null,
    val subtitle: String,
    val tracks: List<Track> = emptyList()
)

/**
 * 4. 歌手推荐头像卡片数据模型。
 */
data class RadioArtistItem(
    val id: Long,
    val name: String,
    val avatarUrl: String? = null
)

/**
 * 5. 推荐歌单卡片数据模型。
 */
data class RadioPlaylistItem(
    val id: Long,
    val title: String,
    val coverUrl: String? = null,
    val playCount: Long? = null,
    val tag: String? = null
)

/**
 * 6. 艺人热门金曲卡片数据模型。
 */
data class RadioArtistHotItem(
    val artistId: Long,
    val artistName: String,
    val title: String,
    val coverUrl: String? = null,
    val playCount: Long? = null,
    val songs: List<Track> = emptyList()
)

/**
 * 艺人热门金曲区块数据，包含领衔艺人信息与艺人卡片列表。
 */
data class ArtistHotSectionData(
    val leadArtistName: String = "AGA",
    val leadArtistAvatarUrl: String? = null,
    val items: List<RadioArtistHotItem> = emptyList()
)

/**
 * 7. 每周新热趋势卡片数据模型。
 */
data class RadioTrendItem(
    val id: Long,
    val title: String,
    val badge: String,
    val coverUrl: String? = null,
    val playCount: Long? = null
)

/**
 * 8. 心情氛围歌单卡片数据模型。
 */
data class RadioMoodItem(
    val id: Long,
    val title: String,
    val moodTag: String,
    val coverUrl: String? = null,
    val playCount: Long? = null
)

/**
 * 9. 关注艺人新动向单曲卡片数据模型。
 */
data class RadioFollowedArtistNewSongItem(
    val songId: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val coverUrl: String? = null,
    val tag: String = "新单曲",
    val track: Track? = null
)

/**
 * 关注艺人新动向区块数据，包含头部叠放头像列表与新单曲列表。
 */
data class FollowedArtistSectionData(
    val artistAvatars: List<String> = emptyList(),
    val items: List<RadioFollowedArtistNewSongItem> = emptyList()
)

