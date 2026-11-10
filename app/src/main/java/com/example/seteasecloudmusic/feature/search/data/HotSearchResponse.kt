package com.example.seteasecloudmusic.feature.search.data

import com.google.gson.annotations.SerializedName

/**
 * /search/hot/detail 详细热搜列表响应。
 */
data class HotSearchDetailResponse(
    val code: Int = 0,
    val data: List<HotSearchDetailItemResponse>? = null,
    val message: String? = null
)

/**
 * 详细热搜条目。
 */
data class HotSearchDetailItemResponse(
    @SerializedName("searchWord")
    val searchWord: String? = null,
    val score: Long? = null,
    val content: String? = null,
    val source: Int? = null,
    val iconType: Int? = null,
    val iconUrl: String? = null,
    val url: String? = null
)

/**
 * /search/hot 简略热门搜索响应。
 */
data class HotSearchResponse(
    val code: Int = 0,
    val result: HotSearchResultResponse? = null
)

/**
 * 简略热门搜索结果包装。
 */
data class HotSearchResultResponse(
    val hots: List<HotSearchBriefItemResponse>? = null
)

/**
 * 简略热门搜索条目。
 */
data class HotSearchBriefItemResponse(
    val first: String? = null,
    val second: Long? = null,
    val third: Any? = null,
    val iconType: Int? = null
)
