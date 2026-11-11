package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.R
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AboutDialog
import com.example.ui.components.FloatingDeepSeekChatWidget
import com.example.ui.theme.DeepSeekPurple
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.VocabularyViewModel

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.repository.YouTubeSyncRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen(
    chatViewModel: ChatViewModel = viewModel(),
    vocabularyViewModel: VocabularyViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var isDeepSeekOpen by remember { mutableStateOf(false) }

    // Automatically check and update educational lessons and scenarios from the official YouTube channel on app open
    LaunchedEffect(Unit) {
        YouTubeSyncRepository.syncLatestContent(coroutineScope)
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    // Fixed DeepSeek button in top bar replacing YouTube icon
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDeepSeekOpen) DeepSeekPurple else DeepSeekPurple.copy(alpha = 0.12f),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { isDeepSeekOpen = !isDeepSeekOpen }
                            .testTag("deepseek_topbar_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "دیپ‌سیک",
                                tint = if (isDeepSeekOpen) Color.White else DeepSeekPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "دیپ‌سیک",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isDeepSeekOpen) Color.White else DeepSeekPurple,
                                    textDirection = TextDirection.Rtl
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.testTag("about_app_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "درباره مدرس و برنامه",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.ChatBubble, contentDescription = "مکالمه") },
                    label = {
                        Text(
                            text = "مکالمه",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                textDirection = TextDirection.Rtl
                            )
                        )
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.Bookmark, contentDescription = "واژگان") },
                    label = {
                        Text(
                            text = "واژگان",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                textDirection = TextDirection.Rtl
                            )
                        )
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.Category, contentDescription = "سناریوها") },
                    label = {
                        Text(
                            text = "سناریوها",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                textDirection = TextDirection.Rtl
                            )
                        )
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(imageVector = Icons.Default.Analytics, contentDescription = "پیشرفت") },
                    label = {
                        Text(
                            text = "پیشرفت",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                                textDirection = TextDirection.Rtl
                            )
                        )
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(imageVector = Icons.Default.Subscriptions, contentDescription = "یوتیوب", tint = if (selectedTab == 4) Color(0xFFFF0000) else MaterialTheme.colorScheme.onSurfaceVariant) },
                    label = {
                        Text(
                            text = "یوتیوب",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal,
                                textDirection = TextDirection.Rtl
                            )
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ChatScreen(
                    viewModel = chatViewModel,
                    onNavigateToScenarios = { selectedTab = 2 },
                    modifier = Modifier.fillMaxSize()
                )
                1 -> VocabularyScreen(
                    viewModel = vocabularyViewModel,
                    modifier = Modifier.fillMaxSize()
                )
                2 -> ScenariosScreen(
                    onSelectScenario = { scenario ->
                        chatViewModel.selectScenario(scenario)
                        selectedTab = 0
                    },
                    modifier = Modifier.fillMaxSize()
                )
                3 -> ProgressScreen(
                    chatViewModel = chatViewModel,
                    vocabularyViewModel = vocabularyViewModel,
                    modifier = Modifier.fillMaxSize()
                )
                4 -> YouTubeScreen(
                    onSelectLessonForPractice = { lesson ->
                        chatViewModel.selectYouTubeLesson(lesson)
                        selectedTab = 0
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Waterfall Dropdown Panel Overlay (DeepSeek AI Assistant) - Cascades down from top bar to bottom menu bar
            FloatingDeepSeekChatWidget(
                isOpen = isDeepSeekOpen,
                onClose = { isDeepSeekOpen = false }
            )
        }
    }
}


