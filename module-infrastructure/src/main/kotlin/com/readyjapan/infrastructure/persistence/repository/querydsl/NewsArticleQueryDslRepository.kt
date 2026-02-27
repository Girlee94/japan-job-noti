package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.readyjapan.core.domain.entity.NewsArticle
import java.time.LocalDateTime

interface NewsArticleQueryDslRepository {

    fun findBySourceIdAndExternalId(sourceId: Long, externalId: String): NewsArticle?

    fun findAllByCreatedAtAfter(dateTime: LocalDateTime): List<NewsArticle>

    fun findAllByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<NewsArticle>

    fun findAllNeedingTranslation(): List<NewsArticle>

    fun findRecentArticles(limit: Int): List<NewsArticle>

    fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Long

    fun existsBySourceIdAndExternalId(sourceId: Long, externalId: String): Boolean
}
