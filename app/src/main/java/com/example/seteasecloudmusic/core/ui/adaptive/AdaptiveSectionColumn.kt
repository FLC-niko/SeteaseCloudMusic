package com.example.seteasecloudmusic.core.ui.adaptive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class AdaptiveSectionScope {
    internal val sections = mutableListOf<MusicSection>()
    fun item(key: Any? = null, fullSpan: Boolean = false, content: @Composable () -> Unit) {
        sections += MusicSection(key ?: "section_${sections.size}", fullSpan, content)
    }
}

internal class MusicSection(val key: Any, val fullSpan: Boolean, val content: @Composable () -> Unit)

/** Reflows editorial sections, while their horizontal carousels keep a comfortable card size. */
@Composable
fun AdaptiveSectionColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: AdaptiveSectionScope.() -> Unit
) {
    val scope = AdaptiveSectionScope().apply(content)
    val window = LocalAdaptiveWindow.current
    val secondaryState = rememberLazyListState()
    var anchor by rememberSaveable { mutableStateOf<String?>(null) }
    var anchorOffset by rememberSaveable { mutableIntStateOf(0) }
    var restoring by remember { mutableStateOf(false) }
    var secondaryWasActive by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(modifier) {
        val split = !LocalPaneAlreadySplit.current && (window.isBook || window.isTabletop)
        val columns = if (maxWidth >= 720.dp) 2 else 1
        val first = scope.sections.filterIndexed { index, section -> section.fullSpan || index % 2 == 0 }
        val second = scope.sections.filterIndexed { index, section -> !section.fullSpan && index % 2 != 0 }
        val rows = mutableListOf<List<MusicSection>>()
        var pending = mutableListOf<MusicSection>()
        scope.sections.forEach { section ->
            if (section.fullSpan) {
                if (pending.isNotEmpty()) rows += pending.toList()
                pending = mutableListOf()
                rows += listOf(section)
            } else {
                pending += section
                if (pending.size == columns) { rows += pending.toList(); pending.clear() }
            }
        }
        if (pending.isNotEmpty()) rows += pending.toList()

        // Save a section identity rather than a row number: the number of rows changes on unfolding.
        LaunchedEffect(split, columns, scope.sections.map { it.key }) {
            restoring = true
            val saved = anchor
            if (saved != null) {
                if (split) {
                    val primaryIndex = first.indexOfFirst { it.key.toString() == saved }
                    val secondaryIndex = second.indexOfFirst { it.key.toString() == saved }
                    if (primaryIndex >= 0) state.scrollToItem(primaryIndex, anchorOffset)
                    else if (secondaryIndex >= 0) secondaryState.scrollToItem(secondaryIndex, anchorOffset)
                } else {
                    val row = rows.indexOfFirst { group -> group.any { it.key.toString() == saved } }
                    if (row >= 0) state.scrollToItem(row, anchorOffset)
                }
            }
            restoring = false
        }
        LaunchedEffect(state, secondaryState, split) {
            var previousPrimary: Pair<String?, Int>? = null
            var previousSecondary: Pair<String?, Int>? = null
            snapshotFlow {
                val primary = state.layoutInfo.visibleItemsInfo.firstOrNull()?.key?.toString() to state.firstVisibleItemScrollOffset
                val secondary = secondaryState.layoutInfo.visibleItemsInfo.firstOrNull()?.key?.toString() to secondaryState.firstVisibleItemScrollOffset
                Triple(primary, secondary, state.isScrollInProgress to secondaryState.isScrollInProgress)
            }.collect { (primary, secondary, scrolling) ->
                val primaryChanged = previousPrimary != null && primary != previousPrimary
                val secondaryChanged = previousSecondary != null && secondary != previousSecondary
                if (!restoring) {
                    if (split) {
                        if (scrolling.second || (secondaryChanged && !primaryChanged)) secondaryWasActive = true
                        else if (scrolling.first || (primaryChanged && !secondaryChanged)) secondaryWasActive = false
                    }
                    // Keep the last interacted pane after scrolling ends; an idle pane must not replace its anchor.
                    val active = if (split && secondaryWasActive) secondary else primary
                    if (active.first != null && (anchor == null || primaryChanged || secondaryChanged || scrolling.first || scrolling.second)) {
                        anchor = active.first
                        anchorOffset = active.second
                    }
                }
                previousPrimary = primary
                previousSecondary = secondary
            }
        }
        if (split) {
            AdaptiveTwoPane(Modifier.fillMaxSize(), primaryFraction = 0.5f, gap = 16.dp,
                primary = { SectionList(first, state, contentPadding, verticalArrangement) },
                secondary = { SectionList(second, secondaryState, contentPadding, verticalArrangement) })
        } else {
            LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = verticalArrangement) {
                items(rows, key = { it.first().key }) { row ->
                    if (row.first().fullSpan || columns == 1) row.first().content()
                    else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { section -> Box(Modifier.weight(1f)) { key(section.key) { section.content() } } }
                        if (row.size < columns) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionList(sections: List<MusicSection>, state: LazyListState, padding: PaddingValues, arrangement: Arrangement.Vertical) {
    LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = padding, verticalArrangement = arrangement) {
        items(sections, key = { it.key }) { it.content() }
    }
}
