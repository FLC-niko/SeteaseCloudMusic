package com.example.seteasecloudmusic.core.network

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

/**
 * 当主节点 [primaryBaseUrl] 发生网络 I/O 异常或返回 5xx 时，自动将请求切换至备用节点 [backupBaseUrl]，
 * 并在 [cooldownMs] 冷却期内保持走备用节点，冷却到期后重新探测主节点。
 */
internal class ApiEndpointFailoverInterceptor(
    primaryBaseUrl: String,
    backupBaseUrl: String,
    private val cooldownMs: Long = DEFAULT_FAILOVER_COOLDOWN_MS, // 主节点故障后的备用线路冷却时长（ms）
    private val clockMs: () -> Long = System::currentTimeMillis,
    private val logWarning: (String) -> Unit = { message -> Log.w(TAG, message) }
) : Interceptor {

    private val primaryUrl: HttpUrl? = primaryBaseUrl.trim().takeIf { it.isNotEmpty() }?.toHttpUrlOrNull()
    private val backupUrl: HttpUrl? = backupBaseUrl.trim().takeIf { it.isNotEmpty() }?.toHttpUrlOrNull()
    private val failoverUntilMs = AtomicLong(0L)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val primary = primaryUrl
        val backup = backupUrl
        if (primary == null || backup == null || !matchesPrimaryEndpoint(originalRequest.url, primary) || sameEndpoint(primary, backup)) {
            return chain.proceed(originalRequest)
        }

        val now = clockMs()
        if (now < failoverUntilMs.get()) {
            val backupRequest = rewriteRequest(originalRequest, primary, backup)
            return try {
                chain.proceed(backupRequest)
            } catch (backupError: IOException) {
                // 备用节点也异常时立即清零冷却期，回探主节点
                failoverUntilMs.set(0L)
                logWarning("Backup line ${backup.host}:${backup.port} failed (${backupError.message}), probing primary ${primary.host}:${primary.port}")
                chain.proceed(originalRequest)
            }
        }

        val primaryResponse = try {
            chain.proceed(originalRequest)
        } catch (primaryError: IOException) {
            failoverUntilMs.set(clockMs() + cooldownMs)
            logWarning(
                "Primary line ${primary.host}:${primary.port} failed (${primaryError::class.java.simpleName}: ${primaryError.message}), switching to backup ${backup.host}:${backup.port}"
            )
            return chain.proceed(rewriteRequest(originalRequest, primary, backup))
        }

        if (primaryResponse.code in 500..599) {
            // 主节点返回 5xx 时关闭原响应并切换至备用节点
            val statusCode = primaryResponse.code
            primaryResponse.close()
            failoverUntilMs.set(clockMs() + cooldownMs)
            logWarning(
                "Primary line ${primary.host}:${primary.port} returned HTTP $statusCode, switching to backup ${backup.host}:${backup.port}"
            )
            return chain.proceed(rewriteRequest(originalRequest, primary, backup))
        }

        return primaryResponse
    }

    private fun matchesPrimaryEndpoint(url: HttpUrl, primary: HttpUrl): Boolean {
        return url.host.equals(primary.host, ignoreCase = true) && url.port == primary.port
    }

    private fun sameEndpoint(first: HttpUrl, second: HttpUrl): Boolean {
        return first.scheme.equals(second.scheme, ignoreCase = true) &&
            first.host.equals(second.host, ignoreCase = true) &&
            first.port == second.port &&
            first.encodedPath.trimEnd('/') == second.encodedPath.trimEnd('/')
    }

    private fun rewriteRequest(request: Request, fromBase: HttpUrl, toBase: HttpUrl): Request {
        val rewrittenUrl = rewriteUrl(request.url, fromBase, toBase)
        return request.newBuilder().url(rewrittenUrl).build()
    }

    private fun rewriteUrl(url: HttpUrl, fromBase: HttpUrl, toBase: HttpUrl): HttpUrl {
        val fromPrefix = fromBase.encodedPath.trimEnd('/')
        val toPrefix = toBase.encodedPath.trimEnd('/')
        if (fromPrefix.isEmpty() && toPrefix.isEmpty()) {
            return url.newBuilder()
                .scheme(toBase.scheme)
                .host(toBase.host)
                .port(toBase.port)
                .build()
        }

        val origPath = url.encodedPath
        val suffixPath = if (fromPrefix.isNotEmpty() && (origPath == fromPrefix || origPath.startsWith("$fromPrefix/"))) {
            origPath.removePrefix(fromPrefix).ifEmpty { "/" }
        } else {
            origPath
        }
        val mergedPath = if (toPrefix.isEmpty()) {
            suffixPath
        } else if (suffixPath == "/") {
            "$toPrefix/"
        } else {
            toPrefix + suffixPath
        }

        return url.newBuilder()
            .scheme(toBase.scheme)
            .host(toBase.host)
            .port(toBase.port)
            .encodedPath(mergedPath)
            .build()
    }

    companion object {
        // 默认主节点故障冷却时长（ms，超时后才会再次尝试主节点）
        const val DEFAULT_FAILOVER_COOLDOWN_MS = 60_000L
        private const val TAG = "NetworkModule"
    }
}
