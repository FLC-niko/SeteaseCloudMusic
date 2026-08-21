package com.example.seteasecloudmusic.feature.player.data

data class LyricResponse(
    val sgc: Boolean? = null,
    val sfy: Boolean? = null,
    val qfy: Boolean? = null,
    val lrc: LyricData? = null,
    val klyric: LyricData? = null,
    val tlyric: LyricData? = null,
    val romalrc: LyricData? = null,
    val yrc: LyricData? = null,
    val code: Int = 0
)

data class LyricData(
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
