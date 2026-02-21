package com.readyjapan.infrastructure.orchestration.result

data class SentimentBatchResult(
    val analyzedCount: Int,
    val failedCount: Int
)
