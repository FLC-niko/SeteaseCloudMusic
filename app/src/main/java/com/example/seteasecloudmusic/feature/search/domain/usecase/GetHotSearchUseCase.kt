package com.example.seteasecloudmusic.feature.search.domain.usecase

import com.example.seteasecloudmusic.feature.search.domain.HotSearchItem
import com.example.seteasecloudmusic.feature.search.domain.SearchRepository
import javax.inject.Inject

/**
 * 获取热门搜索列表 UseCase。
 */
class GetHotSearchUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    suspend operator fun invoke(): Result<List<HotSearchItem>> {
        return searchRepository.getHotSearches()
    }
}
