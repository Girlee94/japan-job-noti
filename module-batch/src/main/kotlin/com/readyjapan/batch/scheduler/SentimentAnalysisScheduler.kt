package com.readyjapan.batch.scheduler

import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.infrastructure.external.llm.service.SentimentAnalysisService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 감정 분석 배치 스케줄러
 * 커뮤니티 글의 감정을 자동으로 분석
 */
@Component
class SentimentAnalysisScheduler(
    private val communityPostRepository: CommunityPostRepository,
    private val sentimentAnalysisService: SentimentAnalysisService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BATCH_SIZE = 20
    }

    /**
     * 30분마다 감정 분석이 필요한 커뮤니티 글 분석
     */
    @Scheduled(fixedRate = 1800000) // 30분
    @Transactional
    fun analyzePendingSentiments() {
        log.info("Starting sentiment analysis batch job")

        val pendingPosts = communityPostRepository.findAllNeedingSentimentAnalysis()
            .take(BATCH_SIZE)

        if (pendingPosts.isEmpty()) {
            log.info("No posts pending sentiment analysis")
            return
        }

        log.debug("Analyzing sentiment for ${pendingPosts.size} posts")

        var analyzed = 0
        var failed = 0

        for (post in pendingPosts) {
            try {
                // 번역된 제목/내용이 있으면 그것을 사용, 없으면 원문 사용
                val title = post.titleTranslated ?: post.title ?: ""
                val content = post.contentTranslated ?: post.content

                val result = sentimentAnalysisService.analyzeWithContext(title, content)

                post.applySentiment(result.sentiment)
                communityPostRepository.save(post)
                analyzed++

                log.debug("Post ${post.id} sentiment: ${result.sentiment} - ${result.reason}")
            } catch (e: Exception) {
                log.error("Failed to analyze sentiment for post: ${post.id}", e)
                failed++
            }
        }

        log.info("Sentiment analysis completed - Analyzed: $analyzed, Failed: $failed")
    }

    /**
     * 수동 감정 분석 트리거
     */
    @Transactional
    fun triggerManualAnalysis(): SentimentAnalysisResult {
        val pendingPosts = communityPostRepository.findAllNeedingSentimentAnalysis()
            .take(BATCH_SIZE)

        var analyzed = 0
        var failed = 0

        for (post in pendingPosts) {
            try {
                val title = post.titleTranslated ?: post.title ?: ""
                val content = post.contentTranslated ?: post.content

                val result = sentimentAnalysisService.analyzeWithContext(title, content)

                post.applySentiment(result.sentiment)
                communityPostRepository.save(post)
                analyzed++
            } catch (e: Exception) {
                log.error("Failed to analyze sentiment for post: ${post.id}", e)
                failed++
            }
        }

        return SentimentAnalysisResult(
            analyzedCount = analyzed,
            failedCount = failed,
            pendingCount = communityPostRepository.findAllNeedingSentimentAnalysis().size
        )
    }
}

data class SentimentAnalysisResult(
    val analyzedCount: Int,
    val failedCount: Int,
    val pendingCount: Int
)
