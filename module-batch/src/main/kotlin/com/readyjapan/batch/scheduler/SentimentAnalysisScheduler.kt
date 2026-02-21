package com.readyjapan.batch.scheduler

import com.readyjapan.infrastructure.orchestration.SentimentOrchestrationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * 감정 분석 배치 스케줄러
 * 커뮤니티 글의 감정을 자동으로 분석
 */
@Component
class SentimentAnalysisScheduler(
    private val sentimentOrchestrationService: SentimentOrchestrationService
) {

    /**
     * 30분마다 감정 분석이 필요한 커뮤니티 글 분석
     */
    @Scheduled(fixedRate = 1800000) // 30분
    fun analyzePendingSentiments() {
        logger.info { "Starting sentiment analysis batch job" }
        val result = sentimentOrchestrationService.analyzePendingSentiments()
        logger.info { "Sentiment analysis completed - Analyzed: ${result.analyzedCount}, Failed: ${result.failedCount}" }
    }
}
