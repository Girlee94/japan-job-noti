package com.readyjapan.batch.scheduler

import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.core.domain.entity.enums.SummaryStatus
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.core.domain.repository.DailySummaryRepository
import com.readyjapan.core.domain.repository.JobPostingRepository
import com.readyjapan.core.domain.repository.NewsArticleRepository
import com.readyjapan.infrastructure.external.llm.service.SummarizationService
import com.readyjapan.infrastructure.external.telegram.TelegramClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 일일 요약 생성 및 텔레그램 전송 스케줄러
 * 매일 오전 9시 (JST) 실행
 */
@Component
class DailySummaryScheduler(
    private val jobPostingRepository: JobPostingRepository,
    private val newsArticleRepository: NewsArticleRepository,
    private val communityPostRepository: CommunityPostRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val summarizationService: SummarizationService,
    private val telegramClient: TelegramClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 매일 오전 9시에 전날 수집된 데이터를 요약하고 텔레그램으로 전송
     */
    @Scheduled(cron = "\${app.scheduler.daily-summary.cron:0 0 9 * * *}")
    @Transactional
    fun generateAndSendDailySummary() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        log.info("Starting daily summary generation for: $yesterday")

        // 이미 요약이 생성되었는지 확인
        if (dailySummaryRepository.existsBySummaryDate(yesterday)) {
            log.info("Daily summary already exists for: $yesterday")
            return
        }

        try {
            // 전날 수집된 데이터 조회
            val startOfYesterday = LocalDateTime.of(yesterday, LocalTime.MIN)
            val endOfYesterday = LocalDateTime.of(yesterday, LocalTime.MAX)

            val jobPostings = jobPostingRepository.findAllByCreatedAtAfter(startOfYesterday)
                .filter { it.createdAt?.isBefore(endOfYesterday) == true }

            val newsArticles = newsArticleRepository.findAllByCreatedAtAfter(startOfYesterday)
                .filter { it.createdAt?.isBefore(endOfYesterday) == true }

            val communityPosts = communityPostRepository.findAllByCreatedAtAfter(startOfYesterday)
                .filter { it.createdAt?.isBefore(endOfYesterday) == true }

            log.info(
                "Data collected for $yesterday - Jobs: ${jobPostings.size}, " +
                        "News: ${newsArticles.size}, Community: ${communityPosts.size}"
            )

            // LLM으로 요약 생성
            val summaryResult = summarizationService.generateDailySummary(
                date = yesterday,
                jobPostings = jobPostings,
                newsArticles = newsArticles,
                communityPosts = communityPosts
            )

            // DailySummary 엔티티 생성 및 저장
            val dailySummary = DailySummary(
                summaryDate = yesterday,
                summaryContent = summaryResult.summary,
                jobPostingCount = summaryResult.stats.jobPostingCount,
                newsArticleCount = summaryResult.stats.newsArticleCount,
                communityPostCount = summaryResult.stats.communityPostCount,
                status = if (summaryResult.success) SummaryStatus.SENT else SummaryStatus.FAILED
            )

            val savedSummary = dailySummaryRepository.save(dailySummary)
            log.info("Daily summary saved with id: ${savedSummary.id}")

            // 텔레그램으로 전송
            val telegramMessage = formatTelegramMessage(yesterday, summaryResult.summary)
            val sent = telegramClient.sendMessageSync(telegramMessage)

            if (sent) {
                savedSummary.markAsSent()
                dailySummaryRepository.save(savedSummary)
                log.info("Daily summary sent to Telegram successfully")
            } else {
                log.warn("Failed to send daily summary to Telegram")
            }

        } catch (e: Exception) {
            log.error("Failed to generate daily summary for: $yesterday", e)

            // 실패 기록 저장
            val failedSummary = DailySummary(
                summaryDate = yesterday,
                summaryContent = "요약 생성 실패: ${e.message}",
                jobPostingCount = 0,
                newsArticleCount = 0,
                communityPostCount = 0,
                status = SummaryStatus.FAILED
            )
            dailySummaryRepository.save(failedSummary)
        }
    }

    /**
     * 수동 요약 생성 (특정 날짜)
     */
    @Transactional
    fun generateSummaryForDate(date: LocalDate): DailySummary {
        log.info("Generating summary for specific date: $date")

        val startOfDay = LocalDateTime.of(date, LocalTime.MIN)
        val endOfDay = LocalDateTime.of(date, LocalTime.MAX)

        val jobPostings = jobPostingRepository.findAllByCreatedAtAfter(startOfDay)
            .filter { it.createdAt?.isBefore(endOfDay) == true }

        val newsArticles = newsArticleRepository.findAllByCreatedAtAfter(startOfDay)
            .filter { it.createdAt?.isBefore(endOfDay) == true }

        val communityPosts = communityPostRepository.findAllByCreatedAtAfter(startOfDay)
            .filter { it.createdAt?.isBefore(endOfDay) == true }

        val summaryResult = summarizationService.generateDailySummary(
            date = date,
            jobPostings = jobPostings,
            newsArticles = newsArticles,
            communityPosts = communityPosts
        )

        val dailySummary = DailySummary(
            summaryDate = date,
            summaryContent = summaryResult.summary,
            jobPostingCount = summaryResult.stats.jobPostingCount,
            newsArticleCount = summaryResult.stats.newsArticleCount,
            communityPostCount = summaryResult.stats.communityPostCount,
            status = if (summaryResult.success) SummaryStatus.SENT else SummaryStatus.FAILED
        )

        return dailySummaryRepository.save(dailySummary)
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
