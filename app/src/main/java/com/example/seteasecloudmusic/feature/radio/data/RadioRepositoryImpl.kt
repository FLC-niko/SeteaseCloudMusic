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

@Singleton
class RadioRepositoryImpl @Inject constructor(
    private val radioService: RadioService,
    private val dailyRecommendService: DailyRecommendService,
    @param:ApplicationContext private val context: Context
) : RadioRepository {

    private fun hasLoginCookie(): Boolean {
        val authCookies = context.getSharedPreferences("auth_cookies", Context.MODE_PRIVATE)
            .getString("cookie_string", null)
        if (!authCookies.isNullOrBlank() && (authCookies.contains("MUSIC_U") || authCookies.contains("__csrf"))) {
            return true
        }
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val cookie = prefs.getString("cookie", null)
        val musicU = prefs.getString("music_u", null)
        return (!cookie.isNullOrBlank() && cookie.contains("MUSIC_U")) || !musicU.isNullOrBlank()
    }

    // ==========================================
    // 1. 排行榜 (严格按 热歌榜、飙升榜、新歌榜 顺序，全真实歌曲补齐)
    // ==========================================
    override suspend fun getToplists(): Result<List<RadioToplistItem>> = withContext(Dispatchers.IO) {
        try {
            val response = radioService.getToplistDetail()
            if (response.code == 200 && response.list.isNotEmpty()) {
                val allList = response.list

                // 严格确定前三位榜单：1. 热歌榜 2. 飙升榜 3. 新歌榜
                val rege = allList.firstOrNull { it.id == 3778678L }
                    ?: allList.firstOrNull { (it.name ?: "").contains("热歌") && !(it.name ?: "").contains("网络") && !(it.name ?: "").contains("欧美") && !(it.name ?: "").contains("黑胶") }
                val biaosheng = allList.firstOrNull { it.id == 19723756L }
                    ?: allList.firstOrNull { (it.name ?: "").contains("飙升") }
                val xinge = allList.firstOrNull { it.id == 3779629L }
                    ?: allList.firstOrNull { (it.name ?: "").contains("新歌") && !(it.name ?: "").contains("高分") && !(it.name ?: "").contains("黑胶") && !(it.name ?: "").contains("欧美") }

                val topThree = listOfNotNull(rege, biaosheng, xinge)
                val topThreeIds = topThree.map { it.id }.toSet()

                // 其他精选热门榜单（如原创榜、民谣榜、国风榜、摇滚榜、古典榜等）
                val remaining = allList.filter { it.id !in topThreeIds }

                // 组合榜单列表：前三名严格保证为 [热歌榜, 飙升榜, 新歌榜]
                val orderedList = (topThree + remaining).take(12)

                // 并行补齐歌曲名：解决部分分类榜单 /toplist/detail 中 tracks 字段为空的问题
                val toplistItems = coroutineScope {
                    orderedList.map { item ->
                        async {
                            val name = item.name ?: "排行榜"
                            val colors = pickToplistGradient(name)
                            var songs = item.tracks.mapNotNull { it.first?.takeIf { s -> s.isNotBlank() } }
                            if (songs.isEmpty()) {
                                try {
                                    val detail = radioService.getPlaylistDetail(item.id)
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

    // ==========================================
    // 2. 雷达歌单 (从 homepage/block/page 的 HOMEPAGE_BLOCK_MGC_PLAYLIST 获取 100% 真实雷达歌单及真实封面)
    // ==========================================
    override suspend fun getRadarPlaylists(): Result<List<RadioRadarItem>> = withContext(Dispatchers.IO) {
        try {
            val radarList = mutableListOf<RadioRadarItem>()

            // 1. 优先从 homepage/block/page 获取真实的雷达歌单块
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

            // 2. 若雷达块未返回，从 recommend/resource 或 playlist/detail 获取官方真实雷达信息
            if (radarList.isEmpty()) {
                val officialRadarIds = listOf(
                    3136952023L, // 私人雷达
                    5320167908L, // 时光雷达
                    5362359247L, // 宝藏雷达
                    5327906368L, // 乐迷雷达
                    5300458264L  // 新歌雷达
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
                                    subtitle = pl.description?.take(30) ?: "",
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

    // ==========================================
    // 3. 从你喜欢的歌曲开始漫游 (真实歌曲，真实封面取色，上下文字均为真实歌名)
    // ==========================================
    override suspend fun getRoamItems(): Result<List<RadioRoamItem>> = withContext(Dispatchers.IO) {
        try {
            val roamList = mutableListOf<RadioRoamItem>()

            // 获取起点歌曲列表：登录用户取每日推荐，未登录取新歌
            val baseSongs = mutableListOf<Pair<Long, String>>() // id to name
            val baseCovers = mutableMapOf<Long, String?>()

            if (hasLoginCookie()) {
                try {
                    val recSongs = dailyRecommendService.getDailyRecommendSongs(false)
                    val songs = recSongs.data?.dailySongs?.takeIf { it.isNotEmpty() } ?: recSongs.dailySongs
                    for (s in songs.take(6)) {
                        baseSongs.add(s.id to s.name)
                        baseCovers[s.id] = s.album?.picUrl
                    }
                } catch (_: Exception) {}
            }

            if (baseSongs.isEmpty()) {
                try {
                    val newsongResp = radioService.getPersonalizedNewsong(limit = 6)
                    for (item in newsongResp.result) {
                        baseSongs.add(item.id to (item.name ?: "未知歌曲"))
                        baseCovers[item.id] = item.picUrl
                    }
                } catch (_: Exception) {}
            }

            // 对每首起点歌曲，调用 /simi/song 获取漫游曲目列表，构造全真实歌名
            for ((songId, songName) in baseSongs) {
                var simiSongNames = emptyList<String>()
                val trackList = mutableListOf<Track>()

                try {
                    val simiResp = radioService.getSimiSongs(songId)
                    if (simiResp.code == 200 && simiResp.songs.isNotEmpty()) {
                        simiSongNames = simiResp.songs.map { it.name }.filter { it.isNotBlank() }
                        trackList.addAll(simiResp.songs.map { mapSongItemToTrack(it) })
                    }
                } catch (_: Exception) {}

                // 上下文字全为歌名：顶部为起点歌名，下方为漫游联播的各首歌名连接（如 "歌名1、歌名2、歌名3..."）
                val subtitleText = if (simiSongNames.isNotEmpty()) {
                    "$songName、${simiSongNames.take(4).joinToString("、")}"
                } else {
                    songName
                }

                val cover = baseCovers[songId]
                roamList.add(
                    RadioRoamItem(
                        id = songId,
                        songTitle = songName,
                        artistName = "",
                        coverUrl = cover,
                        subtitle = subtitleText,
                        tracks = trackList.ifEmpty {
                            listOf(
                                Track(
                                    id = songId,
                                    title = songName,
                                    artists = emptyList(),
                                    album = Album(id = 0L, title = songName, coverUrl = cover),
                                    coverUrl = cover,
                                    recommendReason = "歌曲漫游"
                                )
                            )
                        }
                    )
                )
            }

            Result.success(roamList)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 4. 歌手推荐（排在推荐歌单上面一行，真实 /top/artists）
    // ==========================================
    override suspend fun getRecommendedSingers(): Result<List<RadioArtistItem>> = withContext(Dispatchers.IO) {
        try {
            val response = radioService.getTopArtists(limit = 30)
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

    // ==========================================
    // 5. 推荐歌单 (真实 /personalized 推荐歌单)
    // ==========================================
    override suspend fun getRecommendedPlaylists(): Result<List<RadioPlaylistItem>> = withContext(Dispatchers.IO) {
        try {
            val response = radioService.getPersonalized(limit = 10)
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

    // ==========================================
    // 6. 艺人热门金曲 (真实热门艺人写真与单曲)
    // ==========================================
    override suspend fun getArtistHotSection(): Result<ArtistHotSectionData> = withContext(Dispatchers.IO) {
        try {
            val artistsResp = radioService.getTopArtists(limit = 8)
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

    // ==========================================
    // 7. 每周新热趋势 (真实 /top/playlist?cat=流行 趋势歌单与封面)
    // ==========================================
    override suspend fun getTrends(): Result<List<RadioTrendItem>> = withContext(Dispatchers.IO) {
        try {
            val response = radioService.getTopPlaylist(cat = "流行", limit = 10)
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

    // ==========================================
    // 8. 心情氛围 (真实场景/氛围歌单与封面)
    // ==========================================
    override suspend fun getMoodPlaylists(): Result<List<RadioMoodItem>> = withContext(Dispatchers.IO) {
        try {
            val moodList = mutableListOf<RadioMoodItem>()

            // 优先检查 homepage/block/page 中的专属场景歌单 (HOMEPAGE_BLOCK_OFFICIAL_PLAYLIST)
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

            // 若场景歌单块未返回，调用 /top/playlist?cat=治愈 作为真实歌单数据源
            if (moodList.isEmpty()) {
                val response = radioService.getTopPlaylist(cat = "治愈", limit = 8)
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

    // ==========================================
    // 9. 你关注的艺人新动向 (严格仅展示用户真正关注的歌手及其新动向)
    // ==========================================
    override suspend fun getFollowedArtistSection(): Result<FollowedArtistSectionData> = withContext(Dispatchers.IO) {
        try {
            if (!hasLoginCookie()) {
                // 用户未登录，不展示「你关注的艺人新动向」
                return@withContext Result.success(FollowedArtistSectionData())
            }

            // 1. 获取用户真正关注的歌手列表 (/artist/sublist)
            val sublistResp = try {
                radioService.getArtistSublist(limit = 20)
            } catch (_: Exception) {
                null
            }

            val followedArtists = sublistResp?.data ?: emptyList()
            if (followedArtists.isEmpty()) {
                // 用户未关注任何歌手，绝不伪造或推荐未关注歌手
                return@withContext Result.success(FollowedArtistSectionData())
            }

            // 提取关注歌手头像（取前3位已关注艺人头像）
            val avatars = followedArtists.take(3).mapNotNull { it.picUrl ?: it.img1v1Url }
            val followedArtistIds = followedArtists.map { it.id }.toSet()

            val items = mutableListOf<RadioFollowedArtistNewSongItem>()

            // 2. 尝试从关注歌手新歌接口获取 (/artist/new/song)
            try {
                val newSongResp = radioService.getArtistNewSong(limit = 10)
                if (newSongResp.code == 200 && newSongResp.data.isNotEmpty()) {
                    for (item in newSongResp.data) {
                        val song = item.song ?: continue
                        // 严格校验：必须是用户实际关注的歌手！
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

            // 3. 若关注歌手近期没有新单曲，直接获取关注歌手本人的最新精选单曲，绝不向用户展示未关注的歌手！
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

    // ==========================================
    // 获取歌单歌曲列表
    // ==========================================
    override suspend fun getPlaylistTracks(playlistId: Long): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val response = radioService.getPlaylistDetail(playlistId)
            if (response.code == 200 && response.playlist != null) {
                val pl = response.playlist
                val tracks = (pl.tracks ?: emptyList()).map { item ->
                    mapPlaylistTrackToTrack(item, pl.coverImgUrl)
                }.distinctBy { it.id }
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

    // ==========================================
    // 映射辅助
    // ==========================================
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
