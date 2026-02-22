package com.readyjapan.infrastructure.external.llm.dto

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Gemini generateContent API 요청 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
) {
    companion object {
        fun user(text: String) = GeminiContent("user", listOf(GeminiPart(text)))
    }
}

data class GeminiSystemInstruction(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeminiGenerationConfig(
    val temperature: Double? = null,
    val maxOutputTokens: Int? = null
)
