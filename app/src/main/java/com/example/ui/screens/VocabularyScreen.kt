package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VocabularyItem
import com.example.ui.components.AddWordDialog
import com.example.ui.components.TtsHelper
import com.example.ui.components.VocabularyCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.VocabularyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    viewModel: VocabularyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ttsHelper = remember { TtsHelper(context) }

    val vocabularyList by viewModel.vocabularyList.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val masteredCount by viewModel.masteredCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var isFlashcardMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "گنجینه واژگان (جعبه لغت)",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textDirection = TextDirection.Rtl
                        )
                    )
                },
                actions = {
                    if (vocabularyList.isNotEmpty()) {
                        IconButton(onClick = { isFlashcardMode = !isFlashcardMode }) {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = "حالت فلش‌کارت",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن لغت")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stats Header Banner
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalCount",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "کل واژگان ذخیره‌شده",
                            style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Rtl)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$masteredCount",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        )
                        Text(
                            text = "مسلط شده",
                            style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Rtl)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val percentage = if (totalCount > 0) (masteredCount * 100 / totalCount) else 0
                        Text(
                            text = "$percentage٪",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                        Text(
                            text = "میزان پیشرفت",
                            style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Rtl)
                        )
                    }
                }
            }

            // Search & Filter
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = {
                        Text(
                            text = "جستجوی واژه یا معنی...",
                            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Rtl)
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips by Mastery Level
                val filterOptions = listOf("همه", "نوآموز", "آشنا", "پیشرفته", "مسلط")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterOptions) { option ->
                        val isSelected = selectedFilter == option
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.clickable { viewModel.onFilterSelect(option) }
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textDirection = TextDirection.Rtl
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Flashcard Quiz Mode View OR Vocabulary List
            if (isFlashcardMode && vocabularyList.isNotEmpty()) {
                FlashcardQuizSection(
                    items = vocabularyList,
                    onSpeak = { ttsHelper.speak(it) },
                    onCloseQuiz = { isFlashcardMode = false }
                )
            } else if (vocabularyList.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "هنوز واژه‌ای به گنجینه اضافه نشده است",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDirection = TextDirection.Rtl
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "در حین گفتگو با پارتنر هوش مصنوعی، واژه‌های جدید را دکمه '+' بزنید یا با دکمه دایره پایین دستی اضافه کنید.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = TextDirection.Rtl,
                                textAlign = TextAlign.Center
                            ),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vocabularyList, key = { it.id }) { vocab ->
                        VocabularyCard(
                            item = vocab,
                            onSpeak = { ttsHelper.speak(it) },
                            onToggleMastered = { viewModel.toggleMastered(it) },
                            onUpdateMasteryLevel = { item, level -> viewModel.updateMasteryLevel(item, level) },
                            onDelete = { viewModel.deleteWord(it) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddWordDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { word, definition, meaning, example, category, masteryLevel ->
                    viewModel.addNewWord(word, definition, meaning, example, category, masteryLevel)
                }
            )
        }
    }
}

@Composable
fun FlashcardQuizSection(
    items: List<VocabularyItem>,
    onSpeak: (String) -> Unit,
    onCloseQuiz: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    val currentItem = items.getOrNull(currentIndex) ?: items.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "کارت ${currentIndex + 1} از ${items.size}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textDirection = TextDirection.Rtl
                )
            )
            Button(onClick = onCloseQuiz) {
                Text("خروج از تمرین")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clickable { showAnswer = !showAnswer }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!showAnswer) {
                        Text(
                            text = currentItem.word,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                textDirection = TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (currentItem.phonetic.isNotBlank()) {
                            Text(
                                text = currentItem.phonetic,
                                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "برای دیدن معنی، روی کارت لمس کنید 👆",
                            style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Rtl),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = currentItem.persianMeaning,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                textDirection = TextDirection.Rtl
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (currentItem.definition.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Def: ${currentItem.definition}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    textDirection = TextDirection.Ltr
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (currentItem.exampleSentence.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Example: ${currentItem.exampleSentence}",
                                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    onSpeak(currentItem.word)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("تلفظ صوتی 🔊")
            }

            Button(
                onClick = {
                    showAnswer = false
                    currentIndex = (currentIndex + 1) % items.size
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("کارت بعدی ➡️")
            }
        }
    }
}
