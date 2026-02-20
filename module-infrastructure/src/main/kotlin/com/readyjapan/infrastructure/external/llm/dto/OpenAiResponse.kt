package com.readyjapan.infrastructure.external.llm.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * OpenAI Chat Completion 응답
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiChatResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiChoice(
    val index: Int,
    val message: OpenAiResponseMessage,
    @JsonProperty("finish_reason")
    val finishReason: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiResponseMessage(
    val role: String,
    val content: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiUsage(
    @JsonProperty("prompt_tokens")
    val promptTokens: Int,
    @JsonProperty("completion_tokens")
    val completionTokens: Int,
    @JsonProperty("total_tokens")
    val totalTokens: Int
)
