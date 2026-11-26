package com.example.seteasecloudmusic.feature.discover.domain.usecase

import com.example.seteasecloudmusic.feature.discover.domain.model.DiscoverToplist
import com.example.seteasecloudmusic.feature.discover.domain.repository.DiscoverRepository
import javax.inject.Inject

/**
 * 获取发现页排行榜详情列表的用例。
 */
class GetDiscoverToplistsUseCase @Inject constructor(
    private val discoverRepository: DiscoverRepository
) {
    suspend operator fun invoke(): Result<List<DiscoverToplist>> {
        return discoverRepository.getToplists()
    }
}
