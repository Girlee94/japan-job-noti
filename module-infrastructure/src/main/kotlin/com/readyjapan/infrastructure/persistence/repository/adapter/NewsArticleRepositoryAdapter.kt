package com.readyjapan.infrastructure.persistence.repository.adapter

import com.readyjapan.core.domain.entity.NewsArticle
import com.readyjapan.core.domain.repository.NewsArticleRepository
import com.readyjapan.infrastructure.persistence.repository.JpaNewsArticleRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class NewsArticleRepositoryAdapter(
    private val jpa: JpaNewsArticleRepository
) : NewsArticleRepository {

    override fun findById(id: Long): NewsArticle? = jpa.findById(id).orElse(null)

    override fun findBySourceIdAndExternalId(sourceId: Long, externalId: String): NewsArticle? =
        jpa.findBySourceIdAndExternalId(sourceId, externalId)

    override fun findAllByCreatedAtAfter(dateTime: LocalDateTime): List<NewsArticle> =
        jpa.findAllByCreatedAtAfter(dateTime)

    override fun findAllByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<NewsArticle> =
        jpa.findAllByCreatedAtBetween(start, end)

    override fun findAllNeedingTranslation(): List<NewsArticle> =
        jpa.findAllNeedingTranslation()

    override fun findRecentArticles(limit: Int): List<NewsArticle> =
        jpa.findRecentArticles(limit)

    override fun findByCategory(category: String): List<NewsArticle> =
        jpa.findByCategory(category)

    override fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Int =
        jpa.countByCreatedAtBetween(start, end)

    override fun save(newsArticle: NewsArticle): NewsArticle = jpa.save(newsArticle)

    override fun saveAll(newsArticles: List<NewsArticle>): List<NewsArticle> =
        jpa.saveAll(newsArticles)

    override fun deleteById(id: Long) = jpa.deleteById(id)

    override fun existsBySourceIdAndExternalId(sourceId: Long, externalId: String): Boolean =
        jpa.existsBySourceIdAndExternalId(sourceId, externalId)
}
