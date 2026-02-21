package com.readyjapan.infrastructure.orchestration.persistence

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.NewsArticle
import com.readyjapan.core.domain.entity.enums.SummaryStatus
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.core.domain.repository.DailySummaryRepository
import com.readyjapan.core.domain.repository.JobPostingRepository
import com.readyjapan.core.domain.repository.NewsArticleRepository
import com.readyjapan.infrastructure.external.llm.service.DailySummaryResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

/**
 * 일일 요약 DB 조회/저장 전용 서비스
 * Spring AOP proxy를 통해 @Transactional이 정상 동작하도록 별도 빈으로 분리
 *
 * LLM API 호출은 포함하지 않음 — 순수 DB 작업만 담당
 */
@Service
@Transactional(readOnly = true)
class DailySummaryPersistenceService(
    private val jobPostingRepository: JobPostingRepository,
    private val newsArticleRepository: NewsArticleRepository,
    private val communityPostRepository: CommunityPostRepository,
    private val dailySummaryRepository: DailySummaryRepository
) {

    fun existsBySummaryDateAndSent(date: LocalDate): Boolean {
        val summary = dailySummaryRepository.findBySummaryDate(date)
        return summary != null && summary.status == SummaryStatus.SENT
    }

    fun findJobPostingsBetween(start: LocalDateTime, end: LocalDateTime): List<JobPosting> =
        jobPostingRepository.findAllByCreatedAtBetween(start, end)

    fun findNewsArticlesBetween(start: LocalDateTime, end: LocalDateTime): List<NewsArticle> =
        newsArticleRepository.findAllByCreatedAtBetween(start, end)

    fun findCommunityPostsBetween(start: LocalDateTime, end: LocalDateTime): List<CommunityPost> =
        communityPostRepository.findAllByCreatedAtBetween(start, end)

    /**
     * LLM 요약 결과를 DB에 저장
     * 동일 날짜에 기존 레코드(DRAFT/FAILED)가 있으면 업데이트, 없으면 신규 생성
     */
    @Transactional
    fun saveSummary(targetDate: LocalDate, summaryResult: DailySummaryResult): DailySummary {
        val status = if (summaryResult.success) SummaryStatus.DRAFT else SummaryStatus.FAILED
        val existing = dailySummaryRepository.findBySummaryDate(targetDate)

        val dailySummary = if (existing != null) {
            existing.updateSummary(summaryResult.summary, null, null)
            existing.updateCounts(
                summaryResult.stats.jobPostingCount,
                summaryResult.stats.newsArticleCount,
                summaryResult.stats.communityPostCount
            )
            existing.status = status
            existing
        } else {
            DailySummary(
                summaryDate = targetDate,
                summaryContent = summaryResult.summary,
                jobPostingCount = summaryResult.stats.jobPostingCount,
                newsArticleCount = summaryResult.stats.newsArticleCount,
                communityPostCount = summaryResult.stats.communityPostCount,
                status = status
            )
        }

        val saved = dailySummaryRepository.save(dailySummary)
        logger.info { "Daily summary saved with id: ${saved.id} (${if (existing != null) "updated" else "created"})" }
        return saved
    }

    /**
     * 발송 완료 처리
     * detached entity 문제를 방지하기 위해 ID로 재조회 후 상태 변경
     *
     * @return SENT 상태로 업데이트된 엔티티, 조회 실패 시 null
     */
    @Transactional
    fun markSummaryAsSent(summaryId: Long): DailySummary? {
        require(summaryId > 0L) { "summaryId must be a valid persisted ID, got: $summaryId" }
        val summary = dailySummaryRepository.findById(summaryId) ?: run {
            logger.warn { "Summary not found for marking as sent: id=$summaryId" }
            return null
        }
        summary.markAsSent()
        return dailySummaryRepository.save(summary)
    }

    /**
     * 실패 기록 저장
     * 동일 날짜에 기존 레코드가 있으면 FAILED로 업데이트, 없으면 신규 생성
     */
    @Transactional
    fun saveFailedSummary(targetDate: LocalDate): DailySummary {
        val existing = dailySummaryRepository.findBySummaryDate(targetDate)

        val failedSummary = if (existing != null) {
            existing.markAsFailed()
            existing.updateSummary("요약 생성 실패", null, null)
            existing.updateCounts(0, 0, 0)
            existing
        } else {
            DailySummary(
                summaryDate = targetDate,
                summaryContent = "요약 생성 실패",
                jobPostingCount = 0,
                newsArticleCount = 0,
                communityPostCount = 0,
                status = SummaryStatus.FAILED
            )
        }

        return dailySummaryRepository.save(failedSummary)
    }
}
