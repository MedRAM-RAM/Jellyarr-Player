package com.jellyarr.player.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jellyarr.player.ui.theme.JColors

private data class Tab(val label: String, val icon: ImageVector, val selected: ImageVector)

private val tabs = listOf(
    Tab("الرئيسية", Icons.Outlined.Home, Icons.Filled.Home),
    Tab("استكشاف", Icons.Outlined.Explore, Icons.Filled.Explore),
    Tab("المكتبة", Icons.Outlined.VideoLibrary, Icons.Filled.VideoLibrary),
    Tab("بحث", Icons.Outlined.Search, Icons.Filled.Search),
    Tab("الإعدادات", Icons.Outlined.Settings, Icons.Filled.Settings),
)

@Composable
fun MainScreen() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().background(JColors.Background)) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab",
            modifier = Modifier.fillMaxSize(),
        ) { tab ->
            when (tab) {
                0 -> HomeScreen()
                1 -> DiscoverScreen()
                2 -> LibraryTab()
                3 -> SearchScreen()
                else -> SettingsScreen()
            }
        }
        BottomBar(selected, { selected = it }, Modifier.align(Alignment.BottomCenter))
    }
}

/** Height reserved at the bottom of scrolling content so the floating bar never hides it. */
val BottomBarSpace = 96.dp

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, JColors.Background.copy(alpha = .95f), JColors.Background)))
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(22.dp))
                .background(JColors.SurfaceHigh.copy(alpha = .96f))
                .border(1.dp, JColors.OutlineSoft, RoundedCornerShape(22.dp))
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, t ->
                val active = i == selected
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                        .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(i) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp))
                            .background(if (active) JColors.Primary.copy(alpha = .2f) else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            if (active) t.selected else t.icon, t.label,
                            tint = if (active) JColors.PrimarySoft else JColors.TextMuted,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        t.label, fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) JColors.Text else JColors.TextMuted,
                    )
                }
            }
        }
    }
}
