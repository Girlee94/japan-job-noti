package com.readyjapan.batch.scheduler

import com.readyjapan.infrastructure.orchestration.TranslationOrchestrationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * 번역 배치 스케줄러
 * 번역이 필요한 콘텐츠를 자동으로 번역
 */
@Component
class TranslationScheduler(
    private val translationOrchestrationService: TranslationOrchestrationService
) {

    /**
     * 30분마다 번역이 필요한 콘텐츠 번역
     */
    @Scheduled(fixedRate = 1800000) // 30분
    fun translatePendingContent() {
        logger.info { "Starting translation batch job" }
        val result = translationOrchestrationService.translatePendingContent()
        logger.info {
            "Translation batch completed - Jobs: ${result.jobPostingsTranslated}, " +
                    "News: ${result.newsArticlesTranslated}, Community: ${result.communityPostsTranslated}"
        }
    }
}
