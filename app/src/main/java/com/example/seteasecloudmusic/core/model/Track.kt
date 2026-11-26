package com.example.seteasecloudmusic.core.model

/** 通用曲目模型，统一承载在线歌曲与本地扫描曲目的基础信息与音频元数据。 */
data class Track(
    val id: Long,
    val title: String,
    val artists: List<Artist> = emptyList(),
    val album: Album,

    val qualityTags: List<AudioQuality> = emptyList(),

    val coverUrl: String? = null,
    val durationMs: Long? = null,

    val playableUrl: String? = null,
    val isPlayable: Boolean = true,
    val recommendReason: String? = null,

    // 音频详细元数据（来源、码率、采样率、大小、编码格式、实际音质等级）
    val audioSourceLabel: String? = null,
    val audioBitrateKbps: Int? = null,
    val audioSampleRateHz: Int? = null,
    val audioFileSizeBytes: Long? = null,
    val audioFormat: String? = null,
    val audioQualityLevel: String? = null
)

/** 专辑基础模型。 */
data class Album(
    val id: Long,
    val title: String,
    val coverUrl: String? = null
)

/** 歌手基础模型。 */
data class Artist(
    val id: Long,
    val name: String,
    val coverUrl: String? = null
)

/** 曲目支持的音质等级枚举。 */
enum class AudioQuality{
    STANDARD,
    HIGH,
    LOSSLESS,
    HIRES
}