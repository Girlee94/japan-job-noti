package com.readyjapan.api.controller

import com.readyjapan.core.common.response.ApiResponse
import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.core.domain.entity.enums.SummaryStatus
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.core.domain.repository.DailySummaryRepository
import com.readyjapan.core.domain.repository.JobPostingRepository
import com.readyjapan.core.domain.repository.NewsArticleRepository
import com.readyjapan.infrastructure.external.llm.service.SentimentAnalysisService
import com.readyjapan.infrastructure.external.llm.service.SummarizationService
import com.readyjapan.infrastructure.external.llm.service.TranslationService
import com.readyjapan.infrastructure.external.telegram.TelegramClient
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * LLM 서비스 수동 트리거 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/llm")
class LlmController(
    private val jobPostingRepository: JobPostingRepository,
    private val newsArticleRepository: NewsArticleRepository,
    private val communityPostRepository: CommunityPostRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val translationService: TranslationService,
    private val sentimentAnalysisService: SentimentAnalysisService,
    private val summarizationService: SummarizationService,
    private val telegramClient: TelegramClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BATCH_SIZE = 10
    }

    /**
     * 번역 수동 트리거
     */
    @PostMapping("/translate")
    @Transactional
    fun triggerTranslation(): ApiResponse<TranslationResultDto> {
        log.info("Manual translation triggered")

        var jobsTranslated = 0
        var newsTranslated = 0
        var postsTranslated = 0

        // Job Postings
        jobPostingRepository.findAllNeedingTranslation().take(BATCH_SIZE).forEach { job ->
            try {
                val result = translationService.translateTitleAndContent(job.title, job.description)
                result.translatedTitle?.let {
                    job.applyTranslation(it, result.translatedContent, null)
                    jobPostingRepository.save(job)
                    jobsTranslated++
                }
            } catch (e: Exception) {
                log.error("Failed to translate job: ${job.id}", e)
            }
        }

        // News Articles
        newsArticleRepository.findAllNeedingTranslation().take(BATCH_SIZE).forEach { news ->
            try {
                val titleResult = translationService.translate(news.title)
                val summaryResult = news.summary?.let { translationService.translate(it) }
                if (titleResult != null) {
                    news.applyTranslation(titleResult, summaryResult, null)
                    newsArticleRepository.save(news)
                    newsTranslated++
                }
            } catch (e: Exception) {
                log.error("Failed to translate news: ${news.id}", e)
            }
        }

        // Community Posts
        communityPostRepository.findAllNeedingTranslation().take(BATCH_SIZE).forEach { post ->
            try {
                val title = post.title ?: post.content.take(100)
                val result = translationService.translateTitleAndContent(title, post.content)
                if (result.translatedTitle != null) {
                    post.applyTranslation(result.translatedTitle, result.translatedContent)
                    communityPostRepository.save(post)
                    postsTranslated++
                }
            } catch (e: Exception) {
                log.error("Failed to translate post: ${post.id}", e)
            }
        }

        return ApiResponse.success(
            TranslationResultDto(
                jobPostingsTranslated = jobsTranslated,
                newsArticlesTranslated = newsTranslated,
                communityPostsTranslated = postsTranslated
            )
        )
    }

    /**
     * 감정 분석 수동 트리거
     */
    @PostMapping("/sentiment")
    @Transactional
    fun triggerSentimentAnalysis(): ApiResponse<SentimentAnalysisResultDto> {
        log.info("Manual sentiment analysis triggered")

        var analyzed = 0
        var failed = 0

        communityPostRepository.findAllNeedingSentimentAnalysis().take(BATCH_SIZE).forEach { post ->
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

        return ApiResponse.success(
            SentimentAnalysisResultDto(
                analyzedCount = analyzed,
                failedCount = failed,
                pendingCount = communityPostRepository.findAllNeedingSentimentAnalysis().size
            )
        )
    }

    /**
     * 일일 요약 수동 생성
     */
    @PostMapping("/summary")
    @Transactional
    fun generateDailySummary(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?
    ): ApiResponse<DailySummary> {
        val targetDate = date ?: LocalDate.now().minusDays(1)
        log.info("Manual summary generation for: $targetDate")

        val startOfDay = LocalDateTime.of(targetDate, LocalTime.MIN)
        val endOfDay = LocalDateTime.of(targetDate, LocalTime.MAX)

        val jobPostings = jobPostingRepository.findAllByCreatedAtAfter(startOfDay)
            .filter { it.createdAt?.isBefore(endOfDay) == true }
        val newsArticles = newsArticleRepository.findAllByCreatedAtAfter(startOfDay)
            .filter { it.createdAt?.isBefore(endOfDay) == true }
        val communityPosts = communityPostRepository.findAllByCreatedAtAfter(startOfDay)
            .filter { it.createdAt?.isBefore(endOfDay) == true }

        val summaryResult = summarizationService.generateDailySummary(
            date = targetDate,
            jobPostings = jobPostings,
            newsArticles = newsArticles,
            communityPosts = communityPosts
        )

        val dailySummary = DailySummary(
            summaryDate = targetDate,
            summaryContent = summaryResult.summary,
            jobPostingCount = summaryResult.stats.jobPostingCount,
            newsArticleCount = summaryResult.stats.newsArticleCount,
            communityPostCount = summaryResult.stats.communityPostCount,
            status = if (summaryResult.success) SummaryStatus.DRAFT else SummaryStatus.FAILED
        )

        val saved = dailySummaryRepository.save(dailySummary)
        return ApiResponse.success(saved)
    }

    /**
     * 일일 요약 생성 및 텔레그램 전송
     */
    @PostMapping("/summary/send")
    @Transactional
    fun generateAndSendDailySummary(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?
    ): ApiResponse<Map<String, Any>> {
        val targetDate = date ?: LocalDate.now().minusDays(1)

        // 요약 생성
        val startOfDay = LocalDateTime.of(targetDate, LocalTime.MIN)
        val endOfDay = LocalDateTime.of(targetDate, LocalTime.MAX)

        val jobPostings = jobPostingRepository.findAllByCreatedAtAfter(startOfDay)
            .filter { it.createdAt?.isBefore(endOfDay) == true }
        val newsArticles = newsArticleRepository.findAllByCreatedAtAfter(startOfDay)
            .filter { it.createdAt?.isBefore(endOfDay) == true }
        val communityPosts = communityPostRepository.findAllByCreatedAtAfter(startOfDay)
            .filter { it.createdAt?.isBefore(endOfDay) == true }

        val summaryResult = summarizationService.generateDailySummary(
            date = targetDate,
            jobPostings = jobPostings,
            newsArticles = newsArticles,
            communityPosts = communityPosts
        )

        // 저장
        val dailySummary = DailySummary(
            summaryDate = targetDate,
            summaryContent = summaryResult.summary,
            jobPostingCount = summaryResult.stats.jobPostingCount,
            newsArticleCount = summaryResult.stats.newsArticleCount,
            communityPostCount = summaryResult.stats.communityPostCount,
            status = if (summaryResult.success) SummaryStatus.DRAFT else SummaryStatus.FAILED
        )
        val saved = dailySummaryRepository.save(dailySummary)

        // 텔레그램 전송
        val telegramMessage = """
🇯🇵 *일본 IT 취업 일일 브리핑*
📅 ${targetDate.year}년 ${targetDate.monthValue}월 ${targetDate.dayOfMonth}일

${summaryResult.summary}

---
_Ready Japan Bot 🤖_
        """.trimIndent()

        val sent = telegramClient.sendMessageSync(telegramMessage)

        if (sent) {
            saved.markAsSent()
            dailySummaryRepository.save(saved)
        }

        return ApiResponse.success(
            mapOf(
                "summaryId" to saved.id,
                "date" to targetDate.toString(),
                "telegramSent" to sent,
                "stats" to mapOf(
                    "jobPostings" to summaryResult.stats.jobPostingCount,
                    "newsArticles" to summaryResult.stats.newsArticleCount,
                    "communityPosts" to summaryResult.stats.communityPostCount
                )
            )
        )
    }
}

data class TranslationResultDto(
    val jobPostingsTranslated: Int,
    val newsArticlesTranslated: Int,
    val communityPostsTranslated: Int
) {
    val totalTranslated: Int get() = jobPostingsTranslated + newsArticlesTranslated + communityPostsTranslated
}

data class SentimentAnalysisResultDto(
    val analyzedCount: Int,
    val failedCount: Int,
    val pendingCount: Int
)
