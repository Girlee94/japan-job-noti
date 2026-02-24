package com.readyjapan.infrastructure.crawler.qiita

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CrawlHistoryRepository
import com.readyjapan.core.domain.repository.CrawlSourceRepository
import com.readyjapan.infrastructure.crawler.config.CrawlerConfig
import com.readyjapan.infrastructure.crawler.qiita.dto.QiitaItemResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

private val logger = KotlinLogging.logger {}

/**
 * Qiita 크롤링 서비스
 * Qiita API v2를 통해 태그별 기사를 수집하고 저장합니다.
 *
 * 트랜잭션 관리는 QiitaItemPersistenceService에 위임하며,
 * 이 클래스 자체는 트랜잭션을 선언하지 않는다 (HTTP 호출이 포함되어 있으므로).
 */
@Service
class QiitaCrawlerService(
    private val qiitaApiClient: QiitaApiClient,
    private val qiitaProperties: QiitaProperties,
    private val crawlSourceRepository: CrawlSourceRepository,
    private val crawlHistoryRepository: CrawlHistoryRepository,
    private val qiitaItemPersistenceService: QiitaItemPersistenceService,
    private val crawlerConfig: CrawlerConfig
) {
    companion object {
        private val OBJECT_MAPPER = jacksonObjectMapper()
        private const val API_TIMEOUT_SECONDS = 30L
        private val JST = ZoneId.of("Asia/Tokyo")
    }

    /**
     * 모든 활성화된 Qiita 소스 크롤링
     */
    fun crawlAllSources(): List<CrawlHistory> {
        if (!qiitaApiClient.isEnabled()) {
            logger.warn { "Qiita API is not enabled" }
            return emptyList()
        }

        val sources = crawlSourceRepository.findEnabledBySourceTypeAndPlatform(
            SourceType.COMMUNITY, CommunityPlatform.QIITA
        )

        if (sources.isEmpty()) {
            logger.info { "No active Qiita sources found" }
            return emptyList()
        }

        logger.info { "Starting Qiita crawl for ${sources.size} sources" }

        return sources.map { source ->
            crawlSource(source)
        }
    }

    /**
     * 특정 소스 크롤링
     * HTTP 호출은 트랜잭션 밖에서 수행하고, DB 저장은 PersistenceService를 통해 별도 트랜잭션에서 처리
     */
    fun crawlSource(source: CrawlSource): CrawlHistory {
        val history = CrawlHistory.start(source)
        crawlHistoryRepository.save(history)

        try {
            val config = parseSourceConfig(source.config)
            val tags = parseTags(config)
            val limit = (config["limit"] as? Number)?.toInt() ?: qiitaProperties.perPage

            // 날짜 필터링 이중화 전략:
            // - server-side: Qiita API query(`created:>YYYY-MM-DD`)로 불필요한 데이터 전송량 절감
            // - client-side: 서버/클라이언트 시간 동기화 오차 및 날짜 단위 필터의 시간 정밀도 한계 보완
            val freshnessDays = (crawlerConfig.freshnessHours + 23) / 24
            val createdAfter = LocalDate.now(JST).minusDays(freshnessDays)

            logger.info { "Crawling Qiita tags: $tags (limit: $limit per tag, createdAfter: $createdAfter)" }

            // HTTP 호출 (트랜잭션 밖에서 수행)
            val allItems = mutableListOf<QiitaItemResponse>()
            for (tag in tags) {
                val items = try {
                    qiitaApiClient.getItemsByTag(tag, page = 1, perPage = limit, createdAfter = createdAfter)
                        .block(Duration.ofSeconds(API_TIMEOUT_SECONDS))
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to fetch items for tag: $tag" }
                    null
                }

                if (items != null) {
                    allItems.addAll(items)
                } else {
                    logger.warn { "No items returned for tag: $tag (timeout or null response)" }
                }

                // Rate limit 방지 딜레이
                Thread.sleep(qiitaProperties.requestDelayMs)
            }

            // 중복 제거 (같은 기사가 여러 태그에 걸릴 수 있음)
            val uniqueItems = allItems.distinctBy { it.id }

            // client-side 날짜 필터: 시간 수준 정밀도 검증
            // server-side는 날짜 단위(YYYY-MM-DD)이므로 시간 경계 근처 아이템이 포함될 수 있음
            val cutoffInstant = Instant.now().minus(Duration.ofHours(crawlerConfig.freshnessHours))
            val freshItems = uniqueItems.filter { item ->
                try {
                    val itemInstant = OffsetDateTime.parse(item.createdAt).toInstant()
                    itemInstant.isAfter(cutoffInstant)
                } catch (e: Exception) {
                    // Fail-open 전략: 파싱 실패 시 아이템을 포함하여 데이터 손실을 방지한다.
                    // PersistenceService의 중복 검사(externalId)가 이미 중복 저장을 방지하므로,
                    // 포함해도 사이드 이펙트가 없으며 제외 시 유효한 데이터를 잃을 위험이 있다.
                    logger.warn(e) { "Failed to parse createdAt '${item.createdAt}' for item ${item.id}, including by default" }
                    true
                }
            }

            // DB 저장 (별도 서비스를 통해 트랜잭션 내에서 수행)
            val (savedCount, updatedCount) = qiitaItemPersistenceService.saveCrawledItems(source, freshItems)

            history.complete(
                itemsFound = freshItems.size,
                itemsSaved = savedCount,
                itemsUpdated = updatedCount
            )

            logger.info {
                "Crawl completed for Qiita: " +
                        "found=${freshItems.size}, saved=$savedCount, updated=$updatedCount"
            }

        } catch (e: Exception) {
            logger.error(e) { "Crawl failed for source: ${source.name}" }
            history.fail(e.message ?: "Unknown error")
        }

        return crawlHistoryRepository.save(history)
    }

    /**
     * 소스 설정에서 태그 목록 파싱
     */
    private fun parseTags(config: Map<String, Any>): List<String> {
        val configTags = config["tags"]
        if (configTags is List<*>) {
            return configTags.filterIsInstance<String>()
        }
        return qiitaProperties.tags
    }

    /**
     * 소스 설정 파싱
     */
    private fun parseSourceConfig(configJson: String?): Map<String, Any> {
        if (configJson.isNullOrBlank()) {
            return emptyMap()
        }

        return try {
            @Suppress("UNCHECKED_CAST")
            OBJECT_MAPPER.readValue(configJson, Map::class.java) as Map<String, Any>
        } catch (e: Exception) {
            logger.warn { "Failed to parse source config: ${e.message}" }
            emptyMap()
        }
    }
}
