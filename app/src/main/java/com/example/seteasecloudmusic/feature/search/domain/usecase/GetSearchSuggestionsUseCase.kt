package com.example.seteasecloudmusic.feature.search.domain

import javax.inject.Inject

/**
 * 获取搜索联想建议业务用例：对输入做空白校验与裁剪后，委托 [SearchRepository] 查询单曲、歌手、歌单及匹配词建议。
 */
class GetSearchSuggestionsUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    suspend operator fun invoke(
        query: String,
        type: String? = null
    ): Result<SearchSuggestions> {
        if (query.isBlank()) {
            return Result.success(SearchSuggestions())
        }
        return searchRepository.getSearchSuggestions(query = query.trim(), type = type)
    }
}