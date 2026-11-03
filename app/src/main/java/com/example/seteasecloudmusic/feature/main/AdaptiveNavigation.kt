package com.example.seteasecloudmusic.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.seteasecloudmusic.R
import com.example.seteasecloudmusic.core.model.Track

internal data class SelectedArtist(val id: Long, val name: String, val coverUrl: String?)
internal data class DailyRecommendState(val tracks: List<Track>, val title: String = "每日推荐")

/** Keep large recommendation lists out of saved-instance bundles, but retain them across resizing. */
internal class AppNavigationState : ViewModel() {
    var selectedArtist by mutableStateOf<SelectedArtist?>(null)
    var dailyRecommend by mutableStateOf<DailyRecommendState?>(null)
}

@Composable
internal fun MusicNavigationRail(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple("首页", R.drawable.ic_nav_home, R.drawable.ic_nav_home_filled),
        Triple("电台", R.drawable.ic_nav_radio, R.drawable.ic_nav_radio_filled),
        Triple("我的", R.drawable.ic_nav_mine, R.drawable.ic_nav_mine_filled),
        Triple("搜索", R.drawable.ic_nav_search, R.drawable.ic_nav_search)
    )
    NavigationRail(
        modifier = Modifier.width(88.dp).fillMaxHeight().background(Color(0xFFF8F8FA)),
        containerColor = Color(0xFFF8F8FA),
        header = {
            Text("LUNA", color = Color(0xFFFA233B), fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 28.dp, bottom = 32.dp))
        }
    ) {
        items.forEachIndexed { index, (title, icon, selectedIcon) ->
            NavigationRailItem(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                icon = { Icon(painterResource(if (selectedIndex == index) selectedIcon else icon), title, Modifier.size(24.dp)) },
                label = { Text(title) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color(0xFFFA233B), selectedTextColor = Color(0xFFFA233B),
                    indicatorColor = Color(0xFFFFE8ED), unselectedIconColor = Color(0xFF777780)
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
