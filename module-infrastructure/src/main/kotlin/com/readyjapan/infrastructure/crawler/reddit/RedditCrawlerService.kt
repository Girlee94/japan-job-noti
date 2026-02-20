package com.readyjapan.infrastructure.crawler.reddit

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.core.domain.repository.CrawlHistoryRepository
import com.readyjapan.core.domain.repository.CrawlSourceRepository
import com.readyjapan.infrastructure.crawler.reddit.dto.RedditPostData
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val logger = KotlinLogging.logger {}

/**
 * Reddit 크롤링 서비스
 * Reddit API를 통해 서브레딧의 게시물을 수집하고 저장합니다.
 */
@Service
class RedditCrawlerService(
    private val redditApiClient: RedditApiClient,
    private val crawlSourceRepository: CrawlSourceRepository,
    private val communityPostRepository: CommunityPostRepository,
    private val crawlHistoryRepository: CrawlHistoryRepository
) {
    companion object {
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
    @Transactional
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
     */
    @Transactional
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

            val response = redditApiClient.getSubredditPosts(subreddit, sort, limit)
                .block()

            if (response == null) {
                history.fail("No response from Reddit API")
                return crawlHistoryRepository.save(history)
            }

            val posts = response.data.children
                .map { it.data }
                .filter { isRelevantPost(it) }

            var savedCount = 0
            var updatedCount = 0

            for (postData in posts) {
                val result = saveOrUpdatePost(source, postData)
                when (result) {
                    SaveResult.SAVED -> savedCount++
                    SaveResult.UPDATED -> updatedCount++
                    SaveResult.SKIPPED -> { /* no-op */ }
                }
            }

            source.updateLastCrawledAt()
            crawlSourceRepository.save(source)

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
     * 게시물 저장 또는 업데이트
     */
    private fun saveOrUpdatePost(source: CrawlSource, postData: RedditPostData): SaveResult {
        val existing = communityPostRepository.findBySourceIdAndExternalId(
            source.id, postData.id
        )

        if (existing != null) {
            // 기존 게시물 통계 업데이트
            if (existing.likeCount != postData.score ||
                existing.commentCount != postData.numComments
            ) {
                existing.updateStats(
                    likeCount = postData.score,
                    commentCount = postData.numComments,
                    shareCount = null
                )
                communityPostRepository.save(existing)
                return SaveResult.UPDATED
            }
            return SaveResult.SKIPPED
        }

        // 새 게시물 저장
        val content = postData.selftext?.takeIf { it.isNotBlank() }
            ?: postData.title // self post가 아니면 제목을 본문으로

        val post = CommunityPost(
            source = source,
            externalId = postData.id,
            platform = CommunityPlatform.REDDIT,
            title = postData.title,
            content = content,
            author = postData.author,
            authorProfileUrl = postData.getAuthorProfileUrl(),
            originalUrl = postData.getFullUrl(),
            tags = postData.getTagsJson(),
            likeCount = postData.score,
            commentCount = postData.numComments,
            language = detectLanguage(postData.title, content),
            publishedAt = convertTimestamp(postData.createdUtc)
        )

        communityPostRepository.save(post)
        return SaveResult.SAVED
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
     * 언어 감지 (간단한 휴리스틱)
     */
    private fun detectLanguage(title: String, content: String): String {
        val text = "$title $content"

        // 일본어 문자 포함 여부 확인
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
     * Unix timestamp를 LocalDateTime으로 변환
     */
    private fun convertTimestamp(utcTimestamp: Double): LocalDateTime {
        return LocalDateTime.ofInstant(
            Instant.ofEpochSecond(utcTimestamp.toLong()),
            ZoneId.of("Asia/Tokyo")
        )
    }

    /**
     * URL에서 서브레딧 이름 추출
     */
    private fun extractSubredditFromUrl(url: String): String? {
        val pattern = Regex("reddit\\.com/r/([a-zA-Z0-9_]+)")
        return pattern.find(url)?.groupValues?.get(1)
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
