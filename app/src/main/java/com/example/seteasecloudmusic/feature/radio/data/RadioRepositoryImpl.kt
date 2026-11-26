package com.example.seteasecloudmusic.feature.radio.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.discover.data.PersonalizedNewsongItemResponse
import com.example.seteasecloudmusic.feature.home.data.DailyRecommendService
import com.example.seteasecloudmusic.feature.home.data.DailyRecommendSongItemResponse
import com.example.seteasecloudmusic.feature.mine.data.PlaylistTrackItemResponse
import com.example.seteasecloudmusic.feature.radio.domain.model.ArtistHotSectionData
import com.example.seteasecloudmusic.feature.radio.domain.model.FollowedArtistSectionData
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioArtistHotItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioArtistItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioFollowedArtistNewSongItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioMoodItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioPlaylistItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioRadarItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioRoamItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioToplistItem
import com.example.seteasecloudmusic.feature.radio.domain.model.RadioTrendItem
import com.example.seteasecloudmusic.feature.radio.domain.repository.RadioRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [RadioRepository] 的网络实现，聚合电台页 9 个分区数据，并处理未登录降级、榜单补全与 DTO 映射。
 */
@Singleton
class RadioRepositoryImpl @Inject constructor(
    private val radioService: RadioService,
    private val dailyRecommendService: DailyRecommendService,
    private val mineRepository: com.example.seteasecloudmusic.feature.mine.domain.repository.MineRepository,
    @param:ApplicationContext private val context: Context
) : RadioRepository {

    private fun getLoggedInUserId(): Long {
        return com.example.seteasecloudmusic.core.auth.AuthCookieStore
            .getLoggedInUserId(context) ?: -1L
    }

    private fun hasLoginCookie(): Boolean {
        return com.example.seteasecloudmusic.core.auth.AuthCookieStore
            .hasValidLoginCookie(context)
    }

    // 1. 排行榜（固定前三位为热歌榜、飙升榜、新歌榜，并并发补齐缺失曲目名）
    override suspend fun getToplists(): Result<List<RadioToplistItem>> = withContext(Dispatchers.IO) {
        try {
            val response = radioService.getToplistDetail()
            if (response.code == 200 && response.list.isNotEmpty()) {
                val allList = response.list

                // 固定前三位榜单，ID 未命中时按名称关键字兜底
                val rege = allList.firstOrNull { it.id == 3778678L } // 热歌榜官方 ID
                    ?: allList.firstOrNull { (it.name ?: "").contains("热歌") && !(it.name ?: "").contains("网络") && !(it.name ?: "").contains("欧美") && !(it.name ?: "").contains("黑胶") }
                val biaosheng = allList.firstOrNull { it.id == 19723756L } // 飙升榜官方 ID
                    ?: allList.firstOrNull { (it.name ?: "").contains("飙升") }
                val xinge = allList.firstOrNull { it.id == 3779629L } // 新歌榜官方 ID
                    ?: allList.firstOrNull { (it.name ?: "").contains("新歌") && !(it.name ?: "").contains("高分") && !(it.name ?: "").contains("黑胶") && !(it.name ?: "").contains("欧美") }

                val topThree = listOfNotNull(rege, biaosheng, xinge)
                val topThreeIds = topThree.map { it.id }.toSet()

                // 其余精选热门榜单（如原创榜、民谣榜、国风榜、摇滚榜、古典榜等）
                val remaining = allList.filter { it.id !in topThreeIds }

                // 榜单列表最多截取 12 个卡片，前三位依次为 [热歌榜, 飙升榜, 新歌榜]
                val orderedList = (topThree + remaining).take(12)

                // 并发补齐歌曲名（批大小即 orderedList 上限 12）：部分分类榜 /toplist/detail 返回的 tracks 为空
                val toplistItems = coroutineScope {
                    orderedList.map { item ->
                        async {
                            val name = item.name ?: "排行榜"
                            val colors = pickToplistGradient(name)
                            var songs = item.tracks.mapNotNull { it.first?.takeIf { s -> s.isNotBlank() } }
                            if (songs.isEmpty()) {
                                try {
                                    val detail = radioService.getPlaylistDetail(item.id)
                                    // 每个榜单卡片最多提取前 3 首曲目名用于下方预览
                                    val fetchedSongs = detail.playlist?.tracks?.take(3)?.mapNotNull {
                                        it.name?.takeIf { n -> n.isNotBlank() }
                                    } ?: emptyList()
                                    if (fetchedSongs.isNotEmpty()) {
                                        songs = fetchedSongs
                                    }
                                } catch (_: Exception) {}
                            }
                            RadioToplistItem(
                                id = item.id,
                                name = name,
                                playCount = item.playCount,
                                coverUrl = item.coverImgUrl,
                                topSongs = songs,
                                gradientColors = colors
                            )
                        }
                    }.awaitAll()
                }

                return@withContext Result.success(toplistItems)
            }
            Result.success(emptyList())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 根据榜单名称关键字分配封面渐变双色
    private fun pickToplistGradient(name: String): List<Color> {
        return when {
            name.contains("热歌") -> listOf(Color(0xFFFF5E62), Color(0xFFFF9966))
            name.contains("飙升") -> listOf(Color(0xFF4CA1AF), Color(0xFF2C3E50))
            name.contains("新歌") -> listOf(Color(0xFF11998E), Color(0xFF38EF7D))
            name.contains("原创") -> listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
            name.contains("民谣") -> listOf(Color(0xFFFFB347), Color(0xFFFFCC33))
            name.contains("国风") || name.contains("古风") -> listOf(Color(0xFFF2994A), Color(0xFFF2C94C))
            name.contains("古典") -> listOf(Color(0xFFFF758C), Color(0xFFFF7EB3))
            name.contains("电音") -> listOf(Color(0xFF7F00FF), Color(0xFFE100FF))
            name.contains("说唱") -> listOf(Color(0xFFE52D27), Color(0xFFB31217))
            name.contains("欧美") -> listOf(Color(0xFFB06AB3), Color(0xFF4568DC))
            name.contains("摇滚") -> listOf(Color(0xFF232526), Color(0xFF414345))
            else -> listOf(Color(0xFFFF758C), Color(0xFFFF7EB3))
        }
    }

    // 2. 雷达歌单（优先从 homepage/block/page 的 HOMEPAGE_BLOCK_MGC_PLAYLIST 块提取，空时回退官方雷达歌单 ID）
    override suspend fun getRadarPlaylists(): Result<List<RadioRadarItem>> = withContext(Dispatchers.IO) {
        try {
            val radarList = mutableListOf<RadioRadarItem>()

            // 1. 优先从 homepage/block/page 提取雷达歌单块
            try {
                val blockResp = radioService.getHomepageBlockPage(refresh = false)
                if (blockResp.code == 200 && blockResp.data != null) {
                    val mgcBlock = blockResp.data.blocks.firstOrNull { it.blockCode == "HOMEPAGE_BLOCK_MGC_PLAYLIST" }
                    if (mgcBlock != null) {
                        for (creative in mgcBlock.creatives) {
                            for (resource in creative.resources) {
                                val plId = resource.resourceId?.toLongOrNull() ?: continue
                                val mainTitle = resource.uiElement?.mainTitle?.title
                                    ?: creative.uiElement?.mainTitle?.title
                                    ?: "雷达歌单"
                                val cover = resource.uiElement?.image?.imageUrl

                                val (shortTitle, subtitle) = if (mainTitle.contains("|")) {
                                    val parts = mainTitle.split("|", limit = 2)
                                    parts[1].trim() to parts[0].trim()
                                } else {
                                    mainTitle to (resource.uiElement?.subTitle?.title ?: "")
                                }

                                radarList.add(
                                    RadioRadarItem(
                                        id = plId,
                                        title = shortTitle,
                                        subtitle = subtitle,
                                        coverUrl = cover,
                                        playCount = resource.resourceExtInfo?.playCount
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            // 2. 首页块未返回时，依次请求官方雷达歌单详情作为兜底
            if (radarList.isEmpty()) {
                val officialRadarIds = listOf(
                    3136952023L, // 私人雷达 ID
                    5320167908L, // 时光雷达 ID
                    5362359247L, // 宝藏雷达 ID
                    5327906368L, // 乐迷雷达 ID
                    5300458264L  // 新歌雷达 ID
                )
                for (id in officialRadarIds) {
                    try {
                        val detail = radioService.getPlaylistDetail(id)
                        val pl = detail.playlist
                        if (pl != null) {
                            radarList.add(
                                RadioRadarItem(
                                    id = pl.id ?: id,
                                    title = pl.name ?: "雷达歌单",
                                    subtitle = pl.description?.take(30) ?: "", // 副标题截取歌单简介前 30 字
                                    coverUrl = pl.coverImgUrl,
                                    playCount = pl.playCount
                                )
                            )
                        }
                    } catch (_: Exception) {}
                }
            }

            Result.success(radarList)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. 从你喜欢的歌曲开始漫游（按登录态选取种子曲目并并发生成关联漫游列表）
    override suspend fun getRoamItems(): Result<List<RadioRoamItem>> = withContext(Dispatchers.IO) {
        try {
            val isLoggedIn = hasLoginCookie()
            val userId = getLoggedInUserId()
            var likedPlaylistId = -1L
            val seedPool = mutableListOf<Track>()
            var fromLikedPlaylist = false

            // 1. 优先读取用户“我喜欢的音乐”歌单曲目作为漫游起点
            if (isLoggedIn && userId > 0L) {
                try {
                    val likedPlaylist = mineRepository.getUserPlaylists(userId).getOrNull()?.likedPlaylist
                    if (likedPlaylist != null && likedPlaylist.id > 0L) {
                        likedPlaylistId = likedPlaylist.id
                        val detail = radioService.getPlaylistDetail(likedPlaylistId)
                        val fallbackCover = detail.playlist?.coverImgUrl ?: likedPlaylist.coverUrl
                        val likedTracks = (detail.playlist?.tracks ?: emptyList())
                            .map { mapPlaylistTrackToTrack(it, fallbackCover) }
                            .filter { it.id > 0L && it.title.isNotBlank() }
                        if (likedTracks.isNotEmpty()) {
                            seedPool.addAll(likedTracks)
                            fromLikedPlaylist = true
                        } else {
                            val fullDetail = mineRepository.getPlaylistDetail(likedPlaylistId).getOrNull()
                            val fallbackTracks = fullDetail?.tracks
                                ?.filter { it.id > 0L && it.title.isNotBlank() }
                                ?: emptyList()
                            if (fallbackTracks.isNotEmpty()) {
                                seedPool.addAll(fallbackTracks)
                                fromLikedPlaylist = true
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {}
            }

            // 2. 已登录但喜欢歌单为空时，回退到每日推荐歌曲（afresh=true 强制刷新当日列表）
            if (isLoggedIn && seedPool.isEmpty()) {
                try {
                    val recSongs = dailyRecommendService.getDailyRecommendSongs(true)
                    val songs = recSongs.data?.dailySongs?.takeIf { it.isNotEmpty() } ?: recSongs.dailySongs
                    for (s in songs) {
                        if (s.id <= 0L || s.name.isBlank()) continue
                        val artists = s.artists.map { ar ->
                            Artist(id = ar.id ?: 0L, name = ar.name?.takeIf { n -> n.isNotBlank() } ?: "未知歌手")
                        }.ifEmpty { listOf(Artist(0L, "未知歌手")) }
                        val cover = s.album?.picUrl
                        seedPool.add(
                            Track(
                                id = s.id,
                                title = s.name,
                                artists = artists,
                                album = Album(id = s.album?.id ?: 0L, title = s.album?.name ?: "未知专辑", coverUrl = cover),
                                coverUrl = cover,
                                durationMs = s.dt ?: 0L,
                                isPlayable = (s.fee ?: 0) != 4,
                                recommendReason = s.recommendReason ?: s.reason
                            )
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {}
            }

            // 3. 未登录或前两步皆空时，使用推荐新歌兜底
            if (seedPool.isEmpty()) {
                try {
                    val newsongResp = radioService.getPersonalizedNewsong(limit = 12) // 兜底推荐新歌单次拉取上限
                    seedPool.addAll(
                        newsongResp.result
                            .map { mapNewsongItemToTrack(it) }
                            .filter { it.id > 0L && it.title.isNotBlank() }
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {}
            }

            // 4. 抽取最多 6 首种子歌曲：来自喜欢歌单时从最近 5 首抽 2 首 + 其余随机抽 4 首，保证近期偏好与随机轮换
            val distinctPool = seedPool.distinctBy { it.id }
            val seedTracks = if (fromLikedPlaylist && distinctPool.size > 6) {
                val recentPicks = distinctPool.take(5).shuffled().take(2)
                val recentIds = recentPicks.map { it.id }.toHashSet()
                val restPicks = distinctPool.filterNot { it.id in recentIds }.shuffled().take(4)
                recentPicks + restPicks
            } else {
                distinctPool.shuffled().take(6)
            }

            // 5. 并发为每首种子歌曲拉取心动智能漫游 (/playmode/intelligence/list，批大小上限 6)，失败或未登录时降级 /simi/song
            val roamList = coroutineScope {
                seedTracks.map { seed ->
                    async {
                        val fetchedTracks = mutableListOf<Track>()

                        if (isLoggedIn && likedPlaylistId > 0L) {
                            try {
                                val intelResp = radioService.getIntelligenceList(
                                    id = seed.id,
                                    pid = likedPlaylistId,
                                    sid = seed.id
                                )
                                if (intelResp.code == 200 && intelResp.data.isNotEmpty()) {
                                    fetchedTracks.addAll(
                                        intelResp.data
                                            .mapNotNull { it.songInfo }
                                            .filter { it.id > 0L && it.name.isNotBlank() }
                                            .map { mapSongItemToTrack(it) }
                                    )
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {}
                        }

                        if (fetchedTracks.isEmpty()) {
                            try {
                                val simiResp = radioService.getSimiSongs(seed.id)
                                if (simiResp.code == 200 && simiResp.songs.isNotEmpty()) {
                                    fetchedTracks.addAll(
                                        simiResp.songs
                                            .filter { it.id > 0L && it.name.isNotBlank() }
                                            .map { mapSongItemToTrack(it) }
                                    )
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {}
                        }

                        val roamTracks = (listOf(seed) + fetchedTracks).distinctBy { it.id }
                        val companionNames = roamTracks
                            .drop(1)
                            .map { it.title }
                            .filter { it.isNotBlank() }
                            .take(4) // 卡片底部副标题最多拼接前 4 首伴随曲目名
                        val subtitleText = if (companionNames.isNotEmpty()) {
                            "${seed.title}、${companionNames.joinToString("、")}"
                        } else {
                            seed.title
                        }

                        RadioRoamItem(
                            id = seed.id,
                            songTitle = seed.title,
                            artistName = seed.artists.firstOrNull()?.name ?: "",
                            coverUrl = seed.coverUrl,
                            subtitle = subtitleText,
                            tracks = roamTracks
                        )
                    }
                }.awaitAll()
            }

            Result.success(roamList)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. 歌手推荐（调用 /top/artists）
    override suspend fun getRecommendedSingers(): Result<List<RadioArtistItem>> = withContext(Dispatchers.IO) {
        try {
            val response = radioService.getTopArtists(limit = 30) // 推荐歌手单次拉取条数上限
            if (response.code == 200 && response.artists.isNotEmpty()) {
                val list = response.artists.map { item ->
                    RadioArtistItem(
                        id = item.id,
                        name = item.name,
                        avatarUrl = item.picUrl ?: item.img1v1Url
                    )
                }
                return@withContext Result.success(list)
            }
            Result.success(emptyList())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 5. 推荐歌单（调用 /personalized）
    override suspend fun getRecommendedPlaylists(): Result<List<RadioPlaylistItem>> = withContext(Dispatchers.IO) {
        try {
            val response = radioService.getPersonalized(limit = 10) // 推荐歌单单次拉取条数上限
            if (response.code == 200 && response.result.isNotEmpty()) {
                val list = response.result.map { item ->
                    RadioPlaylistItem(
                        id = item.id,
                        title = item.name,
                        coverUrl = item.picUrl,
                        playCount = item.playCount,
                        tag = item.copywriter
                    )
                }
                return@withContext Result.success(list)
            }
            Result.success(emptyList())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 6. 艺人热门金曲（拉取热门艺人列表构建卡片，点击时再按艺人 ID 加载热门曲目）
    override suspend fun getArtistHotSection(): Result<ArtistHotSectionData> = withContext(Dispatchers.IO) {
        try {
            val artistsResp = radioService.getTopArtists(limit = 8) // 艺人热门金曲展示歌手数上限
            if (artistsResp.code == 200 && artistsResp.artists.isNotEmpty()) {
                val leadArtist = artistsResp.artists.first()
                val items = artistsResp.artists.map { artist ->
                    RadioArtistHotItem(
                        artistId = artist.id,
                        artistName = artist.name,
                        title = "听·${artist.name} 热门精选",
                        coverUrl = artist.picUrl ?: artist.img1v1Url,
                        playCount = null
                    )
                }
                return@withContext Result.success(
                    ArtistHotSectionData(
                        leadArtistName = leadArtist.name,
                        leadArtistAvatarUrl = leadArtist.picUrl ?: leadArtist.img1v1Url,
                        items = items
                    )
                )
            }
            Result.success(ArtistHotSectionData())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 7. 每周新热趋势（调用 /top/playlist?cat=流行）
    override suspend fun getTrends(): Result<List<RadioTrendItem>> = withContext(Dispatchers.IO) {
        try {
            val response = radioService.getTopPlaylist(cat = "流行", limit = 10) // 趋势歌单单次拉取条数上限
            if (response.code == 200 && response.playlists.isNotEmpty()) {
                val list = response.playlists.map { pl ->
                    val badge = when {
                        pl.name.contains("新歌") || pl.name.contains("雷达") -> "新歌雷达"
                        pl.name.contains("华语") -> "华语 TREND"
                        pl.name.contains("影视") || pl.name.contains("OST") -> "影视 TREND"
                        pl.name.contains("欧美") -> "欧美 TREND"
                        pl.name.contains("KPop") || pl.name.contains("韩") -> "K-POP TREND"
                        else -> "流行 TREND"
                    }
                    RadioTrendItem(
                        id = pl.id,
                        title = pl.name,
                        badge = badge,
                        coverUrl = pl.coverImgUrl,
                        playCount = pl.playCount
                    )
                }
                return@withContext Result.success(list)
            }
            Result.success(emptyList())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 8. 心情氛围（优先读取首页场景歌单块 HOMEPAGE_BLOCK_OFFICIAL_PLAYLIST，缺失时回退治愈分类歌单）
    override suspend fun getMoodPlaylists(): Result<List<RadioMoodItem>> = withContext(Dispatchers.IO) {
        try {
            val moodList = mutableListOf<RadioMoodItem>()

            // 优先检查 homepage/block/page 第二页中的场景歌单块 (HOMEPAGE_BLOCK_OFFICIAL_PLAYLIST)
            try {
                val cursorResp = radioService.getHomepageBlockPage(refresh = false)
                val cursor = cursorResp.data?.cursor
                if (!cursor.isNullOrBlank()) {
                    val page2 = radioService.getHomepageBlockPage(refresh = false, cursor = cursor)
                    val officialBlock = page2.data?.blocks?.firstOrNull { it.blockCode == "HOMEPAGE_BLOCK_OFFICIAL_PLAYLIST" }
                    if (officialBlock != null) {
                        for (creative in officialBlock.creatives) {
                            for (res in creative.resources) {
                                val plId = res.resourceId?.toLongOrNull() ?: continue
                                val mainTitle = res.uiElement?.mainTitle?.title ?: ""
                                val cover = res.uiElement?.image?.imageUrl
                                val tag = if (mainTitle.contains("|")) mainTitle.substringBefore("|").trim() else "心情氛围"
                                moodList.add(
                                    RadioMoodItem(
                                        id = plId,
                                        title = mainTitle,
                                        moodTag = tag,
                                        coverUrl = cover,
                                        playCount = res.resourceExtInfo?.playCount
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            // 首页场景歌单块未返回时，回退至治愈分类歌单
            if (moodList.isEmpty()) {
                val response = radioService.getTopPlaylist(cat = "治愈", limit = 8) // 心情氛围兜底歌单拉取条数上限
                if (response.code == 200 && response.playlists.isNotEmpty()) {
                    val list = response.playlists.map { pl ->
                        val tag = if (pl.name.contains("|")) pl.name.substringBefore("|").trim() else "治愈氛围"
                        RadioMoodItem(
                            id = pl.id,
                            title = pl.name,
                            moodTag = tag,
                            coverUrl = pl.coverImgUrl,
                            playCount = pl.playCount
                        )
                    }
                    moodList.addAll(list)
                }
            }

            Result.success(moodList)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 9. 你关注的艺人新动向（仅在已登录且存在关注歌手时展示）
    override suspend fun getFollowedArtistSection(): Result<FollowedArtistSectionData> = withContext(Dispatchers.IO) {
        try {
            if (!hasLoginCookie()) {
                // 未登录时不展示关注艺人区块
                return@withContext Result.success(FollowedArtistSectionData())
            }

            // 1. 拉取用户关注歌手列表 (/artist/sublist)
            val sublistResp = try {
                radioService.getArtistSublist(limit = 20) // 单次拉取关注歌手数量上限
            } catch (_: Exception) {
                null
            }

            val followedArtists = sublistResp?.data ?: emptyList()
            if (followedArtists.isEmpty()) {
                // 未关注任何歌手时返回空数据，不回退到未关注艺人
                return@withContext Result.success(FollowedArtistSectionData())
            }

            // 提取前 3 位关注歌手头像用于区块标题左侧叠放展示
            val avatars = followedArtists.take(3).mapNotNull { it.picUrl ?: it.img1v1Url }
            val followedArtistIds = followedArtists.map { it.id }.toSet()

            val items = mutableListOf<RadioFollowedArtistNewSongItem>()

            // 2. 优先从关注歌手新歌接口获取 (/artist/new/song)
            try {
                val newSongResp = radioService.getArtistNewSong(limit = 10) // 单次拉取关注歌手新歌条数上限
                if (newSongResp.code == 200 && newSongResp.data.isNotEmpty()) {
                    for (item in newSongResp.data) {
                        val song = item.song ?: continue
                        // 仅保留属于已关注歌手的新歌条目
                        val matchedArtist = song.artists.firstOrNull { it.id != null && it.id in followedArtistIds }
                        if (matchedArtist != null) {
                            val artistName = matchedArtist.name ?: (song.artists.firstOrNull()?.name ?: "艺人")
                            val albumName = song.album?.name ?: song.name
                            val track = mapSongItemToTrack(song)
                            items.add(
                                RadioFollowedArtistNewSongItem(
                                    songId = song.id,
                                    title = song.name,
                                    artistName = artistName,
                                    albumName = albumName,
                                    coverUrl = song.album?.picUrl ?: track.coverUrl,
                                    tag = "新单曲",
                                    track = track
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}

            // 3. 新单曲不足 3 条时，并发拉取前 5 位关注歌手的代表热歌补齐（批大小上限 5）
            if (items.size < 3) {
                coroutineScope {
                    val deferredSongs = followedArtists.take(5).map { artist ->
                        async {
                            try {
                                val topResp = radioService.getArtistTopSongs(artist.id)
                                if (topResp.code == 200 && topResp.songs.isNotEmpty()) {
                                    val topSong = topResp.songs.first()
                                    val track = mapSongItemToTrack(topSong)
                                    RadioFollowedArtistNewSongItem(
                                        songId = topSong.id,
                                        title = topSong.name,
                                        artistName = artist.name,
                                        albumName = topSong.album?.name ?: topSong.name,
                                        coverUrl = topSong.album?.picUrl ?: track.coverUrl,
                                        tag = "关注歌手",
                                        track = track
                                    )
                                } else null
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                    val fetchedItems = deferredSongs.awaitAll().filterNotNull()
                    for (newItem in fetchedItems) {
                        if (items.none { it.songId == newItem.songId }) {
                            items.add(newItem)
                        }
                    }
                }
            }

            Result.success(FollowedArtistSectionData(avatars, items))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 获取歌单曲目列表（tracks 未返回全量时回退 mineRepository 补全）
    override suspend fun getPlaylistTracks(playlistId: Long): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val response = radioService.getPlaylistDetail(playlistId)
            if (response.code == 200 && response.playlist != null) {
                val pl = response.playlist
                val tracks = (pl.tracks ?: emptyList()).map { item ->
                    mapPlaylistTrackToTrack(item, pl.coverImgUrl)
                }.distinctBy { it.id }
                val totalIdCount = pl.trackIds?.size ?: 0
                if (totalIdCount > tracks.size) {
                    val completeTracks = mineRepository.getPlaylistDetail(playlistId).getOrNull()?.tracks.orEmpty()
                    if (completeTracks.size > tracks.size) {
                        return@withContext Result.success(completeTracks)
                    }
                }
                if (tracks.isNotEmpty()) {
                    return@withContext Result.success(tracks)
                }
            }
            Result.success(emptyList())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getArtistTopTracks(artistId: Long): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val response = radioService.getArtistTopSongs(artistId)
            if (response.code == 200 && response.songs.isNotEmpty()) {
                val tracks = response.songs.map { mapSongItemToTrack(it) }.distinctBy { it.id }
                return@withContext Result.success(tracks)
            }
            Result.success(emptyList())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // DTO 到领域模型 Track 映射辅助
    private fun mapPlaylistTrackToTrack(item: PlaylistTrackItemResponse, fallbackCover: String?): Track {
        val artists = (item.ar ?: emptyList()).map {
            Artist(id = it.id ?: 0L, name = it.name?.takeIf { n -> n.isNotBlank() } ?: "未知歌手")
        }.ifEmpty { listOf(Artist(0L, "未知歌手")) }

        val cover = item.al?.picUrl ?: fallbackCover
        return Track(
            id = item.id ?: 0L,
            title = item.name?.takeIf { it.isNotBlank() } ?: "未知歌曲",
            artists = artists,
            album = Album(id = item.al?.id ?: 0L, title = item.al?.name ?: "未知专辑", coverUrl = cover),
            coverUrl = cover,
            durationMs = item.dt ?: 0L,
            isPlayable = (item.fee ?: 0) != 4
        )
    }

    private fun mapSongItemToTrack(item: RadioSongItemResponse): Track {
        val artists = item.artists.map {
            Artist(id = it.id ?: 0L, name = it.name?.takeIf { n -> n.isNotBlank() } ?: "未知歌手", coverUrl = it.picUrl)
        }.ifEmpty { listOf(Artist(0L, "未知歌手")) }

        return Track(
            id = item.id,
            title = item.name.ifBlank { "未知歌曲" },
            artists = artists,
            album = Album(id = item.album?.id ?: 0L, title = item.album?.name ?: "未知专辑", coverUrl = item.album?.picUrl),
            coverUrl = item.album?.picUrl,
            durationMs = item.dt ?: 0L,
            isPlayable = (item.fee ?: 0) != 4
        )
    }

    private fun mapNewsongItemToTrack(item: PersonalizedNewsongItemResponse): Track {
        val song = item.song
        val artists = (song?.artists ?: emptyList()).map {
            Artist(id = it.id, name = it.name?.takeIf { n -> n.isNotBlank() } ?: "未知歌手")
        }.ifEmpty { listOf(Artist(0L, "未知歌手")) }

        val cover = item.picUrl ?: song?.album?.picUrl
        return Track(
            id = item.id,
            title = item.name ?: (song?.name ?: "未知歌曲"),
            artists = artists,
            album = Album(id = song?.album?.id ?: 0L, title = song?.album?.name ?: "单曲", coverUrl = cover),
            coverUrl = cover,
            durationMs = null,
            isPlayable = (song?.fee ?: 0) != 4,
            recommendReason = "新单曲"
        )
    }
}
