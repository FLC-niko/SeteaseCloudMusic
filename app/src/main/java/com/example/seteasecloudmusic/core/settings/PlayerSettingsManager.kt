package com.example.seteasecloudmusic.core.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class PlayerStyle(val title: String, val desc: String) {
    AMLL_WEB("Apple Music 动效 (AMLL)", "流光动态渐变背景与逐字动效歌词"),
    NATIVE_COMPOSE("原生质感 (Compose)", "沉浸式专辑主色渐变与原生丝滑歌词滚动")
}

@Singleton
class PlayerSettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _playerStyle = MutableStateFlow(loadSavedStyle())
    val playerStyle: StateFlow<PlayerStyle> = _playerStyle.asStateFlow()

    fun setPlayerStyle(style: PlayerStyle) {
        _playerStyle.value = style
        prefs.edit().putString(KEY_PLAYER_STYLE, style.name).apply()
    }

    private fun loadSavedStyle(): PlayerStyle {
        val name = prefs.getString(KEY_PLAYER_STYLE, PlayerStyle.AMLL_WEB.name)
        return try {
            PlayerStyle.valueOf(name ?: PlayerStyle.AMLL_WEB.name)
        } catch (e: Exception) {
            PlayerStyle.AMLL_WEB
        }
    }

    companion object {
        private const val PREF_NAME = "app_settings_prefs"
        private const val KEY_PLAYER_STYLE = "player_style_mode"
    }
}
