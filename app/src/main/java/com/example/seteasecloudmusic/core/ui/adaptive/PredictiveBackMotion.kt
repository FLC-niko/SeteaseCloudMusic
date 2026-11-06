package com.example.seteasecloudmusic.core.ui.adaptive

/**
 * Android 预测性返回（Predictive Back）交互动效计算工具
 */
object PredictiveBackMotion {

    /** 预测性返回跟手预览时的最大位移比例（预览上一级页面约 22% 宽度，卡片只提前到位移预览位置） */
    const val PREVIEW_MAX_FRACTION = 0.22f

    /**
     * 根据返回手势进度计算向右预览的位移。
     * 无论从屏幕左边缘还是右边缘触发返回，手势拖拽预览期间最大仅向右平移至预览位置（约 22% 屏幕宽度），
     * 露出上一级页面供用户预览，避免在手势拖拽中途就提前将整页滑走。
     *
     * @param progress 0f..1f 手势滑动进度
     * @param fullWidthPx 屏幕或容器总宽度像素
     * @return 向右平移的像素位移 (>= 0f，最大不超过 fullWidthPx * PREVIEW_MAX_FRACTION)
     */
    fun calculateTranslationX(
        progress: Float,
        fullWidthPx: Float
    ): Float {
        val clamped = progress.coerceIn(0f, 1f)
        val maxShift = fullWidthPx * PREVIEW_MAX_FRACTION
        return clamped * maxShift
    }
}
