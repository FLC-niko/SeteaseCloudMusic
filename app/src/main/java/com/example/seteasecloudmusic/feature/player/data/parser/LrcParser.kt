package com.example.seteasecloudmusic.feature.player.data.parser

import com.example.seteasecloudmusic.feature.player.domain.model.LyricLine
import com.example.seteasecloudmusic.feature.player.domain.model.LyricWord
import kotlin.math.abs

object LrcParser {

    private val TIME_PATTERN = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

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
