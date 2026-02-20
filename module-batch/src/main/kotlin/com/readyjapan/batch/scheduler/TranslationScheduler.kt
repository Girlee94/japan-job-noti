package com.readyjapan.batch.scheduler

import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.core.domain.repository.JobPostingRepository
import com.readyjapan.core.domain.repository.NewsArticleRepository
import com.readyjapan.infrastructure.external.llm.service.TranslationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 번역 배치 스케줄러
 * 번역이 필요한 콘텐츠를 자동으로 번역
 */
@Component
class TranslationScheduler(
    private val jobPostingRepository: JobPostingRepository,
    private val newsArticleRepository: NewsArticleRepository,
    private val communityPostRepository: CommunityPostRepository,
    private val translationService: TranslationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BATCH_SIZE = 10
    }

    /**
     * 30분마다 번역이 필요한 콘텐츠 번역
     */
    @Scheduled(fixedRate = 1800000) // 30분
    @Transactional
    fun translatePendingContent() {
        log.info("Starting translation batch job")

        val jobsTranslated = translateJobPostings()
        val newsTranslated = translateNewsArticles()
        val postsTranslated = translateCommunityPosts()

        log.info(
            "Translation batch completed - Jobs: $jobsTranslated, " +
                    "News: $newsTranslated, Community: $postsTranslated"
        )
    }

    private fun translateJobPostings(): Int {
        val pendingJobs = jobPostingRepository.findAllNeedingTranslation()
            .take(BATCH_SIZE)

        if (pendingJobs.isEmpty()) {
            return 0
        }

        log.debug("Translating ${pendingJobs.size} job postings")

        var translated = 0
        for (job in pendingJobs) {
            try {
                val result = translationService.translateTitleAndContent(
                    title = job.title,
                    content = job.description
                )

                result.translatedTitle?.let {
                    job.applyTranslation(it, result.translatedContent, null)
                }
                jobPostingRepository.save(job)
                translated++
            } catch (e: Exception) {
                log.error("Failed to translate job posting: ${job.id}", e)
            }
        }

        return translated
    }

    private fun translateNewsArticles(): Int {
        val pendingNews = newsArticleRepository.findAllNeedingTranslation()
            .take(BATCH_SIZE)

        if (pendingNews.isEmpty()) {
            return 0
        }

        log.debug("Translating ${pendingNews.size} news articles")

        var translated = 0
        for (news in pendingNews) {
            try {
                val titleResult = translationService.translate(news.title)
                val summaryResult = news.summary?.let { translationService.translate(it) }

                if (titleResult != null) {
                    news.applyTranslation(titleResult, summaryResult, null)
                    newsArticleRepository.save(news)
                    translated++
                }
            } catch (e: Exception) {
                log.error("Failed to translate news article: ${news.id}", e)
            }
        }

        return translated
    }

    private fun translateCommunityPosts(): Int {
        val pendingPosts = communityPostRepository.findAllNeedingTranslation()
            .take(BATCH_SIZE)

        if (pendingPosts.isEmpty()) {
            return 0
        }

        log.debug("Translating ${pendingPosts.size} community posts")

        var translated = 0
        for (post in pendingPosts) {
            try {
                val title = post.title ?: post.content.take(100)
                val result = translationService.translateTitleAndContent(
                    title = title,
                    content = post.content
                )

                if (result.translatedTitle != null) {
                    post.applyTranslation(result.translatedTitle, result.translatedContent)
                    communityPostRepository.save(post)
                    translated++
                }
            } catch (e: Exception) {
                log.error("Failed to translate community post: ${post.id}", e)
            }
        }

        return translated
    }

    /**
     * 수동 번역 트리거
     */
    @Transactional
    fun triggerManualTranslation(): TranslationResult {
        val jobsTranslated = translateJobPostings()
        val newsTranslated = translateNewsArticles()
        val postsTranslated = translateCommunityPosts()

        return TranslationResult(
            jobPostingsTranslated = jobsTranslated,
            newsArticlesTranslated = newsTranslated,
            communityPostsTranslated = postsTranslated
        )
    }
}

data class TranslationResult(
    val jobPostingsTranslated: Int,
    val newsArticlesTranslated: Int,
    val communityPostsTranslated: Int
) {
    val totalTranslated: Int get() = jobPostingsTranslated + newsArticlesTranslated + communityPostsTranslated
}
