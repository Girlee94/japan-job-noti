package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.NewsArticle
import com.readyjapan.infrastructure.persistence.repository.querydsl.NewsArticleQueryDslRepository
import org.springframework.data.jpa.repository.JpaRepository

interface JpaNewsArticleRepository : JpaRepository<NewsArticle, Long>, NewsArticleQueryDslRepository {

    fun findByCategory(category: String): List<NewsArticle>
}
