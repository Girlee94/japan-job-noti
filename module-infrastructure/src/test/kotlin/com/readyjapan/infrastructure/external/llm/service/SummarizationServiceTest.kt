package com.readyjapan.infrastructure.external.llm.service

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.NewsArticle
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.infrastructure.external.llm.OpenAiClient
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime

class SummarizationServiceTest : BehaviorSpec({

    val openAiClient = mockk<OpenAiClient>()
    val summarizationService = SummarizationService(openAiClient)

    beforeEach { clearMocks(openAiClient) }

    fun createSource(type: SourceType = SourceType.COMMUNITY): CrawlSource = CrawlSource(
        id = 1L,
        name = "test-source",
        url = "https://example.com",
        sourceType = type,
        platform = if (type == SourceType.COMMUNITY) CommunityPlatform.REDDIT else null
    )

    fun createJobPosting(): JobPosting = JobPosting(
        source = createSource(SourceType.JOB_SITE),
        externalId = "job1",
        title = "エンジニア募集",
        companyName = "テスト株式会社",
        originalUrl = "https://example.com/jobs/1"
    )

    fun createNewsArticle(): NewsArticle = NewsArticle(
        source = createSource(SourceType.NEWS_SITE),
        externalId = "news1",
        title = "IT業界ニュース",
        originalUrl = "https://example.com/news/1"
    )

    fun createCommunityPost(): CommunityPost = CommunityPost(
        source = createSource(),
        externalId = "post1",
        platform = CommunityPlatform.REDDIT,
        content = "テスト投稿",
        originalUrl = "https://reddit.com/r/test/1",
        publishedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
    )

    Given("generateDailySummary") {
        When("정상 요약 생성 시") {
            Then("success true를 반환한다") {
                every {
                    openAiClient.chatCompletion(any(), any(), any(), any())
                } returns "## 오늘의 하이라이트\n요약 내용입니다."

                val result = summarizationService.generateDailySummary(
                    date = LocalDate.of(2026, 1, 15),
                    jobPostings = listOf(createJobPosting()),
                    newsArticles = listOf(createNewsArticle()),
                    communityPosts = listOf(createCommunityPost())
                )

                result.success shouldBe true
                result.summary shouldContain "오늘의 하이라이트"
                result.stats.jobPostingCount shouldBe 1
                result.stats.newsArticleCount shouldBe 1
                result.stats.communityPostCount shouldBe 1
                result.stats.totalCount shouldBe 3
            }
        }
        When("OpenAI null 응답 시") {
            Then("fallback 요약을 생성한다") {
                every {
                    openAiClient.chatCompletion(any(), any(), any(), any())
                } returns null

                val result = summarizationService.generateDailySummary(
                    date = LocalDate.of(2026, 1, 15),
                    jobPostings = listOf(createJobPosting()),
                    newsArticles = emptyList(),
                    communityPosts = emptyList()
                )

                result.success shouldBe false
                result.summary shouldContain "일본 IT 취업 정보"
                result.summary shouldContain "AI 요약 생성 실패"
                result.stats.jobPostingCount shouldBe 1
            }
        }
        When("빈 데이터 시") {
            Then("정상 처리된다") {
                every {
                    openAiClient.chatCompletion(any(), any(), any(), any())
                } returns "데이터가 없는 날의 요약입니다."

                val result = summarizationService.generateDailySummary(
                    date = LocalDate.of(2026, 1, 15),
                    jobPostings = emptyList(),
                    newsArticles = emptyList(),
                    communityPosts = emptyList()
                )

                result.success shouldBe true
                result.stats.totalCount shouldBe 0
            }
        }
        When("fallback 요약 생성 시") {
            Then("회사명이 포함된다") {
                every {
                    openAiClient.chatCompletion(any(), any(), any(), any())
                } returns null

                val result = summarizationService.generateDailySummary(
                    date = LocalDate.of(2026, 1, 15),
                    jobPostings = listOf(createJobPosting()),
                    newsArticles = emptyList(),
                    communityPosts = emptyList()
                )

                result.summary shouldContain "テスト株式会社"
            }
        }
    }
})
