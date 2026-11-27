package com.example.seteasecloudmusic.core.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 应用主题外观模式。 */
enum class AppThemeMode(
    val title: String,
    val desc: String
) {
    SYSTEM("跟随系统", "自动匹配系统深浅色外观"),
    LIGHT("浅色模式", "始终使用浅色外观"),
    DARK("深色模式", "始终使用深色外观")
}

/** 在线播放可选音质等级及其服务端 `levelKey` 与展示文案映射。 */
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
        // 播放器音质菜单可选项顺序
        val PlayerSelectableQualities: List<OnlineAudioQuality> = listOf(
            STANDARD,
            HIGH,
            LOSSLESS,
            HIRES
        )
    }
}

/**
 * 播放器与全局偏好设置管理器，基于 SharedPreferences 持久化并通过 StateFlow 对外广播变更。
 */
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

    private val _topAmbientEnabled = MutableStateFlow(loadSavedTopAmbientEnabled())
    val topAmbientEnabled: StateFlow<Boolean> = _topAmbientEnabled.asStateFlow()

    private val _topAmbientColorArgb = MutableStateFlow(loadSavedTopAmbientColorArgb())
    val topAmbientColorArgb: StateFlow<Int> = _topAmbientColorArgb.asStateFlow()

    private val _liquidGlassOpacity = MutableStateFlow(loadSavedLiquidGlassOpacity())
    val liquidGlassOpacity: StateFlow<Float> = _liquidGlassOpacity.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setTopAmbientEnabled(enabled: Boolean) {
        _topAmbientEnabled.value = enabled
        prefs.edit().putBoolean(KEY_TOP_AMBIENT_ENABLED, enabled).apply()
    }

    fun setTopAmbientColorArgb(argb: Int) {
        _topAmbientColorArgb.value = argb
        prefs.edit().putInt(KEY_TOP_AMBIENT_COLOR_ARGB, argb).apply()
    }

    fun setLiquidGlassOpacity(level: Float, persist: Boolean = true) {
        val clamped = if (level.isNaN()) {
            DEFAULT_LIQUID_GLASS_OPACITY
        } else {
            level.coerceIn(MIN_LIQUID_GLASS_OPACITY, MAX_LIQUID_GLASS_OPACITY)
        }
        _liquidGlassOpacity.value = clamped
        if (persist) {
            prefs.edit().putFloat(KEY_LIQUID_GLASS_OPACITY, clamped).apply()
        }
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
        return prefs.getBoolean(KEY_BLUETOOTH_LYRICS, false)
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

    private fun loadSavedTopAmbientEnabled(): Boolean {
        return prefs.getBoolean(KEY_TOP_AMBIENT_ENABLED, true)
    }

    private fun loadSavedTopAmbientColorArgb(): Int {
        return prefs.getInt(KEY_TOP_AMBIENT_COLOR_ARGB, DEFAULT_TOP_AMBIENT_COLOR_ARGB)
    }

    private fun loadSavedLiquidGlassOpacity(): Float {
        val raw = prefs.getFloat(KEY_LIQUID_GLASS_OPACITY, DEFAULT_LIQUID_GLASS_OPACITY)
        return if (raw.isNaN()) {
            DEFAULT_LIQUID_GLASS_OPACITY
        } else {
            raw.coerceIn(MIN_LIQUID_GLASS_OPACITY, MAX_LIQUID_GLASS_OPACITY)
        }
    }

    companion object {
        private const val PREF_NAME = "app_settings_prefs"                     // 全局设置 SharedPreferences 文件名
        private const val KEY_AUDIO_QUALITY = "player_audio_quality"           // 在线音质偏好 Key（默认 OnlineAudioQuality.STANDARD）
        private const val KEY_BLUETOOTH_LYRICS = "player_bluetooth_lyrics"     // 蓝牙歌词推送开关 Key（默认 false）
        private const val KEY_SUPER_LYRIC_API = "player_super_lyric_api"       // 超级歌词/状态栏歌词开关 Key（默认 true）
        private const val KEY_PLAYLIST_STYLE = "playlist_detail_style"         // 歌单详情页样式标识 Key（默认 "apple_music"）
        private const val KEY_PREDICTIVE_BACK = "predictive_back_enabled"      // 预测性返回手势开关 Key（默认 true）
        private const val KEY_LOCAL_DIRECT_API = "local_direct_api_enabled"    // 本地直连接口开关 Key（默认 true）
        private const val KEY_THEME_MODE = "app_theme_mode"                    // 应用深浅主题模式 Key（默认 AppThemeMode.SYSTEM）
        private const val KEY_TOP_AMBIENT_ENABLED = "top_ambient_enabled"      // 顶部氛围背景开关 Key（默认 true）
        private const val KEY_TOP_AMBIENT_COLOR_ARGB = "top_ambient_color_argb"// 顶部氛围背景色 ARGB 整数值 Key
        private const val KEY_LIQUID_GLASS_OPACITY = "liquid_glass_opacity_level" // 液态玻璃通透度/磨砂调节值 Key（[-1f, 1f]，默认 0f）

        /** 顶部氛围背景默认颜色（ARGB）。 */
        const val DEFAULT_TOP_AMBIENT_COLOR_ARGB: Int = 0xFFFFE8ED.toInt()

        /** 液态玻璃最通透档位下限（Clear）。 */
        const val MIN_LIQUID_GLASS_OPACITY: Float = -1f

        /** 液态玻璃默认档位（0f，保持原有默认光学参数）。 */
        const val DEFAULT_LIQUID_GLASS_OPACITY: Float = 0f

        /** 液态玻璃最磨砂档位上限（Tinted）。 */
        const val MAX_LIQUID_GLASS_OPACITY: Float = 1f

        @Volatile
        private var INSTANCE: PlayerSettingsManager? = null

        fun getInstance(context: Context): PlayerSettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlayerSettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
