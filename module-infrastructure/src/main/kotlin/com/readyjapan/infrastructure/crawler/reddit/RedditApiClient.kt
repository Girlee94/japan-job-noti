package com.readyjapan.infrastructure.crawler.reddit

import com.readyjapan.core.common.exception.ExternalApiException
import com.readyjapan.infrastructure.crawler.reddit.dto.RedditAuthResponse
import com.readyjapan.infrastructure.crawler.reddit.dto.RedditListingResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Reddit API 클라이언트
 * OAuth2 인증 및 API 호출을 담당합니다.
 */
@Component
@EnableConfigurationProperties(RedditProperties::class)
class RedditApiClient(
    private val properties: RedditProperties,
    webClientBuilder: WebClient.Builder
) {
    private val authClient: WebClient = webClientBuilder
        .baseUrl("https://www.reddit.com")
        .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent)
        .build()

    private val apiClient: WebClient = webClientBuilder
        .baseUrl("https://oauth.reddit.com")
        .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent)
        .build()

    // 토큰 캐시
    private var cachedToken: String? = null
    private var tokenExpiresAt: Instant? = null

    /**
     * OAuth2 액세스 토큰 획득
     * Application-only (script) 방식 사용
     */
    private fun getAccessToken(): Mono<String> {
        // 캐시된 토큰이 유효하면 재사용
        val now = Instant.now()
        if (cachedToken != null && tokenExpiresAt != null && now.isBefore(tokenExpiresAt)) {
            logger.debug { "Using cached Reddit access token" }
            return Mono.just(cachedToken!!)
        }

        if (properties.clientId.isBlank() || properties.clientSecret.isBlank()) {
            return Mono.error(ExternalApiException("Reddit", "Client ID or Secret is not configured"))
        }

        val credentials = Base64.getEncoder()
            .encodeToString("${properties.clientId}:${properties.clientSecret}".toByteArray())

        logger.debug { "Requesting new Reddit access token" }

        return authClient.post()
            .uri("/api/v1/access_token")
            .header(HttpHeaders.AUTHORIZATION, "Basic $credentials")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters.fromFormData("grant_type", "client_credentials")
            )
            .retrieve()
            .bodyToMono(RedditAuthResponse::class.java)
            .map { response ->
                cachedToken = response.accessToken
                // 만료 5분 전에 갱신하도록 설정
                tokenExpiresAt = Instant.now().plusSeconds(response.expiresIn - 300)
                logger.info { "Reddit access token acquired, expires in ${response.expiresIn}s" }
                response.accessToken
            }
            .onErrorMap { e ->
                logger.error(e) { "Failed to acquire Reddit access token" }
                ExternalApiException("Reddit Auth", e.message, e)
            }
    }

    /**
     * 서브레딧의 최신 게시물 조회
     *
     * @param subreddit 서브레딧 이름 (예: "japanlife")
     * @param sort 정렬 방식 (new, hot, top, rising)
     * @param limit 조회 개수 (최대 100)
     * @param after 페이지네이션 커서
     */
    fun getSubredditPosts(
        subreddit: String,
        sort: String = "new",
        limit: Int = 50,
        after: String? = null
    ): Mono<RedditListingResponse> {
        if (!properties.enabled) {
            logger.info { "Reddit API is disabled" }
            return Mono.empty()
        }

        return getAccessToken()
            .flatMap { token ->
                val uri = buildString {
                    append("/r/$subreddit/$sort")
                    append("?limit=$limit")
                    append("&raw_json=1") // HTML 엔티티 디코딩
                    after?.let { append("&after=$it") }
                }

                logger.debug { "Fetching Reddit posts: $uri" }

                apiClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .bodyToMono(RedditListingResponse::class.java)
            }
            .doOnNext { response ->
                logger.info {
                    "Fetched ${response.data.children.size} posts from r/$subreddit"
                }
            }
            .onErrorResume { e ->
                when (e) {
                    is WebClientResponseException -> {
                        logger.error {
                            "Reddit API error: ${e.statusCode} - ${e.responseBodyAsString}"
                        }
                        when (e.statusCode.value()) {
                            401 -> {
                                // 토큰 만료 시 캐시 무효화
                                cachedToken = null
                                tokenExpiresAt = null
                            }
                            403 -> logger.warn { "Reddit API access forbidden for r/$subreddit" }
                            404 -> logger.warn { "Subreddit not found: r/$subreddit" }
                            429 -> logger.warn { "Reddit API rate limit exceeded" }
                        }
                    }
                    else -> logger.error(e) { "Reddit API request failed" }
                }
                Mono.error(ExternalApiException("Reddit", "Failed to fetch r/$subreddit: ${e.message}", e))
            }
    }

    /**
     * 여러 서브레딧의 게시물을 순차적으로 조회
     *
     * @param subreddits 서브레딧 목록
     * @param sort 정렬 방식
     * @param limit 서브레딧당 조회 개수
     */
    fun getMultipleSubredditPosts(
        subreddits: List<String>,
        sort: String = "new",
        limit: Int = 50
    ): Mono<List<RedditListingResponse>> {
        if (subreddits.isEmpty()) {
            return Mono.just(emptyList())
        }

        return Mono.defer {
            var result = Mono.just(mutableListOf<RedditListingResponse>())

            for (subreddit in subreddits) {
                result = result.flatMap { list ->
                    getSubredditPosts(subreddit, sort, limit)
                        .delayElement(java.time.Duration.ofMillis(properties.requestDelayMs))
                        .map { response ->
                            list.add(response)
                            list
                        }
                        .onErrorResume { e ->
                            logger.warn { "Skipping r/$subreddit due to error: ${e.message}" }
                            Mono.just(list)
                        }
                }
            }

            result.map { it.toList() }
        }
    }

    /**
     * API 활성화 여부 확인
     */
    fun isEnabled(): Boolean = properties.enabled &&
            properties.clientId.isNotBlank() &&
            properties.clientSecret.isNotBlank()
}
