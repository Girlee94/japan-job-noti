package com.readyjapan.batch.scheduler

import com.readyjapan.infrastructure.orchestration.SentimentOrchestrationService
import com.readyjapan.infrastructure.orchestration.result.SentimentBatchResult
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class SentimentAnalysisSchedulerTest : BehaviorSpec({

    val sentimentOrchestrationService = mockk<SentimentOrchestrationService>()
    val sentimentAnalysisScheduler = SentimentAnalysisScheduler(sentimentOrchestrationService)

    beforeEach {
        clearMocks(sentimentOrchestrationService)
    }

    Given("analyzePendingSentiments") {
        When("스케줄러 실행 시") {
            Then("OrchestrationService에 위임한다") {
                every {
                    sentimentOrchestrationService.analyzePendingSentiments()
                } returns SentimentBatchResult(
                    analyzedCount = 5,
                    failedCount = 1
                )

                sentimentAnalysisScheduler.analyzePendingSentiments()

                verify(exactly = 1) { sentimentOrchestrationService.analyzePendingSentiments() }
            }
        }
    }
})
