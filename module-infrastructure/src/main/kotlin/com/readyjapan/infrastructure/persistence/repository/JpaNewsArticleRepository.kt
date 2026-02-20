package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.NewsArticle
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface JpaNewsArticleRepository : JpaRepository<NewsArticle, Long> {

    @Query("SELECT n FROM NewsArticle n WHERE n.source.id = :sourceId AND n.externalId = :externalId")
    fun findBySourceIdAndExternalId(
        @Param("sourceId") sourceId: Long,
        @Param("externalId") externalId: String
    ): NewsArticle?

    @Query("SELECT n FROM NewsArticle n WHERE n.createdAt > :dateTime ORDER BY n.createdAt DESC")
    fun findAllByCreatedAtAfter(@Param("dateTime") dateTime: LocalDateTime): List<NewsArticle>

    @Query("SELECT n FROM NewsArticle n WHERE n.language = 'ja' AND n.titleTranslated IS NULL")
    fun findAllNeedingTranslation(): List<NewsArticle>

    @Query(
        value = "SELECT * FROM news_articles ORDER BY published_at DESC NULLS LAST LIMIT :limit",
        nativeQuery = true
    )
    fun findRecentArticles(@Param("limit") limit: Int): List<NewsArticle>

    fun findByCategory(category: String): List<NewsArticle>

    @Query("SELECT COUNT(n) FROM NewsArticle n WHERE n.createdAt BETWEEN :start AND :end")
    fun countByCreatedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Int

    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM NewsArticle n WHERE n.source.id = :sourceId AND n.externalId = :externalId")
    fun existsBySourceIdAndExternalId(
        @Param("sourceId") sourceId: Long,
        @Param("externalId") externalId: String
    ): Boolean
}
