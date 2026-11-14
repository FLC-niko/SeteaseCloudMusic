package com.example.seteasecloudmusic.feature.artist.domain.usecase

import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistAlbumDetail
import com.example.seteasecloudmusic.feature.artist.domain.repository.ArtistRepository
import javax.inject.Inject

class GetAlbumDetailUseCase @Inject constructor(
    private val artistRepository: ArtistRepository
) {
    suspend operator fun invoke(albumId: Long): Result<ArtistAlbumDetail> {
        if (albumId <= 0L) {
            return Result.failure(IllegalArgumentException("invalid albumId"))
        }
        return artistRepository.getAlbumDetail(albumId)
    }
}
