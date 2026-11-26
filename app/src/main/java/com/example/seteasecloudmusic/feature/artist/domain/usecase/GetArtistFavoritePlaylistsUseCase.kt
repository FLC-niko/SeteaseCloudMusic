package com.example.seteasecloudmusic.feature.artist.domain.usecase

import com.example.seteasecloudmusic.feature.artist.domain.model.ArtistFavoritePlaylist
import com.example.seteasecloudmusic.feature.artist.domain.repository.ArtistRepository
import javax.inject.Inject

/** 校验关联用户 ID 并获取歌手爱听歌单列表。 */
class GetArtistFavoritePlaylistsUseCase @Inject constructor(
    private val artistRepository: ArtistRepository
) {
    suspend operator fun invoke(userId: Long): Result<List<ArtistFavoritePlaylist>> {
        if (userId <= 0L) {
            return Result.failure(IllegalArgumentException("invalid userId"))
        }
        return artistRepository.getArtistFavoritePlaylists(userId)
    }
}
