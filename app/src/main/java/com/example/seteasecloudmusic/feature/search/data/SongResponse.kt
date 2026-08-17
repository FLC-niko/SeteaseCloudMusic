package com.example.seteasecloudmusic.feature.search.data

import com.google.gson.JsonElement

/**
 * /song/url/v1 接口顶层响应。
 */
data class SongResponse(
	val data: List<SongUrlItemResponse>? = null,
	val code: Int = 0
)

/**
 * 单首歌曲播放地址及其元信息。
 */
data class SongUrlItemResponse(
    val id: Long = 0L,
    val url: String? = null,
    val br: Int? = null,
    val size: Long? = null,
    val md5: String? = null,
    val code: Int? = null,
    val expi: Int? = null,
    val type: String? = null,
    val gain: Double? = null,
    val peak: Double? = null,
    val closedGain: Double? = null,
    val closedPeak: Double? = null,
    val fee: Int? = null,
    val uf: JsonElement? = null,
    val payed: Int? = null,
    val flag: Long? = null,
    val canExtend: Boolean? = null,
    val freeTrialInfo: JsonElement? = null,
    val level: String? = null,
    val encodeType: String? = null,
    val channelLayout: JsonElement? = null,
    val freeTrialPrivilege: SongFreeTrialPrivilegeResponse? = null,
    val freeTimeTrialPrivilege: SongFreeTimeTrialPrivilegeResponse? = null,
    val urlSource: Int? = null,
    val rightSource: Int? = null,
    val podcastCtrp: JsonElement? = null,
    val effectTypes: JsonElement? = null,
    val time: Long? = null,
    val message: String? = null,
    val levelConfuse: JsonElement? = null,
    val musicId: String? = null,
    val accompany: JsonElement? = null,
    val sr: Int? = null,
    val auEff: JsonElement? = null,
    val immerseType: JsonElement? = null,
    val beatType: Int? = null
)

/**
 * 免费试听权限信息。
 */
data class SongFreeTrialPrivilegeResponse(
	val resConsumable: Boolean? = null,
	val userConsumable: Boolean? = null,
	val listenType: Int? = null,
	val cannotListenReason: Int? = null,
	val playReason: String? = null,
	val freeLimitTagType: String? = null
)

/**
 * 限时试听权限信息。
 */
data class SongFreeTimeTrialPrivilegeResponse(
	val resConsumable: Boolean? = null,
	val userConsumable: Boolean? = null,
	val type: Int? = null,
	val remainTime: Long? = null
)