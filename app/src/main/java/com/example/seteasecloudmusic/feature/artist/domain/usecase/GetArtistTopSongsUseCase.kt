package com.example.seteasecloudmusic.feature.artist.domain.usecase

import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistTopSongsResult
import com.example.seteasecloudmusic.feature.artist.domain.repository.ArtistRepository
import javax.inject.Inject

class GetArtistTopSongsUseCase @Inject constructor(
    private val artistRepository: ArtistRepository
) {
    suspend operator fun invoke(artistId: Long): Result<ArtistTopSongsResult> {
        if (artistId <= 0L) {
            return Result.failure(IllegalArgumentException("invalid artistId"))
        }
        return artistRepository.getArtistTopSongs(artistId)
    }
}
