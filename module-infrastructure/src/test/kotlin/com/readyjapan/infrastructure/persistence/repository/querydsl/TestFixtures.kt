package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.readyjapan.core.domain.entity.*
import com.readyjapan.core.domain.entity.enums.*
import jakarta.persistence.EntityManager
import java.time.LocalDate
import java.time.LocalDateTime

object TestFixtures {

    fun crawlSource(
        name: String = "Test Source",
        url: String = "https://example.com",
        sourceType: SourceType = SourceType.COMMUNITY,
        platform: CommunityPlatform? = CommunityPlatform.REDDIT,
        enabled: Boolean = true
    ): CrawlSource = CrawlSource(
        name = name,
        url = url,
        sourceType = sourceType,
        platform = platform,
        enabled = enabled
    )

    fun communityPost(
        source: CrawlSource,
        externalId: String = "ext-${System.nanoTime()}",
        platform: CommunityPlatform = CommunityPlatform.REDDIT,
        title: String? = "テスト投稿",
        content: String = "テスト内容です",
        language: String = "ja",
        sentiment: Sentiment? = null,
        contentTranslated: String? = null,
        likeCount: Int = 0,
        commentCount: Int = 0,
        publishedAt: LocalDateTime = LocalDateTime.now()
    ): CommunityPost = CommunityPost(
        source = source,
        externalId = externalId,
        platform = platform,
        title = title,
        content = content,
        language = language,
        sentiment = sentiment,
        contentTranslated = contentTranslated,
        likeCount = likeCount,
        commentCount = commentCount,
        originalUrl = "https://example.com/posts/$externalId",
        publishedAt = publishedAt
    )

    fun jobPosting(
        source: CrawlSource,
        externalId: String = "job-${System.nanoTime()}",
        title: String = "Kotlinエンジニア募集",
        companyName: String = "テスト株式会社",
        language: String = "ja",
        status: PostingStatus = PostingStatus.ACTIVE,
        titleTranslated: String? = null
    ): JobPosting = JobPosting(
        source = source,
        externalId = externalId,
        title = title,
        companyName = companyName,
        language = language,
        status = status,
        titleTranslated = titleTranslated,
        originalUrl = "https://example.com/jobs/$externalId"
    )

    fun newsArticle(
        source: CrawlSource,
        externalId: String = "news-${System.nanoTime()}",
        title: String = "日本IT業界ニュース",
        language: String = "ja",
        titleTranslated: String? = null,
        publishedAt: LocalDateTime? = LocalDateTime.now()
    ): NewsArticle = NewsArticle(
        source = source,
        externalId = externalId,
        title = title,
        language = language,
        titleTranslated = titleTranslated,
        publishedAt = publishedAt,
        originalUrl = "https://example.com/news/$externalId"
    )

    fun crawlHistory(
        source: CrawlSource,
        status: CrawlStatus = CrawlStatus.SUCCESS,
        startedAt: LocalDateTime = LocalDateTime.now()
    ): CrawlHistory = CrawlHistory(
        source = source,
        status = status,
        startedAt = startedAt
    )

    fun dailySummary(
        summaryDate: LocalDate = LocalDate.now(),
        summaryContent: String = "오늘의 요약입니다.",
        status: SummaryStatus = SummaryStatus.DRAFT
    ): DailySummary = DailySummary(
        summaryDate = summaryDate,
        summaryContent = summaryContent,
        status = status
    )
}

private val ALLOWED_TABLES = setOf(
    "community_posts", "job_postings", "news_articles",
    "crawl_histories", "crawl_sources", "daily_summaries"
)

fun EntityManager.updateCreatedAt(tableName: String, id: Long, createdAt: LocalDateTime) {
    require(tableName in ALLOWED_TABLES) { "Invalid table name: $tableName" }
    createNativeQuery("UPDATE $tableName SET created_at = :createdAt WHERE id = :id")
        .setParameter("createdAt", createdAt)
        .setParameter("id", id)
        .executeUpdate()
    flush()
    clear()
}
