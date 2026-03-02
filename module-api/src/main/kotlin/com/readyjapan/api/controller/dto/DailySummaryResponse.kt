package com.readyjapan.api.controller.dto

import com.readyjapan.core.domain.entity.DailySummary

data class DailySummaryResponse(
    val id: Long,
    val summaryDate: String,
    val summaryContent: String,
    val jobPostingCount: Int,
    val newsArticleCount: Int,
    val communityPostCount: Int,
    val totalCount: Int,
    val status: String,
    val sentAt: String?,
    val createdAt: String
) {
    companion object {
        fun from(entity: DailySummary): DailySummaryResponse {
            return DailySummaryResponse(
                id = entity.id,
                summaryDate = entity.summaryDate.toString(),
                summaryContent = entity.summaryContent.take(500),
                jobPostingCount = entity.jobPostingCount,
                newsArticleCount = entity.newsArticleCount,
                communityPostCount = entity.communityPostCount,
                totalCount = entity.getTotalCount(),
                status = entity.status.name,
                sentAt = entity.sentAt?.toString(),
                createdAt = entity.createdAt.toString()
            )
        }

        fun detail(entity: DailySummary): DailySummaryResponse {
            return DailySummaryResponse(
                id = entity.id,
                summaryDate = entity.summaryDate.toString(),
                summaryContent = entity.summaryContent,
                jobPostingCount = entity.jobPostingCount,
                newsArticleCount = entity.newsArticleCount,
                communityPostCount = entity.communityPostCount,
                totalCount = entity.getTotalCount(),
                status = entity.status.name,
                sentAt = entity.sentAt?.toString(),
                createdAt = entity.createdAt.toString()
            )
        }
    }
}
