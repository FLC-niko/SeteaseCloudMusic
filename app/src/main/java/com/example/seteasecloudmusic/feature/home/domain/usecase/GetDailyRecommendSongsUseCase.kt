package com.example.seteasecloudmusic.feature.home.domain.usecase

import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.home.domain.repository.HomeRecommendRepository
import javax.inject.Inject

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
