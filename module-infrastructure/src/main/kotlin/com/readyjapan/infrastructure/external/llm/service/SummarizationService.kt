package com.readyjapan.infrastructure.external.llm.service

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.NewsArticle
import com.readyjapan.infrastructure.external.llm.LlmClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 일일 요약 생성 서비스
 */
@Service
class SummarizationService(
    private val llmClient: LlmClient
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")

        private const val SYSTEM_PROMPT = """당신은 일본 IT 취업 정보를 분석하고 요약하는 전문가입니다.
한국인이 일본 IT 취업을 준비하는 데 도움이 되는 정보를 제공합니다.

요약 작성 규칙:
1. 핵심 정보만 간결하게 정리합니다.
2. 취업 준비에 실질적으로 도움이 되는 인사이트를 제공합니다.
3. 긍정적/부정적 트렌드를 균형있게 분석합니다.
4. 구체적인 수치나 회사명이 있으면 포함합니다.
5. 한국어로 작성합니다.

출력 형식:
## 📊 오늘의 하이라이트
(가장 중요한 1-2개 소식)

## 💼 채용 동향
(새로운 채용 공고 요약)

## 📰 주요 뉴스
(IT 업계 뉴스 요약)

## 💬 커뮤니티 반응
(Reddit 등 커뮤니티 분위기)

## 💡 취준생을 위한 팁
(오늘의 정보에서 얻을 수 있는 인사이트)"""
    }

    /**
     * 일일 요약 생성
     */
    fun generateDailySummary(
        date: LocalDate,
        jobPostings: List<JobPosting>,
        newsArticles: List<NewsArticle>,
        communityPosts: List<CommunityPost>
    ): DailySummaryResult {
        val dateStr = date.format(DATE_FORMATTER)
        log.info { "Generating daily summary for: $dateStr" }

        val inputData = buildInputData(dateStr, jobPostings, newsArticles, communityPosts)

        val response = llmClient.chatCompletion(
            systemPrompt = SYSTEM_PROMPT,
            userPrompt = inputData,
            temperature = 0.5, // 요약은 약간의 창의성 허용
            maxTokens = 2500
        )

        return if (response != null) {
            log.info { "Daily summary generated successfully" }
            DailySummaryResult(
                summary = response,
                success = true,
                stats = SummaryStats(
                    jobPostingCount = jobPostings.size,
                    newsArticleCount = newsArticles.size,
                    communityPostCount = communityPosts.size
                )
            )
        } else {
            log.error { "Failed to generate daily summary" }
            DailySummaryResult(
                summary = generateFallbackSummary(dateStr, jobPostings, newsArticles, communityPosts),
                success = false,
                stats = SummaryStats(
                    jobPostingCount = jobPostings.size,
                    newsArticleCount = newsArticles.size,
                    communityPostCount = communityPosts.size
                )
            )
        }
    }

    private fun buildInputData(
        dateStr: String,
        jobPostings: List<JobPosting>,
        newsArticles: List<NewsArticle>,
        communityPosts: List<CommunityPost>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("# $dateStr 수집 데이터")
        sb.appendLine()

        // 채용 공고
        sb.appendLine("## 채용 공고 (${jobPostings.size}건)")
        if (jobPostings.isEmpty()) {
            sb.appendLine("- 오늘 새로운 채용 공고가 없습니다.")
        } else {
            jobPostings.take(10).forEach { job ->
                val title = job.titleTranslated ?: job.title
                sb.appendLine("- **${job.companyName}**: $title")
                job.location?.let { sb.appendLine("  - 위치: $it") }
                job.salary?.let { sb.appendLine("  - 급여: $it") }
            }
            if (jobPostings.size > 10) {
                sb.appendLine("- ... 외 ${jobPostings.size - 10}건")
            }
        }
        sb.appendLine()

        // 뉴스
        sb.appendLine("## 뉴스 기사 (${newsArticles.size}건)")
        if (newsArticles.isEmpty()) {
            sb.appendLine("- 오늘 새로운 뉴스가 없습니다.")
        } else {
            newsArticles.take(10).forEach { news ->
                val title = news.titleTranslated ?: news.title
                sb.appendLine("- **$title**")
                news.summaryTranslated?.let { sb.appendLine("  - $it") }
            }
            if (newsArticles.size > 10) {
                sb.appendLine("- ... 외 ${newsArticles.size - 10}건")
            }
        }
        sb.appendLine()

        // 커뮤니티
        sb.appendLine("## 커뮤니티 글 (${communityPosts.size}건)")
        if (communityPosts.isEmpty()) {
            sb.appendLine("- 오늘 새로운 커뮤니티 글이 없습니다.")
        } else {
            communityPosts.take(10).forEach { post ->
                val title = post.titleTranslated ?: post.title
                val sentimentEmoji = when (post.sentiment?.name) {
                    "POSITIVE" -> "😊"
                    "NEGATIVE" -> "😟"
                    else -> "😐"
                }
                sb.appendLine("- $sentimentEmoji **$title** (👍 ${post.likeCount}, 💬 ${post.commentCount})")
            }
            if (communityPosts.size > 10) {
                sb.appendLine("- ... 외 ${communityPosts.size - 10}건")
            }
        }

        return sb.toString()
    }

    private fun generateFallbackSummary(
        dateStr: String,
        jobPostings: List<JobPosting>,
        newsArticles: List<NewsArticle>,
        communityPosts: List<CommunityPost>
    ): String {
        return """
## 📊 $dateStr 일본 IT 취업 정보

### 📈 수집 현황
- 채용 공고: ${jobPostings.size}건
- 뉴스 기사: ${newsArticles.size}건
- 커뮤니티 글: ${communityPosts.size}건

### 💼 주요 채용
${jobPostings.take(5).joinToString("\n") { "- ${it.companyName}: ${it.titleTranslated ?: it.title}" }.ifEmpty { "- 새로운 채용 공고 없음" }}

### 📰 주요 뉴스
${newsArticles.take(5).joinToString("\n") { "- ${it.titleTranslated ?: it.title}" }.ifEmpty { "- 새로운 뉴스 없음" }}

### 💬 커뮤니티 인기글
${communityPosts.take(5).joinToString("\n") { "- ${it.titleTranslated ?: it.title} (👍${it.likeCount})" }.ifEmpty { "- 새로운 글 없음" }}

---
_AI 요약 생성 실패로 기본 포맷으로 제공됩니다._
        """.trimIndent()
    }
}

data class DailySummaryResult(
    val summary: String,
    val success: Boolean,
    val stats: SummaryStats
)

data class SummaryStats(
    val jobPostingCount: Int,
    val newsArticleCount: Int,
    val communityPostCount: Int
) {
    val totalCount: Int get() = jobPostingCount + newsArticleCount + communityPostCount
}
