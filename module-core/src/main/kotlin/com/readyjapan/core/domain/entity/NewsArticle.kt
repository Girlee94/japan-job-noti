package com.readyjapan.core.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 뉴스 기사 엔티티
 * 일본 IT 업계 관련 뉴스 기사를 저장합니다.
 */
@Entity
@Table(
    name = "news_articles",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_news_articles_source_external",
            columnNames = ["source_id", "external_id"]
        )
    ],
    indexes = [
        Index(name = "idx_news_articles_published", columnList = "published_at DESC"),
        Index(name = "idx_news_articles_created", columnList = "created_at DESC"),
        Index(name = "idx_news_articles_category", columnList = "category")
    ]
)
class NewsArticle(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    val source: CrawlSource,

    @Column(name = "external_id", nullable = false, length = 200)
    val externalId: String,

    @Column(nullable = false, length = 500)
    var title: String,

    @Column(name = "title_translated", length = 500)
    var titleTranslated: String? = null,

    @Column(columnDefinition = "TEXT")
    var summary: String? = null,

    @Column(name = "summary_translated", columnDefinition = "TEXT")
    var summaryTranslated: String? = null,

    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    @Column(name = "content_translated", columnDefinition = "TEXT")
    var contentTranslated: String? = null,

    @Column(length = 100)
    var author: String? = null,

    @Column(length = 50)
    var category: String? = null,

    @Column(name = "original_url", nullable = false, length = 1000)
    val originalUrl: String,

    @Column(name = "image_url", length = 1000)
    var imageUrl: String? = null,

    @Column(nullable = false, length = 10)
    var language: String = "ja",

    @Column(name = "published_at")
    var publishedAt: LocalDateTime? = null
) : BaseEntity() {

    /**
     * 번역 적용
     */
    fun applyTranslation(
        titleTranslated: String?,
        summaryTranslated: String?,
        contentTranslated: String?
    ) {
        this.titleTranslated = titleTranslated
        this.summaryTranslated = summaryTranslated
        this.contentTranslated = contentTranslated
    }

    /**
     * 번역 필요 여부 확인
     */
    fun needsTranslation(): Boolean {
        return language == "ja" && titleTranslated == null
    }

    /**
     * 요약 가져오기 (번역본 우선)
     */
    fun getDisplaySummary(): String? {
        return summaryTranslated ?: summary
    }

    /**
     * 제목 가져오기 (번역본 우선)
     */
    fun getDisplayTitle(): String {
        return titleTranslated ?: title
    }
}
