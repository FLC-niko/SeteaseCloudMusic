package com.example.seteasecloudmusic.feature.search.data

import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.AudioQuality
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.TrackPlaybackPreparer
import com.example.seteasecloudmusic.feature.search.domain.ArtistSuggestion
import com.example.seteasecloudmusic.feature.search.domain.PlaylistSuggestion
import com.example.seteasecloudmusic.feature.search.domain.SearchSuggestions
import com.example.seteasecloudmusic.feature.search.domain.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * `data.repository` 模块说明：
 *
 * Repository 实现层负责站在 data 和 domain 中间做衔接：
 * 1. 调用具体数据源，例如 Retrofit API、本地数据库、缓存等。
 * 2. 把网络返回的数据模型转换成 domain 层真正使用的模型。
 * 3. 屏蔽数据来源细节，让上层只依赖抽象接口。
 *
 * 当前 `MusicRepositoryImpl` 负责音乐搜索和歌曲播放链接获取。
 */
class SearchRepositoryImpl @Inject constructor(
    private val musicService: NeteaseMusicService
) : SearchRepository, TrackPlaybackPreparer {

    override suspend operator fun invoke(track: Track): Result<Track> {
        return getTrackUrl(track.id).map { url ->
            track.copy(playableUrl = url, isPlayable = url.isNotBlank())
        }
    }

    override suspend fun searchTracks(
        query: String,
        limit: Int,
        offset: Int
    ): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            // 通过远端搜索接口拉取歌曲列表，再映射成领域模型 Track。
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
            // 播放链接属于运行时数据，不直接写死在 Track 中，而是按需查询。
            val response = musicService.getSongUrl(trackId, level)
            if (response.code == 200) {
                val url = response.data.orEmpty().firstOrNull()?.url
                if (url != null) {
                    Result.success(url)
                } else {
                    Result.failure(Exception("URL parameter is null"))
                }
            } else {
                Result.failure(Exception("API Error with code: ${response.code}"))
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
            qualityTags = emptyList(), // 搜索建议接口不包含音质信息
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
        // 根据接口返回的音质字段，整理出界面更容易消费的标签列表。
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
                    coverUrl = null // 搜索接口一般不返回歌手图，所以这里给空
                )
            },
            album = Album(
                id = song.al?.id ?: 0L,
                title = song.al?.name ?: "未知专辑",
                // 搜索接口通常直接返回专辑封面链接，UI 可以直接使用。
                coverUrl = song.al?.picUrl
            ),
            // Track 再保留一份 coverUrl，减少上层读取 album.coverUrl 的样板代码。
            coverUrl = song.al?.picUrl,
            qualityTags = qualityTags,
            // 播放地址需要单独请求，因此这里只先保留空值。
            playableUrl = null,
            // 这里先用 fee 做一层基础可播判断，后续可再接更细的版权/权限逻辑。
            isPlayable = song.fee != 1 && song.fee != 4
        )
    }
}