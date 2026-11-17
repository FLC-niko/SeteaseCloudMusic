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
 * Generates `data:image/png;base64,...` QR code strings completely offline using ZXing + standard PNG encoding
 * (free of Android framework Bitmap dependencies so JVM unit tests run natively).
 */
internal object LocalQrCodeGenerator {

    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

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

        // Grayscale 8-bit PNG (color type = 0, bit depth = 8): 1 filter byte + width pixel bytes per row
        val rawImage = ByteArray(height * (width + 1))
        for (y in 0 until height) {
            val rowOffset = y * (width + 1)
            rawImage[rowOffset] = 0 // Filter type None
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
            .put(8.toByte()) // bit depth
            .put(0.toByte()) // color type: grayscale
            .put(0.toByte()) // compression
            .put(0.toByte()) // filter
            .put(0.toByte()) // interlace
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
