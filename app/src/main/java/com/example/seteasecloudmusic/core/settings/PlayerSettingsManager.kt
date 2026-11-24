package com.example.seteasecloudmusic.core.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AppThemeMode(
    val title: String,
    val desc: String
) {
    SYSTEM("跟随系统", "自动匹配系统深浅色外观"),
    LIGHT("浅色模式", "始终使用浅色外观"),
    DARK("深色模式", "始终使用深色外观")
}

enum class OnlineAudioQuality(
    val title: String,
    val levelKey: String,
    val desc: String,
    val badgeText: String,
    val playerBadgeText: String = badgeText,
    val playerMenuLabel: String = title
) {
    STANDARD("标准", "standard", "标准码率 · 128 kbps", "标准音质", "标准", "标准 MP3"),
    HIGH("极高", "exhigh", "高清码率 · 320 kbps", "极高音质", "极高", "极高 HQ"),
    LOSSLESS("无损", "lossless", "无损压缩 · FLAC", "无损音质", "无损", "无损 SQ"),
    HIRES("Hi-Res", "hires", "高解析母带 · 24bit/96kHz", "Hi-Res", "Hi-Res", "高解析度无损 Hi-Res");

    companion object {
        val PlayerSelectableQualities: List<OnlineAudioQuality> = listOf(
            STANDARD,
            HIGH,
            LOSSLESS,
            HIRES
        )
    }
}

@Singleton
class PlayerSettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    init {
        INSTANCE = this
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _audioQuality = MutableStateFlow(loadSavedAudioQuality())
    val audioQuality: StateFlow<OnlineAudioQuality> = _audioQuality.asStateFlow()

    private val _bluetoothLyricsEnabled = MutableStateFlow(loadSavedBluetoothLyrics())
    val bluetoothLyricsEnabled: StateFlow<Boolean> = _bluetoothLyricsEnabled.asStateFlow()

    private val _superLyricApiEnabled = MutableStateFlow(loadSavedSuperLyricApi())
    val superLyricApiEnabled: StateFlow<Boolean> = _superLyricApiEnabled.asStateFlow()

    private val _playlistDetailStyle = MutableStateFlow(loadSavedPlaylistDetailStyle())
    val playlistDetailStyle: StateFlow<String> = _playlistDetailStyle.asStateFlow()

    private val _predictiveBackEnabled = MutableStateFlow(loadSavedPredictiveBack())
    val predictiveBackEnabled: StateFlow<Boolean> = _predictiveBackEnabled.asStateFlow()

    private val _localDirectApiEnabled = MutableStateFlow(loadSavedLocalDirectApi())
    val localDirectApiEnabled: StateFlow<Boolean> = _localDirectApiEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(loadSavedThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

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

    fun setPlaylistDetailStyle(styleId: String) {
        _playlistDetailStyle.value = styleId
        prefs.edit().putString(KEY_PLAYLIST_STYLE, styleId).apply()
    }

    fun setPredictiveBackEnabled(enabled: Boolean) {
        _predictiveBackEnabled.value = enabled
        prefs.edit().putBoolean(KEY_PREDICTIVE_BACK, enabled).apply()
    }

    fun setLocalDirectApiEnabled(enabled: Boolean) {
        _localDirectApiEnabled.value = enabled
        prefs.edit().putBoolean(KEY_LOCAL_DIRECT_API, enabled).apply()
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

    private fun loadSavedPlaylistDetailStyle(): String {
        return prefs.getString(KEY_PLAYLIST_STYLE, "apple_music") ?: "apple_music"
    }

    private fun loadSavedPredictiveBack(): Boolean {
        return prefs.getBoolean(KEY_PREDICTIVE_BACK, true)
    }

    private fun loadSavedLocalDirectApi(): Boolean {
        return prefs.getBoolean(KEY_LOCAL_DIRECT_API, true)
    }

    private fun loadSavedThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(name ?: AppThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    companion object {
        private const val PREF_NAME = "app_settings_prefs"
        private const val KEY_AUDIO_QUALITY = "player_audio_quality"
        private const val KEY_BLUETOOTH_LYRICS = "player_bluetooth_lyrics"
        private const val KEY_SUPER_LYRIC_API = "player_super_lyric_api"
        private const val KEY_PLAYLIST_STYLE = "playlist_detail_style"
        private const val KEY_PREDICTIVE_BACK = "predictive_back_enabled"
        private const val KEY_LOCAL_DIRECT_API = "local_direct_api_enabled"
        private const val KEY_THEME_MODE = "app_theme_mode"

        @Volatile
        private var INSTANCE: PlayerSettingsManager? = null

        fun getInstance(context: Context): PlayerSettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlayerSettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
