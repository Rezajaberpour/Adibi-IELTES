package com.example.data.model

data class PracticeScenario(
    val id: String,
    val titlePersian: String,
    val titleEnglish: String,
    val category: String,
    val targetLanguage: String = "انگلیسی",
    val targetLanguageCode: String = "en",
    val difficultyLevel: String = "B1 (متوسط)",
    val descriptionPersian: String,
    val initialAiGreeting: String,
    val initialAiTranslation: String,
    val systemPromptRole: String,
    val keyPhrases: List<Pair<String, String>> = emptyList()
)
