package com.example.seteasecloudmusic.core.network

import android.content.Context
import android.util.Log
import com.example.seteasecloudmusic.BuildConfig
import com.example.seteasecloudmusic.core.network.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    fun provideHttpClient(@ApplicationContext context: Context): OkHttpClient {
        /**
         * 随机中国 IP 参数拦截器：
         * 为所有请求统一附加 randomCNIP=true，规避部分环境下的 460 限制。
         */
        val randomCnIpInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            // 官方网易云接口（如 clientlog3.music.163.com）保持纯净请求，不附加 randomCNIP
            if (originalUrl.host.contains("163.com") || originalUrl.host.contains("netease.com")) {
                return@Interceptor chain.proceed(originalRequest)
            }

            // 若调用方已显式传入 randomCNIP，则保持原值不覆盖。
            val request = if (originalUrl.queryParameter("randomCNIP") != null) {
                originalRequest
            } else {
                val updatedUrl = originalUrl.newBuilder()
                    .addQueryParameter("randomCNIP", "true")
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
            if (BuildConfig.DEBUG) {
                Log.d("NetworkModule", "HTTP ${request.method} ${request.url}")
            }
            chain.proceed(request)
        }

        /**
         * 统一错误处理拦截器：
         * 当服务端返回非 2xx 状态码时，直接抛出异常，
         * 让上层可以用统一方式处理请求失败。
         */
        val errorInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (!response.isSuccessful) {
                val message =
                    "HTTP ${response.code} ${response.message.ifBlank { "Unknown error" }}"
                response.close()
                throw IOException(message)
            }
            response
        }

        val smartDns = object : okhttp3.Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> {
                try {
                    val res = okhttp3.Dns.SYSTEM.lookup(hostname)
                    if (res.isNotEmpty()) return res
                } catch (e: Exception) {
                    Log.w("NetworkModule", "System DNS failed for $hostname: ${e.message}")
                }

                if (hostname.contains("clientlog") || hostname.contains("163.com") || hostname.contains("netease")) {
                    try {
                        val fallbackIps = when {
                            hostname.contains("clientlog3") -> listOf(
                                java.net.InetAddress.getByAddress(hostname, byteArrayOf(220.toByte(), 197.toByte(), 30.toByte(), 68.toByte())),
                                java.net.InetAddress.getByAddress(hostname, byteArrayOf(59.toByte(), 111.toByte(), 181.toByte(), 60.toByte())),
                                java.net.InetAddress.getByAddress(hostname, byteArrayOf(59.toByte(), 111.toByte(), 181.toByte(), 38.toByte()))
                            )
                            hostname.contains("clientlog") -> listOf(
                                java.net.InetAddress.getByAddress(hostname, byteArrayOf(220.toByte(), 197.toByte(), 30.toByte(), 68.toByte())),
                                java.net.InetAddress.getByAddress(hostname, byteArrayOf(59.toByte(), 111.toByte(), 181.toByte(), 60.toByte()))
                            )
                            else -> emptyList()
                        }
                        if (fallbackIps.isNotEmpty()) {
                            Log.d("NetworkModule", "Using SmartDNS fallback for $hostname -> ${fallbackIps.map { it.hostAddress }}")
                            return fallbackIps
                        }
                    } catch (e: Exception) {
                        Log.e("NetworkModule", "Fallback DNS resolution failed for $hostname", e)
                    }
                }
                return okhttp3.Dns.SYSTEM.lookup(hostname)
            }
        }

        return OkHttpClient.Builder()
            .dns(smartDns)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            // 优先补充 randomCNIP 参数，确保后续日志能打印最终 URL。
            .addInterceptor(randomCnIpInterceptor)
            // 先补公共请求头，确保后续拦截器拿到的是完整请求。
            .addInterceptor(commonHeadersInterceptor)
            // 自动管理 Cookie（登录态）。
            .addInterceptor(AuthInterceptor(context))
            // 调试阶段输出请求信息，便于定位网络问题。
            .addInterceptor(loggingInterceptor)
            // 最后统一兜底处理服务端错误响应。
            .addInterceptor(errorInterceptor)
            .build()
    }

    /**
     * 构建 Retrofit 实例并挂载 Gson 转换器。
     */
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(provideBaseUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

}