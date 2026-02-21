package com.readyjapan.infrastructure.orchestration

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.NewsArticle
import com.readyjapan.infrastructure.external.llm.service.TranslationResult
import com.readyjapan.infrastructure.external.llm.service.TranslationService
import com.readyjapan.infrastructure.orchestration.persistence.TranslationPersistenceService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class TranslationOrchestrationServiceTest : BehaviorSpec({

    val persistenceService = mockk<TranslationPersistenceService>()
    val translationService = mockk<TranslationService>()

    val service = TranslationOrchestrationService(persistenceService, translationService)

    beforeEach {
        clearMocks(persistenceService, translationService)
    }

    Given("translatePendingContent") {

        When("번역할 콘텐츠가 없으면") {
            Then("모든 카운트가 0인 결과를 반환한다") {
                every { persistenceService.findAllJobsNeedingTranslation() } returns emptyList()
                every { persistenceService.findAllNewsNeedingTranslation() } returns emptyList()
                every { persistenceService.findAllPostsNeedingTranslation() } returns emptyList()
                every { persistenceService.saveAllJobPostings(any()) } just Runs
                every { persistenceService.saveAllNewsArticles(any()) } just Runs
                every { persistenceService.saveAllCommunityPosts(any()) } just Runs

                val result = service.translatePendingContent()

                result.jobPostingsTranslated shouldBe 0
                result.newsArticlesTranslated shouldBe 0
                result.communityPostsTranslated shouldBe 0
            }
        }

        When("채용 공고 번역 성공 시") {
            Then("번역된 개수를 반환하고 saveAll을 호출한다") {
                val job = mockk<JobPosting>(relaxed = true)
                every { job.title } returns "エンジニア募集"
                every { job.description } returns "詳細"
                every { job.id } returns 1L

                every { persistenceService.findAllJobsNeedingTranslation() } returns listOf(job)
                every { persistenceService.findAllNewsNeedingTranslation() } returns emptyList()
                every { persistenceService.findAllPostsNeedingTranslation() } returns emptyList()
                every { translationService.translateTitleAndContent("エンジニア募集", "詳細") } returns TranslationResult(
                    translatedTitle = "엔지니어 모집",
                    translatedContent = "상세"
                )
                every { persistenceService.saveAllJobPostings(any()) } just Runs
                every { persistenceService.saveAllNewsArticles(any()) } just Runs
                every { persistenceService.saveAllCommunityPosts(any()) } just Runs

                val result = service.translatePendingContent()

                result.jobPostingsTranslated shouldBe 1
                verify(exactly = 1) { job.applyTranslation("엔지니어 모집", "상세", null) }
                verify(exactly = 1) { persistenceService.saveAllJobPostings(match { it.size == 1 }) }
            }
        }

        When("번역 실패 시") {
            Then("실패한 항목은 저장하지 않는다") {
                val job = mockk<JobPosting>(relaxed = true)
                every { job.title } returns "タイトル"
                every { job.description } returns "内容"
                every { job.id } returns 1L

                every { persistenceService.findAllJobsNeedingTranslation() } returns listOf(job)
                every { persistenceService.findAllNewsNeedingTranslation() } returns emptyList()
                every { persistenceService.findAllPostsNeedingTranslation() } returns emptyList()
                every { translationService.translateTitleAndContent(any(), any()) } throws RuntimeException("API error")
                every { persistenceService.saveAllJobPostings(any()) } just Runs
                every { persistenceService.saveAllNewsArticles(any()) } just Runs
                every { persistenceService.saveAllCommunityPosts(any()) } just Runs

                val result = service.translatePendingContent()

                result.jobPostingsTranslated shouldBe 0
                verify(exactly = 1) { persistenceService.saveAllJobPostings(match { it.isEmpty() }) }
            }
        }
    }
})
