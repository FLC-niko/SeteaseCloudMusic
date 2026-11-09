package com.example.seteasecloudmusic.feature.mine.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.seteasecloudmusic.core.common.toCoverThumbnailUrl
import com.example.seteasecloudmusic.feature.home.presentation.ScrollingPosterWall
import com.example.seteasecloudmusic.feature.mine.domain.model.PlaylistDetail
import com.example.seteasecloudmusic.feature.player.presentation.AppleMusicPlayTriangleIcon

private val PlaylistAccentColor = Color(0xFFFA233B)

val PlaylistDetail.displayTitle: String
    get() = if (name.endsWith("喜欢的音乐")) "我喜欢的音乐" else name

/**
 * 风格 1：Apple Music 沉浸卡片风格（仿照截图：居中大封面投影、仅主标题、圆角三角胶囊播放键）
 */
@Composable
fun PlaylistHeroAppleMusicSection(
    detail: PlaylistDetail,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    coverWidthFraction: Float = 0.68f,
    showArtwork: Boolean = true,
    showControls: Boolean = true
) {
    val totalCount = detail.trackCount.coerceAtLeast(detail.tracks.size)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 居中大正方形封面（带高品质立体投影）
        if (showArtwork) Box(
            modifier = Modifier
                .fillMaxWidth(coverWidthFraction)
                .aspectRatio(1f)
                .shadow(
                    elevation = 26.dp,
                    shape = RoundedCornerShape(18.dp),
                    clip = false,
                    spotColor = Color.Black.copy(alpha = 0.72f),
                    ambientColor = Color.Black.copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (!detail.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = detail.coverUrl.toCoverThumbnailUrl(480),
                    contentDescription = detail.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF28364A), Color(0xFF161F2E))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(68.dp)
                    )
                }
            }
        }

        if (!showControls) return@Column

        Spacer(modifier = Modifier.height(18.dp))

        // 2. 居中歌单标题
        Text(
            text = detail.displayTitle,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF111111)
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 中间胶囊播放键（主题红底白字）
        Surface(
            onClick = onPlayAll,
            enabled = totalCount > 0,
            shape = RoundedCornerShape(24.dp),
            color = PlaylistAccentColor,
            border = null,
            shadowElevation = 2.dp,
            modifier = Modifier.height(48.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 38.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AppleMusicPlayTriangleIcon(
                    tint = Color.White,
                    size = 15.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "播放",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * 风格 2：静态海报墙（复刻每日推荐多封面拼贴网格海报墙，把字和灰色遮罩去掉，下侧使用取色渐变遮罩自然融合）
 */
@Composable
fun PlaylistHeroPosterWallSection(
    detail: PlaylistDetail,
    dominantDarkColor: Color,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    isDark: Boolean = true,
    showControls: Boolean = true
) {
    val wallCovers = remember(detail) {
        val tracksCovers = detail.tracks.mapNotNull { it.coverUrl?.takeIf(String::isNotBlank) }
        if (tracksCovers.isNotEmpty()) tracksCovers else listOfNotNull(detail.coverUrl)
    }
    val totalCount = detail.trackCount.coerceAtLeast(detail.tracks.size)
    val maskColor = if (isDark) dominantDarkColor else Color(0xFF333338)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 顶格海报拼贴网格（4 行完整 1:1 正方形封面，平滑无色阶微渐变遮罩，底部外延封死空隙）
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 多封面拼贴海报网格：严格 4 行 x 4 列 = 16 个 1:1 正方形封面
            PosterCoverGrid(
                covers = wallCovers,
                dominantDarkColor = dominantDarkColor,
                modifier = Modifier.fillMaxWidth()
            )

            // 底部柔和微渐变遮罩：开启取色时为 dominantDarkColor，关闭取色时改为灰色遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .background(
                        Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.12f to maskColor.copy(alpha = 0.03f),
                            0.24f to maskColor.copy(alpha = 0.12f),
                            0.36f to maskColor.copy(alpha = 0.28f),
                            0.48f to maskColor.copy(alpha = 0.48f),
                            0.60f to maskColor.copy(alpha = 0.68f),
                            0.72f to maskColor.copy(alpha = 0.84f),
                            0.84f to maskColor.copy(alpha = 0.95f),
                            0.92f to maskColor,
                            1.00f to maskColor
                        )
                    )
            )
        }

        if (!showControls) return@Column

        // 2. 歌单主标题
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = detail.displayTitle,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF111111)
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        // 3. 播放键（主题红底白字）
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            onClick = onPlayAll,
            enabled = totalCount > 0,
            shape = RoundedCornerShape(24.dp),
            color = PlaylistAccentColor,
            border = null,
            shadowElevation = 2.dp,
            modifier = Modifier.height(48.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 38.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AppleMusicPlayTriangleIcon(
                    tint = Color.White,
                    size = 15.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "播放",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * 风格 3：动态海报墙（复刻首页 30° 倾斜交替多列无限滚动海报墙，下侧取色渐变遮罩自然融合）
 */
@Composable
fun PlaylistHeroDynamicPosterWallSection(
    detail: PlaylistDetail,
    dominantDarkColor: Color,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    isDark: Boolean = true,
    showControls: Boolean = true,
    artworkHeight: androidx.compose.ui.unit.Dp? = null
) {
    val wallCovers = remember(detail) {
        val tracksCovers = detail.tracks.mapNotNull { it.coverUrl?.takeIf(String::isNotBlank) }
        if (tracksCovers.isNotEmpty()) tracksCovers else listOfNotNull(detail.coverUrl)
    }
    val totalCount = detail.trackCount.coerceAtLeast(detail.tracks.size)
    val maskColor = if (isDark) dominantDarkColor else Color(0xFF333338)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 顶格 30° 倾斜滚动海报墙（直接复用首页 ScrollingPosterWall）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(artworkHeight ?: if (topInset > 0.dp) 350.dp + topInset else 320.dp)
                .clipToBounds()
        ) {
            ScrollingPosterWall(
                covers = wallCovers,
                modifier = Modifier.fillMaxSize()
            )

            // 底部柔和微渐变遮罩：开启取色时为 dominantDarkColor，关闭取色时改为灰色遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .background(
                        Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.12f to maskColor.copy(alpha = 0.03f),
                            0.24f to maskColor.copy(alpha = 0.12f),
                            0.36f to maskColor.copy(alpha = 0.28f),
                            0.48f to maskColor.copy(alpha = 0.48f),
                            0.60f to maskColor.copy(alpha = 0.68f),
                            0.72f to maskColor.copy(alpha = 0.84f),
                            0.84f to maskColor.copy(alpha = 0.95f),
                            0.92f to maskColor,
                            1.00f to maskColor
                        )
                    )
            )
        }

        if (!showControls) return@Column

        // 2. 歌单主标题
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = detail.displayTitle,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF111111)
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        // 3. 播放键（主题红底白字）
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            onClick = onPlayAll,
            enabled = totalCount > 0,
            shape = RoundedCornerShape(24.dp),
            color = PlaylistAccentColor,
            border = null,
            shadowElevation = 2.dp,
            modifier = Modifier.height(48.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 38.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AppleMusicPlayTriangleIcon(
                    tint = Color.White,
                    size = 15.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "播放",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * 拼贴封面网格辅助组件（严格 4 行 x 4 列，每个封面 1:1 正方形展示，白色网格分割线）
 */
@Composable
private fun PosterCoverGrid(
    covers: List<String>,
    dominantDarkColor: Color,
    modifier: Modifier = Modifier
) {
    val displayItems = remember(covers) {
        val total = 16
        List(total) { index ->
            if (covers.isNotEmpty()) covers[index % covers.size].toCoverThumbnailUrl(180) else null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
    ) {
        repeat(4) { rowIndex ->
            if (rowIndex > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(4) { colIndex ->
                    val itemIndex = rowIndex * 4 + colIndex
                    val url = displayItems.getOrNull(itemIndex)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clipToBounds()
                            .background(dominantDarkColor.copy(alpha = 0.20f))
                            .drawWithContent {
                                drawContent()
                                // 列间 1dp 白色竖向分割线：仅在每行前 3 列的右侧绘制
                                // 第 4 行（底部行）竖线提前 24dp 结束，确保底部完全无白线残留，彻底杜绝闪烁细线
                                if (colIndex < 3) {
                                    val strokePx = 1.dp.toPx()
                                    val bottomCutoff = if (rowIndex == 3) 24.dp.toPx() else 0f
                                    val lineBottom = (size.height - bottomCutoff).coerceAtLeast(0f)
                                    if (lineBottom > 0f) {
                                        drawLine(
                                            color = Color.White,
                                            start = Offset(size.width - strokePx / 2, 0f),
                                            end = Offset(size.width - strokePx / 2, lineBottom),
                                            strokeWidth = strokePx
                                        )
                                    }
                                }
                            }
                    ) {
                        if (!url.isNullOrBlank()) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}

