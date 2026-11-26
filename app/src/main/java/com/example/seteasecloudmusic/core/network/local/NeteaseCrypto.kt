package com.example.seteasecloudmusic.core.network.local

import okio.ByteString.Companion.encodeUtf8
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

/**
 * 纯 Kotlin/JDK 实现的网易云接口加解密工具，支持 `weapi`、`eapi`、`linuxapi` 与匿名登录 ID 编码。
 */
internal object NeteaseCrypto {

    data class WeapiPayload(
        val params: String,
        val encSecKey: String
    )

    private const val IV = "0102030405060708"            // AES-CBC 固定初始化向量（16 字节）
    private const val PRESET_KEY = "0CoJUm6Qyw8W8jud"    // WEAPI 第一轮 AES-CBC 预置密钥
    private const val LINUXAPI_KEY = "rFgB&h#%2?^eDg:Q"  // LINUXAPI AES-ECB 密钥
    private const val EAPI_KEY = "e82ckenh8dichen8"      // EAPI 请求加密与响应解密 AES-ECB 密钥
    private const val ID_XOR_KEY = "3go8&$8*3*3h0k(2)2"  // 匿名游客用户名计算用 XOR 掩码
    private const val BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    // WEAPI 随机密钥 RSA 无填充加密公钥（X.509 DER Base64）
    private const val WEAPI_PUBLIC_KEY_B64 =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDgtQn2JZ34ZC28NWYpAUd98iZ3" +
            "7BUrX/aKzmFbt7clFSs6sXqHauqKWqdtLkF2KexO40H1YTX8z2lSgBBOAxLs" +
            "vaklV8k4cBFK9snQXE9/DDaFt6Rr7iVZMldczhC0JNgTz+SHXT6CBHuX3e9S" +
            "dB1Ua44oncaTWz7OBGLbCiK45wIDAQAB"

    private val secureRandom = SecureRandom()

    private val rsaPublicKey: RSAPublicKey by lazy {
        val der = WEAPI_PUBLIC_KEY_B64.decodeBase64()?.toByteArray()
            ?: error("Invalid embedded RSA public key")
        KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(der)) as RSAPublicKey
    }

    private val rsaModulusBytes: Int by lazy {
        (rsaPublicKey.modulus.bitLength() + 7) / 8
    }

    /**
     * 生成指定长度的随机 Base62 会话密钥（默认 16 位，用于 WEAPI 第二轮 AES 加密）。
     */
    fun generateSecretKey(length: Int = 16): String {
        val chars = CharArray(length)
        for (i in 0 until length) {
            chars[i] = BASE62[secureRandom.nextInt(BASE62.length)]
        }
        return String(chars)
    }

    /**
     * WEAPI 加密：先用 [PRESET_KEY] 再用随机 [secretKey] 做两次 AES-CBC，并将反转后的 [secretKey] 做 RSA 加密。
     */
    fun weapi(plainJson: String, secretKey: String = generateSecretKey()): WeapiPayload {
        val firstPass = aesCbcEncryptBase64(plainJson.toByteArray(Charsets.UTF_8), PRESET_KEY, IV)
        val params = aesCbcEncryptBase64(firstPass.toByteArray(Charsets.UTF_8), secretKey, IV)
        val reversedKey = secretKey.reversed().toByteArray(Charsets.UTF_8)
        val encSecKey = rsaNoPaddingEncryptHex(reversedKey)
        return WeapiPayload(params = params, encSecKey = encSecKey)
    }

    /**
     * EAPI 请求加密：拼接路径、JSON 与 MD5 摘要后执行 AES-ECB 加密并输出大写 Hex。
     */
    fun eapi(urlPath: String, dataJson: String): String {
        val message = "nobody${urlPath}use${dataJson}md5forencrypt"
        val digest = md5Hex(message)
        val payload = "${urlPath}-36cd479b6b5-${dataJson}-36cd479b6b5-${digest}"
        return aesEcbEncryptHex(payload.toByteArray(Charsets.UTF_8), EAPI_KEY)
    }

    /**
     * 解密启用 `e_r=true` 的 EAPI Hex 密文响应体。
     */
    fun eapiResDecrypt(hexCipher: String): String {
        val encrypted = hexToBytes(hexCipher.trim())
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(EAPI_KEY.toByteArray(Charsets.UTF_8), "AES"))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    /**
     * LINUXAPI 请求加密：使用 [LINUXAPI_KEY] 执行 AES-ECB 加密并输出大写 Hex。
     */
    fun linuxapi(objectJson: String): String {
        return aesEcbEncryptHex(objectJson.toByteArray(Charsets.UTF_8), LINUXAPI_KEY)
    }

    fun md5Hex(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytesToHex(digest, uppercase = false)
    }

    /**
     * 基于 [deviceId] 生成匿名游客注册接口所需的编码用户名。
     */
    fun generateAnonymousUsername(deviceId: String): String {
        val xored = ByteArray(deviceId.length)
        for (i in deviceId.indices) {
            xored[i] = (deviceId[i].code xor ID_XOR_KEY[i % ID_XOR_KEY.length].code).toByte()
        }
        val md5Bytes = MessageDigest.getInstance("MD5").digest(xored)
        val encodedDigest = md5Bytes.toByteString().base64()
        return "$deviceId $encodedDigest".encodeUtf8().base64()
    }

    private fun aesCbcEncryptBase64(plainBytes: ByteArray, key: String, iv: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"),
            IvParameterSpec(iv.toByteArray(Charsets.UTF_8))
        )
        return cipher.doFinal(plainBytes).toByteString().base64()
    }

    private fun aesEcbEncryptHex(plainBytes: ByteArray, key: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"))
        return bytesToHex(cipher.doFinal(plainBytes), uppercase = true)
    }

    private fun rsaNoPaddingEncryptHex(plainBytes: ByteArray): String {
        val m = BigInteger(1, plainBytes)
        val encrypted = m.modPow(rsaPublicKey.publicExponent, rsaPublicKey.modulus)
        val raw = encrypted.toByteArray()
        val fixed = ByteArray(rsaModulusBytes)
        val srcOffset = if (raw.size > rsaModulusBytes) raw.size - rsaModulusBytes else 0
        val copyLen = minOf(raw.size, rsaModulusBytes)
        System.arraycopy(raw, srcOffset, fixed, rsaModulusBytes - copyLen, copyLen)
        return bytesToHex(fixed, uppercase = false)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        require(len % 2 == 0) { "Odd hex length: $len" }
        val out = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            out[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return out
    }

    private fun bytesToHex(bytes: ByteArray, uppercase: Boolean): String {
        val alphabet = if (uppercase) HEX_UPPER else HEX_LOWER
        val out = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            out[i * 2] = alphabet[v ushr 4]
            out[i * 2 + 1] = alphabet[v and 0x0F]
        }
        return String(out)
    }

    private val HEX_LOWER = "0123456789abcdef".toCharArray()
    private val HEX_UPPER = "0123456789ABCDEF".toCharArray()
}
