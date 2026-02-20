package com.readyjapan.infrastructure.crawler.qiita

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CrawlHistoryRepository
import com.readyjapan.core.domain.repository.CrawlSourceRepository
import com.readyjapan.infrastructure.crawler.qiita.dto.QiitaItemResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Qiita 크롤링 서비스
 * Qiita API v2를 통해 태그별 기사를 수집하고 저장합니다.
 */
@Service
@Transactional(readOnly = true)
class QiitaCrawlerService(
    private val qiitaApiClient: QiitaApiClient,
    private val qiitaProperties: QiitaProperties,
    private val crawlSourceRepository: CrawlSourceRepository,
    private val crawlHistoryRepository: CrawlHistoryRepository,
    private val qiitaItemPersistenceService: QiitaItemPersistenceService
) {
    companion object {
        private val OBJECT_MAPPER = jacksonObjectMapper()
        private const val API_TIMEOUT_SECONDS = 30L
    }

    /**
     * 모든 활성화된 Qiita 소스 크롤링
     */
    fun crawlAllSources(): List<CrawlHistory> {
        if (!qiitaApiClient.isEnabled()) {
            logger.warn { "Qiita API is not enabled" }
            return emptyList()
        }

        val sources = crawlSourceRepository.findEnabledBySourceType(SourceType.COMMUNITY)
            .filter { it.platform == CommunityPlatform.QIITA }

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
    @Transactional
    fun crawlSource(source: CrawlSource): CrawlHistory {
        val history = CrawlHistory.start(source)
        crawlHistoryRepository.save(history)

        try {
            val config = parseSourceConfig(source.config)
            val tags = parseTags(config)
            val limit = (config["limit"] as? Number)?.toInt() ?: qiitaProperties.perPage

            logger.info { "Crawling Qiita tags: $tags (limit: $limit per tag)" }

            // HTTP 호출 (트랜잭션 밖에서 수행)
            val allItems = mutableListOf<QiitaItemResponse>()
            for (tag in tags) {
                val items = qiitaApiClient.getItemsByTag(tag, page = 1, perPage = limit)
                    .block(Duration.ofSeconds(API_TIMEOUT_SECONDS))

                if (items != null) {
                    allItems.addAll(items)
                }

                // Rate limit 방지 딜레이
                Thread.sleep(qiitaProperties.requestDelayMs)
            }

            // 중복 제거 (같은 기사가 여러 태그에 걸릴 수 있음)
            val uniqueItems = allItems.distinctBy { it.id }

            // DB 저장 (별도 서비스를 통해 트랜잭션 내에서 수행)
            val (savedCount, updatedCount) = qiitaItemPersistenceService.saveCrawledItems(source, uniqueItems)

            history.complete(
                itemsFound = uniqueItems.size,
                itemsSaved = savedCount,
                itemsUpdated = updatedCount
            )

            logger.info {
                "Crawl completed for Qiita: " +
                        "found=${uniqueItems.size}, saved=$savedCount, updated=$updatedCount"
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
