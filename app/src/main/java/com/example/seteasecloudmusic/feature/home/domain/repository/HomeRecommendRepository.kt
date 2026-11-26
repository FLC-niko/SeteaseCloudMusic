package com.example.seteasecloudmusic.feature.home.domain.repository

import com.example.seteasecloudmusic.core.model.Track

/** 首页雷达歌单（私人雷达 / 新歌雷达）展示数据。 */
data class RadarPlaylistData(
    val id: Long,
    val name: String,
    val description: String?,
    val coverUrl: String?,
    val trackCount: Int,
    val tracks: List<Track>
)

/** 首页「根据你喜爱的歌曲推荐」区块数据。 */
data class FavoriteRecommendSectionData(
    val title: String = "根据你喜爱的歌曲推荐",
    val tracks: List<Track> = emptyList()
)

/** 首页推荐数据仓库接口，封装网络拉取、本地缓存读取与降级策略。 */
interface HomeRecommendRepository {
    /**
     * 获取每日推荐歌曲列表（afresh=true 时强制请求新数据）
     */
    suspend fun getDailyRecommendSongs(
        afresh: Boolean = false
    ): Result<List<Track>>

    /**
     * 获取本地缓存的每日推荐歌曲列表
     */
    fun getCachedDailyRecommendSongs(): List<Track>?

    /**
     * 获取雷达歌单（含歌曲列表）
     */
    suspend fun getRadarPlaylist(): Result<RadarPlaylistData>

    /**
     * 获取本地缓存的雷达歌单
     */
    fun getCachedRadarPlaylist(): RadarPlaylistData?

    /**
     * 获取新歌雷达歌单（含歌曲列表）
     */
    suspend fun getNewSongRadarPlaylist(): Result<RadarPlaylistData>

    /**
     * 获取本地缓存的新歌雷达歌单
     */
    fun getCachedNewSongRadarPlaylist(): RadarPlaylistData?

    /**
     * 获取私人 DJ / 私人 FM 推荐歌曲列表
     */
    suspend fun getPrivateDjSongs(): Result<List<Track>>

    /**
     * 获取本地缓存的私人 DJ 歌曲列表
     */
    fun getCachedPrivateDjSongs(): List<Track>?

    /**
     * 获取首页「相似歌曲」漫游推荐歌曲列表
     */
    suspend fun getSimiRecommendSongs(): Result<List<Track>>

    /**
     * 获取本地缓存的相似歌曲列表
     */
    fun getCachedSimiRecommendSongs(): List<Track>?

    /**
     * 获取首页「心动模式」（/playmode/intelligence/list）歌曲列表
     */
    suspend fun getHeartModeSongs(): Result<List<Track>>

    /**
     * 获取本地缓存的心动模式歌曲列表
     */
    fun getCachedHeartModeSongs(): List<Track>?

    /**
     * 获取首页「根据你喜爱的歌曲推荐」（来自 /homepage/block/page 的 HOMEPAGE_BLOCK_STYLE_RCMD）
     */
    suspend fun getFavoriteRecommendSongs(
        afresh: Boolean = false
    ): Result<FavoriteRecommendSectionData>

    /**
     * 获取本地缓存的喜好风格推荐歌曲
     */
    fun getCachedFavoriteRecommendSongs(): FavoriteRecommendSectionData?

    /**
     * 获取用户「我喜欢的音乐」最新红心单曲封面（供心动模式卡片使用）
     */
    suspend fun getUserLikedMusicCover(): String?
}
