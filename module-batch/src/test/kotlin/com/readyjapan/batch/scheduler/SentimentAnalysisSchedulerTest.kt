package com.readyjapan.batch.scheduler

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.Sentiment
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.infrastructure.external.llm.service.SentimentAnalysisService
import com.readyjapan.infrastructure.external.llm.service.SentimentResult
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class SentimentAnalysisSchedulerTest : BehaviorSpec({

    val communityPostRepository = mockk<CommunityPostRepository>()
    val sentimentAnalysisService = mockk<SentimentAnalysisService>()
    val sentimentAnalysisScheduler = SentimentAnalysisScheduler(
        communityPostRepository, sentimentAnalysisService
    )

    beforeEach {
        clearMocks(communityPostRepository, sentimentAnalysisService)
    }

    fun createSource(): CrawlSource = CrawlSource(
        id = 1L,
        name = "test-source",
        url = "https://reddit.com/r/japanlife",
        sourceType = SourceType.COMMUNITY,
        platform = CommunityPlatform.REDDIT
    )

    fun createPost(
        id: Long = 1L,
        title: String? = "Test Title",
        content: String = "Test content"
    ): CommunityPost = CommunityPost(
        id = id,
        source = createSource(),
        externalId = "post$id",
        platform = CommunityPlatform.REDDIT,
        title = title,
        content = content,
        originalUrl = "https://reddit.com/r/japanlife/post$id",
        publishedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
    )

    Given("analyzePendingSentiments") {
        When("빈 목록 시") {
            Then("분석을 건너뛴다") {
                every { communityPostRepository.findAllNeedingSentimentAnalysis() } returns emptyList()

                sentimentAnalysisScheduler.analyzePendingSentiments()

                verify(exactly = 0) { sentimentAnalysisService.analyzeWithContext(any(), any()) }
            }
        }
        When("정상 분석 시") {
            Then("감정을 적용하고 저장한다") {
                val post = createPost()
                every { communityPostRepository.findAllNeedingSentimentAnalysis() } returns listOf(post)
                every {
                    sentimentAnalysisService.analyzeWithContext(any(), any())
                } returns SentimentResult(Sentiment.POSITIVE, "좋은 소식")
                every { communityPostRepository.save(any()) } answers { firstArg() }

                sentimentAnalysisScheduler.analyzePendingSentiments()

                post.sentiment shouldBe Sentiment.POSITIVE
                verify(exactly = 1) { communityPostRepository.save(post) }
            }
        }
        When("개별 실패 시") {
            Then("나머지는 계속 진행한다") {
                val post1 = createPost(id = 1L)
                val post2 = createPost(id = 2L)
                every { communityPostRepository.findAllNeedingSentimentAnalysis() } returns listOf(post1, post2)
                every {
                    sentimentAnalysisService.analyzeWithContext(eq("Test Title"), eq("Test content"))
                } throws RuntimeException("Analysis failed") andThen SentimentResult(Sentiment.NEUTRAL, "중립")
                every { communityPostRepository.save(any()) } answers { firstArg() }

                sentimentAnalysisScheduler.analyzePendingSentiments()

                verify(exactly = 1) { communityPostRepository.save(any()) }
            }
        }
    }

    Given("triggerManualAnalysis") {
        When("정상 분석 시") {
            Then("분석 카운트를 반환한다") {
                val post = createPost()
                every {
                    communityPostRepository.findAllNeedingSentimentAnalysis()
                } returns listOf(post) andThen emptyList()
                every {
                    sentimentAnalysisService.analyzeWithContext(any(), any())
                } returns SentimentResult(Sentiment.NEGATIVE, "부정적")
                every { communityPostRepository.save(any()) } answers { firstArg() }

                val result = sentimentAnalysisScheduler.triggerManualAnalysis()

                result.analyzedCount shouldBe 1
                result.failedCount shouldBe 0
                result.pendingCount shouldBe 0
            }
        }
        When("실패 포함 시") {
            Then("failedCount가 반영된다") {
                val post1 = createPost(id = 1L)
                val post2 = createPost(id = 2L)
                every {
                    communityPostRepository.findAllNeedingSentimentAnalysis()
                } returns listOf(post1, post2) andThen emptyList()

                var callCount = 0
                every {
                    sentimentAnalysisService.analyzeWithContext(any(), any())
                } answers {
                    callCount++
                    if (callCount == 1) throw RuntimeException("Failed")
                    else SentimentResult(Sentiment.NEUTRAL, "중립")
                }
                every { communityPostRepository.save(any()) } answers { firstArg() }

                val result = sentimentAnalysisScheduler.triggerManualAnalysis()

                result.analyzedCount shouldBe 1
                result.failedCount shouldBe 1
            }
        }
    }
})
