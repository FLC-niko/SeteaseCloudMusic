package com.example.seteasecloudmusic.core.network.local

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal data class UpstreamHttpRequest(
    val url: String,
    val headers: Map<String, String>,
    val formFields: Map<String, String>
)

internal data class UpstreamHttpResponse(
    val httpCode: Int,
    val bodyText: String,
    val setCookies: List<String> = emptyList()
)

internal interface NeteaseUpstreamTransport {
    fun postForm(request: UpstreamHttpRequest): UpstreamHttpResponse
}

/**
 * 通过独立 [OkHttpClient] 向网易云官方域名（`music.163.com` / `interface.music.163.com`）发送加密表单请求，
 * 避免复用 Retrofit 客户端拦截器链引发递归拦截。
 */
internal class OkHttpNeteaseUpstreamTransport(
    private val client: OkHttpClient
) : NeteaseUpstreamTransport {

    override fun postForm(request: UpstreamHttpRequest): UpstreamHttpResponse {
        val formBuilder = FormBody.Builder(Charsets.UTF_8)
        for ((key, value) in request.formFields) {
            formBuilder.add(key, value)
        }

        val builder = Request.Builder()
            .url(request.url)
            .post(formBuilder.build())
        for ((name, value) in request.headers) {
            builder.header(name, value)
        }

        client.newCall(builder.build()).execute().use { response ->
            val bodyText = response.body?.string() ?: ""
            val setCookies = response.headers.values("Set-Cookie")
            return UpstreamHttpResponse(
                httpCode = response.code,
                bodyText = bodyText,
                setCookies = setCookies
            )
        }
    }
}

internal class UpstreamTransportException(message: String, cause: Throwable? = null) : IOException(message, cause)
