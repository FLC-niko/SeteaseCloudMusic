package com.example.seteasecloudmusic.feature.radio.domain.model

import androidx.compose.ui.graphics.Color
import com.example.seteasecloudmusic.core.model.Track

/**
 * 1. 排行榜
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
 * 2. 雷达歌单
 */
data class RadioRadarItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val coverUrl: String? = null,
    val playCount: Long? = null
)

/**
 * 3. 从你喜欢的歌曲开始漫游
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
 * 4. 歌手推荐
 */
data class RadioArtistItem(
    val id: Long,
    val name: String,
    val avatarUrl: String? = null
)

/**
 * 5. 推荐歌单
 */
data class RadioPlaylistItem(
    val id: Long,
    val title: String,
    val coverUrl: String? = null,
    val playCount: Long? = null,
    val tag: String? = null
)

/**
 * 6. 艺人热门金曲
 */
data class RadioArtistHotItem(
    val artistId: Long,
    val artistName: String,
    val title: String,
    val coverUrl: String? = null,
    val playCount: Long? = null,
    val songs: List<Track> = emptyList()
)

data class ArtistHotSectionData(
    val leadArtistName: String = "AGA",
    val leadArtistAvatarUrl: String? = null,
    val items: List<RadioArtistHotItem> = emptyList()
)

/**
 * 7. 每周新热趋势
 */
data class RadioTrendItem(
    val id: Long,
    val title: String,
    val badge: String,
    val coverUrl: String? = null,
    val playCount: Long? = null
)

/**
 * 8. 心情氛围
 */
data class RadioMoodItem(
    val id: Long,
    val title: String,
    val moodTag: String,
    val coverUrl: String? = null,
    val playCount: Long? = null
)

/**
 * 9. 你关注的艺人新动向
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

data class FollowedArtistSectionData(
    val artistAvatars: List<String> = emptyList(),
    val items: List<RadioFollowedArtistNewSongItem> = emptyList()
)

