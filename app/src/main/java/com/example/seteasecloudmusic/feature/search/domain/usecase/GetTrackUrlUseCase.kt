package com.example.seteasecloudmusic.feature.search.domain

import javax.inject.Inject

class GetTrackUrlUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    suspend operator fun invoke(trackId: Long, level: String = "standard"): Result<String> {
        if (trackId <= 0L) return Result.failure(IllegalArgumentException("invalid trackId"))
        return searchRepository.getTrackUrl(trackId, level)
    }
}