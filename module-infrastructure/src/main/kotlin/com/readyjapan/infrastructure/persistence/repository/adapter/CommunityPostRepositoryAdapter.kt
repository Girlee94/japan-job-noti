package com.readyjapan.infrastructure.persistence.repository.adapter

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.Sentiment
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.infrastructure.persistence.repository.JpaCommunityPostRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CommunityPostRepositoryAdapter(
    private val jpa: JpaCommunityPostRepository
) : CommunityPostRepository {

    override fun findById(id: Long): CommunityPost? = jpa.findById(id).orElse(null)

    override fun findBySourceIdAndExternalId(sourceId: Long, externalId: String): CommunityPost? =
        jpa.findBySourceIdAndExternalId(sourceId, externalId)

    override fun findAllBySourceIdAndExternalIdIn(sourceId: Long, externalIds: List<String>): List<CommunityPost> =
        jpa.findAllBySourceIdAndExternalIdIn(sourceId, externalIds)

    override fun findAllByPlatform(platform: CommunityPlatform): List<CommunityPost> =
        jpa.findAllByPlatform(platform)

    override fun findAllByCreatedAtAfter(dateTime: LocalDateTime): List<CommunityPost> =
        jpa.findAllByCreatedAtAfter(dateTime)

    override fun findAllNeedingTranslation(): List<CommunityPost> =
        jpa.findAllNeedingTranslation()

    override fun findAllNeedingSentimentAnalysis(): List<CommunityPost> =
        jpa.findAllNeedingSentimentAnalysis()

    override fun findBySentiment(sentiment: Sentiment): List<CommunityPost> =
        jpa.findBySentiment(sentiment)

    override fun findPopularPosts(minLikes: Int, limit: Int): List<CommunityPost> =
        jpa.findPopularPosts(minLikes, limit)

    override fun findRecentPosts(limit: Int): List<CommunityPost> =
        jpa.findRecentPosts(limit)

    override fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Int =
        jpa.countByCreatedAtBetween(start, end)

    override fun countBySentiment(sentiment: Sentiment): Int =
        jpa.countBySentiment(sentiment)

    override fun save(communityPost: CommunityPost): CommunityPost =
        jpa.save(communityPost)

    override fun saveAll(communityPosts: List<CommunityPost>): List<CommunityPost> =
        jpa.saveAll(communityPosts)

    override fun deleteById(id: Long) = jpa.deleteById(id)

    override fun existsBySourceIdAndExternalId(sourceId: Long, externalId: String): Boolean =
        jpa.existsBySourceIdAndExternalId(sourceId, externalId)
}
