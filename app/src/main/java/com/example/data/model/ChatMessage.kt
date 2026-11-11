package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scenarioId: String,
    val sender: String, // "USER" or "AI"
    val text: String,
    val translationPersian: String = "",
    val grammarCorrection: String = "",
    val keyWordsJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
