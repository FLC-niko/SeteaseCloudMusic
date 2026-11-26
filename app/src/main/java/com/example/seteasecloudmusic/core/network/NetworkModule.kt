package com.example.seteasecloudmusic.core.network

import android.content.Context
import android.util.Log
import com.example.seteasecloudmusic.BuildConfig
import com.example.seteasecloudmusic.core.network.interceptor.AuthInterceptor
import com.example.seteasecloudmusic.core.network.local.LocalNeteaseApiInterceptor
import com.example.seteasecloudmusic.core.network.local.LocalNeteaseClient
import com.example.seteasecloudmusic.core.network.local.NeteaseEndpointRegistry
import com.example.seteasecloudmusic.core.network.local.OkHttpNeteaseUpstreamTransport
import com.example.seteasecloudmusic.core.network.local.SharedPreferencesNeteaseTokenStore
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * `di` 模块说明：
 *
 * 这一层负责“组装依赖”，也就是把网络、仓库、用例等对象按需要创建出来。
 * 当前文件先手动承担了简单依赖注入的职责，后续如果接入 Hilt/Koin，
 * 这里的思路仍然一样，只是写法会更框架化。
 *
 * `NetworkModule` 当前主要负责：
 * 1. 定义服务端基础地址。
 * 2. 配置 OkHttp 超时等网络参数。
 * 3. 创建 Retrofit，业务 Service 由应用级 DI 模块按 feature 提供。
 */
@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    /**
     * API 基础地址。
     */
    @Provides
    @Singleton
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    /**
     * 提供统一超时配置的 HTTP 客户端。
     */
    @Provides
    @Singleton
    fun provideHttpClient(
        @ApplicationContext context: Context,
        settingsManager: PlayerSettingsManager,
        networkMonitor: NetworkMonitor
    ): OkHttpClient {
        val ipProbeClient = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()

        val publicIpResolver = PublicIpResolver(httpClient = ipProbeClient).also {
            it.observeNetworkChanges(networkMonitor)
        }

        /**
         * 代理线路真实 IP 附带拦截器：
         * - 本地直连模式（默认）走设备真实出口 IP，不附带伪造 IP，也不触发任何外部查 IP 请求。
         * - 仅当用户手动关闭本地直连、回退海外代理服务器时，才附带测得的国内真实公网 IP
         *   （若开启海外代理或探测失败，自动兜底为本次会话固定的国内运营商 IP）。
         */
        val proxyRealIpInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            // 官方网易云接口（如 clientlog3.music.163.com）保持纯净请求
            if (originalUrl.host.contains("163.com") || originalUrl.host.contains("netease.com")) {
                return@Interceptor chain.proceed(originalRequest)
            }

            // 本地直连模式直接由手机本机连接网易云官方，严禁伪造 randomCNIP / X-Real-IP
            if (settingsManager.localDirectApiEnabled.value) {
                return@Interceptor chain.proceed(originalRequest)
            }

            // 若调用方已显式传入 realIP 或 randomCNIP，则保持原值不覆盖。
            val request = if (
                originalUrl.queryParameter("realIP") != null ||
                originalUrl.queryParameter("randomCNIP") != null
            ) {
                originalRequest
            } else {
                val path = originalUrl.encodedPath.lowercase()
                val isAuthPath = path.contains("login") || path.contains("captcha")
                val resolvedIp = publicIpResolver.resolveProxyClientIp(allowBriefWait = isAuthPath)
                val updatedUrl = originalUrl.newBuilder()
                    .addQueryParameter("realIP", resolvedIp)
                    .build()

                originalRequest.newBuilder()
                    .url(updatedUrl)
                    .build()
            }

            chain.proceed(request)
        }

        /**
         * 通用请求头拦截器：
         * 给所有请求统一补充基础 Header，避免每个接口重复写。
         */
        val commonHeadersInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }

        /**
         * 日志拦截器：
         * 仅在调试环境下打印请求方法和 URL，方便排查接口调用问题。
         */
        val loggingInterceptor = Interceptor { chain ->
            val request = chain.request()
            val safeUrl = if (BuildConfig.DEBUG) CookieSanitizer.sanitizeUrlForLog(request.url) else ""
            if (BuildConfig.DEBUG) {
                Log.d("NetworkModule", "HTTP --> ${request.method} $safeUrl")
            }
            val startNs = System.nanoTime()
            try {
                val response = chain.proceed(request)
                if (BuildConfig.DEBUG) {
                    val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
                    Log.d("NetworkModule", "HTTP <-- ${response.code} ${request.method} $safeUrl (${elapsedMs}ms)")
                }
                response
            } catch (t: Throwable) {
                if (BuildConfig.DEBUG) {
                    val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
                    Log.w(
                        "NetworkModule",
                        "HTTP ERR ${request.method} $safeUrl (${elapsedMs}ms): ${t::class.java.simpleName}: ${t.message}"
                    )
                }
                throw t
            }
        }

        /**
         * 统一错误处理拦截器：
         * 当服务端返回非 2xx 状态码时，直接抛出异常，
         * 让上层可以用统一方式处理请求失败。
         */
        val errorInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (!response.isSuccessful) {
                val rawBody = runCatching { response.peekBody(8192L).string() }.getOrNull()
                val exception = HttpErrorParser.parse(
                    httpStatusCode = response.code,
                    httpMessage = response.message,
                    responseBody = rawBody
                )
                if (BuildConfig.DEBUG) {
                    Log.w("NetworkModule", "HTTP error response: ${exception.message}")
                }
                response.close()
                throw exception
            }
            response
        }

        val smartDns = object : okhttp3.Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> {
                // 系统 DNS 优先；失败时再按域名走网易云固定节点兜底
                SmartDns.systemLookupOrNull(hostname, "NetworkModule")?.let { return it }

                if (hostname.contains("clientlog") || hostname.contains("163.com") || hostname.contains("netease")) {
                    val fallbackCount = when {
                        hostname.contains("clientlog3") -> SmartDns.fallbackNodeCount
                        hostname.contains("clientlog") -> 2
                        else -> 0
                    }
                    if (fallbackCount > 0) {
                        val fallbackIps = SmartDns.addressesFor(hostname, fallbackCount)
                        Log.d(
                            "NetworkModule",
                            "Using SmartDNS fallback for $hostname -> ${fallbackIps.map { it.hostAddress }}"
                        )
                        return fallbackIps
                    }
                }
                return okhttp3.Dns.SYSTEM.lookup(hostname)
            }
        }

        val apiDispatcher = createApiDispatcher()

        val upstreamLoggingInterceptor = Interceptor { chain ->
            val req = chain.request()
            if (BuildConfig.DEBUG) {
                Log.d("NetworkModule", "LOCAL-UPSTREAM --> ${req.method} ${req.url}")
            }
            val startNs = System.nanoTime()
            try {
                val res = chain.proceed(req)
                if (BuildConfig.DEBUG) {
                    val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
                    Log.d("NetworkModule", "LOCAL-UPSTREAM <-- ${res.code} ${req.method} ${req.url} (${elapsedMs}ms)")
                }
                res
            } catch (t: Throwable) {
                if (BuildConfig.DEBUG) {
                    val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
                    Log.w(
                        "NetworkModule",
                        "LOCAL-UPSTREAM ERR ${req.method} ${req.url} (${elapsedMs}ms): ${t::class.java.simpleName}: ${t.message}"
                    )
                }
                throw t
            }
        }

        val upstreamClient = OkHttpClient.Builder()
            .dispatcher(apiDispatcher)
            .dns(smartDns)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(upstreamLoggingInterceptor)
            .build()

        val localNeteaseApiInterceptor = LocalNeteaseApiInterceptor(
            primaryBaseUrl = BuildConfig.BASE_URL,
            backupBaseUrl = BuildConfig.BACKUP_BASE_URL,
            registry = NeteaseEndpointRegistry(
                client = LocalNeteaseClient(
                    transport = OkHttpNeteaseUpstreamTransport(upstreamClient),
                    tokenStore = SharedPreferencesNeteaseTokenStore.getInstance(context)
                )
            ),
            isLocalModeEnabled = { settingsManager.localDirectApiEnabled.value },
            logDebug = { msg ->
                if (BuildConfig.DEBUG) {
                    Log.d("NetworkModule", msg)
                }
            }
        )

        return OkHttpClient.Builder()
            .dispatcher(apiDispatcher)
            .dns(smartDns)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            // 仅在外部代理模式下附带稳定的国内真实 IP；本地直连保持设备真实网络出口。
            .addInterceptor(proxyRealIpInterceptor)
            // 先补公共请求头，确保后续拦截器拿到的是完整请求。
            .addInterceptor(commonHeadersInterceptor)
            // 自动管理 Cookie（登录态）。
            .addInterceptor(AuthInterceptor(context))
            // 统一兜底处理非 2xx 响应，位于本地协议与故障切换之外以便统一处理业务错误码。
            .addInterceptor(errorInterceptor)
            // 默认在应用内本地加密直连网易云官方，不经外部代理服务器；关闭时自动回退主备节点。
            .addInterceptor(localNeteaseApiInterceptor)
            // 主节点超时/网络异常或返回 5xx 时自动切换至备用服务器。
            .addInterceptor(
                ApiEndpointFailoverInterceptor(
                    primaryBaseUrl = BuildConfig.BASE_URL,
                    backupBaseUrl = BuildConfig.BACKUP_BASE_URL
                )
            )
            // 调试阶段输出最终发往节点的请求 URL、状态码与耗时，便于对比主备线路表现。
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * 构建 Retrofit 实例并挂载 Gson 转换器。
     */
    @Provides
    @Singleton
    fun provideRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    companion object {
        private const val API_MAX_REQUESTS = 64
        private const val API_MAX_REQUESTS_PER_HOST = 16

        /**
         * 提高单主机并发请求上限（OkHttp 默认为 5），
         * 避免冷启动首屏批量接口占满连接队列导致 `/song/url/v1` 等关键起播请求被延迟数秒。
         */
        internal fun createApiDispatcher(): Dispatcher = Dispatcher().apply {
            maxRequests = API_MAX_REQUESTS
            maxRequestsPerHost = API_MAX_REQUESTS_PER_HOST
        }
    }

}
