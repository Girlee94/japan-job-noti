package com.readyjapan.core.domain.entity

import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.Sentiment
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 커뮤니티 글 엔티티
 * 커뮤니티에서 수집한 글/게시물을 저장합니다.
 */
@Entity
@Table(
    name = "community_posts",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_community_posts_source_external",
            columnNames = ["source_id", "external_id"]
        )
    ],
    indexes = [
        Index(name = "idx_community_posts_platform", columnList = "platform"),
        Index(name = "idx_community_posts_published", columnList = "published_at DESC"),
        Index(name = "idx_community_posts_created", columnList = "created_at DESC"),
        Index(name = "idx_community_posts_sentiment", columnList = "sentiment"),
        Index(name = "idx_community_posts_like", columnList = "like_count DESC")
    ]
)
class CommunityPost(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    val source: CrawlSource,

    @Column(name = "external_id", nullable = false, length = 200)
    val externalId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val platform: CommunityPlatform,

    @Column(length = 500)
    var title: String? = null,

    @Column(name = "title_translated", length = 500)
    var titleTranslated: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "content_translated", columnDefinition = "TEXT")
    var contentTranslated: String? = null,

    @Column(length = 100)
    var author: String? = null,

    @Column(name = "author_profile_url", length = 500)
    var authorProfileUrl: String? = null,

    @Column(name = "original_url", nullable = false, length = 1000)
    val originalUrl: String,

    @Column(columnDefinition = "jsonb")
    var tags: String? = null,

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,

    @Column(name = "comment_count", nullable = false)
    var commentCount: Int = 0,

    @Column(name = "share_count")
    var shareCount: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var sentiment: Sentiment? = null,

    @Column(nullable = false, length = 10)
    var language: String = "ja",

    @Column(name = "published_at", nullable = false)
    val publishedAt: LocalDateTime
) : BaseEntity() {

    /**
     * 번역 적용
     */
    fun applyTranslation(
        titleTranslated: String?,
        contentTranslated: String?
    ) {
        this.titleTranslated = titleTranslated
        this.contentTranslated = contentTranslated
    }

    /**
     * 감정 분석 결과 적용
     */
    fun applySentiment(sentiment: Sentiment) {
        this.sentiment = sentiment
    }

    /**
     * 번역 필요 여부 확인
     */
    fun needsTranslation(): Boolean {
        return language == "ja" && contentTranslated == null
    }

    /**
     * 감정 분석 필요 여부 확인
     */
    fun needsSentimentAnalysis(): Boolean {
        return sentiment == null
    }

    /**
     * 인기 게시물 여부 확인 (좋아요 10개 이상)
     */
    fun isPopular(): Boolean {
        return likeCount >= 10
    }

    /**
     * 통계 업데이트
     */
    fun updateStats(likeCount: Int, commentCount: Int, shareCount: Int?) {
        this.likeCount = likeCount
        this.commentCount = commentCount
        this.shareCount = shareCount
    }

    /**
     * 제목 가져오기 (번역본 우선)
     */
    fun getDisplayTitle(): String? {
        return titleTranslated ?: title
    }

    /**
     * 본문 가져오기 (번역본 우선)
     */
    fun getDisplayContent(): String {
        return contentTranslated ?: content
    }
}
