package com.example.seteasecloudmusic.core.network

import android.util.Log
import java.net.InetAddress
import okhttp3.Dns

/**
 * 直连网易云官方节点的 DNS 兜底工具，供主网络模块 [NetworkModule] 与日志上报 [com.example.seteasecloudmusic.core.network.ncbl.NcblReporter] 共用。
 *
 * 先查询系统 DNS；失败时提供预置的网易云固定 IPv4 节点列表，具体启用哪些节点由调用方按域名策略决定。
 */
object SmartDns {

    /**
     * 网易云直连兜底节点（IPv4），按顺序排列优先级：
     * `clientlog3` 使用全部 3 个节点，其余 `clientlog` 域名使用前 2 个。
     */
    private val FALLBACK_IP_V4: List<ByteArray> = listOf(
        byteArrayOf(220.toByte(), 197.toByte(), 30.toByte(), 68.toByte()),  // 220.197.30.68
        byteArrayOf(59.toByte(), 111.toByte(), 181.toByte(), 60.toByte()),  // 59.111.181.60
        byteArrayOf(59.toByte(), 111.toByte(), 181.toByte(), 38.toByte())   // 59.111.181.38
    )

    /** 可用的兜底节点总数，用于校验调用方请求的数量。 */
    val fallbackNodeCount: Int get() = FALLBACK_IP_V4.size

    /**
     * 构造绑定到 [hostname] 的兜底地址列表。
     *
     * @param count 取前几个节点，默认全部
     */
    fun addressesFor(hostname: String, count: Int = FALLBACK_IP_V4.size): List<InetAddress> =
        FALLBACK_IP_V4.take(count).map { InetAddress.getByAddress(hostname, it) }

    /**
     * 先尝试系统 DNS。
     *
     * @return 解析成功时返回非空地址列表；失败或结果为空时返回 null（并记录警告日志），
     *         由调用方决定是否回退到 [addressesFor]。
     */
    fun systemLookupOrNull(hostname: String, logTag: String): List<InetAddress>? =
        try {
            Dns.SYSTEM.lookup(hostname).takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(logTag, "System DNS failed for $hostname: ${e.message}")
            null
        }
}
