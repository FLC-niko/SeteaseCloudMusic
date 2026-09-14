package com.example.seteasecloudmusic.core.network

import android.util.Log
import java.net.InetAddress
import okhttp3.Dns

/**
 * 直连（不走系统 VPN / 代理）的 DNS 兜底工具。
 *
 * 本项目有两条独立的自建 OkHttpClient：主网络模块 [NetworkModule] 与匿名登录上报
 * [com.example.seteasecloudmusic.core.network.ncbl.NcblReporter]。两者都需要
 * “先走系统 DNS，失败时回退到网易云固定 IP”的能力，此前各自写了一份，
 * 硬编码 IP 以 byte 数组形式重复出现 —— 一旦节点变更需要改两处，容易漏改。
 *
 * 这里只收敛**完全相同**的两部分：
 * 1. 系统 DNS 查询 + 失败日志
 * 2. 兜底 IP 列表的构造
 *
 * 具体的兜底策略（对哪些域名生效、回退几个节点）仍由调用方决定，避免改变各自的解析行为。
 */
object SmartDns {

    /**
     * 网易云直连兜底节点（IPv4）。
     * 顺序即优先级：clientlog3 使用全部三个节点，其余 clientlog 域名只用前两个。
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
