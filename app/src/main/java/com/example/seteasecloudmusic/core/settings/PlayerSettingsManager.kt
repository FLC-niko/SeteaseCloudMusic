package com.example.seteasecloudmusic.core.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class OnlineAudioQuality(
    val title: String,
    val levelKey: String,
    val desc: String,
    val badgeText: String
) {
    STANDARD("标准", "standard", "标准音质 · 128Kbps", "标准音质"),
    HIGH("极高", "exhigh", "极高音质 · 320Kbps", "极高音质"),
    LOSSLESS("无损", "lossless", "SQ 无损音质 · FLAC", "Lossless"),
    HIRES("Hi-Res", "hires", "Hi-Res 超清母带 · 24bit/96kHz", "Hi-Res Lossless"),
    DOLBY("杜比全景声", "sky", "沉浸环绕声 · Dolby Atmos", "Dolby Atmos")
}

@Singleton
class PlayerSettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _audioQuality = MutableStateFlow(loadSavedAudioQuality())
    val audioQuality: StateFlow<OnlineAudioQuality> = _audioQuality.asStateFlow()

    private val _bluetoothLyricsEnabled = MutableStateFlow(loadSavedBluetoothLyrics())
    val bluetoothLyricsEnabled: StateFlow<Boolean> = _bluetoothLyricsEnabled.asStateFlow()

    private val _superLyricApiEnabled = MutableStateFlow(loadSavedSuperLyricApi())
    val superLyricApiEnabled: StateFlow<Boolean> = _superLyricApiEnabled.asStateFlow()

    fun setAudioQuality(quality: OnlineAudioQuality) {
        _audioQuality.value = quality
        prefs.edit().putString(KEY_AUDIO_QUALITY, quality.name).apply()
    }

    fun setBluetoothLyricsEnabled(enabled: Boolean) {
        _bluetoothLyricsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_BLUETOOTH_LYRICS, enabled).apply()
    }

    fun setSuperLyricApiEnabled(enabled: Boolean) {
        _superLyricApiEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SUPER_LYRIC_API, enabled).apply()
    }

    private fun loadSavedAudioQuality(): OnlineAudioQuality {
        val name = prefs.getString(KEY_AUDIO_QUALITY, OnlineAudioQuality.STANDARD.name)
        return try {
            OnlineAudioQuality.valueOf(name ?: OnlineAudioQuality.STANDARD.name)
        } catch (e: Exception) {
            OnlineAudioQuality.STANDARD
        }
    }

    private fun loadSavedBluetoothLyrics(): Boolean {
        return prefs.getBoolean(KEY_BLUETOOTH_LYRICS, true)
    }

    private fun loadSavedSuperLyricApi(): Boolean {
        return prefs.getBoolean(KEY_SUPER_LYRIC_API, true)
    }

    companion object {
        private const val PREF_NAME = "app_settings_prefs"
        private const val KEY_AUDIO_QUALITY = "player_audio_quality"
        private const val KEY_BLUETOOTH_LYRICS = "player_bluetooth_lyrics"
        private const val KEY_SUPER_LYRIC_API = "player_super_lyric_api"
    }
}
