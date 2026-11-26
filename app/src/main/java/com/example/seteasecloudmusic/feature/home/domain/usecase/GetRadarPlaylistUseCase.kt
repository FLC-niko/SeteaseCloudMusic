package com.example.seteasecloudmusic.feature.home.domain.usecase

import com.example.seteasecloudmusic.feature.home.domain.repository.HomeRecommendRepository
import com.example.seteasecloudmusic.feature.home.domain.repository.RadarPlaylistData
import javax.inject.Inject

/** 获取首页雷达歌单及其本地缓存的用例。 */
class GetRadarPlaylistUseCase @Inject constructor(
    private val homeRecommendRepository: HomeRecommendRepository
) {
    suspend operator fun invoke(): Result<RadarPlaylistData> {
        return homeRecommendRepository.getRadarPlaylist()
    }

    fun getCached(): RadarPlaylistData? {
        return homeRecommendRepository.getCachedRadarPlaylist()
    }
}
