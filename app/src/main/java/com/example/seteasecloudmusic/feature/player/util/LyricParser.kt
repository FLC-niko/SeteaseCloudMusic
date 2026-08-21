package com.example.seteasecloudmusic.feature.player.util

import com.example.seteasecloudmusic.feature.player.data.LyricResponse
import com.example.seteasecloudmusic.feature.player.domain.model.LyricLine
import com.example.seteasecloudmusic.feature.player.domain.model.LyricSource
import com.example.seteasecloudmusic.feature.player.domain.model.LyricWord
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics

object LyricParser {
    // Regex for basic LRC format, e.g. [00:16.21]text or [01:23.456]text
    private val lrcLineRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\](.*)")

    // Regex for YRC format line, e.g. [16210,3460](16210,670,0)还(16880,410,0)没...
    private val yrcLineRegex = Regex("\\[(\\d+),(\\d+)\\](.*)")
    private val yrcWordRegex = Regex("\\((\\d+),(\\d+),\\d+\\)([^(]*)")

    fun parseLyricResponse(response: LyricResponse?): ParsedLyrics {
        if (response == null) return ParsedLyrics(emptyList(), false, LyricSource.NONE)

        // 1. 优先解析 YRC 逐字歌词
        val yrcLyric = response.yrc?.lyric
        if (!yrcLyric.isNullOrBlank()) {
            val yrcParsed = parseYrcString(yrcLyric, response.tlyric?.lyric)
            if (yrcParsed.lines.isNotEmpty()) {
                return yrcParsed
            }
        }

        // 2. 降级解析 LRC 逐行歌词
        val lrcLyric = response.lrc?.lyric
        if (!lrcLyric.isNullOrBlank()) {
            return parseLrcString(lrcLyric, response.tlyric?.lyric)
        }

        return ParsedLyrics(emptyList(), false, LyricSource.NONE)
    }

    private fun parseYrcString(yrc: String, tlyric: String?): ParsedLyrics {
        val transMap = parseTranslationMap(tlyric)
        val lines = mutableListOf<LyricLine>()

        yrc.lineSequence().forEach { rawLine ->
            val text = rawLine.trim()
            if (text.isEmpty() || (text.startsWith("{") && text.endsWith("}"))) return@forEach

            val match = yrcLineRegex.find(text) ?: return@forEach
            val lineStart = match.groupValues[1].toIntOrNull() ?: 0
            val lineDur = match.groupValues[2].toIntOrNull() ?: 0
            val lineEnd = lineStart + lineDur
            val wordsContent = match.groupValues[3]

            val words = mutableListOf<LyricWord>()
            val wordMatches = yrcWordRegex.findAll(wordsContent)
            for (wMatch in wordMatches) {
                val wStart = wMatch.groupValues[1].toIntOrNull() ?: 0
                val wDur = wMatch.groupValues[2].toIntOrNull() ?: 0
                val wText = wMatch.groupValues[3]
                words.add(LyricWord(word = wText, startTime = wStart, endTime = wStart + wDur))
            }

            val fullLineText = if (words.isNotEmpty()) words.joinToString("") { it.word } else wordsContent
            val translated = findTranslation(transMap, lineStart)

            lines.add(
                LyricLine(
                    words = if (words.isNotEmpty()) words else listOf(LyricWord(fullLineText, lineStart, lineEnd)),
                    translatedLyric = translated,
                    startTime = lineStart,
                    endTime = lineEnd
                )
            )
        }

        return ParsedLyrics(lines, hasWordTiming = true, source = LyricSource.YRC)
    }

    private fun parseLrcString(lrc: String, tlyric: String?): ParsedLyrics {
        val transMap = parseTranslationMap(tlyric)
        val rawLines = mutableListOf<Pair<Int, String>>()

        lrc.lineSequence().forEach { line ->
            val text = line.trim()
            if (text.isEmpty()) return@forEach

            val match = lrcLineRegex.find(text) ?: return@forEach
            val min = match.groupValues[1].toIntOrNull() ?: 0
            val sec = match.groupValues[2].toIntOrNull() ?: 0
            val msStr = match.groupValues[3]
            val ms = when (msStr.length) {
                1 -> msStr.toInt() * 100
                2 -> msStr.toInt() * 10
                3 -> msStr.toInt()
                else -> 0
            }
            val timeMs = min * 60000 + sec * 1000 + ms
            val content = match.groupValues[4].trim()
            if (content.isNotBlank()) {
                rawLines.add(timeMs to content)
            }
        }

        val sorted = rawLines.sortedBy { it.first }
        val lines = sorted.mapIndexed { index, (timeMs, content) ->
            val nextTimeMs = sorted.getOrNull(index + 1)?.first ?: (timeMs + 5000)
            val translated = findTranslation(transMap, timeMs)
            LyricLine(
                words = listOf(LyricWord(word = content, startTime = timeMs, endTime = nextTimeMs)),
                translatedLyric = translated,
                startTime = timeMs,
                endTime = nextTimeMs
            )
        }

        return ParsedLyrics(lines, hasWordTiming = false, source = LyricSource.LRC)
    }

    private fun parseTranslationMap(tlyric: String?): Map<Int, String> {
        if (tlyric.isNullOrBlank()) return emptyMap()
        val map = mutableMapOf<Int, String>()
        tlyric.lineSequence().forEach { line ->
            val match = lrcLineRegex.find(line.trim()) ?: return@forEach
            val min = match.groupValues[1].toIntOrNull() ?: 0
            val sec = match.groupValues[2].toIntOrNull() ?: 0
            val msStr = match.groupValues[3]
            val ms = when (msStr.length) {
                1 -> msStr.toInt() * 100
                2 -> msStr.toInt() * 10
                3 -> msStr.toInt()
                else -> 0
            }
            val timeMs = min * 60000 + sec * 1000 + ms
            val text = match.groupValues[4].trim()
            if (text.isNotBlank()) {
                map[timeMs] = text
            }
        }
        return map
    }

    private fun findTranslation(transMap: Map<Int, String>, timeMs: Int): String {
        if (transMap.isEmpty()) return ""
        return transMap.entries.firstOrNull { Math.abs(it.key - timeMs) <= 500 }?.value.orEmpty()
    }
}
