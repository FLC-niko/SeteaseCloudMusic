package com.example.seteasecloudmusic.feature.home.domain.usecase

import com.example.seteasecloudmusic.feature.home.domain.repository.FavoriteRecommendSectionData
import com.example.seteasecloudmusic.feature.home.domain.repository.HomeRecommendRepository
import javax.inject.Inject

/**
 * 获取首页「根据你喜爱的歌曲推荐」（对应 HOMEPAGE_BLOCK_STYLE_RCMD 风格推荐模块）用例。
 */
class GetFavoriteRecommendSongsUseCase @Inject constructor(
    private val repository: HomeRecommendRepository
) {
    suspend operator fun invoke(afresh: Boolean = false): Result<FavoriteRecommendSectionData> {
        return repository.getFavoriteRecommendSongs(afresh)
    }

    fun getCached(): FavoriteRecommendSectionData? {
        return repository.getCachedFavoriteRecommendSongs()
    }
}

