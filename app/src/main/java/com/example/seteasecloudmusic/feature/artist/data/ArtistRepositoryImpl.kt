package com.example.seteasecloudmusic.feature.artist.data

import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.AudioQuality
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistAlbum
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistAlbumDetail
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistDescription
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistDescriptionSection
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistDetailInfo
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistFavoritePlaylist
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistSongOrder
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistSummary
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistTopSongsResult
import com.example.seteasecloudmusic.feature.artist.domain.model.PagedResult
import com.example.seteasecloudmusic.feature.artist.domain.repository.ArtistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

class ArtistRepositoryImpl @Inject constructor(
    private val artistService: ArtistService
) : ArtistRepository {

    override suspend fun getArtistDetail(artistId: Long): Result<ArtistDetailInfo> =
        withContext(Dispatchers.IO) {
            try {
                val response = artistService.getArtistDetail(artistId)
                if (response.code != 200) {
                    return@withContext Result.failure(Exception("API error code: ${response.code}"))
                }

                val profile = response.data?.artist
                val detail = ArtistDetailInfo(
                    id = profile?.id ?: artistId,
                    name = profile?.name?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
                    coverUrl = (profile?.coverUrl ?: response.data?.cover ?: profile?.picUrl).normalizeHttps(),
                    musicCount = profile?.musicSize,
                    albumCount = profile?.albumSize,
                    mvCount = profile?.mvSize,
                    userId = response.data?.user?.userId?.takeIf { it > 0L },
                    alias = profile?.alias?.firstOrNull()?.takeIf { it.isNotBlank() }
                )
                Result.success(detail)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getArtistDescription(artistId: Long): Result<ArtistDescription> =
        withContext(Dispatchers.IO) {
            try {
                val response = artistService.getArtistDescription(artistId)
                if (response.code != 200) {
                    return@withContext Result.failure(Exception("API error code: ${response.code}"))
                }

                val sections = response.introduction
                    .mapNotNull { section ->
                        val title = section.title?.trim().orEmpty()
                        val content = section.content?.trim().orEmpty()
                        if (content.isBlank()) {
                            null
                        } else {
                            ArtistDescriptionSection(
                                title = if (title.isBlank()) "艺人经历" else title,
                                content = content
                            )
                        }
                    }

                Result.success(
                    ArtistDescription(
                        brief = response.briefDesc?.trim().orEmpty(),
                        sections = sections
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getArtistTopSongs(artistId: Long): Result<ArtistTopSongsResult> =
        withContext(Dispatchers.IO) {
            try {
                val topResponse = runCatching { artistService.getArtistTopSongs(artistId) }.getOrNull()
                val rawSongs = if (topResponse != null && topResponse.code == 200 && topResponse.songs.isNotEmpty()) {
                    topResponse.songs.take(50)
                } else {
                    val fallback = artistService.getArtistSongs(
                        artistId = artistId,
                        order = ArtistSongOrder.HOT.value,
                        limit = 50,
                        offset = 0
                    )
                    if (fallback.code != 200) {
                        return@withContext Result.failure(Exception("API error code: ${fallback.code}"))
                    }
                    fallback.songs.take(50)
                }

                val tracks = rawSongs.map(::mapSongToTrack).distinctBy { it.id }
                val publishYears = buildMap {
                    rawSongs.forEach { song ->
                        toYear(song.publishTime)?.let { year ->
                            put(song.id, year)
                        }
                    }
                }
                Result.success(ArtistTopSongsResult(tracks = tracks, publishYears = publishYears))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getArtistSongs(
        artistId: Long,
        order: ArtistSongOrder,
        limit: Int,
        offset: Int
    ): Result<PagedResult<Track>> = withContext(Dispatchers.IO) {
        try {
            val response = artistService.getArtistSongs(
                artistId = artistId,
                order = order.value,
                limit = limit,
                offset = offset
            )
            if (response.code != 200) {
                return@withContext Result.failure(Exception("API error code: ${response.code}"))
            }

            val tracks = response.songs
                .map(::mapSongToTrack)
                .distinctBy { it.id }
            val nextOffset = offset + tracks.size
            val hasMore = response.more || tracks.size >= limit

            Result.success(
                PagedResult(
                    items = tracks,
                    nextOffset = nextOffset,
                    hasMore = hasMore
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getArtistAlbums(
        artistId: Long,
        limit: Int,
        offset: Int
    ): Result<PagedResult<ArtistAlbum>> = withContext(Dispatchers.IO) {
        try {
            val response = artistService.getArtistAlbums(
                artistId = artistId,
                limit = limit,
                offset = offset
            )
            if (response.code != 200) {
                return@withContext Result.failure(Exception("API error code: ${response.code}"))
            }

            val albums = response.hotAlbums
                .map(::mapAlbum)
                .distinctBy { it.id }
            val nextOffset = offset + albums.size
            val hasMore = response.more || albums.size >= limit

            Result.success(
                PagedResult(
                    items = albums,
                    nextOffset = nextOffset,
                    hasMore = hasMore
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAlbumDetail(albumId: Long): Result<ArtistAlbumDetail> =
        withContext(Dispatchers.IO) {
            try {
                val response = artistService.getAlbumDetail(albumId)
                if (response.code != 200) {
                    return@withContext Result.failure(Exception("API error code: ${response.code}"))
                }

                val albumItem = response.album
                val album = ArtistAlbum(
                    id = albumItem?.id ?: albumId,
                    title = albumItem?.name?.takeIf { it.isNotBlank() } ?: "未知专辑",
                    coverUrl = albumItem?.coverUrl.normalizeHttps(),
                    publishYear = toYear(albumItem?.publishTime),
                    trackCount = albumItem?.size ?: response.songs.size
                )
                val tracks = response.songs
                    .map(::mapSongToTrack)
                    .distinctBy { it.id }
                    .map { track ->
                        if (track.coverUrl.isNullOrBlank() && !album.coverUrl.isNullOrBlank()) {
                            track.copy(
                                coverUrl = album.coverUrl,
                                album = track.album.copy(coverUrl = album.coverUrl)
                            )
                        } else {
                            track
                        }
                    }

                val artistName = albumItem?.artist?.name?.takeIf { it.isNotBlank() }
                    ?: tracks.firstOrNull()?.artists?.joinToString(" / ") { it.name }.orEmpty()

                Result.success(
                    ArtistAlbumDetail(
                        album = album,
                        artistName = artistName,
                        description = albumItem?.description?.trim()?.takeIf { it.isNotBlank() },
                        tracks = tracks
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getArtistFavoritePlaylists(userId: Long): Result<List<ArtistFavoritePlaylist>> =
        withContext(Dispatchers.IO) {
            try {
                val response = artistService.getArtistUserPlaylists(uid = userId)
                if (response.code != 200) {
                    return@withContext Result.failure(Exception("API error code: ${response.code}"))
                }

                val playlists = response.playlist
                    .filter { it.id > 0L && !it.name.isNullOrBlank() }
                    .map { item ->
                        ArtistFavoritePlaylist(
                            id = item.id,
                            name = item.name.orEmpty(),
                            coverUrl = item.coverImgUrl.normalizeHttps(),
                            trackCount = item.trackCount,
                            playCount = item.playCount,
                            isLikedPlaylist = item.specialType == 5
                        )
                    }
                    .distinctBy { it.id }

                Result.success(playlists)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getSimilarArtists(artistId: Long): Result<List<ArtistSummary>> =
        withContext(Dispatchers.IO) {
            try {
                val simiArtists = runCatching {
                    val response = artistService.getSimilarArtists(artistId)
                    if (response.code == 200) response.artists else emptyList()
                }.getOrDefault(emptyList())

                val sourceList = if (simiArtists.isNotEmpty()) {
                    simiArtists
                } else {
                    val topResponse = artistService.getTopArtistsFallback(limit = 16, offset = 0)
                    if (topResponse.code != 200) {
                        return@withContext Result.failure(Exception("API error code: ${topResponse.code}"))
                    }
                    topResponse.artists
                }

                val artists = sourceList
                    .filter { it.id > 0L && it.id != artistId }
                    .map { item ->
                        val subtitle = item.alias.firstOrNull()?.takeIf { it.isNotBlank() }
                            ?: item.trans?.takeIf { it.isNotBlank() }
                            ?: item.musicSize?.takeIf { it > 0 }?.let { "${it} 首单曲" }
                        ArtistSummary(
                            id = item.id,
                            name = item.name?.takeIf { value -> value.isNotBlank() } ?: "Unknown Artist",
                            coverUrl = (item.picUrl ?: item.imageUrl).normalizeHttps(),
                            subtitle = subtitle
                        )
                    }
                    .distinctBy { it.id }

                Result.success(artists)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun mapSongToTrack(song: ArtistSongItemResponse): Track {
        val qualityTags = mutableListOf<AudioQuality>()
        if (song.sq != null) qualityTags.add(AudioQuality.LOSSLESS)
        if (song.hr != null) qualityTags.add(AudioQuality.HIRES)
        if (song.h != null) qualityTags.add(AudioQuality.HIGH)
        if (song.l != null || song.m != null) qualityTags.add(AudioQuality.STANDARD)

        val artists = song.artists.map { artist ->
            Artist(
                id = artist.id ?: 0L,
                name = artist.name?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
                coverUrl = (artist.picUrl ?: artist.imageUrl).normalizeHttps()
            )
        }

        val albumCover = song.album?.coverUrl.normalizeHttps()
        val album = Album(
            id = song.album?.id ?: 0L,
            title = song.album?.name?.takeIf { it.isNotBlank() } ?: "Unknown Album",
            coverUrl = albumCover
        )

        return Track(
            id = song.id,
            title = song.name,
            artists = artists,
            album = album,
            qualityTags = qualityTags,
            coverUrl = albumCover,
            durationMs = song.dt,
            // fee == 4 表示单独付费数字专辑，VIP歌曲(fee == 1)正常允许播放
            isPlayable = song.fee != 4
        )
    }

    private fun mapAlbum(album: ArtistAlbumItemResponse): ArtistAlbum {
        return ArtistAlbum(
            id = album.id,
            title = album.name,
            coverUrl = album.coverUrl.normalizeHttps(),
            publishYear = toYear(album.publishTime),
            trackCount = album.size
        )
    }

    private fun toYear(timestampMs: Long?): Int? {
        val validTimestamp = timestampMs ?: return null
        if (validTimestamp <= 0L) {
            return null
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = validTimestamp
        return calendar.get(Calendar.YEAR)
    }

    private fun String?.normalizeHttps(): String? {
        if (this.isNullOrBlank()) return this
        return if (startsWith("http://")) replaceFirst("http://", "https://") else this
    }
}
