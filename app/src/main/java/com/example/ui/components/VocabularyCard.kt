package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MasteryLevel
import com.example.data.model.VocabularyItem
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess

@Composable
fun VocabularyCard(
    item: VocabularyItem,
    onSpeak: (String) -> Unit,
    onToggleMastered: (VocabularyItem) -> Unit,
    onUpdateMasteryLevel: (VocabularyItem, Int) -> Unit,
    onDelete: (VocabularyItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isMastered || item.masteryLevel >= 4) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Word, Phonetic, Meaning and Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onToggleMastered(item) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isMastered || item.masteryLevel >= 4) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "وضعیت تسلط",
                            tint = if (item.isMastered || item.masteryLevel >= 4) EmeraldSuccess else MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.word,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    textDirection = TextDirection.Ltr
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (item.phonetic.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.phonetic,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        textDirection = TextDirection.Ltr
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (item.targetBandScore.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "Band ${item.targetBandScore}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (item.persianMeaning.isNotBlank()) {
                            Text(
                                text = item.persianMeaning,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDirection = TextDirection.Rtl
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onSpeak(item.word) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "تلفظ صوتی",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { onDelete(item) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف واژه",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Definition Section (English IELTS Definition)
            if (item.definition.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = "IELTS Definition:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textDirection = TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = item.definition,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                textDirection = TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Example Sentence Section
            if (item.exampleSentence.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Example: ${item.exampleSentence}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                textDirection = TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.examplePersian.isNotBlank()) {
                            Text(
                                text = item.examplePersian,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    textDirection = TextDirection.Rtl
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Mastery Level Row & Category Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Mastery Level Stars / Indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "سطح تسلط: ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            textDirection = TextDirection.Rtl
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    (1..4).forEach { starLevel ->
                        val isFilled = starLevel <= item.masteryLevel
                        Icon(
                            imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = "سطح $starLevel",
                            tint = if (isFilled) AmberWarning else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onUpdateMasteryLevel(item, starLevel) }
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    val mastery = item.masteryEnum
                    Text(
                        text = "(${mastery.titleFa})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textDirection = TextDirection.Rtl
                        ),
                        color = if (item.masteryLevel >= 4) EmeraldSuccess else MaterialTheme.colorScheme.primary
                    )
                }

                // Category Chip
                if (item.category.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Rtl),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
