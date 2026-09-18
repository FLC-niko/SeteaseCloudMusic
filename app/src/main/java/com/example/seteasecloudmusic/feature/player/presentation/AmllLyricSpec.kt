package com.example.seteasecloudmusic.feature.player.presentation

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * AMLL（Apple Music like Lyrics）歌词视觉规格 —— 实机测量值，唯一数据源。
 *
 * 数值来自对 WebView 内 AMLL 的实机探测（Chrome DevTools Protocol 读
 * `getComputedStyle` + 截图像素分析），不是从源码"推测"的：
 *
 * - **`vh` 单位在该 WebView 里恒为 0**（实测 `height:10vh` → `0px`，而 `10vw` 正常）。
 *   所以 AMLL 页面里 `clamp(30px, 4.2vh, 42px)` 永远取到下限 30px；
 *   翻译行的 `clamp(15px, 2.2vh, 22px)` 永远取到 15px。**它们不随屏幕高度缩放。**
 * - 逐字高亮的 `mask-image` 里是 `NaN%`，非法渐变 ⇒ 计算值 `none` ⇒
 *   WebView 里**根本没有逐字扫光**，激活行是整行均匀纯色。
 * - 因此歌词的纵深感**只来自模糊**：非激活行满亮度 + 越远越糊，激活行反而 0.85 不透明度。
 *
 * 修改本文件前请先用 `scripts/eval_amll.py` 重新测量，否则会失去与 WebView 的一致性。
 */
internal object AmllLyricSpec {

    // ── 字体（CSS px，1 CSS px = 2.75 设备 px @ 本机 440dpi）────────────────────

    /** 主歌词：`clamp(30px, 4.2vh, 42px)` 因 vh=0 落到下限 → 恒为 30。 */
    const val MAIN_FONT_SIZE_PX = 30f

    /** 翻译行：`clamp(15px, 2.2vh, 22px)` 同理 → 恒为 15。 */
    const val SUB_FONT_SIZE_PX = 15f

    /** 主行 CSS `line-height: 1.22em`（用于推算 AMLL 的"盒子高"）。 */
    const val MAIN_LINE_HEIGHT = 1.22f

    /**
     * 主行**实际渲染的行距**（em）。
     *
     * AMLL 的每个词是 `display:inline-block; vertical-align:bottom` 的 span，
     * 行盒里还会叠一个 line-height 的 strut，实测换行后上下两行的距离是
     * 42.2 CSS px（≈ 1.4em），比 CSS line-height（36.6px）大 15%。
     */
    const val MAIN_ROW_PITCH_EM = 1.4f

    /**
     * 行间槽位系数：**实测 1.72**。
     *
     * AMLL 里每行既是文档流的一部分（占一个自身盒高），又带一个
     * `translateY(step)` 的位移（step = 盒高 × 0.72），两者叠加后
     * 相邻行的屏幕距离 = 盒高 × 1.72。实测吻合：
     * 2 行歌词盒子 83 CSS px → 行距 143.5 CSS px；1 行歌词盒子 47 → 80。
     */
    const val LINE_PITCH_MULTIPLIER = 1.72f

    /** 翻译行 `line-height: 1.2em`。 */
    const val SUB_LINE_HEIGHT = 1.2f

    /** `letter-spacing: -0.015em`。 */
    const val LETTER_SPACING_EM = -0.015f

    /** 实测文字颜色 `rgb(237, 238, 240)`（**不是纯白**）。 */
    val TEXT_COLOR = Color(0xFFEDEEF0)

    /** 翻译行 `opacity: 0.75`。 */
    const val SUB_LINE_ALPHA = 0.75f

    // ── 亮度体系（实测：层次靠模糊，不靠透明度）────────────────────────────────

    /** 激活行 `.lyricMainLine` 的 `opacity: 0.85`。 */
    const val ACTIVE_LINE_ALPHA = 0.85f

    /** 非激活行**保持满亮度** `opacity: 1`（它们只是被模糊了）。 */
    const val INACTIVE_LINE_ALPHA = 1f

    // ── 模糊（`resolveBlurLevel`，实测值逐行核对过）──────────────────────────

    /** 上限：`Math.min(5, level)`。 */
    const val MAX_BLUR_PX = 5f

    /** 窄视口（宽 < 500px）模糊强度系数。 */
    const val NARROW_BLUR_FACTOR = 0.8f

    /** 窄视口判定阈值（对应 DOM 侧的 `max-width: 500px` 媒体查询）。 */
    const val NARROW_VIEWPORT_WIDTH_DP = 500f

    // ── 布局 ─────────────────────────────────────────────────────────────────

    /** `.lyricLine` 横向内边距（App 的 CSS 覆盖 `padding: 5px 18px`）。 */
    val LINE_PADDING_X = 18.dp

    /** `.lyricLine` 纵向内边距（同上）。 */
    val LINE_PADDING_Y = 5.dp

    /** 主行与翻译行间距：flex `gap: 0.3em`(9px) + 翻译行 `margin-top: 4px` = 13px。 */
    val MAIN_SUB_GAP = 13.dp

    /** 焦点行在视口中的相对高度（AMLL `LayoutConfig.alignPosition` 默认值）。 */
    const val ALIGN_POSITION = 0.35f

    // ── 行步进（`getLineStep`）───────────────────────────────────────────────

    /**
     * 含副行的高行判定：`h > baseFontSize × 1.4`（baseFontSize = 30 CSS px）。
     *
     * 在 30px 字号下每行的盒高都 ≥ 47px，所以实际取值恒为"高行"，
     * 此时 [LINE_PITCH_MULTIPLIER] 恒定 1.72。保留该常量是为了将来改字号时不失真。
     */
    const val TALL_LINE_THRESHOLD_EM = 1.4f

    /** 行高未知时的兜底步进：`max(40, baseFontSize × 1.35)`。 */
    const val LINE_STEP_FALLBACK_EM = 1.35f
    const val LINE_STEP_FALLBACK_MIN_PX = 40f

    // ── 滚动（`getPosYSpringPolicy` 换算到 Compose）────────────────────────────

    /**
     * 正常播放时的滚动弹簧。
     *
     * AMLL：`stiffness ∈ [170, 220]`、`mass = 0.9`、`damping = 2.2·√stiffness`（过阻尼）。
     * 换算到 Compose（k=170）：`stiffness = k/m ≈ 189`，
     * `dampingRatio = 2.2·√k / (2·√(k·m)) ≈ 1.16`。
     */
    val SCROLL_SPRING: SpringSpec<Float> = spring(dampingRatio = 1.16f, stiffness = 189f)

    /** 跨度超过该像素值时不再补间，直接瞬移（对应 AMLL 的 seek `snapPosY` 策略）。 */
    const val SNAP_THRESHOLD_PX = 1200f

    /** 用户手动滚动后恢复自动对齐的等待时间。 */
    const val AUTO_ALIGN_RESUME_DELAY_MS = 2400L
}
