package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PracticeScenario
import com.example.data.model.ScenarioData
import com.example.ui.components.ScenarioCard
import com.example.ui.theme.EmeraldSuccess

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.repository.YouTubeSyncRepository

data class YouTubeEducationalTip(
    val title: String,
    val description: String,
    val iconTag: String = "💡"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenariosScreen(
    onSelectScenario: (PracticeScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allScenarios by YouTubeSyncRepository.scenarios.collectAsState()
    val isSyncing by YouTubeSyncRepository.isSyncing.collectAsState()
    val syncMessage by YouTubeSyncRepository.syncMessage.collectAsState()

    val openYouTubeChannel = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@adibilearnskills?si=e9kQ2rrem3921gXT"))
        context.startActivity(intent)
    }

    var selectedCategory by remember { mutableStateOf("همه") }
    val categories = listOf("همه", "زندگی روزمره", "سفر و گردشگری", "کسب‌وکار و شغل")

    val youtubeTips = listOf(
        YouTubeEducationalTip(
            title = "تکنیک سایه‌زنی (Shadowing):",
            description = "تکرار دقیق همزمان جملات انگلیسی در سناریوها برای تقویت لحن، لهجه و استرس کلمات."
        ),
        YouTubeEducationalTip(
            title = "قانون ۵ ثانیه مکالمه بدون ترجمه:",
            description = "برای روان‌تر شدن کلام، کلمات کلیدی را سریع بگویید و اصلاح ساختار را به دیپ‌سیک بسپارید."
        ),
        YouTubeEducationalTip(
            title = "یادگیری عبارت‌های هم‌آیند (Collocations):",
            description = "به جای کلمات منفرد، عبارت‌های ۲ الی ۳ کلمه‌ای مثل 'make progress' یا 'take notes' را تمرین کنید."
        ),
        YouTubeEducationalTip(
            title = "پاسخ‌دهی فعال با دیپ‌سیک:",
            description = "در هر سناریو، سوالات گرامری یا ساختار جملات را بلافاصله از پنجره شناور دیپ‌سیک بپرسید."
        )
    )

    val filteredScenarios = remember(selectedCategory, allScenarios) {
        allScenarios.filter {
            selectedCategory == "همه" || it.category == selectedCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "سناریوهای تمرین مکالمه",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textDirection = TextDirection.Rtl
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textDirection = TextDirection.Rtl
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Scenarios List (placed at top) & Educational Tips at bottom
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // YouTube Channel Live Sync Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scenarios_sync_status_card")
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
                                    imageVector = Icons.Default.Subscriptions,
                                    contentDescription = null,
                                    tint = Color(0xFFFF0000),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "سناریوهای متصل به کانال یوتیوب (@adibilearnskills)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        textDirection = TextDirection.Rtl
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$syncMessage (${allScenarios.size} سناریوی فعال)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        textDirection = TextDirection.Rtl
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            val refreshTint = if (isSyncing) disabledColor else primaryColor

                            IconButton(
                                onClick = {
                                    YouTubeSyncRepository.syncLatestContent(coroutineScope)
                                },
                                enabled = !isSyncing,
                                modifier = Modifier.size(32.dp).testTag("refresh_scenarios_feed_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "بروزرسانی سناریوها",
                                    tint = refreshTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Scenarios FIRST - shifted higher up as requested
                items(filteredScenarios) { scenario ->
                    ScenarioCard(
                        scenario = scenario,
                        onSelectScenario = onSelectScenario
                    )
                }

                // Educational YouTube Tips Card placed below scenarios
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("youtube_educational_tips_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF0000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Subscriptions,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "نکات طلایی ویدیوهای آموزش یوتیوب",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            textDirection = TextDirection.Rtl
                                        ),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "متدولوژی اختصاصی خانم زهرا ادیبی (@AdibiLearnSkills)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            textDirection = TextDirection.Rtl
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            youtubeTips.forEach { tip ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = tip.title,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                textDirection = TextDirection.Rtl
                                            ),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = tip.description,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                textDirection = TextDirection.Rtl
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = openYouTubeChannel,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF0000),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "مشاهده آموزش‌های کانال یوتیوب (@adibilearnskills)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        textDirection = TextDirection.Rtl
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
