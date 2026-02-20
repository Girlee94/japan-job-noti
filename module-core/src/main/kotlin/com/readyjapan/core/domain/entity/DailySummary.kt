package com.readyjapan.core.domain.entity

import com.readyjapan.core.domain.entity.enums.SummaryStatus
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 일간 요약 엔티티
 * 매일 생성되는 요약 정보를 저장합니다.
 */
@Entity
@Table(
    name = "daily_summaries",
    indexes = [
        Index(name = "idx_daily_summaries_date", columnList = "summary_date DESC"),
        Index(name = "idx_daily_summaries_status", columnList = "status")
    ]
)
class DailySummary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "summary_date", nullable = false, unique = true)
    val summaryDate: LocalDate,

    @Column(name = "job_posting_count", nullable = false)
    var jobPostingCount: Int = 0,

    @Column(name = "news_article_count", nullable = false)
    var newsArticleCount: Int = 0,

    @Column(name = "community_post_count", nullable = false)
    var communityPostCount: Int = 0,

    @Column(name = "summary_content", nullable = false, columnDefinition = "TEXT")
    var summaryContent: String,

    @Column(name = "trending_topics", columnDefinition = "jsonb")
    var trendingTopics: String? = null,

    @Column(name = "key_highlights", columnDefinition = "jsonb")
    var keyHighlights: String? = null,

    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SummaryStatus = SummaryStatus.DRAFT
) : BaseEntity() {

    /**
     * 발송 완료 처리
     */
    fun markAsSent() {
        this.status = SummaryStatus.SENT
        this.sentAt = LocalDateTime.now()
    }

    /**
     * 발송 실패 처리
     */
    fun markAsFailed() {
        this.status = SummaryStatus.FAILED
    }

    /**
     * 요약 내용 업데이트
     */
    fun updateSummary(
        summaryContent: String,
        trendingTopics: String? = null,
        keyHighlights: String? = null
    ) {
        this.summaryContent = summaryContent
        this.trendingTopics = trendingTopics
        this.keyHighlights = keyHighlights
    }

    /**
     * 통계 업데이트
     */
    fun updateCounts(
        jobPostingCount: Int,
        newsArticleCount: Int,
        communityPostCount: Int
    ) {
        this.jobPostingCount = jobPostingCount
        this.newsArticleCount = newsArticleCount
        this.communityPostCount = communityPostCount
    }

    /**
     * 발송 여부 확인
     */
    fun isSent(): Boolean {
        return status == SummaryStatus.SENT
    }

    /**
     * 총 수집 건수
     */
    fun getTotalCount(): Int {
        return jobPostingCount + newsArticleCount + communityPostCount
    }

    companion object {
        /**
         * 오늘 날짜로 새 요약 생성
         */
        fun createForToday(summaryContent: String): DailySummary {
            return DailySummary(
                summaryDate = LocalDate.now(),
                summaryContent = summaryContent
            )
        }

        /**
         * 특정 날짜로 새 요약 생성
         */
        fun createForDate(date: LocalDate, summaryContent: String): DailySummary {
            return DailySummary(
                summaryDate = date,
                summaryContent = summaryContent
            )
        }
    }
}
