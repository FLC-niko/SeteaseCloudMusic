package com.example.seteasecloudmusic.feature.artist.domain.repository

import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistAlbum
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistAlbumDetail
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistDescription
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistDetailInfo
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistFavoritePlaylist
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistSongOrder
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistSummary
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistTopSongsResult
import com.example.seteasecloudmusic.feature.artist.domain.model.PagedResult

interface ArtistRepository {
    suspend fun getArtistDetail(artistId: Long): Result<ArtistDetailInfo>

    suspend fun getArtistDescription(artistId: Long): Result<ArtistDescription>

    suspend fun getArtistTopSongs(artistId: Long): Result<ArtistTopSongsResult>

    suspend fun getArtistSongs(
        artistId: Long,
        order: ArtistSongOrder = ArtistSongOrder.HOT,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PagedResult<Track>>

    suspend fun getArtistAlbums(
        artistId: Long,
        limit: Int = 30,
        offset: Int = 0
    ): Result<PagedResult<ArtistAlbum>>

    suspend fun getAlbumDetail(albumId: Long): Result<ArtistAlbumDetail>

    suspend fun getArtistFavoritePlaylists(userId: Long): Result<List<ArtistFavoritePlaylist>>

    suspend fun getSimilarArtists(artistId: Long): Result<List<ArtistSummary>>
}
