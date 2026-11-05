package com.example.seteasecloudmusic.feature.player.data.parser

import com.example.seteasecloudmusic.feature.player.domain.model.LyricLine
import com.example.seteasecloudmusic.feature.player.domain.model.LyricWord
import kotlin.math.abs

object YrcParser {

    private val LINE_PATTERN = Regex("""^\[(\d+),(\d+)]""")
    private val WORD_PATTERN = Regex("""\((\d+),(\d+),\d+\)""")
    private val BEGIN_PAREN = Regex("""^[（(]+""")
    private val END_PAREN = Regex("""[）)]+$""")

    fun parse(
        yrcText: String,
        tlyricText: String? = null,
        romalrcText: String? = null
    ): List<LyricLine> {
        val transMap = LrcParser.parseTimedTextMap(tlyricText)
        val romanMap = LrcParser.parseTimedTextMap(romalrcText)

        val lines = yrcText
            .split("\r?\n".toRegex())
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("{") }

        val parsed = lines.mapNotNull { lineStr ->
            val lineMatch = LINE_PATTERN.find(lineStr) ?: return@mapNotNull null
            val lineStart = lineMatch.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val lineDuration = lineMatch.groupValues[2].toIntOrNull() ?: 0

            var content = lineStr.substring(lineMatch.range.last + 1)
            if (content.isBlank()) return@mapNotNull null

            val words = mutableListOf<LyricWord>()
            var lastStart = -1
            var lastEnd = -1

            while (content.isNotEmpty()) {
                val wordMatch = WORD_PATTERN.find(content) ?: break
                val prefixText = content.substring(0, wordMatch.range.first)
                val wordStart = wordMatch.groupValues[1].toIntOrNull() ?: lineStart
                val wordDur = wordMatch.groupValues[2].toIntOrNull() ?: 0
                val wordEnd = wordStart + wordDur

                if (prefixText.isNotEmpty() && lastStart != -1) {
                    words.add(
                        LyricWord(
                            word = prefixText,
                            startTime = lastStart,
                            endTime = maxOf(lastStart, lastEnd)
                        )
                    )
                }

                lastStart = wordStart
                lastEnd = wordEnd
                content = content.substring(wordMatch.range.last + 1)
            }

            if (lastStart != -1 && content.isNotEmpty()) {
                words.add(
                    LyricWord(
                        word = content,
                        startTime = lastStart,
                        endTime = maxOf(lastStart, lastEnd)
                    )
                )
            }

            if (words.isEmpty()) return@mapNotNull null

            val isBG = checkIsBG(words)
            if (isBG) trimBGParentheses(words)
            if (words.all { it.word.isBlank() }) return@mapNotNull null

            val endTime = maxOf(lineStart + lineDuration, words.last().endTime)
            val trans = if (isBG) "" else LrcParser.findNearestTimedText(transMap, lineStart)
            val roman = if (isBG) "" else LrcParser.findNearestTimedText(romanMap, lineStart)

            LyricLine(
                startTime = lineStart,
                endTime = endTime,
                words = words.toList(),
                translatedLyric = trans,
                romanLyric = roman,
                isBG = isBG
            )
        }

        // AMLL 规范：首行若被括号包裹不应孤立为和声行，连续多个和声行只保留紧随主句的第一个为 isBG
        return normalizeBackgroundLines(parsed)
    }

    internal fun normalizeBackgroundLines(lines: List<LyricLine>): List<LyricLine> {
        if (lines.isEmpty()) return lines
        val out = ArrayList<LyricLine>(lines.size)
        var prevWasMain = false
        for (line in lines) {
            if (line.isBG) {
                if (prevWasMain) {
                    out.add(line)
                    prevWasMain = false
                } else {
                    out.add(line.copy(isBG = false))
                    prevWasMain = true
                }
            } else {
                out.add(line)
                prevWasMain = true
            }
        }
        return out
    }

    private fun checkIsBG(words: List<LyricWord>): Boolean {
        if (words.isEmpty()) return false
        val first = words.first().word.trimStart()
        val last = words.last().word.trimEnd()
        return BEGIN_PAREN.containsMatchIn(first) && END_PAREN.containsMatchIn(last)
    }

    private fun trimBGParentheses(words: MutableList<LyricWord>) {
        if (words.isEmpty()) return
        val first = words.first()
        words[0] = first.copy(word = first.word.replaceFirst(BEGIN_PAREN, ""))
        val lastIdx = words.lastIndex
        val last = words[lastIdx]
        words[lastIdx] = last.copy(word = last.word.replace(END_PAREN, ""))
    }
}
