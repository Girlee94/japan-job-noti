package com.readyjapan.infrastructure.crawler.reddit

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.repository.CommunityPostRepository
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
 * Reddit 크롤링 데이터 영속화 서비스
 * 트랜잭션 경계를 분리하여 Spring AOP 프록시가 정상 동작하도록 별도 서비스로 분리
 */
@Service
@Transactional(readOnly = true)
class RedditPostPersistenceService(
    private val communityPostRepository: CommunityPostRepository,
    private val crawlSourceRepository: CrawlSourceRepository
) {
    companion object {
        private val JST_ZONE = ZoneId.of("Asia/Tokyo")
        private val JAPANESE_PATTERN = Regex("[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF]")
        private val KOREAN_PATTERN = Regex("[\uAC00-\uD7AF\u1100-\u11FF]")
    }

    /**
     * 크롤링된 게시물들을 트랜잭션 내에서 일괄 저장
     * 배치 조회로 N+1 문제 해결
     */
    @Transactional
    fun saveCrawledPosts(source: CrawlSource, posts: List<RedditPostData>): Pair<Int, Int> {
        if (posts.isEmpty()) {
            source.updateLastCrawledAt()
            crawlSourceRepository.save(source)
            return Pair(0, 0)
        }

        // 1회 SELECT로 기존 게시물 일괄 조회
        val externalIds = posts.map { it.id }
        val existingMap = communityPostRepository
            .findAllBySourceIdAndExternalIdIn(source.id, externalIds)
            .associateBy { it.externalId }

        val toInsert = mutableListOf<CommunityPost>()
        val toUpdate = mutableListOf<CommunityPost>()

        for (postData in posts) {
            val existing = existingMap[postData.id]
            if (existing != null) {
                if (existing.likeCount != postData.score ||
                    existing.commentCount != postData.numComments
                ) {
                    existing.updateStats(
                        likeCount = postData.score,
                        commentCount = postData.numComments,
                        shareCount = null
                    )
                    toUpdate.add(existing)
                }
            } else {
                val content = postData.selftext?.takeIf { it.isNotBlank() }
                    ?: postData.title

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

                toInsert.add(post)
            }
        }

        if (toUpdate.isNotEmpty()) {
            communityPostRepository.saveAll(toUpdate)
        }
        if (toInsert.isNotEmpty()) {
            communityPostRepository.saveAll(toInsert)
        }

        source.updateLastCrawledAt()
        crawlSourceRepository.save(source)

        return Pair(toInsert.size, toUpdate.size)
    }

    /**
     * 언어 감지 (간단한 휴리스틱)
     */
    private fun detectLanguage(title: String, content: String): String {
        val text = "$title $content"

        val japaneseCount = JAPANESE_PATTERN.findAll(text).count()
        val koreanCount = KOREAN_PATTERN.findAll(text).count()

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
            JST_ZONE
        )
    }
}
