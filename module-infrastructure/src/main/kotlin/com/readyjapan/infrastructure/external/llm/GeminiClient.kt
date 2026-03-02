package com.readyjapan.infrastructure.external.llm

import com.readyjapan.infrastructure.external.llm.dto.GeminiContent
import com.readyjapan.infrastructure.external.llm.dto.GeminiGenerateRequest
import com.readyjapan.infrastructure.external.llm.dto.GeminiGenerateResponse
import com.readyjapan.infrastructure.external.llm.dto.GeminiGenerationConfig
import com.readyjapan.infrastructure.external.llm.dto.GeminiPart
import com.readyjapan.infrastructure.external.llm.dto.GeminiSystemInstruction
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Duration

/**
 * Google Gemini API 클라이언트
 */
@Component
@ConditionalOnProperty(name = ["app.llm.provider"], havingValue = "gemini", matchIfMissing = true)
class GeminiClient(
    private val llmProperties: LlmProperties,
    webClientBuilder: WebClient.Builder
) : LlmClient {
    private val log = KotlinLogging.logger {}

    private val webClient: WebClient = webClientBuilder
        .baseUrl("https://generativelanguage.googleapis.com/v1beta")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

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

        val request = GeminiGenerateRequest(
            contents = listOf(GeminiContent.user(userPrompt)),
            systemInstruction = GeminiSystemInstruction(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = temperature ?: llmProperties.temperature,
                maxOutputTokens = maxTokens ?: llmProperties.maxTokens
            )
        )

        return try {
            val response = retryOnTransientError(operationName = "Gemini") {
                webClient.post()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/models/{model}:generateContent")
                            .build(mapOf("model" to llmProperties.model))
                    }
                    .header("x-goog-api-key", llmProperties.apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono<GeminiGenerateResponse>()
                    .timeout(Duration.ofSeconds(llmProperties.timeoutSeconds))
                    .block()
            }

            val content = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (content != null) {
                log.debug { "Gemini API response received, tokens used: ${response.usageMetadata?.totalTokenCount}" }
            } else {
                log.warn { "Gemini API returned empty content" }
            }

            content
        } catch (e: Exception) {
            log.error(e) { "Failed to call Gemini API: ${e.message}" }
            null
        }
    }
}
