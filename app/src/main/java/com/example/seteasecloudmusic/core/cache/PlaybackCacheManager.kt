package com.example.seteasecloudmusic.core.cache

import android.content.Context
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.core.player.PlaybackMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SavedPlaybackState(
    val queueTracks: List<Track> = emptyList(),
    val currentQueueIndex: Int = -1,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL
)

@Singleton
class PlaybackCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun savePlaybackState(state: SavedPlaybackState) {
        if (state.queueTracks.isEmpty()) return
        runCatching {
            val json = gson.toJson(state)
            prefs.edit().putString(KEY_PLAYBACK_STATE, json).apply()
        }
    }

    fun getSavedPlaybackState(): SavedPlaybackState? {
        val json = prefs.getString(KEY_PLAYBACK_STATE, null) ?: return null
        return runCatching {
            gson.fromJson<SavedPlaybackState>(json, object : TypeToken<SavedPlaybackState>() {}.type)
        }.getOrNull()
    }

    fun clearPlaybackState() {
        prefs.edit().remove(KEY_PLAYBACK_STATE).apply()
    }

    companion object {
        private const val PREF_NAME = "playback_cache_prefs"
        private const val KEY_PLAYBACK_STATE = "saved_playback_state"
    }
}
