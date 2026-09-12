package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.studio.StudioScreen
import com.example.ui.screens.voice.LiveVoiceScreen
import com.example.ui.theme.GeminiBackground
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiOutline
import com.example.ui.theme.GeminiSurface
import com.example.ui.theme.GeminiTextMuted
import com.example.ui.theme.GeminiTextSecondary

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isLiveVoiceActive by viewModel.isLiveVoiceActive.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = GeminiBackground,
            bottomBar = {
                NavigationBar(
                    containerColor = GeminiSurface,
                    contentColor = GeminiTextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GeminiOutline)
                        .testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.CHAT,
                        onClick = { viewModel.selectTab(AppTab.CHAT) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "Gemini Chat",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Gemini", fontSize = 11.sp, fontWeight = if (currentTab == AppTab.CHAT) FontWeight.Bold else FontWeight.Normal) },
                        colors = geminiNavItemColors(),
                        modifier = Modifier.testTag("tab_chat")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.STUDIO,
                        onClick = { viewModel.selectTab(AppTab.STUDIO) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Studio",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Studio", fontSize = 11.sp, fontWeight = if (currentTab == AppTab.STUDIO) FontWeight.Bold else FontWeight.Normal) },
                        colors = geminiNavItemColors(),
                        modifier = Modifier.testTag("tab_studio")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.HISTORY,
                        onClick = { viewModel.selectTab(AppTab.HISTORY) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Recent", fontSize = 11.sp, fontWeight = if (currentTab == AppTab.HISTORY) FontWeight.Bold else FontWeight.Normal) },
                        colors = geminiNavItemColors(),
                        modifier = Modifier.testTag("tab_history")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.SETTINGS,
                        onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Settings",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Settings", fontSize = 11.sp, fontWeight = if (currentTab == AppTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                        colors = geminiNavItemColors(),
                        modifier = Modifier.testTag("tab_settings")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    AppTab.CHAT -> ChatScreen(viewModel = viewModel)
                    AppTab.STUDIO -> StudioScreen(viewModel = viewModel)
                    AppTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                    AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    else -> ChatScreen(viewModel = viewModel)
                }
            }
        }

        // Overlaid Fullscreen Google Gemini Live Voice Dialog
        AnimatedVisibility(
            visible = isLiveVoiceActive,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            LiveVoiceScreen(viewModel = viewModel)
        }
    }
}

@Composable
private fun geminiNavItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = GeminiBlue,
    selectedTextColor = GeminiBlue,
    unselectedIconColor = GeminiTextMuted,
    unselectedTextColor = GeminiTextMuted,
    indicatorColor = GeminiBlue.copy(alpha = 0.15f)
)
