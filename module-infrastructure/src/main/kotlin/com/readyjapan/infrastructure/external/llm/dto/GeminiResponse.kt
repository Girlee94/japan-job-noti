package com.readyjapan.infrastructure.external.llm.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Gemini generateContent API 응답 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null,
    val usageMetadata: GeminiUsageMetadata? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeminiCandidate(
    val content: GeminiResponseContent? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeminiResponseContent(
    val parts: List<GeminiResponsePart>? = null,
    val role: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeminiResponsePart(
    val text: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeminiUsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null
)
