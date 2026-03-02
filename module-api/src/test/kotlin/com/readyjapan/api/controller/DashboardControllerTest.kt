package com.readyjapan.api.controller

import com.readyjapan.api.controller.dto.CrawlSourceResponse
import com.readyjapan.api.controller.dto.DailySummaryResponse
import com.readyjapan.api.controller.dto.DashboardStatsResponse
import com.readyjapan.api.service.DashboardService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class DashboardControllerTest : BehaviorSpec({

    val dashboardService = mockk<DashboardService>()
    val dashboardController = DashboardController(dashboardService)

    beforeEach {
        clearMocks(dashboardService)
    }

    Given("getStats") {
        When("대시보드 통계 조회 시") {
            Then("오늘의 수집 현황과 최신 요약을 반환한다") {
                val stats = DashboardStatsResponse(
                    todayCommunityPosts = 10,
                    todayJobPostings = 5,
                    todayNewsArticles = 8,
                    latestSummary = DailySummaryResponse(
                        id = 1L,
                        summaryDate = "2026-01-01",
                        summaryContent = "Today's summary",
                        jobPostingCount = 5,
                        newsArticleCount = 8,
                        communityPostCount = 10,
                        totalCount = 23,
                        status = "DRAFT",
                        sentAt = null,
                        createdAt = "2026-01-01T09:00:00"
                    ),
                    activeSources = 3,
                    recentCrawlStatus = "정상"
                )
                every { dashboardService.getStats() } returns stats

                val response = dashboardController.getStats()

                response.success shouldBe true
                response.data!!.todayCommunityPosts shouldBe 10
                response.data!!.todayJobPostings shouldBe 5
                response.data!!.todayNewsArticles shouldBe 8
                response.data!!.latestSummary shouldNotBe null
                response.data!!.activeSources shouldBe 3
                response.data!!.recentCrawlStatus shouldBe "정상"
            }
        }
    }

    Given("getSources") {
        When("크롤 소스 목록 조회 시") {
            Then("전체 소스 목록을 반환한다") {
                val sources = listOf(
                    CrawlSourceResponse(
                        id = 1L, name = "Source 1", url = "https://example.com/1",
                        sourceType = "COMMUNITY", platform = "REDDIT", enabled = true,
                        lastCrawledAt = "2026-01-01T12:00:00", createdAt = "2026-01-01T00:00:00"
                    ),
                    CrawlSourceResponse(
                        id = 2L, name = "Source 2", url = "https://example.com/2",
                        sourceType = "NEWS_SITE", platform = null, enabled = true,
                        lastCrawledAt = null, createdAt = "2026-01-01T00:00:00"
                    )
                )
                every { dashboardService.getSources() } returns sources

                val response = dashboardController.getSources()

                response.success shouldBe true
                response.data!! shouldHaveSize 2
            }
        }
    }
})
