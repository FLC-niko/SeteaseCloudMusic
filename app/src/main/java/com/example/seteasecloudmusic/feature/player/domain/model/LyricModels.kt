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
