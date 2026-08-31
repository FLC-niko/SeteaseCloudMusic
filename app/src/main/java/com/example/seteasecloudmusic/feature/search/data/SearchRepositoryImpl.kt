package com.example.seteasecloudmusic.feature.search.data

import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.AudioQuality
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.TrackPlaybackPreparer
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.feature.mine.domain.repository.LocalMusicRepository
import com.example.seteasecloudmusic.feature.search.domain.ArtistSuggestion
import com.example.seteasecloudmusic.feature.search.domain.PlaylistSuggestion
import com.example.seteasecloudmusic.feature.search.domain.SearchRepository
import com.example.seteasecloudmusic.feature.search.domain.SearchSuggestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * `data.repository` 模块说明：
 *
 * Repository 实现层负责站在 data 和 domain 中间做衔接：
 * 1. 调用具体数据源，例如 Retrofit API、本地数据库、缓存等。
 * 2. 把网络返回的数据模型转换成 domain 层真正使用的模型。
 * 3. 屏蔽数据来源细节，让上层只依赖抽象接口。
 */
class SearchRepositoryImpl @Inject constructor(
    private val musicService: NeteaseMusicService,
    private val localMusicRepository: LocalMusicRepository,
    private val playerSettingsManager: PlayerSettingsManager,
    private val ncblReporter: com.example.seteasecloudmusic.core.network.ncbl.NcblReporter
) : SearchRepository, TrackPlaybackPreparer {

    // 内存高速缓存：按 trackId + 音质等级 联合缓存已解析的歌曲播放直链 (0ms 极速切歌)
    private val trackUrlCache = ConcurrentHashMap<String, String>()

    override suspend fun searchTracks(
        query: String,
        limit: Int,
        offset: Int
    ): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val response = musicService.searchSongs(query, limit, offset)
            if (response.code == 200) {
                val tracks = response.result?.songs?.map { song ->
                    mapToDomainTrack(song)
                } ?: emptyList()
                Result.success(tracks)
            } else {
                Result.failure(Exception("API Error with code: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrackUrl(
        trackId: Long,
        level: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cacheKey = "$trackId-$level"
        trackUrlCache[cacheKey]?.let { cachedUrl ->
            return@withContext Result.success(cachedUrl)
        }

        try {
            val response = musicService.getSongUrl(id = trackId, level = level)
            val songData = response.data.firstOrNull()
            val url = songData?.url

            if (response.code == 200 && !url.isNullOrBlank()) {
                val secureUrl = if (url.startsWith("http://")) {
                    url.replaceFirst("http://", "https://")
                } else {
                    url
                }
                trackUrlCache[cacheKey] = secureUrl
                Result.success(secureUrl)
            } else {
                Result.failure(Exception("No playable URL available for track $trackId at level $level"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun invoke(track: Track): Result<Track> {
        // 1. 若当前曲目已带有可用播放直链（本地扫描或预先准备），0ms 立即起播
        if (!track.playableUrl.isNullOrBlank() && track.isPlayable) {
            return Result.success(track)
        }

        // 2. 核心省流量机制：在线歌单曲目优先检索本地已下载/已存在的本地音频
        val localMatch = localMusicRepository.findMatchingLocalTrack(track)
        if (localMatch != null && !localMatch.playableUrl.isNullOrBlank()) {
            android.util.Log.d("SearchRepo", "Online track '${track.title}' (ID: ${track.id}) matched LOCAL FILE: ${localMatch.playableUrl}")
            return Result.success(
                track.copy(
                    playableUrl = localMatch.playableUrl,
                    isPlayable = true
                )
            )
        }
        val currentQualityLevel = playerSettingsManager.audioQuality.value.levelKey
        android.util.Log.d("SearchRepo", "Online track '${track.title}' (ID: ${track.id}) no local match, fetching online stream for quality: $currentQualityLevel")

        // 3. 本地无匹配，根据用户设定的音质等级向云端请求直链
        return getTrackUrl(track.id, level = currentQualityLevel).map { url ->
            track.copy(playableUrl = url, isPlayable = url.isNotBlank())
        }
    }

    override suspend fun scrobbleStart(
        trackId: Long,
        sourceId: Long?,
        title: String,
        artist: String,
        totalDurationSeconds: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val sid = sourceId ?: 0L
        ncblReporter.reportPlv(
            songId = trackId,
            totalDurationSeconds = totalDurationSeconds,
            sourceId = sid
        )
    }

    override suspend fun scrobble(
        trackId: Long,
        durationSeconds: Int,
        sourceId: Long?,
        title: String,
        artist: String,
        totalDurationSeconds: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val sid = sourceId ?: 0L

        // 1. 优先使用 NCBL 桌面端官方加密日志上报 PLD（直传 clientlog3.music.163.com，100% 实时落库官方今日收听与听歌足迹）
        val ncblResult = ncblReporter.reportPld(
            songId = trackId,
            playDurationSeconds = durationSeconds,
            totalDurationSeconds = totalDurationSeconds,
            sourceId = sid,
            endReason = "playend"
        )

        // 2. 同时发起 Weblog /scrobble 兜底
        try {
            val response = musicService.scrobble(id = trackId, sourceId = sid, time = durationSeconds)
            android.util.Log.d("Scrobble", ">>> Server /scrobble response code: ${response.code}")
        } catch (e: Exception) {
            android.util.Log.w("Scrobble", ">>> Server /scrobble warning: ${e.message}")
        }

        ncblResult
    }

    private val localToOnlineIdCache = java.util.concurrent.ConcurrentHashMap<String, Long>()

    override suspend fun resolveOnlineTrackId(title: String, artist: String): Long? = withContext(Dispatchers.IO) {
        val cacheKey = "$title-$artist".lowercase().trim()
        localToOnlineIdCache[cacheKey]?.let { return@withContext it }

        try {
            val query = if (artist.isNotBlank() && !artist.contains("未知")) "$title $artist" else title
            val searchRes = searchTracks(query, limit = 5, offset = 0).getOrNull() ?: emptyList()
            val matched = searchRes.firstOrNull { t ->
                val normSearch = t.title.lowercase().replace(" ", "")
                val normLocal = title.lowercase().replace(" ", "")
                normSearch == normLocal || normSearch.contains(normLocal) || normLocal.contains(normSearch)
            } ?: searchRes.firstOrNull()

            val onlineId = matched?.id?.takeIf { it > 0L }
            if (onlineId != null) {
                localToOnlineIdCache[cacheKey] = onlineId
                android.util.Log.d("Scrobble", ">>> Resolved local track '$title - $artist' to online NetEase ID: $onlineId")
            }
            onlineId
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getSearchSuggestions(
        query: String,
        type: String?
    ): Result<SearchSuggestions> = withContext(Dispatchers.IO) {
        try {
            val response = musicService.getSearchSuggestions(query, type)
            if (response.code == 200) {
                val result = response.result
                val suggestions = SearchSuggestions(
                    songs = result?.songs?.map { mapToDomainTrackFromSuggest(it) } ?: emptyList(),
                    artists = result?.artists?.map { mapToArtistSuggestion(it) } ?: emptyList(),
                    playlists = result?.playlists?.map { mapToPlaylistSuggestion(it) }
                        ?: emptyList(),
                    allMatch = result?.allMatch?.map { it.keyword ?: "" }
                        ?.filter { it.isNotEmpty() } ?: emptyList()
                )
                Result.success(suggestions)
            } else {
                Result.failure(Exception("API Error with code: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 内部映射函数：
     * 把搜索建议中的歌曲响应转换成 domain 层的 Track。
     */
    private fun mapToDomainTrackFromSuggest(song: SearchSuggestSongResponse): Track {
        return Track(
            id = song.id,
            title = song.name ?: "",
            durationMs = song.duration,
            artists = song.artists?.map { artist ->
                Artist(
                    id = artist.id ?: 0L,
                    name = artist.name ?: "未知歌手",
                    coverUrl = null
                )
            } ?: emptyList(),
            album = Album(
                id = song.album?.id ?: 0L,
                title = song.album?.name ?: "未知专辑",
                coverUrl = song.album?.coverUrl
            ),
            coverUrl = song.album?.coverUrl,
            qualityTags = emptyList(),
            playableUrl = null,
            isPlayable = true
        )
    }

    /**
     * 内部映射函数：
     * 把搜索建议中的歌手响应转换成 domain 层的 ArtistSuggestion。
     */
    private fun mapToArtistSuggestion(artist: SearchSuggestArtistResponse): ArtistSuggestion {
        return ArtistSuggestion(
            id = artist.id,
            name = artist.name ?: "未知歌手",
            coverUrl = artist.coverUrl
        )
    }

    /**
     * 内部映射函数：
     * 把搜索建议中的歌单响应转换成 domain 层的 PlaylistSuggestion。
     */
    private fun mapToPlaylistSuggestion(playlist: SearchSuggestPlaylistResponse): PlaylistSuggestion {
        return PlaylistSuggestion(
            id = playlist.id,
            name = playlist.name ?: "未知歌单",
            coverUrl = playlist.coverUrl,
            trackCount = playlist.trackCount ?: 0
        )
    }

    /**
     * 内部映射函数：
     * 把接口层的 `SearchSongItemResponse` 转换成 domain 层统一使用的 `Track`。
     */
    private fun mapToDomainTrack(song: SearchSongItemResponse): Track {
        val qualityTags = mutableListOf<AudioQuality>()
        if (song.sq != null) qualityTags.add(AudioQuality.LOSSLESS)
        if (song.hr != null) qualityTags.add(AudioQuality.HIRES)
        if (song.h != null) qualityTags.add(AudioQuality.HIGH)
        if (song.l != null || song.m != null) qualityTags.add(AudioQuality.STANDARD)

        return Track(
            id = song.id,
            title = song.name,
            durationMs = song.dt,
            artists = song.ar.map { artist ->
                Artist(
                    id = artist.id ?: 0L,
                    name = artist.name ?: "未知歌手",
                    coverUrl = null
                )
            },
            album = Album(
                id = song.al?.id ?: 0L,
                title = song.al?.name ?: "未知专辑",
                coverUrl = song.al?.picUrl
            ),
            coverUrl = song.al?.picUrl,
            qualityTags = qualityTags,
            playableUrl = null,
            isPlayable = song.fee != 4
        )
    }
}