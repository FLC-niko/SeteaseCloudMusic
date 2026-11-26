package com.example.seteasecloudmusic.feature.discover.domain.usecase

import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.discover.domain.repository.DiscoverRepository
import javax.inject.Inject

/**
 * 获取发现页推荐新歌列表的用例。
 */
class GetDiscoverNewsongsUseCase @Inject constructor(
    private val discoverRepository: DiscoverRepository
) {
    suspend operator fun invoke(limit: Int = 10): Result<List<Track>> {
        return discoverRepository.getPersonalizedNewsongs(limit)
    }
}
