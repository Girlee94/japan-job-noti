package com.readyjapan.infrastructure.crawler.qiita

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.core.domain.repository.CrawlSourceRepository
import com.readyjapan.infrastructure.crawler.qiita.dto.QiitaItemResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

private val logger = KotlinLogging.logger {}

/**
 * Qiita 크롤링 데이터 영속화 서비스
 * 트랜잭션 경계를 분리하여 Spring AOP 프록시가 정상 동작하도록 별도 서비스로 분리
 */
@Service
@Transactional(readOnly = true)
class QiitaItemPersistenceService(
    private val communityPostRepository: CommunityPostRepository,
    private val crawlSourceRepository: CrawlSourceRepository
) {
    companion object {
        private val JST_ZONE = ZoneId.of("Asia/Tokyo")
        private val JAPANESE_PATTERN = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF]")
        private val KOREAN_PATTERN = Regex("[\\uAC00-\\uD7AF\\u1100-\\u11FF]")
    }

    /**
     * 크롤링된 아이템들을 트랜잭션 내에서 일괄 저장
     * 배치 조회로 N+1 문제 해결
     */
    @Transactional
    fun saveCrawledItems(
        source: CrawlSource,
        items: List<QiitaItemResponse>
    ): Pair<Int, Int> {
        if (items.isEmpty()) {
            return Pair(0, 0)
        }

        // 1회 SELECT로 기존 게시물 일괄 조회
        val externalIds = items.map { it.id }
        val existingMap = communityPostRepository
            .findAllBySourceIdAndExternalIdIn(source.id, externalIds)
            .associateBy { it.externalId }

        val toUpdate = mutableListOf<CommunityPost>()
        val toInsert = mutableListOf<CommunityPost>()

        for (item in items) {
            val existing = existingMap[item.id]
            if (existing != null) {
                if (existing.likeCount != item.likesCount ||
                    existing.commentCount != item.commentsCount
                ) {
                    existing.updateStats(
                        likeCount = item.likesCount,
                        commentCount = item.commentsCount,
                        shareCount = null
                    )
                    toUpdate.add(existing)
                }
            } else {
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
     * ISO 8601 날짜 문자열을 JST LocalDateTime으로 변환
     */
    private fun parseDateTime(dateTimeStr: String): LocalDateTime {
        return try {
            OffsetDateTime.parse(dateTimeStr)
                .atZoneSameInstant(JST_ZONE)
                .toLocalDateTime()
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse datetime: $dateTimeStr, using now()" }
            LocalDateTime.now(JST_ZONE)
        }
    }
}
