package com.example.seteasecloudmusic.core.network

import com.example.seteasecloudmusic.core.network.local.NeteaseRequestContextBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 在内存中探测并缓存设备当前的中国大陆真实公网 IPv4 地址，
 * 仅在手动关闭本地直连、使用外部代理服务器模式（`localDirectApiEnabled == false`）时生效。
 *
 * - 探测到的 IP 仅驻留内存，绝不写入磁盘或输出日志。
 * - 网络切换（如 Wi-Fi 与移动数据互切）时自动失效缓存并刷新。
 * - 若设备当前处于海外代理/VPN 环境或探测超时失败，自动兜底为本次会话内固定的国内运营商 IP。
 */
internal class PublicIpResolver(
    private val httpClient: OkHttpClient,
    private val fallbackIpProvider: () -> String = { NeteaseRequestContextBuilder.sessionChineseIp() },
    private val endpoints: List<IpProbeEndpoint> = DEFAULT_ENDPOINTS,
    private val clockMs: () -> Long = System::currentTimeMillis
) {
    internal data class IpProbeResult(
        val ip: String,
        val isMainlandChina: Boolean
    )

    internal data class IpProbeEndpoint(
        val url: String,
        val parser: (String) -> IpProbeResult?
    )

    private data class CachedIp(
        val ip: String,
        val isRealUserIp: Boolean,
        val fetchedAtMs: Long
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = AtomicReference<CachedIp?>(null)
    private val refreshInFlight = AtomicBoolean(false)
    @Volatile
    private var activeLatch: CountDownLatch? = null

    fun observeNetworkChanges(networkMonitor: NetworkMonitor) {
        scope.launch {
            networkMonitor.networkChanged.collectLatest {
                invalidateCache()
            }
        }
    }

    fun invalidateCache() {
        cache.set(null)
    }

    /**
     * 返回用于外部代理请求的稳定中国大陆 IPv4 地址；若缓存为空或已过期则在后台异步刷新。
     *
     * @param allowBriefWait 为 true 时（如登录/验证码接口），若首次探测正在进行中会短暂等待其完成，
     * 尽量保证首次认证请求即可带上用户真实 IP。
     */
    fun resolveProxyClientIp(
        allowBriefWait: Boolean = false,
        maxWaitMs: Long = DEFAULT_AUTH_WAIT_MS
    ): String {
        val now = clockMs()
        val existing = cache.get()
        if (existing != null && now - existing.fetchedAtMs < CACHE_TTL_MS) {
            return existing.ip
        }

        val latch = triggerAsyncRefresh()
        if (allowBriefWait && latch != null && existing == null) {
            runCatching { latch.await(maxWaitMs, TimeUnit.MILLISECONDS) }
            cache.get()?.let { return it.ip }
        }

        return existing?.ip ?: fallbackIpProvider()
    }

    internal fun refreshNowBlocking(): String {
        val detected = probeRealMainlandChinaIp()
        val now = clockMs()
        val resolved = if (detected != null) {
            CachedIp(ip = detected, isRealUserIp = true, fetchedAtMs = now)
        } else {
            CachedIp(ip = fallbackIpProvider(), isRealUserIp = false, fetchedAtMs = now)
        }
        cache.set(resolved)
        return resolved.ip
    }

    private fun triggerAsyncRefresh(): CountDownLatch? {
        if (!refreshInFlight.compareAndSet(false, true)) {
            return activeLatch
        }
        val latch = CountDownLatch(1)
        activeLatch = latch
        scope.launch {
            try {
                refreshNowBlocking()
            } finally {
                latch.countDown()
                refreshInFlight.set(false)
            }
        }
        return latch
    }

    private fun probeRealMainlandChinaIp(): String? {
        for (endpoint in endpoints) {
            val result = runCatching {
                val request = Request.Builder()
                    .url(endpoint.url)
                    .header("Accept", "application/json")
                    .get()
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) return@use null
                    endpoint.parser(body)
                }
            }.getOrNull() ?: continue

            if (result.isMainlandChina && isValidPublicIpv4(result.ip)) {
                return result.ip
            }
            // 若已明确探测到有效的非中国大陆公网 IP（例如正开启海外代理/VPN），
            // 直接停止后续探测，回退为本次会话内固定的国内运营商 IP。
            if (!result.isMainlandChina && isValidPublicIpv4(result.ip)) {
                return null
            }
        }
        return null
    }

    companion object {
        private const val CACHE_TTL_MS = 15 * 60 * 1000L
        private const val DEFAULT_AUTH_WAIT_MS = 1_200L

        private val NON_MAINLAND_REGIONS = setOf("香港", "澳门", "台湾", "香港特别行政区", "澳门特别行政区", "台湾省")

        internal val DEFAULT_ENDPOINTS: List<IpProbeEndpoint> = listOf(
            IpProbeEndpoint(
                url = "https://myip.ipip.net/json",
                parser = ::parseIpipJson
            ),
            IpProbeEndpoint(
                url = "https://whois.pconline.com.cn/ipJson.jsp?json=true",
                parser = ::parsePconlineJson
            ),
            IpProbeEndpoint(
                url = "https://api-ipv4.ip.sb/geoip",
                parser = ::parseIpSbJson
            )
        )

        internal fun parseIpipJson(raw: String): IpProbeResult? {
            val root = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull() ?: return null
            if (root.get("ret")?.asString != "ok") return null
            val data = root.getAsJsonObject("data") ?: return null
            val ip = data.get("ip")?.asString?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val locations = data.getAsJsonArray("location")
            val country = locations?.takeIf { it.size() > 0 }?.get(0)?.asString?.trim().orEmpty()
            val province = locations?.takeIf { it.size() > 1 }?.get(1)?.asString?.trim().orEmpty()
            val isMainland = country == "中国" && NON_MAINLAND_REGIONS.none { province.contains(it) }
            return IpProbeResult(ip = ip, isMainlandChina = isMainland)
        }

        internal fun parsePconlineJson(raw: String): IpProbeResult? {
            val root = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull() ?: return null
            val ip = root.get("ip")?.asString?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val err = root.get("err")?.asString?.trim().orEmpty()
            val pro = root.get("pro")?.asString?.trim().orEmpty()
            val addr = root.get("addr")?.asString?.trim().orEmpty()
            val isMainland = err.isEmpty() &&
                pro.isNotEmpty() &&
                NON_MAINLAND_REGIONS.none { pro.contains(it) || addr.contains(it) }
            return IpProbeResult(ip = ip, isMainlandChina = isMainland)
        }

        internal fun parseIpSbJson(raw: String): IpProbeResult? {
            val root = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull() ?: return null
            val ip = root.get("ip")?.asString?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val countryCode = root.get("country_code")?.asString?.trim().orEmpty()
            val region = root.get("region")?.asString?.trim().orEmpty()
            val isMainland = countryCode.equals("CN", ignoreCase = true) &&
                NON_MAINLAND_REGIONS.none { region.contains(it) }
            return IpProbeResult(ip = ip, isMainlandChina = isMainland)
        }

        internal fun isValidPublicIpv4(ip: String): Boolean {
            val parts = ip.trim().split('.')
            if (parts.size != 4) return false
            val octets = IntArray(4)
            for (i in 0..3) {
                val n = parts[i].toIntOrNull() ?: return false
                if (n !in 0..255) return false
                octets[i] = n
            }
            val a = octets[0]
            val b = octets[1]
            // 过滤私网、回环、链路本地、运营商级 NAT（CGNAT）、组播及保留网段
            if (a == 0 || a == 10 || a == 127 || a >= 224) return false
            if (a == 100 && b in 64..127) return false
            if (a == 169 && b == 254) return false
            if (a == 172 && b in 16..31) return false
            if (a == 192 && b == 168) return false
            return true
        }
    }
}
