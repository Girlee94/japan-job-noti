package com.readyjapan.batch.scheduler

import com.readyjapan.infrastructure.external.telegram.AlertService
import com.readyjapan.infrastructure.orchestration.TranslationOrchestrationService
import com.readyjapan.infrastructure.orchestration.result.TranslationBatchResult
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class TranslationSchedulerTest : BehaviorSpec({

    val translationOrchestrationService = mockk<TranslationOrchestrationService>()
    val alertService = mockk<AlertService>()
    val translationScheduler = TranslationScheduler(translationOrchestrationService, alertService)

    beforeEach {
        clearMocks(translationOrchestrationService, alertService)
        justRun { alertService.sendAlert(any(), any(), any()) }
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

        When("예외 발생 시") {
            Then("예외가 전파되지 않고 알림이 전송된다") {
                every {
                    translationOrchestrationService.translatePendingContent()
                } throws RuntimeException("Translation error")

                translationScheduler.translatePendingContent()

                verify(exactly = 1) { alertService.sendAlert("translation-batch", any(), any()) }
            }
        }
    }
})
