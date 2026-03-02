package com.readyjapan.api.controller.dto

import com.readyjapan.core.domain.entity.NewsArticle

data class NewsArticleResponse(
    val id: Long,
    val title: String,
    val titleTranslated: String?,
    val summary: String?,
    val summaryTranslated: String?,
    val author: String?,
    val category: String?,
    val originalUrl: String,
    val imageUrl: String?,
    val language: String,
    val publishedAt: String?,
    val createdAt: String
) {
    companion object {
        fun from(entity: NewsArticle): NewsArticleResponse {
            return NewsArticleResponse(
                id = entity.id,
                title = entity.getDisplayTitle(),
                titleTranslated = entity.titleTranslated,
                summary = entity.getDisplaySummary(),
                summaryTranslated = entity.summaryTranslated,
                author = entity.author,
                category = entity.category,
                originalUrl = entity.originalUrl,
                imageUrl = entity.imageUrl,
                language = entity.language,
                publishedAt = entity.publishedAt?.toString(),
                createdAt = entity.createdAt.toString()
            )
        }
    }
}
