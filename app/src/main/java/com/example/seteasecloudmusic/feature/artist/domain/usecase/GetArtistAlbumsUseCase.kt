package com.example.seteasecloudmusic.feature.artist.domain.usecase

import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistAlbum
import com.example.seteasecloudmusic.feature.artist.domain.model.PagedResult
import com.example.seteasecloudmusic.feature.artist.domain.repository.ArtistRepository
import javax.inject.Inject

/** 校验参数并分页获取歌手专辑列表（默认每页 30 张）。 */
class GetArtistAlbumsUseCase @Inject constructor(
    private val artistRepository: ArtistRepository
) {
    suspend operator fun invoke(
        artistId: Long,
        limit: Int = 30,
        offset: Int = 0
    ): Result<PagedResult<ArtistAlbum>> {
        if (artistId <= 0L) {
            return Result.failure(IllegalArgumentException("invalid artistId"))
        }
        if (limit <= 0 || offset < 0) {
            return Result.failure(IllegalArgumentException("invalid paging params"))
        }
        return artistRepository.getArtistAlbums(
            artistId = artistId,
            limit = limit,
            offset = offset
        )
    }
}
