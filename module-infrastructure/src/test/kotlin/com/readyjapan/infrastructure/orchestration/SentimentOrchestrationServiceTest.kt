package com.readyjapan.infrastructure.orchestration

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.enums.Sentiment
import com.readyjapan.infrastructure.external.llm.service.SentimentAnalysisService
import com.readyjapan.infrastructure.external.llm.service.SentimentResult
import com.readyjapan.infrastructure.orchestration.persistence.SentimentPersistenceService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class SentimentOrchestrationServiceTest : BehaviorSpec({

    val persistenceService = mockk<SentimentPersistenceService>()
    val sentimentAnalysisService = mockk<SentimentAnalysisService>()

    val service = SentimentOrchestrationService(persistenceService, sentimentAnalysisService)

    beforeEach {
        clearMocks(persistenceService, sentimentAnalysisService)
    }

    Given("analyzePendingSentiments") {

        When("분석할 게시글이 없으면") {
            Then("0/0 결과를 반환한다") {
                every { persistenceService.findAllNeedingSentimentAnalysis() } returns emptyList()

                val result = service.analyzePendingSentiments()

                result.analyzedCount shouldBe 0
                result.failedCount shouldBe 0
                verify(exactly = 0) { persistenceService.saveAllAnalyzed(any()) }
            }
        }

        When("모든 게시글 분석 성공 시") {
            Then("전체 개수를 반환하고 saveAll을 호출한다") {
                val post1 = mockk<CommunityPost>(relaxed = true)
                val post2 = mockk<CommunityPost>(relaxed = true)
                every { post1.titleTranslated } returns "제목1"
                every { post1.title } returns null
                every { post1.contentTranslated } returns "내용1"
                every { post1.content } returns "원문1"
                every { post1.id } returns 1L
                every { post2.titleTranslated } returns null
                every { post2.title } returns "제목2"
                every { post2.contentTranslated } returns null
                every { post2.content } returns "내용2"
                every { post2.id } returns 2L

                every { persistenceService.findAllNeedingSentimentAnalysis() } returns listOf(post1, post2)
                every { sentimentAnalysisService.analyzeWithContext(any(), any()) } returns SentimentResult(
                    sentiment = Sentiment.POSITIVE,
                    reason = "긍정적"
                )
                every { persistenceService.saveAllAnalyzed(any()) } just Runs

                val result = service.analyzePendingSentiments()

                result.analyzedCount shouldBe 2
                result.failedCount shouldBe 0
                verify(exactly = 1) { persistenceService.saveAllAnalyzed(match { it.size == 2 }) }
            }
        }

        When("일부 게시글 분석 실패 시") {
            Then("성공/실패 개수를 정확히 반환한다") {
                val post1 = mockk<CommunityPost>(relaxed = true)
                val post2 = mockk<CommunityPost>(relaxed = true)
                every { post1.titleTranslated } returns "제목1"
                every { post1.contentTranslated } returns "내용1"
                every { post1.id } returns 1L
                every { post2.titleTranslated } returns "제목2"
                every { post2.contentTranslated } returns "내용2"
                every { post2.id } returns 2L

                every { persistenceService.findAllNeedingSentimentAnalysis() } returns listOf(post1, post2)
                every { sentimentAnalysisService.analyzeWithContext("제목1", "내용1") } returns SentimentResult(
                    sentiment = Sentiment.POSITIVE,
                    reason = "긍정적"
                )
                every { sentimentAnalysisService.analyzeWithContext("제목2", "내용2") } throws RuntimeException("LLM error")
                every { persistenceService.saveAllAnalyzed(any()) } just Runs

                val result = service.analyzePendingSentiments()

                result.analyzedCount shouldBe 1
                result.failedCount shouldBe 1
                verify(exactly = 1) { persistenceService.saveAllAnalyzed(match { it.size == 1 }) }
            }
        }
    }
})
