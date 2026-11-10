package com.example.seteasecloudmusic.feature.search.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索历史本地持久化与状态管理。
 *
 * 1. 内存中以 [StateFlow] 维护最新搜索词列表，提供即时响应；
 * 2. 变更时自动写入 [android.content.SharedPreferences] 保持跨生命周期持久化；
 * 3. 自动去重并将最新搜索词置顶，限制最大历史记录数量。
 */
@Singleton
class SearchHistoryManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _historyFlow = MutableStateFlow<List<String>>(loadHistory())
    val historyFlow: StateFlow<List<String>> = _historyFlow.asStateFlow()

    private fun loadHistory(): List<String> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val type = TypeToken.getParameterized(List::class.java, String::class.java).type
            val list = gson.fromJson<List<String>>(json, type)
            list?.filterNotNull()?.filter { it.isNotBlank() } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<String>) {
        _historyFlow.value = list
        prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply()
    }

    /**
     * 添加新的搜索记录。
     * 若已存在则移至最前；限制最多 [MAX_HISTORY_COUNT] 条。
     */
    @Synchronized
    fun addHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val current = _historyFlow.value.toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val limited = current.take(MAX_HISTORY_COUNT)
        persist(limited)
    }

    /**
     * 删除指定关键词的搜索记录。
     */
    @Synchronized
    fun removeHistory(query: String) {
        val current = _historyFlow.value.toMutableList()
        if (current.removeAll { it.equals(query, ignoreCase = true) }) {
            persist(current)
        }
    }

    /**
     * 清空全部搜索历史。
     */
    @Synchronized
    fun clearHistory() {
        persist(emptyList())
    }

    companion object {
        private const val PREF_NAME = "search_history_prefs"
        private const val KEY_HISTORY = "search_history_list"
        private const val MAX_HISTORY_COUNT = 30
    }
}
