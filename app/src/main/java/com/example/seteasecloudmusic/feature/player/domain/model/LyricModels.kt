package com.example.seteasecloudmusic.feature.player.domain.model

data class LyricWord(
    val word: String,
    val startTime: Int,
    val endTime: Int,
    val romanWord: String? = null,
    val obscene: Boolean = false,
    val emptyBeat: Int? = null,
    val ruby: List<RubyWord>? = null
)

data class RubyWord(
    val word: String,
    val startTime: Int,
    val endTime: Int
)

data class LyricLine(
    val words: List<LyricWord>,
    val translatedLyric: String = "",
    val romanLyric: String = "",
    val isBG: Boolean = false,
    val isDuet: Boolean = false,
    val startTime: Int,
    val endTime: Int
)

data class ParsedLyrics(
    val lines: List<LyricLine>,
    val hasWordTiming: Boolean = false,
    val source: LyricSource = LyricSource.NONE
)

enum class LyricSource {
    TTML_DB,
    YRC,
    LRC,
    NONE
}

/** 歌词领域模型，隔离网络响应 DTO，供播放器 UI 和用例使用。 */
data class Lyrics(
    val lrc: LyricContent? = null,
    val klyric: LyricContent? = null,
    val tlyric: LyricContent? = null,
    val romalrc: LyricContent? = null,
    val yrc: LyricContent? = null,
    val code: Int = 0
)

data class LyricContent(
    val version: Int = 0,
    val lyric: String = ""
)
