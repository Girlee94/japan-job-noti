package com.readyjapan.api.controller.dto

data class DashboardStatsResponse(
    val todayCommunityPosts: Int,
    val todayJobPostings: Int,
    val todayNewsArticles: Int,
    val latestSummary: DailySummaryResponse?,
    val activeSources: Int,
    val recentCrawlStatus: String
)
