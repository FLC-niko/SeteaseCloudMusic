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
 * 全局网络依赖注入模块（Hilt SingletonComponent）：
 * 1. 提供 API 基础地址 [provideBaseUrl]。
 * 2. 组装主 [OkHttpClient] 与本地直连上游客户端（含并发 Dispatcher、SmartDNS、认证 Cookie、主备故障切换与错误解析）。
 * 3. 构建挂载 Gson 转换器的 [Retrofit] 实例。
 */
@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    /**
     * 提供默认 API 基础地址。
     */
    @Provides
    @Singleton
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    /**
     * 提供主网络请求使用的 [OkHttpClient]，统一挂载本地直连、主备切换、Cookie 认证与超时策略。
     */
    @Provides
    @Singleton
    fun provideHttpClient(
        @ApplicationContext context: Context,
        settingsManager: PlayerSettingsManager,
        networkMonitor: NetworkMonitor
    ): OkHttpClient {
        // 外部公网 IP 探测专用客户端：使用较短超时（2 秒）避免阻塞常规接口
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
         * - 本地直连模式（默认）走设备真实出口 IP，不附带伪造 IP，也不触发外部查 IP 请求。
         * - 仅当关闭本地直连、回退海外代理服务器时，才附带测得的国内真实公网 IP
         *   （若开启海外代理或探测失败，自动兜底为本次会话固定的国内运营商 IP）。
         */
        val proxyRealIpInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            // 官方网易云接口（如 clientlog3.music.163.com）保持原始请求参数
            if (originalUrl.host.contains("163.com") || originalUrl.host.contains("netease.com")) {
                return@Interceptor chain.proceed(originalRequest)
            }

            // 本地直连模式由设备直接连接网易云官方，不附带 randomCNIP / realIP
            if (settingsManager.localDirectApiEnabled.value) {
                return@Interceptor chain.proceed(originalRequest)
            }

            // 若调用方已显式传入 realIP 或 randomCNIP，则保持原值不覆盖
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

        // 统一补充 Accept: application/json 请求头
        val commonHeadersInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }

        // 调试日志拦截器：仅在 DEBUG 模式下输出脱敏后的请求方法、URL、状态码与耗时
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

        // 非 2xx 响应拦截器：预览错误体并解析为 ApiHttpException 抛出
        val errorInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (!response.isSuccessful) {
                // 最多预览 8KB 错误响应体用于解析业务错误码
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
                // 系统 DNS 优先；失败时按域名规则回退网易云固定节点
                SmartDns.systemLookupOrNull(hostname, "NetworkModule")?.let { return it }

                if (hostname.contains("clientlog") || hostname.contains("163.com") || hostname.contains("netease")) {
                    // clientlog3 使用全部兜底节点，其余 clientlog 域名使用前 2 个节点
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

        // 本地直连上游请求客户端（连接 10s / 读写 15s）
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
            .connectTimeout(10, TimeUnit.SECONDS) // 主客户端连接超时（秒）
            .readTimeout(15, TimeUnit.SECONDS)    // 主客户端读取超时（秒）
            .writeTimeout(15, TimeUnit.SECONDS)   // 主客户端写入超时（秒）
            // 仅在外部代理模式下附带稳定的国内真实 IP；本地直连保持设备真实网络出口
            .addInterceptor(proxyRealIpInterceptor)
            // 先补公共请求头，确保后续拦截器拿到的是完整请求
            .addInterceptor(commonHeadersInterceptor)
            // 自动管理 Cookie（登录态）
            .addInterceptor(AuthInterceptor(context))
            // 统一处理非 2xx 响应，位于本地协议与故障切换之外以便统一解析业务错误码
            .addInterceptor(errorInterceptor)
            // 默认在应用内本地加密直连网易云官方；关闭时回退外部主备节点
            .addInterceptor(localNeteaseApiInterceptor)
            // 主节点超时/网络异常或返回 5xx 时自动切换至备用服务器
            .addInterceptor(
                ApiEndpointFailoverInterceptor(
                    primaryBaseUrl = BuildConfig.BASE_URL,
                    backupBaseUrl = BuildConfig.BACKUP_BASE_URL
                )
            )
            // 调试阶段输出最终发往节点的请求 URL、状态码与耗时
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
        private const val API_MAX_REQUESTS = 64          // OkHttp 全局最大并发请求数
        private const val API_MAX_REQUESTS_PER_HOST = 16 // 单主机最大并发请求数（默认 5，调高避免首屏批量请求阻塞起播）

        /**
         * 构造提高单主机并发上限的 [Dispatcher]，避免冷启动首屏批量接口占满连接队列导致 `/song/url/v1` 等起播请求排队延迟。
         */
        internal fun createApiDispatcher(): Dispatcher = Dispatcher().apply {
            maxRequests = API_MAX_REQUESTS
            maxRequestsPerHost = API_MAX_REQUESTS_PER_HOST
        }
    }

}
