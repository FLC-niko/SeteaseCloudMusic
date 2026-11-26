package com.example.seteasecloudmusic.feature.search.domain

import javax.inject.Inject

/**
 * 获取歌曲可播放直链业务用例：校验 `trackId` 合法性后调用 [SearchRepository.getTrackUrl]。
 *
 * [level] 默认为 `"standard"`（标准音质）。
 */
class GetTrackUrlUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    suspend operator fun invoke(trackId: Long, level: String = "standard"): Result<String> {
        if (trackId <= 0L) return Result.failure(IllegalArgumentException("invalid trackId"))
        return searchRepository.getTrackUrl(trackId, level)
    }
}