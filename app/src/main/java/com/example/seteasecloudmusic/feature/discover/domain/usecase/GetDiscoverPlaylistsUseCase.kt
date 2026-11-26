package com.example.seteasecloudmusic.feature.discover.domain.usecase

import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverPlaylist
import com.example.seteasecloudmusic.feature.discover.domain.repository.DiscoverRepository
import javax.inject.Inject

/**
 * 获取发现页个性化推荐歌单的用例。
 */
class GetDiscoverPlaylistsUseCase @Inject constructor(
    private val discoverRepository: DiscoverRepository
) {
    suspend operator fun invoke(limit: Int = 8): Result<List<DiscoverPlaylist>> {
        return discoverRepository.getPersonalizedPlaylists(limit)
    }
}
