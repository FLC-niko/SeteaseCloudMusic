package com.example.seteasecloudmusic.feature.search.domain

import com.example.seteasecloudmusic.core.model.Track
import javax.inject.Inject

/**
 * 分页搜索歌曲业务用例：空白关键词直接返回空列表，非空关键词裁剪后委托 [SearchRepository.searchTracks]。
 */
class SearchMusicUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    suspend operator fun invoke(
        query: String,
        limit: Int = 30, // 单页返回条数
        offset: Int = 0 // 分页偏移量
    ): Result<List<Track>> {
        if (query.isBlank()) return Result.success(emptyList())
        return searchRepository.searchTracks(query = query.trim(), limit = limit, offset = offset)
    }
}