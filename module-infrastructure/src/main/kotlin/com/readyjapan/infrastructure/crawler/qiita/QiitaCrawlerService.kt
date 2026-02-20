package com.readyjapan.infrastructure.crawler.qiita

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.core.domain.repository.CrawlHistoryRepository
import com.readyjapan.core.domain.repository.CrawlSourceRepository
import com.readyjapan.infrastructure.crawler.qiita.dto.QiitaItemResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

private val logger = KotlinLogging.logger {}

/**
 * Qiita 크롤링 서비스
 * Qiita API v2를 통해 태그별 기사를 수집하고 저장합니다.
 */
@Service
class QiitaCrawlerService(
    private val qiitaApiClient: QiitaApiClient,
    private val qiitaProperties: QiitaProperties,
    private val crawlSourceRepository: CrawlSourceRepository,
    private val communityPostRepository: CommunityPostRepository,
    private val crawlHistoryRepository: CrawlHistoryRepository
) {
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
     * HTTP 호출은 트랜잭션 밖에서 수행하고, DB 저장만 트랜잭션 내에서 처리
     */
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
                    .block()

                if (items != null) {
                    allItems.addAll(items)
                }

                // Rate limit 방지 딜레이
                Thread.sleep(qiitaProperties.requestDelayMs)
            }

            // 중복 제거 (같은 기사가 여러 태그에 걸릴 수 있음)
            val uniqueItems = allItems.distinctBy { it.id }

            // DB 저장 (트랜잭션 내에서 수행)
            val (savedCount, updatedCount) = saveCrawledItems(source, uniqueItems)

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
     * 크롤링된 아이템들을 트랜잭션 내에서 저장
     */
    @Transactional
    fun saveCrawledItems(
        source: CrawlSource,
        items: List<QiitaItemResponse>
    ): Pair<Int, Int> {
        var savedCount = 0
        var updatedCount = 0

        for (item in items) {
            val result = saveOrUpdateItem(source, item)
            when (result) {
                SaveResult.SAVED -> savedCount++
                SaveResult.UPDATED -> updatedCount++
                SaveResult.SKIPPED -> { /* no-op */ }
            }
        }

        source.updateLastCrawledAt()
        crawlSourceRepository.save(source)

        return Pair(savedCount, updatedCount)
    }

    /**
     * 기사 저장 또는 업데이트
     */
    private fun saveOrUpdateItem(source: CrawlSource, item: QiitaItemResponse): SaveResult {
        val existing = communityPostRepository.findBySourceIdAndExternalId(
            source.id, item.id
        )

        if (existing != null) {
            // 기존 게시물 통계 업데이트
            if (existing.likeCount != item.likesCount ||
                existing.commentCount != item.commentsCount
            ) {
                existing.updateStats(
                    likeCount = item.likesCount,
                    commentCount = item.commentsCount,
                    shareCount = null
                )
                communityPostRepository.save(existing)
                return SaveResult.UPDATED
            }
            return SaveResult.SKIPPED
        }

        // 새 게시물 저장
        val content = item.body?.takeIf { it.isNotBlank() }
            ?: item.title

        val post = CommunityPost(
            source = source,
            externalId = item.id,
            platform = CommunityPlatform.QIITA,
            title = item.title,
            content = content,
            author = item.user?.id,
            authorProfileUrl = item.getAuthorProfileUrl(),
            originalUrl = item.url,
            tags = item.getTagsJson(),
            likeCount = item.likesCount,
            commentCount = item.commentsCount,
            language = detectLanguage(item.title, content),
            publishedAt = parseDateTime(item.createdAt)
        )

        communityPostRepository.save(post)
        return SaveResult.SAVED
    }

    /**
     * 언어 감지 (간단한 휴리스틱)
     */
    private fun detectLanguage(title: String, content: String): String {
        val text = "$title $content"

        val japanesePattern = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF]")
        val koreanPattern = Regex("[\\uAC00-\\uD7AF\\u1100-\\u11FF]")

        val japaneseCount = japanesePattern.findAll(text).count()
        val koreanCount = koreanPattern.findAll(text).count()

        return when {
            koreanCount > japaneseCount && koreanCount > 5 -> "ko"
            japaneseCount > 5 -> "ja"
            else -> "en"
        }
    }

    /**
     * ISO 8601 날짜 문자열을 JST LocalDateTime으로 변환
     */
    private fun parseDateTime(dateTimeStr: String): LocalDateTime {
        return try {
            OffsetDateTime.parse(dateTimeStr)
                .atZoneSameInstant(ZoneId.of("Asia/Tokyo"))
                .toLocalDateTime()
        } catch (e: Exception) {
            logger.warn { "Failed to parse datetime: $dateTimeStr, using now()" }
            LocalDateTime.now(ZoneId.of("Asia/Tokyo"))
        }
    }

    /**
     * 소스 설정에서 태그 목록 파싱
     */
    @Suppress("UNCHECKED_CAST")
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
            com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                .readValue(configJson, Map::class.java) as Map<String, Any>
        } catch (e: Exception) {
            logger.warn { "Failed to parse source config: ${e.message}" }
            emptyMap()
        }
    }

    private enum class SaveResult {
        SAVED, UPDATED, SKIPPED
    }
}
