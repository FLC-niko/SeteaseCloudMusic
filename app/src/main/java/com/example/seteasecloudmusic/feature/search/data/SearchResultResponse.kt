package com.example.seteasecloudmusic.feature.search.data

import com.google.gson.annotations.SerializedName

/**
 * cloudsearch/search 接口的顶层响应。
 */
data class SearchResultResponse(
	val result: SearchResultBodyResponse? = null,
	val code: Int = 0,
	val trp: SearchTrackPathResponse? = null
)

/**
 * 搜索结果主体，包含歌曲、歌手、歌单、专辑列表与总数。
 */
data class SearchResultBodyResponse(
	val songs: List<SearchSongItemResponse> = emptyList(),
	val songCount: Int = 0,
	val artists: List<SearchArtistResponse> = emptyList(),
	val artistCount: Int = 0,
	val albums: List<SearchAlbumResponse> = emptyList(),
	val albumCount: Int = 0,
	val playlists: List<SearchPlaylistResponse> = emptyList(),
	val playlistCount: Int = 0,
	val hasMore: Boolean? = null
)

/**
 * 单首歌曲的搜索结果条目。
 */
data class SearchSongItemResponse(
	val no: Int = 0,
	val rt: String? = null,
	val copyright: Int? = null,
	val fee: Int? = null,
	val privilege: SongPrivilegeResponse? = null,
	val mst: Int? = null,
	val pst: Int? = null,
	val pop: Int? = null,
	val dt: Long? = null,
	val rtype: Int? = null,
	@SerializedName("s_id")
	val songAliasId: Long? = null,
	val rtUrls: List<String> = emptyList(),
	val resourceState: Boolean? = null,
	val id: Long = 0L,
	val sq: SongQualityResponse? = null,
	val hr: SongQualityResponse? = null,
	val st: Int? = null,
	val cd: String? = null,
	val publishTime: Long? = null,
	val cf: String? = null,
	val originCoverType: Int? = null,
	val h: SongQualityResponse? = null,
	val mv: Long? = null,
	val al: SearchAlbumResponse? = null,
	val originSongSimpleData: OriginSongSimpleDataResponse? = null,
	val l: SongQualityResponse? = null,
	val m: SongQualityResponse? = null,
	val version: Int? = null,
	val cp: Int? = null,
	val alia: List<String> = emptyList(),
	val djId: Long? = null,
	val single: Int? = null,
	val ar: List<SearchArtistResponse> = emptyList(),
	val ftype: Int? = null,
	val t: Int? = null,
	val v: Int? = null,
	val name: String = "",
	val mark: Long? = null
)

/**
 * 歌曲权限信息（可播、可下、码率等级等）。
 */
data class SongPrivilegeResponse(
	val flag: Long? = null,
	val code: Int? = null,
	val dlLevel: String? = null,
	val subp: Int? = null,
	val fl: Long? = null,
	val fee: Int? = null,
	val dl: Long? = null,
	val plLevel: String? = null,
	val maxBrLevel: String? = null,
	val rightSource: Int? = null,
	val maxbr: Long? = null,
	val id: Long? = null,
	val sp: Int? = null,
	val payed: Int? = null,
	val st: Int? = null,
	val chargeInfoList: List<ChargeInfoResponse> = emptyList(),
	val freeTrialPrivilege: FreeTrialPrivilegeResponse? = null,
	val downloadMaxbr: Long? = null,
	val downloadMaxBrLevel: String? = null,
	val cp: Int? = null,
	val preSell: Boolean? = null,
	val playMaxBrLevel: String? = null,
	val cs: Boolean? = null,
	val toast: Boolean? = null,
	val playMaxbr: Long? = null,
	val flLevel: String? = null,
	val pl: Long? = null
)

/**
 * 权益计费信息条目。
 */
data class ChargeInfoResponse(
	val rate: Long? = null,
	val chargeType: Int? = null
)

/**
 * 免费试听权限信息。
 */
data class FreeTrialPrivilegeResponse(
	val userConsumable: Boolean? = null,
	val resConsumable: Boolean? = null,
	val cannotListenReason: Int? = null,
	val listenType: Int? = null
)

/**
 * 音质信息（码率、采样率、文件大小等）。
 */
data class SongQualityResponse(
	val br: Long? = null,
	val fid: Long? = null,
	val size: Long? = null,
	val vd: Long? = null,
	val sr: Int? = null
)

/**
 * 专辑信息。
 */
data class SearchAlbumResponse(
	val picUrl: String? = null,
	val name: String? = null,
	val tns: List<String> = emptyList(),
	@SerializedName("pic_str")
	val picStr: String? = null,
	val id: Long? = null,
	val pic: Long? = null
)

/**
 * 歌手信息。
 */
data class SearchArtistResponse(
	val name: String? = null,
	val tns: List<String> = emptyList(),
	val alias: List<String> = emptyList(),
	val id: Long? = null,
	val alia: List<String> = emptyList(),
	@SerializedName("picUrl")
	val picUrl: String? = null,
	@SerializedName("img1v1Url")
	val img1v1Url: String? = null
)

/**
 * 歌单搜索条目。
 */
data class SearchPlaylistResponse(
	val id: Long = 0L,
	val name: String? = null,
	@SerializedName("coverImgUrl")
	val coverImgUrl: String? = null,
	val trackCount: Int = 0
)

/**
 * 改编/关联原曲信息。
 */
data class OriginSongSimpleDataResponse(
	val artists: List<OriginSongArtistResponse> = emptyList(),
	val name: String? = null,
	val songId: Long? = null,
	val albumMeta: OriginSongAlbumMetaResponse? = null
)

/**
 * 原曲歌手信息。
 */
data class OriginSongArtistResponse(
	val name: String? = null,
	val id: Long? = null
)

/**
 * 原曲专辑信息。
 */
data class OriginSongAlbumMetaResponse(
	val name: String? = null,
	val id: Long? = null
)

/**
 * 搜索链路规则信息（实验/策略字段）。
 */
data class SearchTrackPathResponse(
	val rules: List<String> = emptyList()
)