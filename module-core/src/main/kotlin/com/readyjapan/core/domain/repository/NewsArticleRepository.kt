package com.readyjapan.core.domain.repository

import com.readyjapan.core.domain.entity.NewsArticle
import java.time.LocalDateTime

/**
 * 뉴스 기사 리포지토리 인터페이스
 */
interface NewsArticleRepository {
    fun findById(id: Long): NewsArticle?
    fun findBySourceIdAndExternalId(sourceId: Long, externalId: String): NewsArticle?
    fun findAllByCreatedAtAfter(dateTime: LocalDateTime): List<NewsArticle>
    fun findAllNeedingTranslation(): List<NewsArticle>
    fun findRecentArticles(limit: Int): List<NewsArticle>
    fun findByCategory(category: String): List<NewsArticle>
    fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Int
    fun save(newsArticle: NewsArticle): NewsArticle
    fun saveAll(newsArticles: List<NewsArticle>): List<NewsArticle>
    fun deleteById(id: Long)
    fun existsBySourceIdAndExternalId(sourceId: Long, externalId: String): Boolean
}
