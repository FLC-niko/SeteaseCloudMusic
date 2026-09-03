package com.example.seteasecloudmusic.feature.player.data

import com.example.seteasecloudmusic.feature.player.data.parser.LrcParser
import com.example.seteasecloudmusic.feature.player.data.parser.TtmlParser
import com.example.seteasecloudmusic.feature.player.data.parser.YrcParser
import com.example.seteasecloudmusic.feature.player.domain.model.LyricSource
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import com.example.seteasecloudmusic.feature.player.domain.repository.ParsedLyricsRepository
import com.example.seteasecloudmusic.core.common.runCatchingCancellable
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor(
    private val lyricService: LyricService,
    @param:Named("ttmlClient") private val ttmlClient: OkHttpClient
) : ParsedLyricsRepository {

    private val ttmlMirrors = listOf(
        "https://ghproxy.net/https://raw.githubusercontent.com/amll-dev/amll-ttml-db/main/ncm-lyrics/",
        "https://cdn.jsdelivr.net/gh/amll-dev/amll-ttml-db@main/ncm-lyrics/",
        "https://raw.githubusercontent.com/amll-dev/amll-ttml-db/main/ncm-lyrics/"
    )

    override suspend fun getLyrics(songId: Long): Result<ParsedLyrics> {
        // 1. 依次尝试 TTML 镜像源（每个 2 秒超时）
        for (baseUrl in ttmlMirrors) {
            val ttmlResult = runCatchingCancellable {
                // withTimeoutOrNull 只把该镜像自己的超时转换成 null；外层取消仍会继续向上传播。
                withTimeoutOrNull(2000) { fetchTTML(songId, baseUrl) }
            }.getOrNull()
            if (ttmlResult != null) {
                return Result.success(ttmlResult)
            }
        }

        // 2. 降级到网易云 /lyric/new（YRC 逐字歌词）
        val yrcResult = runCatchingCancellable {
            val response = lyricService.getYrcLyrics(songId)
            val yrcText = response.yrc?.lyric
            if (!yrcText.isNullOrEmpty()) {
                val lines = YrcParser.parse(yrcText)
                if (lines.isNotEmpty()) {
                    return@runCatchingCancellable ParsedLyrics(
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
                    return@runCatchingCancellable ParsedLyrics(
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
        return runCatchingCancellable {
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

        return suspendCancellableCoroutine { continuation ->
            val call = ttmlClient.newCall(request)
            continuation.invokeOnCancellation {
                // 页面/播放器切换时，立即取消 OkHttp 请求，而不是只取消等待它的协程。
                call.cancel()
            }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(e))
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val parsed = response.use {
                            if (!it.isSuccessful) {
                                null
                            } else {
                                it.body?.string()?.let(TtmlParser::parse)
                            }
                        }
                        if (continuation.isActive) {
                            continuation.resume(parsed)
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.failure(e))
                        }
                    }
                }
            })
        }
    }
}
