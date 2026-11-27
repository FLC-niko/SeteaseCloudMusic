package com.example.seteasecloudmusic.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seteasecloudmusic.core.settings.PlayerSettingsManager
import com.example.seteasecloudmusic.core.ui.adaptive.AdaptiveAlertDialog
import com.example.seteasecloudmusic.core.ui.theme.AppTheme
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 下载目录配置弹窗：支持手动输入路径及一键选用默认音乐目录 / 网易云音乐目录。
 */
@Composable
fun DownloadDirectorySettingsDialog(
    currentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = AppTheme.colors
    var inputPath by remember(currentPath) {
        mutableStateOf(
            PlayerSettingsManager.normalizeDirectoryPath(currentPath)
                .ifBlank { PlayerSettingsManager.DEFAULT_DOWNLOAD_DIRECTORY }
        )
    }

    val quickPaths = remember {
        listOf(
            "默认音乐目录" to PlayerSettingsManager.DEFAULT_DOWNLOAD_DIRECTORY,
            "网易云音乐目录" to PlayerSettingsManager.NETEASE_DOWNLOAD_DIRECTORY
        )
    }

    AdaptiveAlertDialog(
        onDismissRequest = onDismiss,
        title = "设置下载目录",
        confirmText = "保存",
        confirmColor = colors.accent,
        onConfirm = {
            val normalized = PlayerSettingsManager.normalizeDirectoryPath(inputPath)
                .ifBlank { PlayerSettingsManager.DEFAULT_DOWNLOAD_DIRECTORY }
            onConfirm(normalized)
        },
        dismissText = "取消",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "新下载的歌曲将直接保存至该目录：",
                style = MiuixTheme.textStyles.footnote1.copy(color = colors.textSecondary)
            )
            @Suppress("DEPRECATION")
            TextField(
                value = inputPath,
                onValueChange = { inputPath = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = PlayerSettingsManager.DEFAULT_DOWNLOAD_DIRECTORY,
                useLabelAsPlaceholder = true,
                textStyle = MiuixTheme.textStyles.body1.copy(color = colors.textPrimary, fontSize = 13.5.sp),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = colors.surfaceVariant,
                    labelColor = colors.textSecondary,
                    borderColor = colors.accent
                ),
                cursorBrush = SolidColor(colors.accent)
            )
            Text(
                text = "快捷路径：",
                style = MiuixTheme.textStyles.footnote1.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val normalizedCurrent = PlayerSettingsManager.normalizeDirectoryPath(inputPath)
                quickPaths.forEach { (label, path) ->
                    val selected = normalizedCurrent == path
                    Surface(
                        onClick = { inputPath = path },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) colors.accentSubtle else colors.surfaceVariant,
                        border = BorderStroke(
                            width = if (selected) 1.dp else 0.5.dp,
                            color = if (selected) colors.accent else colors.divider
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(
                                text = label,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) colors.accent else colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = path,
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 本地扫描目录配置弹窗：支持多目录勾选/移除、一键恢复默认双目录及添加自定义目录。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanDirectoriesSettingsDialog(
    currentDirectories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val colors = AppTheme.colors
    var selectedDirs by remember(currentDirectories) {
        mutableStateOf(
            currentDirectories
                .map { PlayerSettingsManager.normalizeDirectoryPath(it) }
                .filter { it.isNotBlank() }
                .distinct()
                .ifEmpty { PlayerSettingsManager.DEFAULT_SCAN_DIRECTORIES }
        )
    }
    var customInput by remember { mutableStateOf("") }

    val quickOptions = remember {
        listOf(
            "Music 目录" to PlayerSettingsManager.DEFAULT_DOWNLOAD_DIRECTORY,
            "网易云目录" to PlayerSettingsManager.NETEASE_DOWNLOAD_DIRECTORY
        )
    }

    AdaptiveAlertDialog(
        onDismissRequest = onDismiss,
        title = "设置本地扫描目录",
        confirmText = "保存并扫描",
        confirmColor = colors.accent,
        onConfirm = {
            val pendingCustom = PlayerSettingsManager.normalizeDirectoryPath(customInput)
            val finalDirs = (selectedDirs + listOfNotNull(pendingCustom.takeIf { it.isNotBlank() }))
                .distinct()
                .ifEmpty { PlayerSettingsManager.DEFAULT_SCAN_DIRECTORIES }
            onConfirm(finalDirs)
        },
        dismissText = "取消",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已启用目录 (${selectedDirs.size})",
                    style = MiuixTheme.textStyles.footnote1.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "恢复默认",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent,
                    modifier = Modifier
                        .clickable { selectedDirs = PlayerSettingsManager.DEFAULT_SCAN_DIRECTORIES }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                selectedDirs.forEach { dir ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceVariant,
                        border = BorderStroke(0.5.dp, colors.divider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dir,
                                fontSize = 11.5.sp,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedDirs.size > 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "移除",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.accent,
                                    modifier = Modifier
                                        .clickable { selectedDirs = selectedDirs.filterNot { it == dir } }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "快捷开关：",
                style = MiuixTheme.textStyles.footnote1.copy(
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickOptions.forEach { (label, path) ->
                    val enabled = path in selectedDirs
                    Surface(
                        onClick = {
                            selectedDirs = if (enabled) {
                                if (selectedDirs.size > 1) selectedDirs.filterNot { it == path } else selectedDirs
                            } else {
                                (selectedDirs + path).distinct()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (enabled) colors.accentSubtle else colors.surfaceVariant,
                        border = BorderStroke(
                            width = if (enabled) 1.dp else 0.5.dp,
                            color = if (enabled) colors.accent else colors.divider
                        )
                    ) {
                        Text(
                            text = if (enabled) "✓ $label" else "+ $label",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (enabled) colors.accent else colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                @Suppress("DEPRECATION")
                TextField(
                    value = customInput,
                    onValueChange = { customInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = "添加其他扫描目录...",
                    useLabelAsPlaceholder = true,
                    textStyle = MiuixTheme.textStyles.body1.copy(color = colors.textPrimary, fontSize = 13.sp),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = colors.surfaceVariant,
                        labelColor = colors.textSecondary,
                        borderColor = colors.accent
                    ),
                    cursorBrush = SolidColor(colors.accent)
                )
                Surface(
                    onClick = {
                        val normalized = PlayerSettingsManager.normalizeDirectoryPath(customInput)
                        if (normalized.isNotBlank()) {
                            selectedDirs = (selectedDirs + normalized).distinct()
                            customInput = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = colors.accentSubtle,
                    border = BorderStroke(0.5.dp, colors.accent)
                ) {
                    Text(
                        text = "添加",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
