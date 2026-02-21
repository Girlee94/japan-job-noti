package com.readyjapan.batch.scheduler

import com.readyjapan.infrastructure.orchestration.DailySummaryOrchestrationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

private val logger = KotlinLogging.logger {}

/**
 * 일일 요약 생성 및 텔레그램 전송 스케줄러
 * 매일 오전 9시 (JST) 실행
 */
@Component
class DailySummaryScheduler(
    private val dailySummaryOrchestrationService: DailySummaryOrchestrationService
) {

    /**
     * 매일 오전 9시에 전날 수집된 데이터를 요약하고 텔레그램으로 전송
     */
    @Scheduled(cron = "\${app.scheduler.daily-summary.cron:0 0 9 * * *}")
    fun generateAndSendDailySummary() {
        val yesterday = LocalDate.now(ZoneId.of("Asia/Tokyo")).minusDays(1)
        logger.info { "Starting daily summary generation for: $yesterday" }

        val result = dailySummaryOrchestrationService.generateAndSendDailySummary(
            targetDate = yesterday,
            skipIfExists = true
        )

        when {
            result.skipped -> logger.info { "Daily summary skipped: ${result.skippedReason}" }
            result.failed -> logger.error { "Daily summary failed: ${result.errorMessage}" }
            result.telegramSent -> logger.info { "Daily summary generated and sent successfully" }
            else -> logger.warn { "Daily summary generated but Telegram send failed" }
        }
    }
}
