package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.Sentiment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface JpaCommunityPostRepository : JpaRepository<CommunityPost, Long> {

    @Query("SELECT c FROM CommunityPost c WHERE c.source.id = :sourceId AND c.externalId = :externalId")
    fun findBySourceIdAndExternalId(
        @Param("sourceId") sourceId: Long,
        @Param("externalId") externalId: String
    ): CommunityPost?

    @Query("SELECT c FROM CommunityPost c WHERE c.source.id = :sourceId AND c.externalId IN :externalIds")
    fun findAllBySourceIdAndExternalIdIn(
        @Param("sourceId") sourceId: Long,
        @Param("externalIds") externalIds: List<String>
    ): List<CommunityPost>

    fun findAllByPlatform(platform: CommunityPlatform): List<CommunityPost>

    @Query("SELECT c FROM CommunityPost c WHERE c.createdAt > :dateTime ORDER BY c.createdAt DESC")
    fun findAllByCreatedAtAfter(@Param("dateTime") dateTime: LocalDateTime): List<CommunityPost>

    @Query("SELECT c FROM CommunityPost c WHERE c.language = 'ja' AND c.contentTranslated IS NULL")
    fun findAllNeedingTranslation(): List<CommunityPost>

    @Query("SELECT c FROM CommunityPost c WHERE c.sentiment IS NULL")
    fun findAllNeedingSentimentAnalysis(): List<CommunityPost>

    fun findBySentiment(sentiment: Sentiment): List<CommunityPost>

    @Query(
        value = "SELECT * FROM community_posts WHERE like_count >= :minLikes ORDER BY like_count DESC LIMIT :limit",
        nativeQuery = true
    )
    fun findPopularPosts(
        @Param("minLikes") minLikes: Int,
        @Param("limit") limit: Int
    ): List<CommunityPost>

    @Query(
        value = "SELECT * FROM community_posts ORDER BY published_at DESC NULLS LAST LIMIT :limit",
        nativeQuery = true
    )
    fun findRecentPosts(@Param("limit") limit: Int): List<CommunityPost>

    @Query("SELECT COUNT(c) FROM CommunityPost c WHERE c.createdAt BETWEEN :start AND :end")
    fun countByCreatedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Int

    @Query("SELECT COUNT(c) FROM CommunityPost c WHERE c.sentiment = :sentiment")
    fun countBySentiment(@Param("sentiment") sentiment: Sentiment): Int

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CommunityPost c WHERE c.source.id = :sourceId AND c.externalId = :externalId")
    fun existsBySourceIdAndExternalId(
        @Param("sourceId") sourceId: Long,
        @Param("externalId") externalId: String
    ): Boolean
}
