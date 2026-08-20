package com.example.seteasecloudmusic.core.network

import android.content.Context
import android.util.Log
import com.example.seteasecloudmusic.BuildConfig
import com.example.seteasecloudmusic.feature.artist.data.ArtistService
import com.example.seteasecloudmusic.core.network.interceptor.AuthInterceptor
import com.example.seteasecloudmusic.feature.auth.data.AuthService
import com.example.seteasecloudmusic.feature.discover.data.DiscoverService
import com.example.seteasecloudmusic.feature.home.data.DailyRecommendService
import com.example.seteasecloudmusic.feature.search.data.NeteaseMusicService
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
 * 3. 创建 Retrofit 并产出 `NeteaseMusicService`。
 */
@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    /**
     * API 基础地址。
     */
    @Provides
    @Singleton
    fun provideBaseUrl(): String = "http://57.158.26.135:3000/";

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

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
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

    /**
     * 暴露音乐 API 服务实例。
     */
    @Provides
    @Singleton
    fun provideNeteaseMusicService(retrofit: Retrofit): NeteaseMusicService =
        retrofit.create(NeteaseMusicService::class.java)

    /**
     * Expose artist API service.
     */
    @Provides
    @Singleton
    fun provideArtistService(retrofit: Retrofit): ArtistService =
        retrofit.create(ArtistService::class.java)

    /**
     * 暴露认证 API 服务实例。
     */
    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService =
        retrofit.create(AuthService::class.java)

    /**
     * 暴露主页每日推荐 API 服务实例。
     */
    @Provides
    @Singleton
    fun provideDailyRecommendService(retrofit: Retrofit): DailyRecommendService =
        retrofit.create(DailyRecommendService::class.java)

    /**
     * 暴露发现页 API 服务实例。
     */
    @Provides
    @Singleton
    fun provideDiscoverService(retrofit: Retrofit): DiscoverService =
        retrofit.create(DiscoverService::class.java)

    //改成依赖注入的方式后，提供 NeteaseMusicService 的方法可以直接注入 Retrofit 实例，无需手动调用 provideHttpClient 和 provideRetrofit。

//    fun provideNeteaseMusicService(): NeteaseMusicService {
//        val client: OkHttpClient = provideHttpClient()
//        val retrofit: Retrofit = provideRetrofit(client)
//        return retrofit.create(NeteaseMusicService::class.java)
//    }
}