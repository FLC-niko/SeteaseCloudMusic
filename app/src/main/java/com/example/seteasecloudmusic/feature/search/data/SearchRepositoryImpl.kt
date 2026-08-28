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
    private val playerSettingsManager: PlayerSettingsManager
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
        try {
            val targetLevel = if (level.isNotBlank() && level != "hires") level else "standard"
            val cacheKey = "$trackId-$targetLevel"

            // 1. 命中内存缓存直接秒级返回 (0ms)
            trackUrlCache[cacheKey]?.let { cachedUrl ->
                if (cachedUrl.isNotBlank()) {
                    return@withContext Result.success(cachedUrl)
                }
            }

            // 2. 单次快速直出音频 URL
            val response = musicService.getSongUrl(trackId, targetLevel)
            if (response.code == 200) {
                val item = response.data.firstOrNull()
                val url = item?.url
                if (!url.isNullOrBlank()) {
                    trackUrlCache[cacheKey] = url
                    return@withContext Result.success(url)
                }
            }

            // 3. 兜底请求 standard
            if (targetLevel != "standard") {
                val fallbackKey = "$trackId-standard"
                val fallbackResponse = musicService.getSongUrl(trackId, "standard")
                if (fallbackResponse.code == 200) {
                    val fallbackUrl = fallbackResponse.data.firstOrNull()?.url
                    if (!fallbackUrl.isNullOrBlank()) {
                        trackUrlCache[fallbackKey] = fallbackUrl
                        return@withContext Result.success(fallbackUrl)
                    }
                }
            }

            Result.failure(Exception("无法获取歌曲播放直链"))
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