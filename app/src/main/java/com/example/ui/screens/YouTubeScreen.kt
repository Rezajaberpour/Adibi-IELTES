package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.YouTubeLesson
import com.example.data.model.YouTubeLessonData

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.repository.YouTubeSyncRepository

@Composable
fun YouTubeScreen(
    onSelectLessonForPractice: ((YouTubeLesson) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val channelUrl = YouTubeLessonData.OFFICIAL_CHANNEL_URL
    val coroutineScope = rememberCoroutineScope()

    val allLessons by YouTubeSyncRepository.lessons.collectAsState()
    val isSyncing by YouTubeSyncRepository.isSyncing.collectAsState()
    val syncMessage by YouTubeSyncRepository.syncMessage.collectAsState()

    var selectedSeries by remember { mutableStateOf("همه سرفصل‌ها") }

    val filteredLessons = remember(selectedSeries, allLessons) {
        if (selectedSeries == "همه سرفصل‌ها") {
            allLessons
        } else {
            allLessons.filter { it.category == selectedSeries }
        }
    }

    val openChannel: () -> Unit = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(channelUrl))
        context.startActivity(intent)
    }

    val openVideo: (String) -> Unit = { url ->
        val target = if (url.isNotBlank()) url else channelUrl
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
        context.startActivity(intent)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // YouTube Channel Official Header Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .testTag("youtube_channel_header_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Banner with YouTube Brand Colors
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFF0000), // YouTube Red
                                        Color(0xFFCC0000),
                                        Color(0xFF212121)
                                    )
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Subscriptions,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "کانال رسمی یوتیوب آکادمی ادیبی",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textDirection = TextDirection.Rtl
                                    )
                                )
                                Text(
                                    text = "${YouTubeLessonData.CHANNEL_HANDLE} | مدرس: ${YouTubeLessonData.INSTRUCTOR_NAME}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.95f),
                                        textDirection = TextDirection.Ltr
                                    )
                                )
                            }
                        }
                    }

                    // Channel Profile Info Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_ai_tutor),
                                    contentDescription = YouTubeLessonData.INSTRUCTOR_NAME,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "آموزش تخصصی آیلتس و مکالمه - خانم زهرا ادیبی",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        textDirection = TextDirection.Rtl
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "تمامی مطالب و سرفصل‌های این اپلیکیشن کاملاً منطبق بر دوره‌های آموزشی کانال یوتیوب طراحی شده است.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textDirection = TextDirection.Rtl
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Open Youtube Button
                        Button(
                            onClick = openChannel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF0000),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_youtube_channel_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ورود به کانال یوتیوب (@adibilearnskills)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        textDirection = TextDirection.Rtl
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Real-time YouTube Channel Live Sync Status Bar
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("youtube_sync_status_card")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFFFF0000)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "وضعیت همگام‌سازی",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = syncMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        textDirection = TextDirection.Rtl
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                val refreshTint = if (isSyncing) disabledColor else primaryColor

                                IconButton(
                                    onClick = {
                                        YouTubeSyncRepository.syncLatestContent(coroutineScope)
                                    },
                                    enabled = !isSyncing,
                                    modifier = Modifier.size(32.dp).testTag("refresh_youtube_feed_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "بروزرسانی از یوتیوب",
                                        tint = refreshTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Series Filter Chips
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "دسته‌بندی‌های آموزشی کانال یوتیوب",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDirection = TextDirection.Rtl
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(YouTubeLessonData.seriesCategories) { category ->
                        val isSelected = selectedSeries == category
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            tonalElevation = if (isSelected) 4.dp else 1.dp,
                            modifier = Modifier.clickable { selectedSeries = category }
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textDirection = TextDirection.Rtl
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Lesson Items
        items(filteredLessons, key = { it.id }) { lesson ->
            CuratedYouTubeLessonCard(
                lesson = lesson,
                onOpenVideo = { openVideo(lesson.videoUrl) },
                onPracticeInChat = { onSelectLessonForPractice?.invoke(lesson) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CuratedYouTubeLessonCard(
    lesson: YouTubeLesson,
    onOpenVideo: () -> Unit,
    onPracticeInChat: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("curated_lesson_card_${lesson.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = lesson.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textDirection = TextDirection.Rtl
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = lesson.bandScore,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = YouTubeLessonData.CHANNEL_HANDLE,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = Color(0xFFFF0000),
                        fontWeight = FontWeight.Bold,
                        textDirection = TextDirection.Ltr
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = lesson.titlePersian,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textDirection = TextDirection.Rtl
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = lesson.titleEnglish,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDirection = TextDirection.Ltr
                ),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = lesson.summaryPersian,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDirection = TextDirection.Rtl
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Key Points Bullet List
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "📌 نکات کلیدی و تدریس ویدیو:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            textDirection = TextDirection.Rtl
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    lesson.keyPoints.forEach { point ->
                        Text(
                            text = "• $point",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                textDirection = TextDirection.Rtl
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPracticeInChat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تمرین این سرفصل در مکالمه",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDirection = TextDirection.Rtl
                        )
                    )
                }

                OutlinedButton(
                    onClick = onOpenVideo,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مشاهده ویدیو",
                        style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Rtl)
                    )
                }
            }
        }
    }
}
