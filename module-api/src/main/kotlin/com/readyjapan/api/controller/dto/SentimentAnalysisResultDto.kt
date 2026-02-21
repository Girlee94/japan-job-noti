package com.readyjapan.api.controller.dto

import com.readyjapan.infrastructure.orchestration.result.SentimentBatchResult

data class SentimentAnalysisResultDto(
    val analyzedCount: Int,
    val failedCount: Int
) {
    companion object {
        fun from(result: SentimentBatchResult): SentimentAnalysisResultDto =
            SentimentAnalysisResultDto(
                analyzedCount = result.analyzedCount,
                failedCount = result.failedCount
            )
    }
}
