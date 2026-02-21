package com.readyjapan.infrastructure.orchestration

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.infrastructure.external.llm.service.SentimentAnalysisService
import com.readyjapan.infrastructure.orchestration.persistence.SentimentPersistenceService
import com.readyjapan.infrastructure.orchestration.result.SentimentBatchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * 감정 분석 오케스트레이션 서비스
 * Controller와 Scheduler에서 공유하는 감정 분석 비즈니스 로직
 *
 * @Transactional 없음 — 외부 LLM API 호출은 트랜잭션 밖에서,
 * DB 저장은 SentimentPersistenceService(별도 빈)에서 수행
 *
 * 엔티티 라이프사이클:
 * 1. PersistenceService(readOnly TX)에서 fetch → TX 종료 후 엔티티는 detached 상태
 * 2. LLM API 호출 후 detached 엔티티에 감정 분석 결과 적용 (applySentiment)
 * 3. PersistenceService(write TX)에서 saveAll → JPA merge로 변경사항 반영
 * DB에서 fetch된 엔티티는 항상 유효한 ID(> 0)를 가지므로 merge 동작이 보장됨.
 */
@Service
class SentimentOrchestrationService(
    private val persistenceService: SentimentPersistenceService,
    private val sentimentAnalysisService: SentimentAnalysisService
) {

    companion object {
        private const val DEFAULT_BATCH_SIZE = 20
    }

    fun analyzePendingSentiments(batchSize: Int = DEFAULT_BATCH_SIZE): SentimentBatchResult {
        logger.info { "Starting sentiment analysis batch (batchSize=$batchSize)" }

        val pendingPosts = persistenceService.findAllNeedingSentimentAnalysis()
            .take(batchSize)

        if (pendingPosts.isEmpty()) {
            logger.info { "No posts pending sentiment analysis" }
            return SentimentBatchResult(analyzedCount = 0, failedCount = 0)
        }

        logger.debug { "Analyzing sentiment for ${pendingPosts.size} posts" }

        var failed = 0
        val toSave = mutableListOf<CommunityPost>()

        for (post in pendingPosts) {
            try {
                val title = post.titleTranslated ?: post.title ?: ""
                val content = post.contentTranslated ?: post.content

                val result = sentimentAnalysisService.analyzeWithContext(title, content)

                post.applySentiment(result.sentiment)
                toSave.add(post)

                logger.debug { "Post ${post.id} sentiment: ${result.sentiment} - ${result.reason}" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to analyze sentiment for post: ${post.id}" }
                failed++
            }
        }

        persistenceService.saveAllAnalyzed(toSave)

        logger.info { "Sentiment analysis completed - Analyzed: ${toSave.size}, Failed: $failed" }

        return SentimentBatchResult(
            analyzedCount = toSave.size,
            failedCount = failed
        )
    }
}
