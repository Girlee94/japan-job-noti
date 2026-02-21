package com.readyjapan.infrastructure.orchestration

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.NewsArticle
import com.readyjapan.infrastructure.external.llm.service.TranslationService
import com.readyjapan.infrastructure.orchestration.persistence.TranslationPersistenceService
import com.readyjapan.infrastructure.orchestration.result.TranslationBatchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * 번역 오케스트레이션 서비스
 * Controller와 Scheduler에서 공유하는 번역 비즈니스 로직
 *
 * @Transactional 없음 — 외부 LLM API 호출은 트랜잭션 밖에서,
 * DB 저장은 TranslationPersistenceService(별도 빈)에서 수행
 *
 * 엔티티 라이프사이클:
 * 1. PersistenceService(readOnly TX)에서 fetch → TX 종료 후 엔티티는 detached 상태
 * 2. LLM API 호출 후 detached 엔티티에 번역 결과 적용 (applyTranslation)
 * 3. PersistenceService(write TX)에서 saveAll → JPA merge로 변경사항 반영
 * DB에서 fetch된 엔티티는 항상 유효한 ID(> 0)를 가지므로 merge 동작이 보장됨.
 */
@Service
class TranslationOrchestrationService(
    private val persistenceService: TranslationPersistenceService,
    private val translationService: TranslationService
) {

    companion object {
        private const val DEFAULT_BATCH_SIZE = 10
    }

    fun translatePendingContent(batchSize: Int = DEFAULT_BATCH_SIZE): TranslationBatchResult {
        logger.info { "Starting translation batch (batchSize=$batchSize)" }

        val jobsTranslated = translateJobPostings(batchSize)
        val newsTranslated = translateNewsArticles(batchSize)
        val postsTranslated = translateCommunityPosts(batchSize)

        logger.info {
            "Translation batch completed - Jobs: $jobsTranslated, " +
                    "News: $newsTranslated, Community: $postsTranslated"
        }

        return TranslationBatchResult(
            jobPostingsTranslated = jobsTranslated,
            newsArticlesTranslated = newsTranslated,
            communityPostsTranslated = postsTranslated
        )
    }

    private fun translateJobPostings(batchSize: Int): Int {
        val pendingJobs = persistenceService.findAllJobsNeedingTranslation()
            .take(batchSize)

        if (pendingJobs.isEmpty()) return 0

        logger.debug { "Translating ${pendingJobs.size} job postings" }

        val toSave = mutableListOf<JobPosting>()
        for (job in pendingJobs) {
            try {
                val result = translationService.translateTitleAndContent(
                    title = job.title,
                    content = job.description
                )

                result.translatedTitle?.let {
                    job.applyTranslation(it, result.translatedContent, null)
                    toSave.add(job)
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to translate job posting: ${job.id}" }
            }
        }

        persistenceService.saveAllJobPostings(toSave)
        return toSave.size
    }

    private fun translateNewsArticles(batchSize: Int): Int {
        val pendingNews = persistenceService.findAllNewsNeedingTranslation()
            .take(batchSize)

        if (pendingNews.isEmpty()) return 0

        logger.debug { "Translating ${pendingNews.size} news articles" }

        val toSave = mutableListOf<NewsArticle>()
        for (news in pendingNews) {
            try {
                val titleResult = translationService.translate(news.title)
                val summaryResult = news.summary?.let { translationService.translate(it) }

                if (titleResult != null) {
                    news.applyTranslation(titleResult, summaryResult, null)
                    toSave.add(news)
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to translate news article: ${news.id}" }
            }
        }

        persistenceService.saveAllNewsArticles(toSave)
        return toSave.size
    }

    private fun translateCommunityPosts(batchSize: Int): Int {
        val pendingPosts = persistenceService.findAllPostsNeedingTranslation()
            .take(batchSize)

        if (pendingPosts.isEmpty()) return 0

        logger.debug { "Translating ${pendingPosts.size} community posts" }

        val toSave = mutableListOf<CommunityPost>()
        for (post in pendingPosts) {
            try {
                val title = post.title ?: post.content.take(100)
                val result = translationService.translateTitleAndContent(
                    title = title,
                    content = post.content
                )

                if (result.translatedTitle != null) {
                    post.applyTranslation(result.translatedTitle, result.translatedContent)
                    toSave.add(post)
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to translate community post: ${post.id}" }
            }
        }

        persistenceService.saveAllCommunityPosts(toSave)
        return toSave.size
    }
}
