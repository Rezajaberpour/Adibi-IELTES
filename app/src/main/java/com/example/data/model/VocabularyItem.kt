package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mastery level for IELTS vocabulary acquisition.
 */
enum class MasteryLevel(val level: Int, val titleEn: String, val titleFa: String) {
    NOVICE(1, "Novice", "نوآموز"),
    FAMILIAR(2, "Familiar", "آشنا"),
    PROFICIENT(3, "Proficient", "پیشرفته"),
    MASTERED(4, "Mastered", "مسلط");

    companion object {
        fun fromLevel(level: Int): MasteryLevel {
            return entries.find { it.level == level } ?: NOVICE
        }
    }
}

/**
 * Room Database entity representing an IELTS vocabulary item.
 * Includes word, definition, example sentence, and user's mastery level.
 */
@Entity(tableName = "vocabulary")
data class VocabularyItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val definition: String = "",
    val exampleSentence: String = "",
    val masteryLevel: Int = 1,
    val phonetic: String = "",
    val persianMeaning: String = "",
    val examplePersian: String = "",
    val targetLanguage: String = "English",
    val category: String = "IELTS Academic",
    val isMastered: Boolean = false,
    val targetBandScore: String = "7.0+",
    val createdAt: Long = System.currentTimeMillis()
) {
    val masteryEnum: MasteryLevel
        get() = MasteryLevel.fromLevel(masteryLevel)
}
