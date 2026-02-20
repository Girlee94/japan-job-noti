package com.readyjapan.infrastructure.external.llm

import com.readyjapan.infrastructure.external.llm.dto.OpenAiChatRequest
import com.readyjapan.infrastructure.external.llm.dto.OpenAiChatResponse
import com.readyjapan.infrastructure.external.llm.dto.OpenAiMessage
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Duration

/**
 * OpenAI API 클라이언트
 */
@Component
class OpenAiClient(
    private val llmProperties: LlmProperties,
    webClientBuilder: WebClient.Builder
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val webClient: WebClient = webClientBuilder
        .baseUrl("https://api.openai.com/v1")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    /**
     * Chat Completion API 호출
     */
    fun chatCompletion(
        systemPrompt: String,
        userPrompt: String,
        temperature: Double? = null,
        maxTokens: Int? = null
    ): String? {
        if (!llmProperties.enabled) {
            log.warn("LLM API is disabled")
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
                log.debug("OpenAI API response received, tokens used: ${response.usage?.totalTokens}")
            } else {
                log.warn("OpenAI API returned empty content")
            }

            content
        } catch (e: Exception) {
            log.error("Failed to call OpenAI API: ${e.message}", e)
            null
        }
    }

    /**
     * 여러 메시지로 Chat Completion API 호출
     */
    fun chatCompletionWithMessages(
        messages: List<OpenAiMessage>,
        temperature: Double? = null,
        maxTokens: Int? = null
    ): String? {
        if (!llmProperties.enabled) {
            log.warn("LLM API is disabled")
            return null
        }

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

            response?.choices?.firstOrNull()?.message?.content
        } catch (e: Exception) {
            log.error("Failed to call OpenAI API: ${e.message}", e)
            null
        }
    }
}
