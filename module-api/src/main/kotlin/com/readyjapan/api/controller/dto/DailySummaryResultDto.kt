package com.readyjapan.api.controller.dto

import com.readyjapan.infrastructure.orchestration.result.DailySummaryGenerationResult

data class DailySummaryResultDto(
    val summaryId: Long?,
    val date: String?,
    val telegramSent: Boolean,
    val skipped: Boolean,
    val skippedReason: String?,
    val failed: Boolean,
    val errorMessage: String?,
    val stats: Map<String, Int>?
) {
    companion object {
        fun from(result: DailySummaryGenerationResult): DailySummaryResultDto =
            DailySummaryResultDto(
                summaryId = result.dailySummary?.id,
                date = result.dailySummary?.summaryDate?.toString(),
                telegramSent = result.telegramSent,
                skipped = result.skipped,
                skippedReason = result.skippedReason,
                failed = result.failed,
                errorMessage = if (result.failed) "요약 생성에 실패했습니다" else null,
                stats = result.stats?.let {
                    mapOf(
                        "jobPostings" to it.jobPostingCount,
                        "newsArticles" to it.newsArticleCount,
                        "communityPosts" to it.communityPostCount
                    )
                }
            )
    }
}
