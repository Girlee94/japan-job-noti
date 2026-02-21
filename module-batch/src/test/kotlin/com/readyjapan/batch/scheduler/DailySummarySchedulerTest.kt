package com.readyjapan.batch.scheduler

import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.core.domain.entity.enums.SummaryStatus
import com.readyjapan.infrastructure.external.llm.service.SummaryStats
import com.readyjapan.infrastructure.orchestration.DailySummaryOrchestrationService
import com.readyjapan.infrastructure.orchestration.result.DailySummaryGenerationResult
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.ZoneId

class DailySummarySchedulerTest : BehaviorSpec({

    val dailySummaryOrchestrationService = mockk<DailySummaryOrchestrationService>()
    val dailySummaryScheduler = DailySummaryScheduler(dailySummaryOrchestrationService)

    beforeEach {
        clearMocks(dailySummaryOrchestrationService)
    }

    Given("generateAndSendDailySummary") {
        When("스케줄러 실행 시") {
            Then("OrchestrationService에 skipIfExists=true로 위임한다") {
                val yesterday = LocalDate.now(ZoneId.of("Asia/Tokyo")).minusDays(1)
                every {
                    dailySummaryOrchestrationService.generateAndSendDailySummary(
                        targetDate = yesterday,
                        skipIfExists = true
                    )
                } returns DailySummaryGenerationResult(
                    dailySummary = DailySummary(
                        summaryDate = yesterday,
                        summaryContent = "테스트 요약",
                        jobPostingCount = 1,
                        newsArticleCount = 2,
                        communityPostCount = 3,
                        status = SummaryStatus.SENT
                    ),
                    telegramSent = true,
                    skipped = false,
                    skippedReason = null,
                    failed = false,
                    errorMessage = null,
                    stats = SummaryStats(1, 2, 3)
                )

                dailySummaryScheduler.generateAndSendDailySummary()

                verify(exactly = 1) {
                    dailySummaryOrchestrationService.generateAndSendDailySummary(
                        targetDate = yesterday,
                        skipIfExists = true
                    )
                }
            }
        }

        When("이미 요약이 존재하는 경우") {
            Then("스킵 결과를 로그에 남긴다") {
                val yesterday = LocalDate.now(ZoneId.of("Asia/Tokyo")).minusDays(1)
                every {
                    dailySummaryOrchestrationService.generateAndSendDailySummary(
                        targetDate = yesterday,
                        skipIfExists = true
                    )
                } returns DailySummaryGenerationResult.alreadyExists(yesterday)

                dailySummaryScheduler.generateAndSendDailySummary()

                verify(exactly = 1) {
                    dailySummaryOrchestrationService.generateAndSendDailySummary(
                        targetDate = yesterday,
                        skipIfExists = true
                    )
                }
            }
        }

        When("요약 생성 실패 시") {
            Then("실패 결과를 로그에 남기며 예외가 전파되지 않는다") {
                val yesterday = LocalDate.now(ZoneId.of("Asia/Tokyo")).minusDays(1)
                val failureResult = DailySummaryGenerationResult.failure("DB error")
                every {
                    dailySummaryOrchestrationService.generateAndSendDailySummary(
                        targetDate = yesterday,
                        skipIfExists = true
                    )
                } returns failureResult

                // 실패 결과여도 스케줄러가 예외 없이 정상 실행됨을 검증
                dailySummaryScheduler.generateAndSendDailySummary()

                verify(exactly = 1) {
                    dailySummaryOrchestrationService.generateAndSendDailySummary(
                        targetDate = yesterday,
                        skipIfExists = true
                    )
                }
                // 결과 상태 검증
                failureResult.failed shouldBe true
                failureResult.telegramSent shouldBe false
            }
        }
    }
})
