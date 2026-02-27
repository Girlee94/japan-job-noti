package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.enums.Sentiment
import java.time.LocalDateTime

interface CommunityPostQueryDslRepository {

    fun findBySourceIdAndExternalId(sourceId: Long, externalId: String): CommunityPost?

    fun findAllBySourceIdAndExternalIdIn(sourceId: Long, externalIds: List<String>): List<CommunityPost>

    fun findAllByCreatedAtAfter(dateTime: LocalDateTime): List<CommunityPost>

    fun findAllByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<CommunityPost>

    fun findAllNeedingTranslation(): List<CommunityPost>

    fun findAllNeedingSentimentAnalysis(): List<CommunityPost>

    fun findPopularPosts(minLikes: Int, limit: Int): List<CommunityPost>

    fun findRecentPosts(limit: Int): List<CommunityPost>

    fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Long

    fun countBySentiment(sentiment: Sentiment): Long

    fun existsBySourceIdAndExternalId(sourceId: Long, externalId: String): Boolean
}
