package com.example.seteasecloudmusic.feature.artist.data

import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.AudioQuality
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistAlbum
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistDescription
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistDescriptionSection
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistDetailInfo
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistSongOrder
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistSummary
import com.example.seteasecloudmusic.feature.artist.domain.model.PagedResult
import com.example.seteasecloudmusic.feature.artist.domain.repository.ArtistRepository
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
                    coverUrl = profile?.coverUrl ?: response.data?.cover ?: profile?.picUrl,
                    musicCount = profile?.musicSize,
                    albumCount = profile?.albumSize,
                    mvCount = profile?.mvSize
                )
                Result.success(detail)
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

                val sections = response.introduction.orEmpty()
                    .mapNotNull { section ->
                        val title = section.title?.trim().orEmpty()
                        val content = section.content?.trim().orEmpty()
                        if (content.isBlank()) {
                            null
                        } else {
                            ArtistDescriptionSection(
                                title = if (title.isBlank()) "Artist Story" else title,
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

            val tracks = response.songs.orEmpty()
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

            val albums = response.hotAlbums.orEmpty()
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSimilarArtists(artistId: Long): Result<List<ArtistSummary>> =
        withContext(Dispatchers.IO) {
            try {
                val response = artistService.getSimilarArtists(artistId)
                if (response.code != 200) {
                    return@withContext Result.failure(Exception("API error code: ${response.code}"))
                }

                val artists = response.artists
                    .map {
                        ArtistSummary(
                            id = it.id,
                            name = it.name?.takeIf { value -> value.isNotBlank() } ?: "Unknown Artist",
                            coverUrl = it.picUrl ?: it.imageUrl
                        )
                    }
                    .distinctBy { it.id }

                Result.success(artists)
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

        val artists = song.artists.orEmpty().map { artist ->
            Artist(
                id = artist.id ?: 0L,
                name = artist.name?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
                coverUrl = artist.picUrl ?: artist.imageUrl
            )
        }

        val album = Album(
            id = song.album?.id ?: 0L,
            title = song.album?.name?.takeIf { it.isNotBlank() } ?: "Unknown Album",
            coverUrl = song.album?.coverUrl
        )

        return Track(
            id = song.id,
            title = song.name,
            artists = artists,
            album = album,
            qualityTags = qualityTags,
            coverUrl = song.album?.coverUrl,
            durationMs = song.dt,
            playableUrl = null,
            isPlayable = song.fee != 1 && song.fee != 4
        )
    }

    private fun mapAlbum(album: ArtistAlbumItemResponse): ArtistAlbum {
        return ArtistAlbum(
            id = album.id,
            title = album.name,
            coverUrl = album.coverUrl,
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
}
