package com.example.seteasecloudmusic.feature.player.presentation.lyric

import com.example.seteasecloudmusic.feature.player.domain.model.LyricLine
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics

data class FlamingoLyricData(
    val lyrics: List<List<Pair<Float, String>>>,
    val sideFlags: List<Boolean>
)

object LyricDataAdapter {

    private const val EMPTY_LINE_GAP_MS = 4000f

    fun toFlamingoFormat(parsed: ParsedLyrics): FlamingoLyricData {
        val filteredLines = parsed.lines.filter { !it.isBG }
        val lyrics = mutableListOf<List<Pair<Float, String>>>()
        val sideFlags = mutableListOf<Boolean>()

        for (i in filteredLines.indices) {
            val line = filteredLines[i]

            // 检测与前行的时间间隔，插入空行（触发倒计时动画）
            if (i > 0) {
                val prevEnd = filteredLines[i - 1].endTime.toFloat()
                val gap = line.startTime - prevEnd
                if (gap > EMPTY_LINE_GAP_MS) {
                    lyrics.add(emptyFlamingoLine(prevEnd))
                    sideFlags.add(false)
                }
            }

            lyrics.add(toFlamingoLine(line, parsed.hasWordTiming))
            sideFlags.add(line.isDuet)
        }

        return FlamingoLyricData(lyrics, sideFlags)
    }

    private fun toFlamingoLine(line: LyricLine, hasWordTiming: Boolean): List<Pair<Float, String>> {
        val result = mutableListOf<Pair<Float, String>>()

        if (hasWordTiming && line.words.size > 1) {
            line.words.forEach { word ->
                result.add(word.startTime.toFloat() to word.word)
            }
        } else {
            val text = line.words.joinToString("") { it.word }
            result.add(line.startTime.toFloat() to text)
        }

        result.add(0f to line.translatedLyric)

        return result
    }

    private fun emptyFlamingoLine(timeMs: Float): List<Pair<Float, String>> {
        return listOf(timeMs to "", 0f to "")
    }
}
