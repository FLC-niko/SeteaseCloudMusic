package com.example.seteasecloudmusic.feature.player.domain.model

/** 单个歌词词元（含时间戳与可选注音/音译元数据）。 */
data class LyricWord(
    val word: String,
    val startTime: Int,
    val endTime: Int,
    val romanWord: String? = null,
    val obscene: Boolean = false,
    val emptyBeat: Int? = null,
    val ruby: List<RubyWord>? = null
)

/** 日文/汉字振假名注音词元。 */
data class RubyWord(
    val word: String,
    val startTime: Int,
    val endTime: Int
)

/** 单行歌词（含词元列表、翻译/音译文本及和声/对唱标记）。 */
data class LyricLine(
    val words: List<LyricWord>,
    val translatedLyric: String = "",
    val romanLyric: String = "",
    val isBG: Boolean = false,
    val isDuet: Boolean = false,
    val startTime: Int,
    val endTime: Int
)

/** 解析后的完整歌词结构。 */
data class ParsedLyrics(
    val lines: List<LyricLine>,
    val hasWordTiming: Boolean = false,
    val source: LyricSource = LyricSource.NONE
)

/** 歌词数据来源类型。 */
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

/** 单段原始歌词内容与版本号。 */
data class LyricContent(
    val version: Int = 0,
    val lyric: String = ""
)
