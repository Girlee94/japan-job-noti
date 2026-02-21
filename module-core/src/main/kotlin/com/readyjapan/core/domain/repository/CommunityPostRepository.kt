package com.readyjapan.core.domain.repository

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.Sentiment
import java.time.LocalDateTime

/**
 * 커뮤니티 글 리포지토리 인터페이스
 */
interface CommunityPostRepository {
    fun findById(id: Long): CommunityPost?
    fun findBySourceIdAndExternalId(sourceId: Long, externalId: String): CommunityPost?
    fun findAllBySourceIdAndExternalIdIn(sourceId: Long, externalIds: List<String>): List<CommunityPost>
    fun findAllByPlatform(platform: CommunityPlatform): List<CommunityPost>
    fun findAllByCreatedAtAfter(dateTime: LocalDateTime): List<CommunityPost>

    fun findAllByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<CommunityPost>
    fun findAllNeedingTranslation(): List<CommunityPost>
    fun findAllNeedingSentimentAnalysis(): List<CommunityPost>
    fun findBySentiment(sentiment: Sentiment): List<CommunityPost>
    fun findPopularPosts(minLikes: Int, limit: Int): List<CommunityPost>
    fun findRecentPosts(limit: Int): List<CommunityPost>
    fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Int
    fun countBySentiment(sentiment: Sentiment): Int
    fun save(communityPost: CommunityPost): CommunityPost
    fun saveAll(communityPosts: List<CommunityPost>): List<CommunityPost>
    fun deleteById(id: Long)
    fun existsBySourceIdAndExternalId(sourceId: Long, externalId: String): Boolean
}
