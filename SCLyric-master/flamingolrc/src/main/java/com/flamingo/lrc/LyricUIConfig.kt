package com.flamingo.lrc

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.font.FontWeight

/**
 * FlamingoLyricView UI 配置类
 *
 * @param edgeFade 是否启用边缘渐隐效果
 * @param formatText 是否启用歌词规整功能
 * @param noLrcText 无歌词时的提示文本
 * @param blankHeight 列表首尾的填充块高度，单位 dp
 * @param mainTextSize 主要文本的大小，单位 sp
 * @param subTextSize 次要文本的大小，单位 sp
 * @param mainTextBasicColor 主要文本的底色 (ARGB Long)
 * @param subTextBasicColor 次要文本的底色 (ARGB Long)
 * @param fontWeight 歌词字重
 * @param lineBalance 是否启用平衡行模式
 */
@Stable
data class LyricUIConfig(
    val edgeFade: Boolean = true,
    val formatText: Boolean = true,
    val noLrcText: String = "No lyrics",
    val blankHeight: Int = 70,
    val mainTextSize: Int = 34,
    val subTextSize: Int = 16,
    val mainTextBasicColor: Long = 0xFFF2F2F2,
    val subTextBasicColor: Long = 0xFF919191,
    val fontWeight: FontWeight = FontWeight.ExtraBold,
    val lineBalance: Boolean = false
)
