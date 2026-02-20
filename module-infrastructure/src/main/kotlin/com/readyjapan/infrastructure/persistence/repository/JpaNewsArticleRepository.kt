package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.NewsArticle
import com.readyjapan.core.domain.repository.NewsArticleRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface JpaNewsArticleRepository : JpaRepository<NewsArticle, Long>, NewsArticleRepository {

    @Query("SELECT n FROM NewsArticle n WHERE n.source.id = :sourceId AND n.externalId = :externalId")
    override fun findBySourceIdAndExternalId(
        @Param("sourceId") sourceId: Long,
        @Param("externalId") externalId: String
    ): NewsArticle?

    @Query("SELECT n FROM NewsArticle n WHERE n.createdAt > :dateTime ORDER BY n.createdAt DESC")
    override fun findAllByCreatedAtAfter(@Param("dateTime") dateTime: LocalDateTime): List<NewsArticle>

    @Query("SELECT n FROM NewsArticle n WHERE n.language = 'ja' AND n.titleTranslated IS NULL")
    override fun findAllNeedingTranslation(): List<NewsArticle>

    @Query("SELECT n FROM NewsArticle n ORDER BY n.publishedAt DESC NULLS LAST")
    fun findAllOrderByPublishedAtDesc(pageable: PageRequest): List<NewsArticle>

    override fun findRecentArticles(limit: Int): List<NewsArticle> {
        return findAllOrderByPublishedAtDesc(PageRequest.of(0, limit))
    }

    override fun findByCategory(category: String): List<NewsArticle>

    @Query("SELECT COUNT(n) FROM NewsArticle n WHERE n.createdAt BETWEEN :start AND :end")
    override fun countByCreatedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Int

    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM NewsArticle n WHERE n.source.id = :sourceId AND n.externalId = :externalId")
    override fun existsBySourceIdAndExternalId(
        @Param("sourceId") sourceId: Long,
        @Param("externalId") externalId: String
    ): Boolean
}
