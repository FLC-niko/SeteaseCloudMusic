package com.flamingo.lrc

import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastJoinToString

/**
 * LRC 歌词解析器
 *
 * 支持标准 LRC 格式以及 Apple Music 风格的逐字歌词格式。
 * 同时支持对唱检测（通过 [歌手:] 标记或行尾冒号切换左右布局）。
 */
class LyricParser(private val formatText: Boolean = true) {

    /**
     * 解析 LRC 文本
     *
     * @param lrcText LRC 格式的原始文本
     * @return [LyricResult] 包含解析后的歌词条目和对唱标记
     */
    fun parse(lrcText: String): LyricResult {
        val lrcLines = lrcText.lines()
        val timeLyricPairs = mutableListOf<MutableList<Pair<Float, String>>>()

        lrcLines.fastForEachIndexed { index, line ->
            // 将文本中完全相同而且重复的两个时间轴修改为一个
            var remainingLine =
                line.replace(Regex("([\\[\\]]){2,}"), "$1")
                    .replace(Regex("<([^>]+)>"), "[$1]")
                    .replace(Regex("(\\[\\d{2}:\\d{2}\\.\\d{2,3}]){2,}"), "$1")

            val currentLinePairs = mutableListOf<Pair<Float, String>>()

            while (remainingLine.isNotEmpty()) {
                val timeIndex = remainingLine.indexOf("[")
                if (timeIndex == -1) break
                val timeAfter = remainingLine.indexOf("]")
                if (timeAfter == -1) break

                val timeText = remainingLine.substring(timeIndex + 1, timeAfter)
                val timeParts = timeText.split(":")
                if (timeParts.size != 2) break

                val minutes = timeParts[0].toIntOrNull() ?: break
                val seconds = timeParts[1].toFloatOrNull() ?: break
                val time = (minutes * 60 + seconds) * 1000

                // 空行过滤：若当前行无歌词，且与下一行时间差 <= 4.2s，则跳过
                if (remainingLine.substring(timeAfter + 1).isBlank()
                    && remainingLine.substring(0, timeIndex).isBlank()
                ) {
                    if (index + 1 < lrcLines.size) {
                        val nextLine = lrcLines[index + 1]
                        val nextTimeIndex = nextLine.indexOf("[")
                        val nextTimeAfter = nextLine.indexOf("]")
                        if (nextTimeIndex != -1 && nextTimeAfter != -1) {
                            val nextTimeText = nextLine.substring(nextTimeIndex + 1, nextTimeAfter)
                            val nextTimeParts = nextTimeText.split(":")
                            if (nextTimeParts.size == 2) {
                                val nextMinutes = nextTimeParts[0].toIntOrNull()
                                val nextSeconds = nextTimeParts[1].toFloatOrNull()
                                if (nextMinutes != null && nextSeconds != null) {
                                    val nextTime = (nextMinutes * 60 + nextSeconds) * 1000
                                    if (nextTime - time <= 4200) {
                                        break
                                    }
                                }
                            }
                        }
                    } else {
                        break
                    }
                }

                val nextTimeIndex = remainingLine.substring(timeAfter + 1).indexOf("[")
                var lyric = remainingLine.substring(0, timeIndex)

                if (lyric.isEmpty()) {
                    lyric = ""
                    currentLinePairs.add(time to lyric.replace(Regex("(?!\\n)\\s+"), " "))
                } else {
                    if (lyric.trim() != "//") {
                        currentLinePairs.add(
                            time to lyric.replace(Regex("(?!\\n)\\s+"), " ")
                        )
                    }
                }

                remainingLine = remainingLine.substring(timeAfter + 1)
                if (nextTimeIndex == -1) {
                    if (lyric == "") {
                        currentLinePairs.add(
                            time to remainingLine.replace("//", "")
                                .replace(Regex("(?!\\n)\\s+"), " ")
                        )
                    }
                    remainingLine = ""
                }
            }

            if (currentLinePairs.isNotEmpty()) {
                val existingList =
                    timeLyricPairs.find { it.firstOrNull()?.first == currentLinePairs.first().first }
                if (existingList != null) {
                    existingList.addAll(currentLinePairs)
                } else {
                    currentLinePairs.add(currentLinePairs[0].first to "")
                    timeLyricPairs.add(currentLinePairs)
                }
            }
        }

        val (processedEntries, sideFlags) = processOtherSide(timeLyricPairs)
        return LyricResult(
            entries = processedEntries.filter { it.isNotEmpty() },
            sideFlags = sideFlags
        )
    }

    private fun processOtherSide(
        lrcEntries: List<List<Pair<Float, String>>>
    ): Pair<List<List<Pair<Float, String>>>, List<Boolean>> {
        val otherSideResult = mutableListOf<Boolean>()
        var otherSide = false
        var lastSinger: String? = null
        var otherSideFirstTime = false

        val filteredLrcEntries = lrcEntries.map { lines ->
            val lyric = lines.fastJoinToString(separator = "", transform = { it.second })

            var deleteType = -1

            if (lyric.endsWith(":") || lyric.endsWith("：")) {
                otherSide = !otherSide
            } else if (lines.size > 1) {
                val currentSinger = lines[1].second
                if (currentSinger.matches(Regex(".+\\s*:\\s*"))) {
                    deleteType = 0
                    if (lastSinger != null && lastSinger == currentSinger) {
                        // 保持 otherSide 不变
                    } else {
                        if (otherSideFirstTime) {
                            otherSide = !otherSide
                        } else {
                            otherSideFirstTime = true
                        }
                    }
                    lastSinger = currentSinger
                }
            }

            otherSideResult.add(otherSide)

            lines.filterIndexed { index, char ->
                !((index == 1 && char.second.matches(Regex(".+\\s*:\\s*"))) && deleteType == 0)
            }
        }

        return filteredLrcEntries to otherSideResult
    }
}
