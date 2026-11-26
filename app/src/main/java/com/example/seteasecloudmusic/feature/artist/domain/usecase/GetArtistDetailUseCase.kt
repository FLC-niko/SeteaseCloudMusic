package com.example.seteasecloudmusic.feature.artist.domain.usecase

import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistDetailInfo
import com.example.seteasecloudmusic.feature.artist.domain.repository.ArtistRepository
import javax.inject.Inject

/** 校验歌手 ID 并获取歌手基础资料。 */
class GetArtistDetailUseCase @Inject constructor(
    private val artistRepository: ArtistRepository
) {
    suspend operator fun invoke(artistId: Long): Result<ArtistDetailInfo> {
        if (artistId <= 0L) {
            return Result.failure(IllegalArgumentException("invalid artistId"))
        }
        return artistRepository.getArtistDetail(artistId)
    }
}
