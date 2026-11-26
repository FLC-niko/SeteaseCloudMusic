package com.example.seteasecloudmusic.core.network.local

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * 纯离线生成 `data:image/png;base64,...` 二维码字符串（基于 ZXing 与标准灰度 PNG 编码），
 * 不依赖 Android `Bitmap`，可在 JVM 单元测试环境直接运行。
 */
internal object LocalQrCodeGenerator {

    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    /**
     * 生成 Base64 Data URL 格式的二维码 PNG。
     * @param sizePx 二维码输出边长（像素，默认 280）
     * @param marginModules 二维码四周留白模块数（默认 2）
     */
    fun toDataUrl(
        content: String,
        sizePx: Int = 280,
        marginModules: Int = 2
    ): String {
        val pngBytes = encodePng(content, sizePx, marginModules)
        return "data:image/png;base64," + pngBytes.toByteString().base64()
    }

    internal fun encodePng(
        content: String,
        sizePx: Int = 280,
        marginModules: Int = 2
    ): ByteArray {
        require(content.isNotBlank()) { "QR content must not be blank" }
        require(sizePx > 0) { "sizePx must be positive" }

        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to marginModules
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = matrix.width
        val height = matrix.height

        // 8-bit 灰度 PNG（color type = 0, bit depth = 8）：每行含 1 字节滤波标记 + width 字节像素
        val rawImage = ByteArray(height * (width + 1))
        for (y in 0 until height) {
            val rowOffset = y * (width + 1)
            rawImage[rowOffset] = 0 // 滤波类型 None
            for (x in 0 until width) {
                rawImage[rowOffset + 1 + x] = if (matrix.get(x, y)) 0x00 else 0xFF.toByte()
            }
        }

        val compressed = deflate(rawImage)
        val out = ByteArrayOutputStream(compressed.size + 64)
        out.write(PNG_SIGNATURE)

        val ihdr = ByteBuffer.allocate(13)
            .putInt(width)
            .putInt(height)
            .put(8.toByte()) // 位深
            .put(0.toByte()) // 颜色类型：灰度
            .put(0.toByte()) // 压缩方式
            .put(0.toByte()) // 滤波方式
            .put(0.toByte()) // 隔行扫描
            .array()
        writeChunk(out, "IHDR", ihdr)
        writeChunk(out, "IDAT", compressed)
        writeChunk(out, "IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun deflate(input: ByteArray): ByteArray {
        val deflater = Deflater()
        return try {
            deflater.setInput(input)
            deflater.finish()
            // Zlib 压缩临时缓冲区大小（字节）
            val buf = ByteArray(4096)
            val baos = ByteArrayOutputStream()
            while (!deflater.finished()) {
                val n = deflater.deflate(buf)
                baos.write(buf, 0, n)
            }
            baos.toByteArray()
        } finally {
            deflater.end()
        }
    }

    private fun writeChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        val lenBuf = ByteBuffer.allocate(4).putInt(data.size).array()
        out.write(lenBuf)
        out.write(typeBytes)
        out.write(data)
        val crc = CRC32().apply {
            update(typeBytes)
            update(data)
        }.value.toInt()
        out.write(ByteBuffer.allocate(4).putInt(crc).array())
    }
}
