package com.example.seteasecloudmusic.feature.home.data

import com.google.gson.annotations.SerializedName

/**
 * /recommend/songs 接口顶层响应。
 */
data class DailyRecommendSongsResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: DailyRecommendSongsDataResponse? = null,
    val dailySongs: List<DailyRecommendSongItemResponse> = emptyList()
)

/**
 * 推荐数据主体。
 */
data class DailyRecommendSongsDataResponse(
    val fromCache: Boolean = false,
    val dailySongs: List<DailyRecommendSongItemResponse> = emptyList(),
    val orderSongs: List<Long> = emptyList(),
    val recommendReasons: List<DailyRecommendReasonResponse> = emptyList(),
    val mvResourceInfos: Any? = null,
    val demote: Boolean? = null,
    val algReturnDemote: Boolean? = null,
    val dailyRecommendInfo: Any? = null
)

data class DailyRecommendReasonResponse(
    val songId: Long = 0L,
    val reason: String? = null,
    val reasonId: String? = null,
    val targetUrl: String? = null
)

/**
 * 每日推荐歌曲条目。
 */
data class DailyRecommendSongItemResponse(
    val id: Long = 0L,
    val name: String = "",
    val mainTitle: String? = null,
    val additionalTitle: String? = null,
    val pst: Int? = null,
    val t: Int? = null,
    val dt: Long? = null,
    val fee: Int? = null,
    val pop: Int? = null,
    val st: Int? = null,
    val rt: String? = null,
    val v: Int? = null,
    val crbt: String? = null,
    val cf: String? = null,
    val mv: Long? = null,
    val publishTime: Long? = null,
    val cd: String? = null,
    val no: Int? = null,
    val rtUrl: String? = null,
    val ftype: Int? = null,
    val rtUrls: List<String> = emptyList(),
    val djId: Long? = null,
    val copyright: Int? = null,
    @SerializedName("s_id")
    val songAliasId: Long? = null,
    val mark: Long? = null,
    val originCoverType: Int? = null,
    val originSongSimpleData: Any? = null,
    val tagPicList: Any? = null,
    val resourceState: Boolean? = null,
    val version: Int? = null,
    val songJumpInfo: Any? = null,
    val entertainmentTags: Any? = null,
    val awardTags: Any? = null,
    val displayTags: Any? = null,
    val markTags: List<Any> = emptyList(),
    val single: Int? = null,
    val noCopyrightRcmd: Any? = null,
    val rtype: Int? = null,
    val rurl: String? = null,
    val mst: Int? = null,
    val cp: Int? = null,
    val alg: String? = null,
    val alia: List<String> = emptyList(),
    val tns: List<String> = emptyList(),
    val reason: String? = null,
    val recommendReason: String? = null,
    @SerializedName("al")
    val album: DailyRecommendAlbumResponse? = null,
    @SerializedName("ar")
    val artists: List<DailyRecommendArtistResponse> = emptyList(),
    val h: DailyRecommendQualityResponse? = null,
    val m: DailyRecommendQualityResponse? = null,
    val l: DailyRecommendQualityResponse? = null,
    val sq: DailyRecommendQualityResponse? = null,
    val hr: DailyRecommendQualityResponse? = null,
    val privilege: DailyRecommendPrivilegeResponse? = null
)

data class DailyRecommendAlbumResponse(
    val id: Long? = null,
    val name: String? = null,
    val picUrl: String? = null,
    val tns: List<String> = emptyList(),
    val pic: Long? = null,
    @SerializedName("pic_str")
    val picStr: String? = null
)

data class DailyRecommendArtistResponse(
    val id: Long? = null,
    val name: String? = null,
    val alias: List<String> = emptyList(),
    val tns: List<String> = emptyList()
)

data class DailyRecommendQualityResponse(
    val br: Long? = null,
    val fid: Long? = null,
    val sr: Int? = null,
    val size: Long? = null,
    val vd: Long? = null
)

data class DailyRecommendPrivilegeResponse(
    val id: Long? = null,
    val fee: Int? = null,
    val payed: Int? = null,
    val realPayed: Int? = null,
    val code: Int? = null,
    val message: String? = null,
    val st: Int? = null,
    val pl: Long? = null,
    val dl: Long? = null,
    val sp: Int? = null,
    val cp: Int? = null,
    val subp: Int? = null,
    val cs: Boolean? = null,
    val maxbr: Long? = null,
    val fl: Long? = null,
    val pc: Any? = null,
    val toast: Boolean? = null,
    val flag: Long? = null,
    val paidBigBang: Boolean? = null,
    val preSell: Boolean? = null,
    val playMaxbr: Long? = null,
    val downloadMaxbr: Long? = null,
    val maxBrLevel: String? = null,
    val playMaxBrLevel: String? = null,
    val downloadMaxBrLevel: String? = null,
    val plLevel: String? = null,
    val dlLevel: String? = null,
    val flLevel: String? = null,
    val rscl: Any? = null,
    val rightSource: Int? = null,
    val chargeInfoList: List<DailyRecommendChargeInfoResponse> = emptyList(),
    val freeTrialPrivilege: DailyRecommendFreeTrialPrivilegeResponse? = null,
    val plLevels: Any? = null,
    val dlLevels: Any? = null,
    val ignoreCache: Any? = null,
    val bd: Any? = null
)

data class DailyRecommendChargeInfoResponse(
    val rate: Long? = null,
    val chargeUrl: String? = null,
    val chargeMessage: String? = null,
    val chargeType: Int? = null
)

data class DailyRecommendFreeTrialPrivilegeResponse(
    val resConsumable: Boolean? = null,
    val userConsumable: Boolean? = null,
    val listenType: Int? = null,
    val cannotListenReason: Int? = null,
    val playReason: String? = null,
    val freeLimitTagType: Int? = null
)

/**
 * /recommend/resource 推荐歌单接口响应
 */
data class RecommendResourceResponse(
    val code: Int? = 0,
    val featureFirst: Boolean? = false,
    val haveRcmdSongs: Boolean? = false,
    val recommend: List<RecommendResourcePlaylistItem> = emptyList()
)

data class RecommendResourcePlaylistItem(
    val id: Long? = 0L,
    val name: String? = null,
    val picUrl: String? = null,
    val playcount: Long? = 0L,
    val trackCount: Int? = 0,
    val copywriter: String? = null,
    val description: String? = null,
    val creator: RecommendResourceCreator? = null
)

data class RecommendResourceCreator(
    val nickname: String? = null,
    val userId: Long? = null,
    val avatarUrl: String? = null
)

/**
 * /personal_fm 与 /personal/fm/mode 接口响应
 */
data class PersonalFmResponse(
    val code: Int = 0,
    val popAdjust: Boolean? = null,
    val data: List<PersonalFmSongItemResponse> = emptyList()
)

data class PersonalFmSongItemResponse(
    val id: Long = 0L,
    val name: String = "",
    @SerializedName("duration", alternate = ["dt"])
    val duration: Long? = null,
    val fee: Int? = null,
    val reason: String? = null,
    @SerializedName("artists", alternate = ["ar"])
    val artists: List<DailyRecommendArtistResponse> = emptyList(),
    @SerializedName("album", alternate = ["al"])
    val album: DailyRecommendAlbumResponse? = null,
    val privilege: DailyRecommendPrivilegeResponse? = null
)
