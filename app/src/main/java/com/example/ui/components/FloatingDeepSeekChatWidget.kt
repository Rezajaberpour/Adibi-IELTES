package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.DeepSeekChatMessage
import com.example.ui.theme.DeepSeekPurple
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.DeepSeekFloatingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingDeepSeekChatWidget(
    isOpen: Boolean = false,
    onClose: () -> Unit = {},
    viewModel: DeepSeekFloatingViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var showVpnWarning by remember { mutableStateOf(false) }

    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedEngine by viewModel.selectedEngine.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val suggestedQuestions = listOf(
        "تفاوت Look و See چیست؟",
        "توضیح زمان حال کامل با مثال",
        "اصطلاحات احوالپرسی نیتیو",
        "اصلاح گرامر: I have went to school"
    )

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 3-second VPN warning timer on activation
    LaunchedEffect(isOpen) {
        if (isOpen) {
            showVpnWarning = true
            kotlinx.coroutines.delay(3000L)
            showVpnWarning = false
        }
    }

    // Waterfall Dropdown Chat Window Overlay - Cascades from top bar down to bottom navigation
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .shadow(12.dp, shape = RoundedCornerShape(20.dp))
                .testTag("floating_deepseek_window"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar - Contains ChatGPT & DeepSeek selector
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    DeepSeekPurple,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "دستیار هوشمند چت‌جی‌پی‌تی و دیپ‌سیک",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = Color.White,
                                textDirection = TextDirection.Rtl
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { viewModel.clearChat() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "پاک کردن",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Model Selection Toggle Row: ChatGPT 4o vs DeepSeek AI
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "انتخاب مدل:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                textDirection = TextDirection.Rtl
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // ChatGPT Button
                        Surface(
                            onClick = { viewModel.setEngine(com.example.data.repository.AiEngine.CHATGPT) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedEngine == com.example.data.repository.AiEngine.CHATGPT)
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (selectedEngine == com.example.data.repository.AiEngine.CHATGPT) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 6.dp)
                            ) {
                                Text(
                                    text = "🤖 ChatGPT 4o",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selectedEngine == com.example.data.repository.AiEngine.CHATGPT)
                                            Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        // DeepSeek Button
                        Surface(
                            onClick = { viewModel.setEngine(com.example.data.repository.AiEngine.DEEPSEEK) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedEngine == com.example.data.repository.AiEngine.DEEPSEEK)
                                DeepSeekPurple else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (selectedEngine == com.example.data.repository.AiEngine.DEEPSEEK) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 6.dp)
                            ) {
                                Text(
                                    text = "⚡ DeepSeek AI",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selectedEngine == com.example.data.repository.AiEngine.DEEPSEEK)
                                            Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                // 3-Second Temporary VPN Warning Box upon DeepSeek Activation
                AnimatedVisibility(
                    visible = showVpnWarning,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = Color(0xFFFFF3CD),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("deepseek_vpn_warning_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFC107)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚠️", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "هشدار: جهت اتصال و دریافت پاسخ دیپ‌سیک، فیلترشکن (VPN) باید روشن باشد.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF856404),
                                    textDirection = TextDirection.Rtl
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Suggested Questions horizontal row with guidance label
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "💡 پرسش‌های پیشنهادی دیپ‌سیک (برای ارسال لمس کنید):",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepSeekPurple,
                            textDirection = TextDirection.Rtl
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(suggestedQuestions) { question ->
                            Surface(
                                onClick = { viewModel.askQuestion(question) },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = DeepSeekPurple,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = question,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Rtl)
                                    )
                                }
                            }
                        }
                    }
                }

                // Chat messages LazyColumn - Enlarged Dialogue Area
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { item ->
                        val isUser = item.sender == "USER"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                ),
                                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.widthIn(max = 320.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            textDirection = TextDirection.Rtl
                                        ),
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (!isUser) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "کپی",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        clipboardManager.setText(AnnotatedString(item.text))
                                                        Toast.makeText(context, "کپی شد", Toast.LENGTH_SHORT).show()
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = DeepSeekPurple
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "دیپ‌سیک در حال پاسخگویی...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Input field bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "سوال خود را از دیپ‌سیک بپرسید...",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Rtl)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("deepseek_floating_input"),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepSeekPurple,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val query = inputText
                                inputText = ""
                                viewModel.askQuestion(query)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isLoading) DeepSeekPurple else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("deepseek_floating_send_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "ارسال",
                            tint = if (inputText.isNotBlank() && !isLoading) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

