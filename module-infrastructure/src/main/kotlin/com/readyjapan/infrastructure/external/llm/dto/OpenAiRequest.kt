package com.readyjapan.infrastructure.external.llm.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * OpenAI Chat Completion 요청
 */
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.3,
    @JsonProperty("max_tokens")
    val maxTokens: Int = 2000
)

data class OpenAiMessage(
    val role: String,
    val content: String
) {
    companion object {
        fun system(content: String) = OpenAiMessage("system", content)
        fun user(content: String) = OpenAiMessage("user", content)
        fun assistant(content: String) = OpenAiMessage("assistant", content)
    }
}
