package com.example.seteasecloudmusic.feature.main.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Navigation and playback share the same centred width, including in a fold's display segment. */
internal fun Modifier.horizontalChromeFrame(horizontalPadding: Dp = 20.dp): Modifier =
    padding(horizontal = horizontalPadding).widthIn(max = 620.dp).fillMaxWidth()

/** Reserve button space inside the capped frame, so the width limit cannot consume the inset. */
internal fun Modifier.horizontalMiniPlayerFrame(
    progress: Float,
    horizontalPadding: Dp = 20.dp,
    buttonSize: Dp = 54.dp,
    buttonGap: Dp = 10.dp
): Modifier = horizontalChromeFrame(horizontalPadding)
    .padding(horizontal = (buttonSize + buttonGap) * progress.coerceIn(0f, 1f))
