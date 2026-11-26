package com.example.seteasecloudmusic.feature.player.data.parser

import com.example.seteasecloudmusic.feature.player.domain.model.LyricLine
import com.example.seteasecloudmusic.feature.player.domain.model.LyricWord
import kotlin.math.abs

/** 标准 LRC 逐行歌词解析器，支持同时对齐翻译与罗马音时间轴。 */
object LrcParser {

    // 匹配 LRC 时间标签，如 [01:23.45] 或 [01:23:456]
    private val TIME_PATTERN = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    /** 解析主歌词与可选侧挂翻译/音译文本为按时间升序排列的 [LyricLine] 列表。 */
    fun parse(
        lrcText: String,
        tlyricText: String? = null,
        romalrcText: String? = null
    ): List<LyricLine> {
        val transMap = parseTimedTextMap(tlyricText)
        val romanMap = parseTimedTextMap(romalrcText)

        val timedTexts = extractTimedPairs(lrcText)
        if (timedTexts.isEmpty()) return emptyList()

        timedTexts.sortBy { it.first }

        val result = ArrayList<LyricLine>(timedTexts.size)
        for (i in timedTexts.indices) {
            val (startTime, text) = timedTexts[i]
            if (text.isBlank()) continue
            val endTime = if (i + 1 < timedTexts.size) {
                // 最短行时长 400 ms，避免相邻时间戳过近；末行默认持续 5000 ms
                maxOf(startTime + 400, timedTexts[i + 1].first)
            } else {
                startTime + 5000
            }

            val word = LyricWord(
                word = text,
                startTime = startTime,
                endTime = endTime
            )

            result.add(
                LyricLine(
                    words = listOf(word),
                    translatedLyric = findNearestTimedText(transMap, startTime),
                    romanLyric = findNearestTimedText(romanMap, startTime),
                    startTime = startTime,
                    endTime = endTime
                )
            )
        }

        return result
    }

    internal fun parseTimedTextMap(rawLrc: String?): List<Pair<Int, String>> {
        if (rawLrc.isNullOrBlank()) return emptyList()
        return extractTimedPairs(rawLrc)
            .filter { it.second.isNotBlank() && it.second != "//" }
            .sortedBy { it.first }
    }

    /**
     * 在已按时间升序排列的文本列表中查找距离 [targetMs] 最近的条目。
     *
     * @param toleranceMs 最大时间匹配容差（ms，越大越宽松）
     */
    internal fun findNearestTimedText(
        sortedPairs: List<Pair<Int, String>>,
        targetMs: Int,
        toleranceMs: Int = 650
    ): String {
        if (sortedPairs.isEmpty()) return ""
        var bestText = ""
        var bestDiff = Int.MAX_VALUE
        for ((timeMs, text) in sortedPairs) {
            val diff = abs(timeMs - targetMs)
            if (diff <= toleranceMs && diff < bestDiff) {
                bestDiff = diff
                bestText = text
                if (diff == 0) break
            } else if (timeMs - targetMs > toleranceMs) {
                break
            }
        }
        return bestText
    }

    private fun extractTimedPairs(lrcText: String): MutableList<Pair<Int, String>> {
        val rawLines = lrcText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("{") }

        val timedTexts = mutableListOf<Pair<Int, String>>()
        for (line in rawLines) {
            val timeMatches = TIME_PATTERN.findAll(line).toList()
            if (timeMatches.isEmpty()) continue

            var text = line
            for (match in timeMatches) {
                text = text.replace(match.value, "")
            }
            text = text.trim()

            for (match in timeMatches) {
                val minutes = match.groupValues[1].toIntOrNull() ?: continue
                val seconds = match.groupValues[2].toIntOrNull() ?: continue
                val fraction = match.groupValues[3]
                val fractionMs = when (fraction.length) {
                    1 -> (fraction.toIntOrNull() ?: 0) * 100
                    2 -> (fraction.toIntOrNull() ?: 0) * 10
                    3 -> fraction.toIntOrNull() ?: 0
                    else -> 0
                }
                val millis = (minutes * 60 + seconds) * 1000 + fractionMs
                timedTexts.add(millis to text)
            }
        }
        return timedTexts
    }
}
