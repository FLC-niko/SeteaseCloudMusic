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

/** 单段歌词文本与版本 DTO */
data class LyricDataDto(
    val version: Int = 0,
    val lyric: String = ""
)

/** YRC 歌词头部元数据条目（如词曲作者、编曲信息） */
data class YrcMetadata(
    val t: Int,
    val c: List<YrcMetadataItem>
)

/** YRC 头部元数据文本片段 */
data class YrcMetadataItem(
    val tx: String,
    val li: String? = null,
    val or: String? = null
)
