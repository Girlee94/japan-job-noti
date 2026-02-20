package com.readyjapan.batch.scheduler

import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.core.domain.entity.enums.SummaryStatus
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.core.domain.repository.DailySummaryRepository
import com.readyjapan.core.domain.repository.JobPostingRepository
import com.readyjapan.core.domain.repository.NewsArticleRepository
import com.readyjapan.infrastructure.external.llm.service.DailySummaryResult
import com.readyjapan.infrastructure.external.llm.service.SummarizationService
import com.readyjapan.infrastructure.external.llm.service.SummaryStats
import com.readyjapan.infrastructure.external.telegram.TelegramClient
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate

class DailySummarySchedulerTest : BehaviorSpec({

    val jobPostingRepository = mockk<JobPostingRepository>()
    val newsArticleRepository = mockk<NewsArticleRepository>()
    val communityPostRepository = mockk<CommunityPostRepository>()
    val dailySummaryRepository = mockk<DailySummaryRepository>()
    val summarizationService = mockk<SummarizationService>()
    val telegramClient = mockk<TelegramClient>()
    val dailySummaryScheduler = DailySummaryScheduler(
        jobPostingRepository, newsArticleRepository, communityPostRepository,
        dailySummaryRepository, summarizationService, telegramClient
    )

    beforeEach {
        clearMocks(
            jobPostingRepository, newsArticleRepository, communityPostRepository,
            dailySummaryRepository, summarizationService, telegramClient
        )
    }

    Given("generateAndSendDailySummary") {
        When("이미 요약 존재 시") {
            Then("스킵한다") {
                val yesterday = LocalDate.now().minusDays(1)
                every { dailySummaryRepository.existsBySummaryDate(yesterday) } returns true

                dailySummaryScheduler.generateAndSendDailySummary()

                verify(exactly = 0) { summarizationService.generateDailySummary(any(), any(), any(), any()) }
            }
        }
        When("정상 생성 및 전송 시") {
            Then("요약 생성과 텔레그램 전송이 수행된다") {
                val yesterday = LocalDate.now().minusDays(1)
                every { dailySummaryRepository.existsBySummaryDate(yesterday) } returns false
                every { jobPostingRepository.findAllByCreatedAtAfter(any()) } returns emptyList()
                every { newsArticleRepository.findAllByCreatedAtAfter(any()) } returns emptyList()
                every { communityPostRepository.findAllByCreatedAtAfter(any()) } returns emptyList()
                every {
                    summarizationService.generateDailySummary(any(), any(), any(), any())
                } returns DailySummaryResult(
                    summary = "오늘의 요약입니다.",
                    success = true,
                    stats = SummaryStats(jobPostingCount = 0, newsArticleCount = 0, communityPostCount = 0)
                )
                val summarySlot = slot<DailySummary>()
                every { dailySummaryRepository.save(capture(summarySlot)) } answers { summarySlot.captured }
                every { telegramClient.sendMessageSync(any()) } returns true

                dailySummaryScheduler.generateAndSendDailySummary()

                verify(exactly = 1) { summarizationService.generateDailySummary(any(), any(), any(), any()) }
                verify(exactly = 1) { telegramClient.sendMessageSync(any()) }
                verify(atLeast = 2) { dailySummaryRepository.save(any()) }
            }
        }
        When("텔레그램 실패 시") {
            Then("markAsSent가 호출되지 않는다") {
                val yesterday = LocalDate.now().minusDays(1)
                every { dailySummaryRepository.existsBySummaryDate(yesterday) } returns false
                every { jobPostingRepository.findAllByCreatedAtAfter(any()) } returns emptyList()
                every { newsArticleRepository.findAllByCreatedAtAfter(any()) } returns emptyList()
                every { communityPostRepository.findAllByCreatedAtAfter(any()) } returns emptyList()
                every {
                    summarizationService.generateDailySummary(any(), any(), any(), any())
                } returns DailySummaryResult(
                    summary = "요약 내용",
                    success = true,
                    stats = SummaryStats(0, 0, 0)
                )
                val summarySlot = slot<DailySummary>()
                every { dailySummaryRepository.save(capture(summarySlot)) } answers { summarySlot.captured }
                every { telegramClient.sendMessageSync(any()) } returns false

                dailySummaryScheduler.generateAndSendDailySummary()

                verify(exactly = 1) { telegramClient.sendMessageSync(any()) }
                verify(exactly = 1) { dailySummaryRepository.save(any()) }
            }
        }
        When("예외 발생 시") {
            Then("FAILED 요약을 저장한다") {
                val yesterday = LocalDate.now().minusDays(1)
                every { dailySummaryRepository.existsBySummaryDate(yesterday) } returns false
                every { jobPostingRepository.findAllByCreatedAtAfter(any()) } throws RuntimeException("DB error")
                val summarySlot = slot<DailySummary>()
                every { dailySummaryRepository.save(capture(summarySlot)) } answers { summarySlot.captured }

                dailySummaryScheduler.generateAndSendDailySummary()

                verify(exactly = 1) { dailySummaryRepository.save(any()) }
                summarySlot.captured.status shouldBe SummaryStatus.FAILED
                summarySlot.captured.summaryContent shouldContain "요약 생성 실패"
            }
        }
    }

    Given("generateSummaryForDate") {
        When("지정 날짜로 요약 생성 시") {
            Then("해당 날짜의 요약을 저장한다") {
                val date = LocalDate.of(2026, 1, 15)
                every { jobPostingRepository.findAllByCreatedAtAfter(any()) } returns emptyList()
                every { newsArticleRepository.findAllByCreatedAtAfter(any()) } returns emptyList()
                every { communityPostRepository.findAllByCreatedAtAfter(any()) } returns emptyList()
                every {
                    summarizationService.generateDailySummary(any(), any(), any(), any())
                } returns DailySummaryResult(
                    summary = "1월 15일 요약",
                    success = true,
                    stats = SummaryStats(0, 0, 0)
                )
                val summarySlot = slot<DailySummary>()
                every { dailySummaryRepository.save(capture(summarySlot)) } answers { summarySlot.captured }

                val result = dailySummaryScheduler.generateSummaryForDate(date)

                result.summaryDate shouldBe date
                result.summaryContent shouldBe "1월 15일 요약"
                result.status shouldBe SummaryStatus.SENT
            }
        }
    }
})
