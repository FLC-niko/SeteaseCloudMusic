package com.example.seteasecloudmusic.feature.player.data

import com.example.seteasecloudmusic.feature.player.data.parser.LrcParser
import com.example.seteasecloudmusic.feature.player.data.parser.TtmlParser
import com.example.seteasecloudmusic.feature.player.data.parser.YrcParser
import com.example.seteasecloudmusic.feature.player.domain.model.LyricSource
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor(
    private val lyricService: LyricService,
    @param:Named("ttmlClient") private val ttmlClient: OkHttpClient
) {

    private val ttmlMirrors = listOf(
        "https://ghproxy.net/https://raw.githubusercontent.com/amll-dev/amll-ttml-db/main/ncm-lyrics/",
        "https://cdn.jsdelivr.net/gh/amll-dev/amll-ttml-db@main/ncm-lyrics/",
        "https://raw.githubusercontent.com/amll-dev/amll-ttml-db/main/ncm-lyrics/"
    )

    suspend fun getLyrics(songId: Long): Result<ParsedLyrics> {
        // 1. 依次尝试 TTML 镜像源（每个 2 秒超时）
        for (baseUrl in ttmlMirrors) {
            val ttmlResult = runCatching {
                withTimeout(2000) { fetchTTML(songId, baseUrl) }
            }.getOrNull()
            if (ttmlResult != null) {
                return Result.success(ttmlResult)
            }
        }

        // 2. 降级到网易云 /lyric/new（YRC 逐字歌词）
        val yrcResult = runCatching {
            val response = lyricService.getYrcLyrics(songId)
            val yrcText = response.yrc?.lyric
            if (!yrcText.isNullOrEmpty()) {
                val lines = YrcParser.parse(yrcText)
                if (lines.isNotEmpty()) {
                    return@runCatching ParsedLyrics(
                        lines = lines,
                        hasWordTiming = true,
                        source = LyricSource.YRC
                    )
                }
            }
            // 如果 yrc 为空或解析失败，尝试 lrc
            val lrcText = response.lrc?.lyric
            if (!lrcText.isNullOrEmpty()) {
                val lines = LrcParser.parse(lrcText)
                if (lines.isNotEmpty()) {
                    return@runCatching ParsedLyrics(
                        lines = lines,
                        hasWordTiming = false,
                        source = LyricSource.LRC
                    )
                }
            }
            throw IllegalStateException("No lyrics found")
        }

        if (yrcResult.isSuccess) {
            return yrcResult
        }

        // 3. 最后降级到 /lyric（LRC）
        return runCatching {
            val response = lyricService.getLyrics(songId)
            val lrcText = response.lrc?.lyric
                ?: response.tlyric?.lyric
                ?: throw IllegalStateException("No lyrics found")
            val lines = LrcParser.parse(lrcText)
            if (lines.isEmpty()) throw IllegalStateException("No lyrics found")
            ParsedLyrics(
                lines = lines,
                hasWordTiming = false,
                source = LyricSource.LRC
            )
        }
    }

    private suspend fun fetchTTML(songId: Long, baseUrl: String): ParsedLyrics? {
        val request = Request.Builder()
            .url("$baseUrl$songId.ttml")
            .build()
        val response = ttmlClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        return TtmlParser.parse(body)
    }
}
