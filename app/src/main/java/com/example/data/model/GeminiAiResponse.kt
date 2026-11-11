package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiAiResponse(
    val replyText: String = "",
    val translationPersian: String = "",
    val grammarCorrection: String = "",
    val suggestedReplies: List<String> = emptyList(),
    val keyVocabulary: List<KeyWord> = emptyList()
)

@JsonClass(generateAdapter = true)
data class KeyWord(
    val word: String = "",
    val phonetic: String = "",
    val persianMeaning: String = "",
    val example: String = ""
)
