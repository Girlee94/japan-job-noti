package com.readyjapan.batch.scheduler

import com.readyjapan.infrastructure.orchestration.TranslationOrchestrationService
import com.readyjapan.infrastructure.orchestration.result.TranslationBatchResult
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class TranslationSchedulerTest : BehaviorSpec({

    val translationOrchestrationService = mockk<TranslationOrchestrationService>()
    val translationScheduler = TranslationScheduler(translationOrchestrationService)

    beforeEach {
        clearMocks(translationOrchestrationService)
    }

    Given("translatePendingContent") {
        When("스케줄러 실행 시") {
            Then("OrchestrationService에 위임한다") {
                every {
                    translationOrchestrationService.translatePendingContent()
                } returns TranslationBatchResult(
                    jobPostingsTranslated = 2,
                    newsArticlesTranslated = 1,
                    communityPostsTranslated = 3
                )

                translationScheduler.translatePendingContent()

                verify(exactly = 1) { translationOrchestrationService.translatePendingContent() }
            }
        }
    }
})
