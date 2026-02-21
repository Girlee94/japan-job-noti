package com.readyjapan.infrastructure.orchestration

import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.core.domain.entity.enums.SummaryStatus
import com.readyjapan.infrastructure.external.llm.service.DailySummaryResult
import com.readyjapan.infrastructure.external.llm.service.SummarizationService
import com.readyjapan.infrastructure.external.llm.service.SummaryStats
import com.readyjapan.infrastructure.external.telegram.TelegramClient
import com.readyjapan.infrastructure.orchestration.persistence.DailySummaryPersistenceService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DailySummaryOrchestrationServiceTest : BehaviorSpec({

    val persistenceService = mockk<DailySummaryPersistenceService>()
    val summarizationService = mockk<SummarizationService>()
    val telegramClient = mockk<TelegramClient>()

    val service = DailySummaryOrchestrationService(
        persistenceService,
        summarizationService,
        telegramClient
    )

    val targetDate = LocalDate.of(2025, 1, 15)
    val startOfDay = LocalDateTime.of(targetDate, LocalTime.MIN)
    val endOfDay = LocalDateTime.of(targetDate, LocalTime.MAX)

    val summaryResult = DailySummaryResult(
        summary = "테스트 요약",
        success = true,
        stats = SummaryStats(jobPostingCount = 2, newsArticleCount = 3, communityPostCount = 5)
    )

    val savedSummary = DailySummary(
        summaryDate = targetDate,
        summaryContent = "테스트 요약",
        jobPostingCount = 2,
        newsArticleCount = 3,
        communityPostCount = 5,
        status = SummaryStatus.DRAFT
    )

    beforeEach {
        clearMocks(persistenceService, summarizationService, telegramClient)
    }

    Given("generateDailySummary") {

        When("skipIfExists=true이고 SENT 상태 요약이 존재하면") {
            Then("alreadyExists 결과를 반환한다") {
                every { persistenceService.existsBySummaryDateAndSent(targetDate) } returns true

                val result = service.generateDailySummary(targetDate, skipIfExists = true)

                result.skipped shouldBe true
                result.skippedReason shouldBe "Daily summary already exists for: $targetDate"
                verify(exactly = 0) { summarizationService.generateDailySummary(any(), any(), any(), any()) }
            }
        }

        When("skipIfExists=true이고 DRAFT 상태 요약만 존재하면") {
            Then("스킵하지 않고 새로 생성한다") {
                every { persistenceService.existsBySummaryDateAndSent(targetDate) } returns false
                every { persistenceService.findJobPostingsBetween(startOfDay, endOfDay) } returns emptyList()
                every { persistenceService.findNewsArticlesBetween(startOfDay, endOfDay) } returns emptyList()
                every { persistenceService.findCommunityPostsBetween(startOfDay, endOfDay) } returns emptyList()
                every { summarizationService.generateDailySummary(targetDate, any(), any(), any()) } returns summaryResult
                every { persistenceService.saveSummary(targetDate, summaryResult) } returns savedSummary

                val result = service.generateDailySummary(targetDate, skipIfExists = true)

                result.skipped shouldBe false
                result.failed shouldBe false
                result.dailySummary shouldBe savedSummary
            }
        }

        When("요약 생성 성공 시") {
            Then("저장된 요약과 stats를 반환한다") {
                every { persistenceService.existsBySummaryDateAndSent(targetDate) } returns false
                every { persistenceService.findJobPostingsBetween(startOfDay, endOfDay) } returns emptyList()
                every { persistenceService.findNewsArticlesBetween(startOfDay, endOfDay) } returns emptyList()
                every { persistenceService.findCommunityPostsBetween(startOfDay, endOfDay) } returns emptyList()
                every { summarizationService.generateDailySummary(targetDate, any(), any(), any()) } returns summaryResult
                every { persistenceService.saveSummary(targetDate, summaryResult) } returns savedSummary

                val result = service.generateDailySummary(targetDate, skipIfExists = false)

                result.failed shouldBe false
                result.telegramSent shouldBe false
                result.stats shouldBe summaryResult.stats
            }
        }

        When("LLM 호출 실패 시") {
            Then("실패 요약을 저장하고 failure 결과를 반환한다") {
                val failedSummary = DailySummary(
                    summaryDate = targetDate,
                    summaryContent = "요약 생성 실패",
                    jobPostingCount = 0,
                    newsArticleCount = 0,
                    communityPostCount = 0,
                    status = SummaryStatus.FAILED
                )
                every { persistenceService.findJobPostingsBetween(startOfDay, endOfDay) } returns emptyList()
                every { persistenceService.findNewsArticlesBetween(startOfDay, endOfDay) } returns emptyList()
                every { persistenceService.findCommunityPostsBetween(startOfDay, endOfDay) } returns emptyList()
                every { summarizationService.generateDailySummary(targetDate, any(), any(), any()) } throws RuntimeException("OpenAI timeout")
                every { persistenceService.saveFailedSummary(targetDate) } returns failedSummary

                val result = service.generateDailySummary(targetDate, skipIfExists = false)

                result.failed shouldBe true
                result.stats shouldBe null
                verify(exactly = 1) { persistenceService.saveFailedSummary(targetDate) }
            }
        }
    }

    Given("generateAndSendDailySummary") {

        When("텔레그램 전송 성공 시") {
            Then("markSummaryAsSent를 호출하고 SENT 상태 엔티티를 반환한다") {
                val sentSummary = DailySummary(
                    summaryDate = targetDate,
                    summaryContent = "테스트 요약",
                    jobPostingCount = 2,
                    newsArticleCount = 3,
                    communityPostCount = 5,
                    status = SummaryStatus.SENT
                )
                every { persistenceService.existsBySummaryDateAndSent(targetDate) } returns false
                every { persistenceService.findJobPostingsBetween(startOfDay, endOfDay) } returns emptyList()
                every { persistenceService.findNewsArticlesBetween(startOfDay, endOfDay) } returns emptyList()
                every { persistenceService.findCommunityPostsBetween(startOfDay, endOfDay) } returns emptyList()
                every { summarizationService.generateDailySummary(targetDate, any(), any(), any()) } returns summaryResult
                every { persistenceService.saveSummary(targetDate, summaryResult) } returns savedSummary
                every { telegramClient.sendMessageSync(any()) } returns true
                every { persistenceService.markSummaryAsSent(any()) } returns sentSummary

                val result = service.generateAndSendDailySummary(targetDate, skipIfExists = false)

                result.telegramSent shouldBe true
                result.failed shouldBe false
                result.dailySummary shouldBe sentSummary
                verify(exactly = 1) { persistenceService.markSummaryAsSent(savedSummary.id) }
            }
        }

        When("텔레그램 전송 실패 시") {
            Then("markSummaryAsSent를 호출하지 않는다") {
                every { persistenceService.existsBySummaryDateAndSent(targetDate) } returns false
                every { persistenceService.findJobPostingsBetween(startOfDay, endOfDay) } returns emptyList()
                every { persistenceService.findNewsArticlesBetween(startOfDay, endOfDay) } returns emptyList()
                every { persistenceService.findCommunityPostsBetween(startOfDay, endOfDay) } returns emptyList()
                every { summarizationService.generateDailySummary(targetDate, any(), any(), any()) } returns summaryResult
                every { persistenceService.saveSummary(targetDate, summaryResult) } returns savedSummary
                every { telegramClient.sendMessageSync(any()) } returns false

                val result = service.generateAndSendDailySummary(targetDate, skipIfExists = false)

                result.telegramSent shouldBe false
                result.failed shouldBe false
                verify(exactly = 0) { persistenceService.markSummaryAsSent(any()) }
            }
        }
    }
})
