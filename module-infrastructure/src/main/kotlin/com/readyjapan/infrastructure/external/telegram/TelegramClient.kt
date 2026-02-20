package com.readyjapan.infrastructure.external.telegram

import com.readyjapan.core.common.exception.ExternalApiException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Component
@EnableConfigurationProperties(TelegramProperties::class)
class TelegramClient(
    private val properties: TelegramProperties,
    webClientBuilder: WebClient.Builder
) {
    private val webClient: WebClient = webClientBuilder
        .baseUrl("https://api.telegram.org")
        .build()

    /**
     * 텔레그램 메시지 전송
     * @param message 전송할 메시지 (Markdown 형식 지원)
     * @return 전송 성공 여부
     */
    fun sendMessage(message: String): Mono<Boolean> {
        if (!properties.enabled) {
            logger.info { "Telegram is disabled. Skipping message send." }
            return Mono.just(true)
        }

        if (properties.botToken.isBlank() || properties.chatId.isBlank()) {
            logger.warn { "Telegram bot token or chat ID is not configured." }
            return Mono.just(false)
        }

        return webClient.post()
            .uri("/bot${properties.botToken}/sendMessage")
            .bodyValue(
                mapOf(
                    "chat_id" to properties.chatId,
                    "text" to message,
                    "parse_mode" to "Markdown"
                )
            )
            .retrieve()
            .bodyToMono(Map::class.java)
            .map { response ->
                val ok = response["ok"] as? Boolean ?: false
                if (ok) {
                    logger.info { "Telegram message sent successfully" }
                } else {
                    logger.warn { "Telegram API returned ok=false: $response" }
                }
                ok
            }
            .onErrorResume { e ->
                when (e) {
                    is WebClientResponseException -> {
                        logger.error(e) { "Telegram API error: ${e.statusCode} - ${e.responseBodyAsString}" }
                    }
                    else -> {
                        logger.error(e) { "Failed to send Telegram message" }
                    }
                }
                Mono.error(ExternalApiException("Telegram", e.message, e))
            }
    }

    /**
     * 텔레그램 메시지 동기 전송 (블로킹)
     * 배치 작업에서 사용
     */
    fun sendMessageSync(message: String): Boolean {
        return try {
            sendMessage(message).block() ?: false
        } catch (e: Exception) {
            logger.error(e) { "Failed to send Telegram message synchronously" }
            false
        }
    }
}
