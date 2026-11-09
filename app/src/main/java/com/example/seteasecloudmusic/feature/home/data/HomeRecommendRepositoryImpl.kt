package com.example.seteasecloudmusic.feature.home.data

import android.content.Context
import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.AudioQuality
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.discover.data.DiscoverService
import com.example.seteasecloudmusic.feature.discover.data.NewsongItemResponse
import com.example.seteasecloudmusic.feature.home.domain.repository.FavoriteRecommendSectionData
import com.example.seteasecloudmusic.feature.home.domain.repository.HomeRecommendRepository
import com.example.seteasecloudmusic.feature.mine.data.MineCacheManager
import com.example.seteasecloudmusic.feature.mine.data.MineService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HomeRecommendRepositoryImpl @Inject constructor(
    private val dailyRecommendService: DailyRecommendService,
    private val discoverService: DiscoverService,
    private val homeRecommendCache: HomeRecommendCache,
    private val mineService: MineService,
    private val mineCacheManager: MineCacheManager,
    @param:ApplicationContext private val context: Context
) : HomeRecommendRepository {

    override fun getCachedDailyRecommendSongs(): List<Track>? {
        return homeRecommendCache.getDailyRecommend()
    }

    override fun getCachedRadarPlaylist(): com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData? {
        return homeRecommendCache.getRadarPlaylist()
    }

    override suspend fun getRadarPlaylist(): Result<com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData> =
        withContext(Dispatchers.IO) {
            try {
                // 1. 若有登录态，优先尝试从推荐歌单 /recommend/resource 中获取含“雷达”的歌单 ID
                var targetPlaylistId = 3136952023L // 官方网易云“私人雷达”默认歌单 ID
                if (hasLoginCookie()) {
                    try {
                        val resourceResp = dailyRecommendService.getRecommendResource()
                        if (resourceResp.code == 200 && resourceResp.recommend.isNotEmpty()) {
                            val radarPl = resourceResp.recommend.firstOrNull { it.name?.contains("雷达") == true }
                                ?: resourceResp.recommend.firstOrNull()
                            if (radarPl?.id != null && radarPl.id > 0L) {
                                targetPlaylistId = radarPl.id
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // 继续使用默认私人雷达 ID
                    }
                }

                // 2. 调用 /playlist/detail?id=... 获取完整歌单与曲目
                try {
                    val detailResp = dailyRecommendService.getPlaylistDetail(targetPlaylistId)
                    if (detailResp.code == 200 && detailResp.playlist != null) {
                        val pl = detailResp.playlist
                        val tracks = (pl.tracks ?: emptyList()).map { item ->
                            Track(
                                id = item.id ?: 0L,
                                title = item.name?.takeIf { it.isNotBlank() } ?: "未知歌曲",
                                artists = (item.ar ?: emptyList()).map { ar ->
                                    Artist(
                                        id = ar.id ?: 0L,
                                        name = ar.name?.takeIf { it.isNotBlank() } ?: "未知歌手",
                                        coverUrl = null
                                    )
                                }.ifEmpty { listOf(Artist(id = 0L, name = "未知歌手")) },
                                album = Album(
                                    id = item.al?.id ?: 0L,
                                    title = item.al?.name?.takeIf { it.isNotBlank() } ?: "未知专辑",
                                    coverUrl = item.al?.picUrl
                                ),
                                coverUrl = item.al?.picUrl ?: pl.coverImgUrl,
                                durationMs = item.dt ?: 0L,
                                playableUrl = null,
                                isPlayable = (item.fee ?: 0) != 4,
                                recommendReason = "私人雷达"
                            )
                        }.distinctBy { it.id }

                        if (tracks.isNotEmpty()) {
                            val radarData = com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData(
                                id = pl.id ?: targetPlaylistId,
                                name = pl.name?.takeIf { it.isNotBlank() } ?: "私人雷达",
                                description = pl.description?.takeIf { it.isNotBlank() } ?: "反复聆听你最爱的歌",
                                coverUrl = pl.coverImgUrl,
                                trackCount = pl.trackCount ?: tracks.size,
                                tracks = tracks
                            )
                            homeRecommendCache.saveRadarPlaylist(radarData)
                            return@withContext Result.success(radarData)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 走缓存或兜底
                }

                // 3. 查本地缓存
                val cached = homeRecommendCache.getRadarPlaylist()
                if (cached != null && cached.tracks.isNotEmpty()) {
                    return@withContext Result.success(cached)
                }

                // 4. 极致兜底：静态预设雷达歌单
                Result.success(getDefaultRadarPlaylist())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val cached = homeRecommendCache.getRadarPlaylist()
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.success(getDefaultRadarPlaylist())
                }
            }
        }

    override suspend fun getDailyRecommendSongs(afresh: Boolean): Result<List<Track>> =
        withContext(Dispatchers.IO) {
            try {
                // 1. 若有登录态，优先尝试官方每日推荐接口 /recommend/songs
                if (hasLoginCookie()) {
                    try {
                        val response = dailyRecommendService.getDailyRecommendSongs(afresh)
                        if (response.code == 200) {
                            val songs = response.data?.dailySongs
                                ?.takeIf { it.isNotEmpty() }
                                ?: response.dailySongs

                            val tracks = songs
                                .map(::mapToTrack)
                                .distinctBy { it.id }

                            if (tracks.isNotEmpty()) {
                                homeRecommendCache.saveDailyRecommend(tracks)
                                return@withContext Result.success(tracks)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // 网络或接口异常时继续向下走兜底
                    }
                }

                // 2. 若未登录或日推失败，先看本地持久化缓存
                val cached = homeRecommendCache.getDailyRecommend()
                if (!cached.isNullOrEmpty()) {
                    return@withContext Result.success(cached)
                }

                // 3. 本地无缓存时，降级调用无需登录的推荐新歌接口 /personalized/newsong
                try {
                    val newsongResponse = discoverService.getPersonalizedNewsong(limit = 18)
                    if (newsongResponse.code == 200 && newsongResponse.result.isNotEmpty()) {
                        val fallbackTracks = newsongResponse.result.mapNotNull { item ->
                            item.song?.let { song ->
                                mapNewsongToTrack(song, item.picUrl)
                            }
                        }.distinctBy { it.id }

                        if (fallbackTracks.isNotEmpty()) {
                            homeRecommendCache.saveDailyRecommend(fallbackTracks)
                            return@withContext Result.success(fallbackTracks)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 继续
                }

                // 4. 极致兜底：静态预设推荐歌曲（确保冷启动与断网均能完美呈现）
                Result.success(getDefaultFallbackTracks())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val cached = homeRecommendCache.getDailyRecommend()
                if (!cached.isNullOrEmpty()) {
                    Result.success(cached)
                } else {
                    Result.success(getDefaultFallbackTracks())
                }
            }
        }

    private fun mapToTrack(song: DailyRecommendSongItemResponse): Track {
        val qualityTags = mutableListOf<AudioQuality>()
        if (song.sq != null) qualityTags.add(AudioQuality.LOSSLESS)
        if (song.hr != null) qualityTags.add(AudioQuality.HIRES)
        if (song.h != null) qualityTags.add(AudioQuality.HIGH)
        if (song.l != null || song.m != null) qualityTags.add(AudioQuality.STANDARD)

        val artists = song.artists
            .map {
                Artist(
                    id = it.id ?: 0L,
                    name = it.name?.takeIf { value -> value.isNotBlank() } ?: "未知歌手",
                    coverUrl = null
                )
            }
            .ifEmpty { listOf(Artist(id = 0L, name = "未知歌手")) }

        val album = Album(
            id = song.album?.id ?: 0L,
            title = song.album?.name?.takeIf { it.isNotBlank() } ?: "未知专辑",
            coverUrl = song.album?.picUrl
        )

        val playableByPrivilege = song.privilege?.let { privilege ->
            val blockedStatus = (privilege.st ?: 0) < 0
            val hasPlayableLevel = (privilege.pl ?: 0L) > 0L
            !blockedStatus && hasPlayableLevel
        }

        val isPlayable = when (playableByPrivilege) {
            true -> true
            false -> false
            null -> song.fee != 4
        }

        val recommendTag = when {
            !song.recommendReason.isNullOrBlank() -> song.recommendReason
            !song.reason.isNullOrBlank() -> song.reason
            song.privilege?.fee == 1 -> "VIP"
            (song.pop ?: 0) >= 80 -> "百万红心"
            (song.pop ?: 0) >= 50 -> "昨日上万播放"
            else -> "推荐"
        }

        return Track(
            id = song.id,
            title = song.name.ifBlank { "未知歌曲" },
            artists = artists,
            album = album,
            qualityTags = qualityTags,
            coverUrl = song.album?.picUrl,
            durationMs = song.dt,
            playableUrl = null,
            isPlayable = isPlayable,
            recommendReason = recommendTag
        )
    }

    private fun mapNewsongToTrack(song: NewsongItemResponse, fallbackCoverUrl: String?): Track {
        val qualityTags = mutableListOf<AudioQuality>()
        val pl = song.privilege?.pl
        if (pl != null && pl >= 320000L) {
            qualityTags.add(AudioQuality.LOSSLESS)
        }

        val artists = song.artists.map { artist ->
            Artist(
                id = artist.id,
                name = artist.name?.takeIf { it.isNotBlank() } ?: "未知歌手",
                coverUrl = null
            )
        }.ifEmpty {
            listOf(Artist(id = 0L, name = "未知歌手"))
        }

        val album = Album(
            id = song.album?.id ?: 0L,
            title = song.album?.name?.takeIf { it.isNotBlank() } ?: "未知专辑",
            coverUrl = song.album?.picUrl ?: fallbackCoverUrl
        )

        val isPlayable = song.privilege?.let { privilege ->
            val blockedStatus = (privilege.st ?: 0) < 0
            val hasPlayableLevel = (privilege.pl ?: 0L) > 0L
            !blockedStatus && hasPlayableLevel
        } ?: (song.fee != 4)

        val tag = if (song.fee == 1) "VIP" else "推荐新歌"

        return Track(
            id = song.id,
            title = song.name?.takeIf { it.isNotBlank() } ?: "未知歌曲",
            artists = artists,
            album = album,
            qualityTags = qualityTags,
            coverUrl = song.album?.picUrl ?: fallbackCoverUrl,
            durationMs = null,
            playableUrl = null,
            isPlayable = isPlayable,
            recommendReason = tag
        )
    }

    private fun getDefaultFallbackTracks(): List<Track> {
        return listOf(
            Track(
                id = 1827600686L,
                title = "你的眼神是一场盛大的废墟",
                artists = listOf(Artist(id = 1L, name = "Bo Peep")),
                album = Album(id = 1L, title = "你的眼神是一场盛大的废墟", coverUrl = "https://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"),
                recommendReason = "昨日上万播放"
            ),
            Track(
                id = 28793088L,
                title = "一拍即爱",
                artists = listOf(Artist(id = 2L, name = "Super Girls")),
                album = Album(id = 2L, title = "一拍即爱", coverUrl = "https://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"),
                recommendReason = "VIP"
            ),
            Track(
                id = 31877470L,
                title = "东京不太热",
                artists = listOf(Artist(id = 3L, name = "封茗囧菌")),
                album = Album(id = 3L, title = "东京不太热", coverUrl = "https://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"),
                recommendReason = "百万红心"
            )
        )
    }

    private fun getDefaultRadarPlaylist(): com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData {
        val defaultTracks = listOf(
            Track(
                id = 1407551413L,
                title = "表白",
                artists = listOf(Artist(id = 1L, name = "萧亚轩")),
                album = Album(id = 1L, title = "1087", coverUrl = "http://p1.music.126.net/J5Gf2W1XSuwOS0gegA9ryA==/109951168645398914.jpg"),
                coverUrl = "http://p1.music.126.net/J5Gf2W1XSuwOS0gegA9ryA==/109951168645398914.jpg",
                recommendReason = "私人雷达"
            ),
            Track(
                id = 1807799307L,
                title = "褪黑素2.0",
                artists = listOf(Artist(id = 2L, name = "法老 / 泥鳅Niko")),
                album = Album(id = 2L, title = "科幻小说", coverUrl = "http://p1.music.126.net/J5Gf2W1XSuwOS0gegA9ryA==/109951168645398914.jpg"),
                coverUrl = "http://p1.music.126.net/J5Gf2W1XSuwOS0gegA9ryA==/109951168645398914.jpg",
                recommendReason = "私人雷达"
            ),
            Track(
                id = 487530777L,
                title = "九张机",
                artists = listOf(Artist(id = 3L, name = "叶炫清")),
                album = Album(id = 3L, title = "双世宠妃 影视原声带", coverUrl = "http://p1.music.126.net/J5Gf2W1XSuwOS0gegA9ryA==/109951168645398914.jpg"),
                coverUrl = "http://p1.music.126.net/J5Gf2W1XSuwOS0gegA9ryA==/109951168645398914.jpg",
                recommendReason = "私人雷达"
            ),
            Track(
                id = 1888496450L,
                title = "普通人生",
                artists = listOf(Artist(id = 4L, name = "毛不易")),
                album = Album(id = 4L, title = "幼鸟指南", coverUrl = "http://p1.music.126.net/J5Gf2W1XSuwOS0gegA9ryA==/109951168645398914.jpg"),
                coverUrl = "http://p1.music.126.net/J5Gf2W1XSuwOS0gegA9ryA==/109951168645398914.jpg",
                recommendReason = "私人雷达"
            ),
            Track(
                id = 1436910205L,
                title = "MOM",
                artists = listOf(Artist(id = 5L, name = "蜡笔小心")),
                album = Album(id = 5L, title = "MOM", coverUrl = "http://p1.music.126.net/J5Gf2W1XSuwOS0gegA9ryA==/109951168645398914.jpg"),
                coverUrl = "http://p1.music.126.net/J5Gf2W1XSuwOS0gegA9ryA==/109951168645398914.jpg",
                recommendReason = "私人雷达"
            )
        )
        return com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData(
            id = 3136952023L,
            name = "私人雷达",
            description = "反复聆听你最爱的歌，红心越多，推荐越准～",
            coverUrl = "http://p1.music.126.net/J5Gf2W1XSuwOS0gegA9ryA==/109951168645398914.jpg",
            trackCount = defaultTracks.size,
            tracks = defaultTracks
        )
    }

    override fun getCachedFavoriteRecommendSongs(): FavoriteRecommendSectionData? {
        return homeRecommendCache.getFavoriteRecommend()
    }

    override suspend fun getFavoriteRecommendSongs(afresh: Boolean): Result<FavoriteRecommendSectionData> =
        withContext(Dispatchers.IO) {
            try {
                // 1. 调用官方首页 Block 接口 /homepage/block/page
                try {
                    val blockResponse = dailyRecommendService.getHomepageBlockPage(refresh = afresh)
                    if (blockResponse.code == 200 && blockResponse.data != null) {
                        val blocks = blockResponse.data.blocks
                        val styleBlock = blocks.firstOrNull { it.blockCode == "HOMEPAGE_BLOCK_STYLE_RCMD" }
                        if (styleBlock != null) {
                            val blockTitle = styleBlock.uiElement?.subTitle?.title
                                ?.takeIf { it.isNotBlank() }
                                ?: "根据你喜爱的歌曲推荐"

                            val tracks = mutableListOf<Track>()
                            for (creative in styleBlock.creatives) {
                                for (resource in creative.resources) {
                                    val mappedTrack = mapHomepageResourceToTrack(resource)
                                    if (mappedTrack != null) {
                                        tracks.add(mappedTrack)
                                    }
                                }
                            }

                            val distinctTracks = tracks.distinctBy { it.id }
                            if (distinctTracks.isNotEmpty()) {
                                val sectionData = FavoriteRecommendSectionData(
                                    title = blockTitle,
                                    tracks = distinctTracks
                                )
                                homeRecommendCache.saveFavoriteRecommend(sectionData)
                                return@withContext Result.success(sectionData)
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 网络不可用或接口异常时平滑向下走缓存
                }

                // 2. 本地持久化缓存
                val cached = homeRecommendCache.getFavoriteRecommend()
                if (cached != null && cached.tracks.isNotEmpty()) {
                    return@withContext Result.success(cached)
                }

                // 3. 极致兜底：静态预设风格推荐数据（保证冷启动秒显）
                Result.success(getDefaultFavoriteRecommendSection())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val cached = homeRecommendCache.getFavoriteRecommend()
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.success(getDefaultFavoriteRecommendSection())
                }
            }
        }

    override suspend fun getUserLikedMusicCover(): String? = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val userId = prefs.getLong("user_id", -1L).takeIf { it > 0 } ?: return@withContext null

            // 1. 优先从我的页面缓存中提取“我喜欢的音乐”封面
            val cachedGroup = mineCacheManager.getUserPlaylists(userId)
            val cachedCover = cachedGroup?.likedPlaylist?.coverUrl
            if (!cachedCover.isNullOrBlank()) {
                return@withContext cachedCover
            }

            // 2. 尝试调用用户歌单接口获取
            val response = mineService.getUserPlaylists(uid = userId)
            if ((response.code ?: 0) == 200) {
                val playlists = response.playlist ?: emptyList()
                val likedItem = playlists.firstOrNull { (it.specialType ?: 0) == 5 }
                    ?: playlists.firstOrNull { it.subscribed != true && it.name?.contains("我喜欢") == true }
                    ?: playlists.firstOrNull { it.subscribed != true }
                return@withContext likedItem?.coverImgUrl
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun mapHomepageResourceToTrack(resource: HomepageResourceItemResponse): Track? {
        val ext = resource.resourceExtInfo ?: return null
        val songData = ext.songData
        val songId = songData?.id ?: resource.resourceId?.toLongOrNull() ?: return null
        if (songId <= 0L) return null

        val title = resource.uiElement?.mainTitle?.title?.takeIf { it.isNotBlank() }
            ?: songData?.name?.takeIf { it.isNotBlank() }
            ?: "未知歌曲"

        val reason = resource.uiElement?.subTitle?.title?.takeIf { it.isNotBlank() }
            ?: when {
                ext.songPrivilege?.fee == 1 || songData?.fee == 1 -> "VIP"
                else -> "推荐"
            }

        val artists = (ext.artists.takeIf { it.isNotEmpty() } ?: songData?.artists ?: emptyList())
            .map {
                Artist(
                    id = it.id ?: 0L,
                    name = it.name?.takeIf { n -> n.isNotBlank() } ?: "未知歌手",
                    coverUrl = it.picUrl ?: it.img1v1Url
                )
            }.ifEmpty { listOf(Artist(id = 0L, name = "未知歌手")) }

        val coverUrl = resource.uiElement?.image?.imageUrl
            ?: songData?.album?.picUrl
            ?: songData?.album?.blurPicUrl

        val album = Album(
            id = songData?.album?.id ?: 0L,
            title = songData?.album?.name?.takeIf { it.isNotBlank() } ?: "未知专辑",
            coverUrl = coverUrl
        )

        val qualityTags = mutableListOf<AudioQuality>()
        if (songData?.hrMusic != null) qualityTags.add(AudioQuality.HIRES)
        if (songData?.sqMusic != null) qualityTags.add(AudioQuality.LOSSLESS)
        if (songData?.hMusic != null) qualityTags.add(AudioQuality.HIGH)

        val isPlayable = ext.songPrivilege?.let { priv ->
            val blockedStatus = (priv.st ?: 0) < 0
            val hasPlayableLevel = (priv.pl ?: 0L) > 0L
            !blockedStatus && hasPlayableLevel
        } ?: (songData?.fee != 4)

        return Track(
            id = songId,
            title = title,
            artists = artists,
            album = album,
            qualityTags = qualityTags,
            coverUrl = coverUrl,
            durationMs = songData?.duration ?: 0L,
            playableUrl = null,
            isPlayable = isPlayable,
            recommendReason = reason
        )
    }

    private fun getDefaultFavoriteRecommendSection(): FavoriteRecommendSectionData {
        val defaultTracks = listOf(
            Track(
                id = 2650821035L,
                title = "如果",
                artists = listOf(Artist(id = 1L, name = "菠萝赛东 / Bo Peep")),
                album = Album(id = 1L, title = "如果", coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"),
                coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg",
                recommendReason = "❤️ 悲伤联盟",
                qualityTags = listOf(AudioQuality.LOSSLESS)
            ),
            Track(
                id = 1827600686L,
                title = "都市传说",
                artists = listOf(Artist(id = 2L, name = "Bo Peep")),
                album = Album(id = 2L, title = "都市传说", coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"),
                coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg",
                recommendReason = "超76%人播放",
                qualityTags = listOf(AudioQuality.LOSSLESS)
            ),
            Track(
                id = 2101344445L,
                title = "梦影",
                artists = listOf(Artist(id = 3L, name = "黄霄雲")),
                album = Album(id = 3L, title = "梦影", coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"),
                coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg",
                recommendReason = "Hi-Res",
                qualityTags = listOf(AudioQuality.HIRES, AudioQuality.LOSSLESS)
            ),
            Track(
                id = 21274655L,
                title = "Lonely",
                artists = listOf(Artist(id = 40437L, name = "Nana")),
                album = Album(id = 1965897L, title = "Nana", coverUrl = "http://p4.music.126.net/2I1AKsgJksthorB_ndCNdA==/778454232476457.jpg"),
                coverUrl = "http://p4.music.126.net/2I1AKsgJksthorB_ndCNdA==/778454232476457.jpg",
                recommendReason = "超44%人收藏",
                qualityTags = listOf(AudioQuality.LOSSLESS)
            ),
            Track(
                id = 441120471L,
                title = "That's What I Like",
                artists = listOf(Artist(id = 17191L, name = "Bruno Mars")),
                album = Album(id = 35005118L, title = "24K Magic", coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"),
                coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg",
                recommendReason = "超71%人播放",
                qualityTags = listOf(AudioQuality.LOSSLESS)
            ),
            Track(
                id = 26092806L,
                title = "Take Me Hand",
                artists = listOf(Artist(id = 811051L, name = "DAISHI DANCE / Cecile Corbel")),
                album = Album(id = 2390886L, title = "Take Me Hand", coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"),
                coverUrl = "http://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg",
                recommendReason = "前奏跪 开口脆",
                qualityTags = listOf(AudioQuality.LOSSLESS)
            )
        )
        return FavoriteRecommendSectionData(
            title = "根据你喜爱的歌曲推荐",
            tracks = defaultTracks
        )
    }

    private fun hasLoginCookie(): Boolean {
        val cookie = context.getSharedPreferences(COOKIE_PREF_NAME, Context.MODE_PRIVATE)
            .getString(COOKIE_KEY, null)
        if (!cookie.isNullOrBlank()) return true
        return !context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .getString("cookie", null).isNullOrBlank()
    }

    companion object {
        private const val COOKIE_PREF_NAME = "auth_cookies"
        private const val COOKIE_KEY = "cookie_string"
    }
}
