package com.example.seteasecloudmusic.feature.discover.data

import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.AudioQuality
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverPlaylist
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverToplist
import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverTrackPreview
import com.example.seteasecloudmusic.feature.discover.domain.repository.DiscoverRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DiscoverRepositoryImpl @Inject constructor(
    private val discoverService: DiscoverService
) : DiscoverRepository {
    override suspend fun getPersonalizedPlaylists(limit: Int): Result<List<DiscoverPlaylist>> =
        withContext(Dispatchers.IO) {
            try {
                val response = discoverService.getPersonalized(limit)
                if (response.code != 200) {
                    return@withContext Result.failure(Exception("API error code: ${response.code}"))
                }

                val playlists = response.result.map {
                    DiscoverPlaylist(
                        id = it.id,
                        name = it.name,
                        coverUrl = it.picUrl,
                        playCount = it.playCount,
                        trackCount = it.trackCount,
                        copywriter = it.copywriter?.takeIf(String::isNotBlank)
                    )
                }
                Result.success(playlists)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getPersonalizedNewsongs(limit: Int): Result<List<Track>> =
        withContext(Dispatchers.IO) {
            try {
                val response = discoverService.getPersonalizedNewsong(limit)
                if (response.code != 200) {
                    return@withContext Result.failure(Exception("API error code: ${response.code}"))
                }

                val tracks = response.result
                    .mapNotNull { item -> item.song?.let { mapSongToTrack(it, item.picUrl) } }
                    .distinctBy { it.id }
                Result.success(tracks)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getToplists(): Result<List<DiscoverToplist>> =
        withContext(Dispatchers.IO) {
            try {
                val response = discoverService.getToplistDetail()
                if (response.code != 200) {
                    return@withContext Result.failure(Exception("API error code: ${response.code}"))
                }

                val toplists = response.list.map { item ->
                    DiscoverToplist(
                        id = item.id,
                        name = item.name.orEmpty(),
                        coverUrl = item.coverImgUrl,
                        updateFrequency = item.updateFrequency,
                        previews = item.tracks.map { preview ->
                            DiscoverTrackPreview(
                                title = preview.first.orEmpty(),
                                artistName = preview.second.orEmpty()
                            )
                        }
                    )
                }
                Result.success(toplists)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getHotPlaylists(limit: Int, offset: Int): Result<List<DiscoverPlaylist>> =
        withContext(Dispatchers.IO) {
            try {
                val response = discoverService.getTopPlaylist(limit = limit, offset = offset)
                if (response.code != 200) {
                    return@withContext Result.failure(Exception("API error code: ${response.code}"))
                }

                val playlists = response.playlists.map {
                    DiscoverPlaylist(
                        id = it.id,
                        name = it.name,
                        coverUrl = it.coverImgUrl,
                        playCount = it.playCount,
                        trackCount = it.trackCount,
                        copywriter = it.creator?.nickname
                    )
                }
                Result.success(playlists)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun mapSongToTrack(song: NewsongItemResponse, fallbackCoverUrl: String?): Track {
        val qualityTags = mutableListOf<AudioQuality>()
        if (song.privilege?.pl != null && (song.privilege.pl ?: 0L) >= 320000L) {
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

        return Track(
            id = song.id,
            title = song.name?.takeIf { it.isNotBlank() } ?: "未知歌曲",
            artists = artists,
            album = album,
            qualityTags = qualityTags,
            coverUrl = song.album?.picUrl ?: fallbackCoverUrl,
            durationMs = null,
            playableUrl = null,
            isPlayable = isPlayable
        )
    }
}
