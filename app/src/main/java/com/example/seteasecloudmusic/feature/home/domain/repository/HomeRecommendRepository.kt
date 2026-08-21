package com.example.seteasecloudmusic.feature.home.domain.repository

import com.example.seteasecloudmusic.core.model.Track

interface HomeRecommendRepository {
    suspend fun getDailyRecommendSongs(
        afresh: Boolean = false
    ): Result<List<Track>>

    fun getCachedDailyRecommendSongs(): List<Track>?
}
