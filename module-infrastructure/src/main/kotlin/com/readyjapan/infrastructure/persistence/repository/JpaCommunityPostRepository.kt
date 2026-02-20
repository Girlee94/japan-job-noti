package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.Sentiment
import com.readyjapan.core.domain.repository.CommunityPostRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface JpaCommunityPostRepository : JpaRepository<CommunityPost, Long>, CommunityPostRepository {

    @Query("SELECT c FROM CommunityPost c WHERE c.source.id = :sourceId AND c.externalId = :externalId")
    override fun findBySourceIdAndExternalId(
        @Param("sourceId") sourceId: Long,
        @Param("externalId") externalId: String
    ): CommunityPost?

    override fun findAllByPlatform(platform: CommunityPlatform): List<CommunityPost>

    @Query("SELECT c FROM CommunityPost c WHERE c.createdAt > :dateTime ORDER BY c.createdAt DESC")
    override fun findAllByCreatedAtAfter(@Param("dateTime") dateTime: LocalDateTime): List<CommunityPost>

    @Query("SELECT c FROM CommunityPost c WHERE c.language = 'ja' AND c.contentTranslated IS NULL")
    override fun findAllNeedingTranslation(): List<CommunityPost>

    @Query("SELECT c FROM CommunityPost c WHERE c.sentiment IS NULL")
    override fun findAllNeedingSentimentAnalysis(): List<CommunityPost>

    override fun findBySentiment(sentiment: Sentiment): List<CommunityPost>

    @Query("SELECT c FROM CommunityPost c WHERE c.likeCount >= :minLikes ORDER BY c.likeCount DESC")
    fun findByLikeCountGreaterThanEqualOrderByLikeCountDesc(
        @Param("minLikes") minLikes: Int,
        pageable: PageRequest
    ): List<CommunityPost>

    override fun findPopularPosts(minLikes: Int, limit: Int): List<CommunityPost> {
        return findByLikeCountGreaterThanEqualOrderByLikeCountDesc(minLikes, PageRequest.of(0, limit))
    }

    @Query("SELECT c FROM CommunityPost c ORDER BY c.publishedAt DESC")
    fun findAllOrderByPublishedAtDesc(pageable: PageRequest): List<CommunityPost>

    override fun findRecentPosts(limit: Int): List<CommunityPost> {
        return findAllOrderByPublishedAtDesc(PageRequest.of(0, limit))
    }

    @Query("SELECT COUNT(c) FROM CommunityPost c WHERE c.createdAt BETWEEN :start AND :end")
    override fun countByCreatedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Int

    @Query("SELECT COUNT(c) FROM CommunityPost c WHERE c.sentiment = :sentiment")
    override fun countBySentiment(@Param("sentiment") sentiment: Sentiment): Int

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CommunityPost c WHERE c.source.id = :sourceId AND c.externalId = :externalId")
    override fun existsBySourceIdAndExternalId(
        @Param("sourceId") sourceId: Long,
        @Param("externalId") externalId: String
    ): Boolean
}
