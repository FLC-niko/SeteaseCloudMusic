package com.example.seteasecloudmusic.feature.artist.domain.usecase

import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistSongOrder
import com.example.seteasecloudmusic.feature.artist.domain.model.PagedResult
import com.example.seteasecloudmusic.feature.artist.domain.repository.ArtistRepository
import javax.inject.Inject

/** 校验参数并分页获取歌手歌曲列表（默认每页 50 首）。 */
class GetArtistSongsUseCase @Inject constructor(
    private val artistRepository: ArtistRepository
) {
    suspend operator fun invoke(
        artistId: Long,
        order: ArtistSongOrder = ArtistSongOrder.HOT,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PagedResult<Track>> {
        if (artistId <= 0L) {
            return Result.failure(IllegalArgumentException("invalid artistId"))
        }
        if (limit <= 0 || offset < 0) {
            return Result.failure(IllegalArgumentException("invalid paging params"))
        }
        return artistRepository.getArtistSongs(
            artistId = artistId,
            order = order,
            limit = limit,
            offset = offset
        )
    }
}
