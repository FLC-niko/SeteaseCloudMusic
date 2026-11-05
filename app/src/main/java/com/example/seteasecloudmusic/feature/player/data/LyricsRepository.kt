package com.example.seteasecloudmusic.feature.player.data

import android.util.LruCache
import com.example.seteasecloudmusic.core.common.runCatchingCancellable
import com.example.seteasecloudmusic.feature.player.data.parser.LrcParser
import com.example.seteasecloudmusic.feature.player.data.parser.TtmlParser
import com.example.seteasecloudmusic.feature.player.data.parser.YrcParser
import com.example.seteasecloudmusic.feature.player.domain.model.LyricSource
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import com.example.seteasecloudmusic.feature.player.domain.repository.ParsedLyricsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor(
    private val lyricService: LyricService,
    @param:Named("ttmlClient") private val ttmlClient: OkHttpClient
) : ParsedLyricsRepository {

    private val ttmlMirrors = listOf(
        "https://amlldb.bikonoo.com/ncm-lyrics/",
        "https://cdn.jsdelivr.net/gh/amll-dev/amll-ttml-db@main/ncm-lyrics/",
        "https://cdn.jsdelivr.net/gh/Steve-xmh/amll-ttml-db@main/ncm-lyrics/",
        "https://ghproxy.net/https://raw.githubusercontent.com/amll-dev/amll-ttml-db/main/ncm-lyrics/",
        "https://raw.githubusercontent.com/amll-dev/amll-ttml-db/main/ncm-lyrics/"
    )

    private val cache = LruCache<Long, ParsedLyrics>(64)
    private val inFlightMutex = Mutex()
    private val inFlightRequests = mutableMapOf<Long, Deferred<Result<ParsedLyrics>>>()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun getLyrics(songId: Long): Result<ParsedLyrics> {
        if (songId <= 0L) {
            return Result.failure(IllegalArgumentException("Local or invalid songId: $songId"))
        }

        synchronized(cache) {
            cache.get(songId)
        }?.let { return Result.success(it) }

        val deferred = inFlightMutex.withLock {
            synchronized(cache) {
                cache.get(songId)
            }?.let { return Result.success(it) }

            inFlightRequests[songId]?.takeIf { it.isActive } ?: repoScope.async {
                try {
                    loadLyricsUncached(songId).also { result ->
                        result.getOrNull()?.let { parsed ->
                            if (parsed.lines.isNotEmpty()) {
                                synchronized(cache) {
                                    cache.put(songId, parsed)
                                }
                            }
                        }
                    }
                } finally {
                    inFlightMutex.withLock {
                        inFlightRequests.remove(songId)
                    }
                }
            }.also { inFlightRequests[songId] = it }
        }

        return deferred.await()
    }

    private suspend fun loadLyricsUncached(songId: Long): Result<ParsedLyrics> = withContext(Dispatchers.IO) {
        // 1. 最高优先级：并发竞速请求 AMLL TTML 逐字歌词库（任一镜像率先命中即取消其余请求；全部 404 时立即秒回降级）
        val ttmlLyrics = withTimeoutOrNull(2400L) {
            raceTtmlMirrors(songId)
        }

        if (ttmlLyrics != null && ttmlLyrics.lines.isNotEmpty()) {
            // 若 TTML 未内嵌翻译，尝试轻量拉取网易云翻译（1.2s 超时）按时间戳补齐翻译与罗马音，绝不阻塞或影响原逐字轴
            val enriched = if (ttmlLyrics.lines.none { it.translatedLyric.isNotBlank() }) {
                enrichTtmlTranslationsIfAvailable(songId, ttmlLyrics)
            } else {
                ttmlLyrics
            }
            return@withContext Result.success(enriched)
        }

        // 2. 降级到网易云 /lyric/new（YRC 逐字歌词 + tlyric 翻译 + romalrc 音译）
        val yrcResult = runCatchingCancellable {
            val response = lyricService.getYrcLyrics(songId)
            val tlyric = response.tlyric?.lyric
            val romalrc = response.romalrc?.lyric

            val yrcText = response.yrc?.lyric
            if (!yrcText.isNullOrBlank()) {
                val lines = YrcParser.parse(yrcText, tlyricText = tlyric, romalrcText = romalrc)
                if (lines.isNotEmpty()) {
                    return@runCatchingCancellable ParsedLyrics(
                        lines = lines,
                        hasWordTiming = true,
                        source = LyricSource.YRC
                    )
                }
            }

            val lrcText = response.lrc?.lyric
            if (!lrcText.isNullOrBlank()) {
                val lines = LrcParser.parse(lrcText, tlyricText = tlyric, romalrcText = romalrc)
                if (lines.isNotEmpty()) {
                    return@runCatchingCancellable ParsedLyrics(
                        lines = lines,
                        hasWordTiming = false,
                        source = LyricSource.LRC
                    )
                }
            }
            throw IllegalStateException("No lyrics found in /lyric/new")
        }

        if (yrcResult.isSuccess) {
            return@withContext yrcResult
        }

        // 3. 最终兜底：网易云 /lyric（LRC + tlyric + romalrc）
        runCatchingCancellable {
            val response = lyricService.getLyrics(songId)
            val tlyric = response.tlyric?.lyric
            val romalrc = response.romalrc?.lyric
            val lrcText = response.lrc?.lyric
                ?: tlyric
                ?: throw IllegalStateException("No lyrics found")
            val lines = LrcParser.parse(
                lrcText = lrcText,
                tlyricText = if (response.lrc?.lyric != null) tlyric else null,
                romalrcText = romalrc
            )
            if (lines.isEmpty()) throw IllegalStateException("No lyrics found")
            ParsedLyrics(
                lines = lines,
                hasWordTiming = false,
                source = LyricSource.LRC
            )
        }
    }

    private suspend fun raceTtmlMirrors(songId: Long): ParsedLyrics? = coroutineScope {
        val winner = CompletableDeferred<ParsedLyrics?>()
        val remaining = AtomicInteger(ttmlMirrors.size)

        val scopeJob = coroutineContext
        for (baseUrl in ttmlMirrors) {
            launch {
                val parsed = runCatchingCancellable {
                    fetchTTML(songId, baseUrl)
                }.getOrNull()

                if (parsed != null && parsed.lines.isNotEmpty()) {
                    if (winner.complete(parsed)) {
                        scopeJob.cancelChildren()
                    }
                } else if (remaining.decrementAndGet() == 0) {
                    winner.complete(null)
                }
            }
        }

        winner.await()
    }

    private suspend fun enrichTtmlTranslationsIfAvailable(
        songId: Long,
        ttml: ParsedLyrics
    ): ParsedLyrics {
        val sidecar = withTimeoutOrNull(1200L) {
            runCatchingCancellable { lyricService.getYrcLyrics(songId) }.getOrNull()
        } ?: return ttml

        val transMap = LrcParser.parseTimedTextMap(sidecar.tlyric?.lyric)
        val romanMap = LrcParser.parseTimedTextMap(sidecar.romalrc?.lyric)
        if (transMap.isEmpty() && romanMap.isEmpty()) return ttml

        val merged = ttml.lines.map { line ->
            if (line.isBG) {
                line
            } else {
                val trans = line.translatedLyric.ifBlank {
                    LrcParser.findNearestTimedText(transMap, line.startTime, toleranceMs = 900)
                }
                val roman = line.romanLyric.ifBlank {
                    LrcParser.findNearestTimedText(romanMap, line.startTime, toleranceMs = 900)
                }
                if (trans != line.translatedLyric || roman != line.romanLyric) {
                    line.copy(translatedLyric = trans, romanLyric = roman)
                } else {
                    line
                }
            }
        }
        return ttml.copy(lines = merged)
    }

    private suspend fun fetchTTML(songId: Long, baseUrl: String): ParsedLyrics? {
        val request = Request.Builder()
            .url("$baseUrl$songId.ttml")
            .build()

        return suspendCancellableCoroutine { continuation ->
            val call = ttmlClient.newCall(request)
            continuation.invokeOnCancellation {
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
                        val parsed = response.use { resp ->
                            if (!resp.isSuccessful) {
                                null
                            } else {
                                val raw = resp.body?.string()
                                if (raw.isNullOrBlank() || !raw.contains("<tt")) {
                                    null
                                } else {
                                    TtmlParser.parse(raw).takeIf { it.lines.isNotEmpty() }
                                }
                            }
                        }
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.success(parsed))
                        }
                    } catch (t: Throwable) {
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.failure(t))
                        }
                    }
                }
            })
        }
    }
}
