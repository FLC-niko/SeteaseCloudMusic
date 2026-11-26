package com.example.seteasecloudmusic.feature.mine.presentation

/**
 * 歌单详情页的视觉风格定义
 */
enum class PlaylistDetailStyle(
    val id: String,
    val title: String
) {
    /**
     * 大封面墙（默认风格）：
     * 居中大封面、仅主标题、圆角三角胶囊播放键与动态取色背景。
     */
    APPLE_MUSIC(
        id = "apple_music",
        title = "大封面墙"
    ),

    /**
     * 静态海报墙：
     * 4x4 多封面拼贴网格，底部使用取色渐变遮罩衔接页面背景。
     */
    POSTER_WALL(
        id = "poster_wall",
        title = "静态海报墙"
    ),

    /**
     * 动态海报墙：
     * 30° 倾斜交替多列滚动海报墙，底部使用取色渐变遮罩衔接页面背景。
     */
    DYNAMIC_POSTER_WALL(
        id = "dynamic_poster_wall",
        title = "动态海报墙"
    ),

    /**
     * 全屏封面：
     * 顶部全宽封面，底部通过渐变融入取色背景，保留歌单名与播放键。
     */
    FULL_COVER(
        id = "full_cover",
        title = "全屏封面"
    );

    val subtitle: String
        get() = ""
}
