package com.readyjapan.infrastructure.crawler.reddit

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CrawlHistoryRepository
import com.readyjapan.core.domain.repository.CrawlSourceRepository
import com.readyjapan.infrastructure.crawler.config.CrawlerConfig
import com.readyjapan.infrastructure.crawler.reddit.dto.RedditPostData
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Reddit 크롤링 서비스
 * Reddit API를 통해 서브레딧의 게시물을 수집하고 저장합니다.
 *
 * 트랜잭션 관리는 RedditPostPersistenceService에 위임하며,
 * 이 클래스 자체는 트랜잭션을 선언하지 않는다 (HTTP 호출이 포함되어 있으므로).
 */
@Service
class RedditCrawlerService(
    private val redditApiClient: RedditApiClient,
    private val crawlSourceRepository: CrawlSourceRepository,
    private val crawlHistoryRepository: CrawlHistoryRepository,
    private val redditPostPersistenceService: RedditPostPersistenceService,
    private val crawlerConfig: CrawlerConfig
) {
    companion object {
        private val OBJECT_MAPPER = jacksonObjectMapper()
        private val SUBREDDIT_URL_PATTERN = Regex("reddit\\.com/r/([a-zA-Z0-9_]+)")

        // 일본 취업 관련 키워드
        private val JAPAN_JOB_KEYWORDS = listOf(
            // 영어 키워드
            "job", "work", "career", "hire", "hiring", "employ", "salary", "interview",
            "resume", "visa", "engineer", "developer", "it", "tech", "company",
            "offer", "position", "remote", "office",
            // 일본어 키워드
            "就職", "転職", "仕事", "キャリア", "給料", "面接", "エンジニア", "開発者",
            "ビザ", "会社", "求人", "採用"
        )
    }

    /**
     * 모든 활성화된 Reddit 소스 크롤링
     */
    fun crawlAllSources(): List<CrawlHistory> {
        if (!redditApiClient.isEnabled()) {
            logger.warn { "Reddit API is not enabled or configured" }
            return emptyList()
        }

        val sources = crawlSourceRepository.findEnabledBySourceType(SourceType.COMMUNITY)
            .filter { it.platform == CommunityPlatform.REDDIT }

        if (sources.isEmpty()) {
            logger.info { "No active Reddit sources found" }
            return emptyList()
        }

        logger.info { "Starting Reddit crawl for ${sources.size} sources" }

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
            val subreddit = config["subreddit"] as? String
                ?: extractSubredditFromUrl(source.url)
                ?: throw IllegalArgumentException("Cannot determine subreddit from source")

            val sort = config["sort"] as? String ?: "new"
            val limit = (config["limit"] as? Number)?.toInt() ?: 50

            logger.info { "Crawling r/$subreddit (sort: $sort, limit: $limit)" }

            // HTTP 호출 (트랜잭션 밖에서 수행)
            val response = redditApiClient.getSubredditPosts(subreddit, sort, limit)
                .block()

            if (response == null) {
                history.fail("No response from Reddit API")
                return crawlHistoryRepository.save(history)
            }

            // cutoff를 한 번만 계산하여 모든 게시물에 동일한 기준 적용
            val cutoff = Instant.now().minus(Duration.ofHours(crawlerConfig.freshnessHours))

            val posts = response.data.children
                .map { it.data }
                .filter { isWithinFreshnessWindow(it, cutoff) }
                .filter { isRelevantPost(it) }

            // DB 저장 (별도 서비스를 통해 트랜잭션 내에서 수행)
            val (savedCount, updatedCount) = redditPostPersistenceService.saveCrawledPosts(source, posts)

            history.complete(
                itemsFound = posts.size,
                itemsSaved = savedCount,
                itemsUpdated = updatedCount
            )

            logger.info {
                "Crawl completed for r/$subreddit: " +
                        "found=${posts.size}, saved=$savedCount, updated=$updatedCount"
            }

        } catch (e: Exception) {
            logger.error(e) { "Crawl failed for source: ${source.name}" }
            history.fail(e.message ?: "Unknown error")
        }

        return crawlHistoryRepository.save(history)
    }

    /**
     * 게시물이 수집 기준 시간(freshnessHours) 이내인지 확인
     */
    private fun isWithinFreshnessWindow(postData: RedditPostData, cutoff: Instant): Boolean {
        val postInstant = Instant.ofEpochSecond(postData.createdUtc.toLong())
        return postInstant.isAfter(cutoff)
    }

    /**
     * 관련 게시물 여부 확인
     * 일본 취업 관련 키워드가 포함되어 있거나, 관련 서브레딧인 경우 true
     */
    private fun isRelevantPost(postData: RedditPostData): Boolean {
        // 삭제/잠금된 게시물 제외
        if (postData.removed == true || postData.locked) {
            return false
        }

        // 고정 게시물 제외 (보통 규칙 등)
        if (postData.stickied) {
            return false
        }

        // 특정 서브레딧은 모든 게시물 수집
        val relevantSubreddits = listOf("japanlife", "movingtojapan", "japandev")
        if (postData.subreddit.lowercase() in relevantSubreddits) {
            return true
        }

        // 키워드 필터링
        val text = "${postData.title} ${postData.selftext ?: ""}".lowercase()
        return JAPAN_JOB_KEYWORDS.any { keyword ->
            text.contains(keyword.lowercase())
        }
    }

    /**
     * URL에서 서브레딧 이름 추출
     */
    private fun extractSubredditFromUrl(url: String): String? {
        return SUBREDDIT_URL_PATTERN.find(url)?.groupValues?.get(1)
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
