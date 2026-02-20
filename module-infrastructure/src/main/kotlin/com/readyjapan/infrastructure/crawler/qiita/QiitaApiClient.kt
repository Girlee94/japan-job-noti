package com.readyjapan.infrastructure.crawler.qiita

import com.readyjapan.core.common.exception.ExternalApiException
import com.readyjapan.infrastructure.crawler.qiita.dto.QiitaItemResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

/**
 * Qiita API v2 클라이언트
 * 인증 없이 시간당 60회, access token 사용 시 1000회 요청 가능
 */
@Component
@EnableConfigurationProperties(QiitaProperties::class)
class QiitaApiClient(
    private val properties: QiitaProperties,
    webClientBuilder: WebClient.Builder
) {
    private val apiClient: WebClient = webClientBuilder
        .baseUrl("https://qiita.com")
        .defaultHeader(HttpHeaders.ACCEPT, "application/json")
        .apply {
            if (properties.accessToken.isNotBlank()) {
                it.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${properties.accessToken}")
            }
        }
        .build()

    /**
     * 태그로 기사 검색
     *
     * @param tag 검색할 태그명
     * @param page 페이지 번호 (1부터 시작)
     * @param perPage 페이지당 개수 (최대 100)
     */
    fun getItemsByTag(
        tag: String,
        page: Int = 1,
        perPage: Int = properties.perPage
    ): Mono<List<QiitaItemResponse>> {
        if (!properties.enabled) {
            logger.info { "Qiita API is disabled" }
            return Mono.just(emptyList())
        }

        val uri = "/api/v2/items?query=tag:$tag&page=$page&per_page=$perPage"

        logger.debug { "Fetching Qiita items: $uri" }

        return apiClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<QiitaItemResponse>>() {})
            .doOnNext { items ->
                logger.info { "Fetched ${items.size} items from Qiita tag:$tag" }
            }
            .onErrorResume { e ->
                when (e) {
                    is WebClientResponseException -> {
                        logger.error {
                            "Qiita API error: ${e.statusCode} - ${e.responseBodyAsString}"
                        }
                        when (e.statusCode.value()) {
                            403 -> logger.warn { "Qiita API access forbidden" }
                            429 -> logger.warn { "Qiita API rate limit exceeded" }
                        }
                    }
                    else -> logger.error(e) { "Qiita API request failed" }
                }
                Mono.error(
                    ExternalApiException("Qiita", "Failed to fetch tag:$tag: ${e.message}", e)
                )
            }
    }

    /**
     * API 활성화 여부 확인
     */
    fun isEnabled(): Boolean = properties.enabled
}
