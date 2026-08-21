package com.example.seteasecloudmusic.feature.search.data

import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.AudioQuality
import com.example.seteasecloudmusic.core.model.Track
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
    private val musicService: NeteaseMusicService
) : SearchRepository {

    // 内存高速缓存：已解析的歌曲播放直链，避免重复发起耗时网络请求（0ms 极速切歌）
    private val trackUrlCache = ConcurrentHashMap<Long, String>()

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
            // 1. 命中内存缓存直接秒级返回 (0ms)
            trackUrlCache[trackId]?.let { cachedUrl ->
                if (cachedUrl.isNotBlank()) {
                    return@withContext Result.success(cachedUrl)
                }
            }

            // 2. 优化请求策略：优先请求无损/极高标准，单次网络往返直出最佳音源
            val primaryLevel = if (level.isNotBlank()) level else "lossless"
            val levelsToTry = if (primaryLevel == "hires") {
                listOf("hires", "lossless", "standard")
            } else {
                listOf(primaryLevel, "standard")
            }.distinct()

            var bestUrl: String? = null
            var trialUrl: String? = null

            for (lvl in levelsToTry) {
                val response = musicService.getSongUrl(trackId, lvl)
                if (response.code == 200) {
                    val item = response.data.firstOrNull()
                    val url = item?.url
                    if (!url.isNullOrBlank()) {
                        // 判断是否为试听限制（freeTrialInfo 存在或者不可播原因）
                        val isTrial = item.freeTrialInfo != null ||
                            (item.freeTimeTrialPrivilege?.type != null && item.freeTimeTrialPrivilege.type != 0) ||
                            item.freeTrialPrivilege?.cannotListenReason == 1

                        if (!isTrial) {
                            bestUrl = url
                            break
                        } else if (trialUrl == null) {
                            trialUrl = url
                        }
                    }
                }
            }

            val finalUrl = bestUrl ?: trialUrl
            if (finalUrl != null) {
                trackUrlCache[trackId] = finalUrl
                Result.success(finalUrl)
            } else {
                Result.failure(Exception("URL parameter is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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