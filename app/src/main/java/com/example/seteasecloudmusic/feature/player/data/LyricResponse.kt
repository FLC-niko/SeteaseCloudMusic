package com.example.seteasecloudmusic.feature.player.data

/** 网易云歌词接口响应 DTO，仅允许 data 层和 Retrofit Service 使用。 */
data class LyricResponseDto(
    val sgc: Boolean? = null,
    val sfy: Boolean? = null,
    val qfy: Boolean? = null,
    val lrc: LyricDataDto? = null,
    val klyric: LyricDataDto? = null,
    val tlyric: LyricDataDto? = null,
    val romalrc: LyricDataDto? = null,
    val yrc: LyricDataDto? = null,
    val code: Int = 0
)

data class LyricDataDto(
    val version: Int = 0,
    val lyric: String = ""
)

data class YrcMetadata(
    val t: Int,
    val c: List<YrcMetadataItem>
)

data class YrcMetadataItem(
    val tx: String,
    val li: String? = null,
    val or: String? = null
)
