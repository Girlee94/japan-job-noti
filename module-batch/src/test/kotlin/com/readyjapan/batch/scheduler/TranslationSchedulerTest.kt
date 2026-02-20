package com.readyjapan.batch.scheduler

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.NewsArticle
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.core.domain.repository.JobPostingRepository
import com.readyjapan.core.domain.repository.NewsArticleRepository
import com.readyjapan.infrastructure.external.llm.service.TranslationResult
import com.readyjapan.infrastructure.external.llm.service.TranslationService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class TranslationSchedulerTest : BehaviorSpec({

    val jobPostingRepository = mockk<JobPostingRepository>()
    val newsArticleRepository = mockk<NewsArticleRepository>()
    val communityPostRepository = mockk<CommunityPostRepository>()
    val translationService = mockk<TranslationService>()
    val translationScheduler = TranslationScheduler(
        jobPostingRepository, newsArticleRepository, communityPostRepository, translationService
    )

    beforeEach {
        clearMocks(jobPostingRepository, newsArticleRepository, communityPostRepository, translationService)
    }

    fun createSource(type: SourceType = SourceType.JOB_SITE): CrawlSource = CrawlSource(
        id = 1L,
        name = "test-source",
        url = "https://example.com",
        sourceType = type,
        platform = if (type == SourceType.COMMUNITY) CommunityPlatform.REDDIT else null
    )

    Given("translatePendingContent") {
        When("모든 목록 비어있을 시") {
            Then("번역을 수행하지 않는다") {
                every { jobPostingRepository.findAllNeedingTranslation() } returns emptyList()
                every { newsArticleRepository.findAllNeedingTranslation() } returns emptyList()
                every { communityPostRepository.findAllNeedingTranslation() } returns emptyList()

                translationScheduler.translatePendingContent()

                verify(exactly = 0) { translationService.translate(any()) }
                verify(exactly = 0) { translationService.translateTitleAndContent(any(), any()) }
            }
        }
        When("번역 실패 시") {
            Then("save는 호출된다") {
                val job = JobPosting(
                    source = createSource(),
                    externalId = "j1",
                    title = "エンジニア募集",
                    companyName = "テスト社",
                    originalUrl = "https://example.com/j1"
                )
                every { jobPostingRepository.findAllNeedingTranslation() } returns listOf(job)
                every {
                    translationService.translateTitleAndContent(any(), any())
                } returns TranslationResult(null, null)
                every { jobPostingRepository.save(any()) } answers { firstArg() }
                every { newsArticleRepository.findAllNeedingTranslation() } returns emptyList()
                every { communityPostRepository.findAllNeedingTranslation() } returns emptyList()

                translationScheduler.translatePendingContent()

                verify(exactly = 1) { jobPostingRepository.save(any()) }
            }
        }
    }

    Given("triggerManualTranslation") {
        When("채용공고 번역 시") {
            Then("정상 카운트를 반환한다") {
                val job = JobPosting(
                    source = createSource(),
                    externalId = "j1",
                    title = "エンジニア募集",
                    companyName = "テスト社",
                    originalUrl = "https://example.com/j1"
                )
                every { jobPostingRepository.findAllNeedingTranslation() } returns listOf(job)
                every {
                    translationService.translateTitleAndContent(any(), any())
                } returns TranslationResult("엔지니어 모집", "상세 설명")
                every { jobPostingRepository.save(any()) } answers { firstArg() }
                every { newsArticleRepository.findAllNeedingTranslation() } returns emptyList()
                every { communityPostRepository.findAllNeedingTranslation() } returns emptyList()

                val result = translationScheduler.triggerManualTranslation()

                result.jobPostingsTranslated shouldBe 1
                result.newsArticlesTranslated shouldBe 0
                result.communityPostsTranslated shouldBe 0
            }
        }
        When("뉴스 번역 시") {
            Then("정상 카운트를 반환한다") {
                every { jobPostingRepository.findAllNeedingTranslation() } returns emptyList()
                val news = NewsArticle(
                    source = createSource(SourceType.NEWS_SITE),
                    externalId = "n1",
                    title = "IT業界ニュース",
                    summary = "要約テスト",
                    originalUrl = "https://example.com/n1"
                )
                every { newsArticleRepository.findAllNeedingTranslation() } returns listOf(news)
                every { translationService.translate("IT業界ニュース") } returns "IT 업계 뉴스"
                every { translationService.translate("要約テスト") } returns "요약 테스트"
                every { newsArticleRepository.save(any()) } answers { firstArg() }
                every { communityPostRepository.findAllNeedingTranslation() } returns emptyList()

                val result = translationScheduler.triggerManualTranslation()

                result.newsArticlesTranslated shouldBe 1
            }
        }
        When("커뮤니티 글 번역 시") {
            Then("정상 카운트를 반환한다") {
                every { jobPostingRepository.findAllNeedingTranslation() } returns emptyList()
                every { newsArticleRepository.findAllNeedingTranslation() } returns emptyList()
                val post = CommunityPost(
                    source = createSource(SourceType.COMMUNITY),
                    externalId = "p1",
                    platform = CommunityPlatform.REDDIT,
                    title = "テスト投稿",
                    content = "テスト内容です",
                    originalUrl = "https://reddit.com/r/japanlife/p1",
                    publishedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
                )
                every { communityPostRepository.findAllNeedingTranslation() } returns listOf(post)
                every {
                    translationService.translateTitleAndContent(any(), any())
                } returns TranslationResult("테스트 투고", "테스트 내용입니다")
                every { communityPostRepository.save(any()) } answers { firstArg() }

                val result = translationScheduler.triggerManualTranslation()

                result.communityPostsTranslated shouldBe 1
            }
        }
    }
})
