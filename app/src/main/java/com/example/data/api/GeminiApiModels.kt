package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRawRequest(
    @Json(name = "contents") val contents: List<GeminiRawContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiRawConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiRawContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRawConfig(
    @Json(name = "temperature") val temperature: Float? = 0.7f,
    @Json(name = "responseMimeType") val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRawContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiRawPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRawPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiRawResponse(
    @Json(name = "candidates") val candidates: List<GeminiRawCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiRawCandidate(
    @Json(name = "content") val content: GeminiRawContent?
)
