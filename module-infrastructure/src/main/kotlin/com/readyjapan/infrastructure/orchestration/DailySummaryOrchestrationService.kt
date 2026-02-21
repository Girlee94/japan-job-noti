package com.readyjapan.infrastructure.orchestration

import com.readyjapan.infrastructure.external.llm.service.DailySummaryResult as LlmDailySummaryResult
import com.readyjapan.infrastructure.external.llm.service.SummarizationService
import com.readyjapan.infrastructure.external.telegram.TelegramClient
import com.readyjapan.infrastructure.orchestration.persistence.DailySummaryPersistenceService
import com.readyjapan.infrastructure.orchestration.result.DailySummaryGenerationResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private val logger = KotlinLogging.logger {}

/**
 * 일일 요약 오케스트레이션 서비스
 * Controller와 Scheduler에서 공유하는 일일 요약 생성/전송 비즈니스 로직
 *
 * @Transactional 없음 — DB 저장은 DailySummaryPersistenceService(별도 빈)에서,
 * LLM API 호출과 HTTP 호출(텔레그램)은 트랜잭션 밖에서 수행
 *
 * 참고: saveSummary와 markSummaryAsSent는 별도 트랜잭션이므로
 * 텔레그램 전송 성공 후 markSummaryAsSent 호출 전에 프로세스가 중단되면
 * DRAFT 상태로 남을 수 있음. shouldSkip은 SENT 상태만 스킵하므로
 * 다음 스케줄러 실행 시 DRAFT 레코드에 대해 재시도됨.
 */
@Service
class DailySummaryOrchestrationService(
    private val persistenceService: DailySummaryPersistenceService,
    private val summarizationService: SummarizationService,
    private val telegramClient: TelegramClient
) {

    companion object {
        private const val SUMMARY_FAILURE_MESSAGE = "요약 생성에 실패했습니다"
    }

    /**
     * 요약만 생성 (텔레그램 전송 없음)
     */
    fun generateDailySummary(
        targetDate: LocalDate,
        skipIfExists: Boolean = false
    ): DailySummaryGenerationResult {
        logger.info { "Generating daily summary for: $targetDate (skipIfExists=$skipIfExists)" }

        if (shouldSkip(targetDate, skipIfExists)) {
            return DailySummaryGenerationResult.alreadyExists(targetDate)
        }

        return try {
            val summaryResult = generateSummaryForDate(targetDate)
            val savedSummary = persistenceService.saveSummary(targetDate, summaryResult)

            DailySummaryGenerationResult(
                dailySummary = savedSummary,
                telegramSent = false,
                skipped = false,
                skippedReason = null,
                failed = false,
                errorMessage = null,
                stats = summaryResult.stats
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate daily summary for: $targetDate" }
            val failedSummary = persistenceService.saveFailedSummary(targetDate)

            DailySummaryGenerationResult(
                dailySummary = failedSummary,
                telegramSent = false,
                skipped = false,
                skippedReason = null,
                failed = true,
                errorMessage = SUMMARY_FAILURE_MESSAGE,
                stats = null
            )
        }
    }

    /**
     * 요약 생성 + 텔레그램 전송
     */
    fun generateAndSendDailySummary(
        targetDate: LocalDate,
        skipIfExists: Boolean = false
    ): DailySummaryGenerationResult {
        logger.info { "Generating and sending daily summary for: $targetDate (skipIfExists=$skipIfExists)" }

        if (shouldSkip(targetDate, skipIfExists)) {
            return DailySummaryGenerationResult.alreadyExists(targetDate)
        }

        return try {
            // 1. DB 조회 (@Transactional readOnly): 데이터 수집
            // 2. LLM API 호출 (트랜잭션 외부): 요약 생성
            val summaryResult = generateSummaryForDate(targetDate)

            // 3. DB 저장 (@Transactional): 요약 저장
            val savedSummary = persistenceService.saveSummary(targetDate, summaryResult)

            // 4. HTTP 호출 (트랜잭션 외부): 텔레그램 전송
            val telegramMessage = formatTelegramMessage(targetDate, summaryResult.summary)
            val sent = telegramClient.sendMessageSync(telegramMessage)

            // 5. DB 업데이트 (@Transactional): 발송 상태 변경 (ID 기반 재조회)
            val resultSummary = if (sent) {
                val updatedSummary = persistenceService.markSummaryAsSent(savedSummary.id)
                if (updatedSummary != null) {
                    logger.info { "Daily summary sent to Telegram successfully" }
                } else {
                    logger.warn { "Telegram sent but failed to mark summary as SENT: id=${savedSummary.id}" }
                }
                updatedSummary ?: savedSummary
            } else {
                logger.warn { "Failed to send daily summary to Telegram" }
                savedSummary
            }

            DailySummaryGenerationResult(
                dailySummary = resultSummary,
                telegramSent = sent,
                skipped = false,
                skippedReason = null,
                failed = false,
                errorMessage = null,
                stats = summaryResult.stats
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate daily summary for: $targetDate" }
            val failedSummary = persistenceService.saveFailedSummary(targetDate)

            DailySummaryGenerationResult(
                dailySummary = failedSummary,
                telegramSent = false,
                skipped = false,
                skippedReason = null,
                failed = true,
                errorMessage = SUMMARY_FAILURE_MESSAGE,
                stats = null
            )
        }
    }

    /**
     * 날짜 기준 데이터 수집 + LLM 요약 생성
     * DB 조회는 PersistenceService(@Transactional readOnly)를 통해,
     * LLM API 호출은 트랜잭션 밖에서 수행
     */
    private fun generateSummaryForDate(targetDate: LocalDate): LlmDailySummaryResult {
        val startOfDay = LocalDateTime.of(targetDate, LocalTime.MIN)
        val endOfDay = LocalDateTime.of(targetDate, LocalTime.MAX)

        val jobPostings = persistenceService.findJobPostingsBetween(startOfDay, endOfDay)
        val newsArticles = persistenceService.findNewsArticlesBetween(startOfDay, endOfDay)
        val communityPosts = persistenceService.findCommunityPostsBetween(startOfDay, endOfDay)

        logger.info {
            "Data collected for $targetDate - Jobs: ${jobPostings.size}, " +
                    "News: ${newsArticles.size}, Community: ${communityPosts.size}"
        }

        return summarizationService.generateDailySummary(
            date = targetDate,
            jobPostings = jobPostings,
            newsArticles = newsArticles,
            communityPosts = communityPosts
        )
    }

    /**
     * SENT 상태의 요약만 스킵 — DRAFT/FAILED 상태는 재시도 허용
     */
    private fun shouldSkip(targetDate: LocalDate, skipIfExists: Boolean): Boolean {
        if (skipIfExists && persistenceService.existsBySummaryDateAndSent(targetDate)) {
            logger.info { "Daily summary already sent for: $targetDate" }
            return true
        }
        return false
    }

    private fun formatTelegramMessage(date: LocalDate, summary: String): String {
        return """
🇯🇵 *일본 IT 취업 일일 브리핑*
📅 ${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일

$summary

---
_Ready Japan Bot 🤖_
        """.trimIndent()
    }
}
