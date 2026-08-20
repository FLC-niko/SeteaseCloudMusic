package com.example.seteasecloudmusic.feature.discover.domain.usecase

import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverPlaylist
import com.example.seteasecloudmusic.feature.discover.domain.repository.DiscoverRepository
import javax.inject.Inject

class GetDiscoverHotPlaylistsUseCase @Inject constructor(
    private val discoverRepository: DiscoverRepository
) {
    suspend operator fun invoke(limit: Int = 10, offset: Int = 0): Result<List<DiscoverPlaylist>> {
        return discoverRepository.getHotPlaylists(limit = limit, offset = offset)
    }
}
