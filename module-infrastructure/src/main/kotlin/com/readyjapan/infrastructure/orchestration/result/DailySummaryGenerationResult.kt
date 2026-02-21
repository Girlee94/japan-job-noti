package com.readyjapan.infrastructure.orchestration.result

import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.infrastructure.external.llm.service.SummaryStats

/**
 * 일일 요약 생성 결과
 *
 * [errorMessage]는 내부 로깅/디버깅용 메시지로, API 응답에 직접 노출하지 않아야 함.
 * 클라이언트에게는 sanitize된 메시지를 사용할 것 (DailySummaryResultDto 참고).
 */
data class DailySummaryGenerationResult(
    val dailySummary: DailySummary?,
    val telegramSent: Boolean,
    val skipped: Boolean,
    val skippedReason: String?,
    val failed: Boolean,
    val errorMessage: String?,
    val stats: SummaryStats?
) {
    companion object {
        fun alreadyExists(date: java.time.LocalDate): DailySummaryGenerationResult =
            DailySummaryGenerationResult(
                dailySummary = null,
                telegramSent = false,
                skipped = true,
                skippedReason = "Daily summary already exists for: $date",
                failed = false,
                errorMessage = null,
                stats = null
            )

        fun failure(errorMessage: String): DailySummaryGenerationResult =
            DailySummaryGenerationResult(
                dailySummary = null,
                telegramSent = false,
                skipped = false,
                skippedReason = null,
                failed = true,
                errorMessage = errorMessage,
                stats = null
            )
    }
}
