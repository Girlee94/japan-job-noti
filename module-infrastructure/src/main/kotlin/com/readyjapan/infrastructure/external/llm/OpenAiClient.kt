package com.readyjapan.infrastructure.external.llm

import com.readyjapan.infrastructure.external.llm.dto.OpenAiChatRequest
import com.readyjapan.infrastructure.external.llm.dto.OpenAiChatResponse
import com.readyjapan.infrastructure.external.llm.dto.OpenAiMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Duration

/**
 * OpenAI API 클라이언트
 */
@Component
@ConditionalOnProperty(name = ["app.llm.provider"], havingValue = "openai")
class OpenAiClient(
    private val llmProperties: LlmProperties,
    webClientBuilder: WebClient.Builder
) : LlmClient {
    private val log = KotlinLogging.logger {}

    private val webClient: WebClient = webClientBuilder
        .baseUrl("https://api.openai.com/v1")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    /**
     * Chat Completion API 호출
     */
    override fun chatCompletion(
        systemPrompt: String,
        userPrompt: String,
        temperature: Double?,
        maxTokens: Int?
    ): String? {
        if (!llmProperties.enabled) {
            log.warn { "LLM API is disabled" }
            return null
        }

        val messages = listOf(
            OpenAiMessage.system(systemPrompt),
            OpenAiMessage.user(userPrompt)
        )

        val request = OpenAiChatRequest(
            model = llmProperties.model,
            messages = messages,
            temperature = temperature ?: llmProperties.temperature,
            maxTokens = maxTokens ?: llmProperties.maxTokens
        )

        return try {
            val response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${llmProperties.apiKey}")
                .bodyValue(request)
                .retrieve()
                .bodyToMono<OpenAiChatResponse>()
                .timeout(Duration.ofSeconds(llmProperties.timeoutSeconds))
                .block()

            val content = response?.choices?.firstOrNull()?.message?.content

            if (content != null) {
                log.debug { "OpenAI API response received, tokens used: ${response.usage?.totalTokens}" }
            } else {
                log.warn { "OpenAI API returned empty content" }
            }

            content
        } catch (e: Exception) {
            log.error(e) { "Failed to call OpenAI API: ${e.message}" }
            null
        }
    }
}
