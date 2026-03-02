package com.readyjapan.api.service

import com.readyjapan.api.controller.dto.CrawlSourceResponse
import com.readyjapan.api.controller.dto.DailySummaryResponse
import com.readyjapan.api.controller.dto.DashboardStatsResponse
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.core.domain.repository.CrawlSourceRepository
import com.readyjapan.core.domain.repository.DailySummaryRepository
import com.readyjapan.core.domain.repository.JobPostingRepository
import com.readyjapan.core.domain.repository.NewsArticleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val communityPostRepository: CommunityPostRepository,
    private val jobPostingRepository: JobPostingRepository,
    private val newsArticleRepository: NewsArticleRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val crawlSourceRepository: CrawlSourceRepository
) {

    fun getStats(): DashboardStatsResponse {
        val today = LocalDate.now()
        val todayStart = today.atStartOfDay()
        val todayEnd = LocalDateTime.of(today, LocalTime.MAX)

        val todayPosts = communityPostRepository.countByCreatedAtBetween(todayStart, todayEnd)
        val todayJobs = jobPostingRepository.countByCreatedAtBetween(todayStart, todayEnd)
        val todayNews = newsArticleRepository.countByCreatedAtBetween(todayStart, todayEnd)
        val latestSummary = dailySummaryRepository.findLatest()
        val enabledSources = crawlSourceRepository.findAllEnabled()

        return DashboardStatsResponse(
            todayCommunityPosts = todayPosts,
            todayJobPostings = todayJobs,
            todayNewsArticles = todayNews,
            latestSummary = latestSummary?.let { DailySummaryResponse.from(it) },
            activeSources = enabledSources.size,
            recentCrawlStatus = when {
                enabledSources.isEmpty() -> "소스 없음"
                enabledSources.all { it.lastCrawledAt != null } -> "정상"
                else -> "일부 미실행"
            }
        )
    }

    fun getSources(): List<CrawlSourceResponse> {
        return crawlSourceRepository.findAll().map { CrawlSourceResponse.from(it) }
    }
}
