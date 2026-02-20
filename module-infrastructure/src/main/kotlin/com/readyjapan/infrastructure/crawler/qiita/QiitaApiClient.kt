package com.readyjapan.infrastructure.crawler.qiita

import com.readyjapan.core.common.exception.ExternalApiException
import com.readyjapan.infrastructure.crawler.config.CrawlerConfig
import com.readyjapan.infrastructure.crawler.qiita.dto.QiitaItemResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelOption
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Qiita API v2 클라이언트
 * 인증 없이 시간당 60회, access token 사용 시 1000회 요청 가능
 */
@Component
@EnableConfigurationProperties(QiitaProperties::class)
class QiitaApiClient(
    private val properties: QiitaProperties,
    crawlerConfig: CrawlerConfig,
    webClientBuilder: WebClient.Builder
) {
    companion object {
        private const val MAX_IN_MEMORY_SIZE_BYTES = 2 * 1024 * 1024 // 2 MB — Qiita 기사 본문 포함 응답 대응
    }

    private val apiClient: WebClient = webClientBuilder
        .clone()
        .baseUrl("https://qiita.com")
        .defaultHeader(HttpHeaders.ACCEPT, "application/json")
        .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES) }
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (crawlerConfig.timeoutSeconds * 1000).toInt())
                    .responseTimeout(Duration.ofSeconds(crawlerConfig.timeoutSeconds))
            )
        )
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

        logger.debug { "Fetching Qiita items: tag=$tag, page=$page, perPage=$perPage" }

        return apiClient.get()
            .uri { builder ->
                builder
                    .path("/api/v2/items")
                    .queryParam("query", "tag:$tag")
                    .queryParam("page", page)
                    .queryParam("per_page", perPage)
                    .build()
            }
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
