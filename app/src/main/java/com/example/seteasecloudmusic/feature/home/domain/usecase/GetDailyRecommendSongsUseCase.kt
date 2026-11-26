package com.example.seteasecloudmusic.feature.home.domain.usecase

import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.home.domain.repository.HomeRecommendRepository
import javax.inject.Inject

/** 获取首页每日推荐歌曲及本地缓存的用例。 */
class GetDailyRecommendSongsUseCase @Inject constructor(
    private val homeRecommendRepository: HomeRecommendRepository
) {
    suspend operator fun invoke(
        afresh: Boolean = false
    ): Result<List<Track>> {
        return homeRecommendRepository.getDailyRecommendSongs(afresh)
    }

    fun getCached(): List<Track>? {
        return homeRecommendRepository.getCachedDailyRecommendSongs()
    }
}
