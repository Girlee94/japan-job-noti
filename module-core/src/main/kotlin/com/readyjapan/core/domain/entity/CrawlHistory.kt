package com.readyjapan.core.domain.entity

import com.readyjapan.core.domain.entity.enums.CrawlStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 크롤링 이력 엔티티
 * 크롤링 실행 이력을 기록합니다.
 */
@Entity
@Table(
    name = "crawl_histories",
    indexes = [
        Index(name = "idx_crawl_histories_source", columnList = "source_id"),
        Index(name = "idx_crawl_histories_started", columnList = "started_at DESC"),
        Index(name = "idx_crawl_histories_status", columnList = "status")
    ]
)
class CrawlHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    val source: CrawlSource,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CrawlStatus = CrawlStatus.RUNNING,

    @Column(name = "items_found", nullable = false)
    var itemsFound: Int = 0,

    @Column(name = "items_saved", nullable = false)
    var itemsSaved: Int = 0,

    @Column(name = "items_updated", nullable = false)
    var itemsUpdated: Int = 0,

    @Column(name = "items_translated", nullable = false)
    var itemsTranslated: Int = 0,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "duration_ms")
    var durationMs: Long? = null,

    @Column(name = "started_at", nullable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null
) : BaseEntity() {

    /**
     * 크롤링 성공 처리
     */
    fun complete(
        itemsFound: Int,
        itemsSaved: Int,
        itemsUpdated: Int = 0,
        itemsTranslated: Int = 0
    ) {
        this.status = CrawlStatus.SUCCESS
        this.itemsFound = itemsFound
        this.itemsSaved = itemsSaved
        this.itemsUpdated = itemsUpdated
        this.itemsTranslated = itemsTranslated
        this.finishedAt = LocalDateTime.now()
        this.durationMs = ChronoUnit.MILLIS.between(startedAt, finishedAt)
    }

    /**
     * 크롤링 실패 처리
     */
    fun fail(errorMessage: String) {
        this.status = CrawlStatus.FAILED
        this.errorMessage = errorMessage
        this.finishedAt = LocalDateTime.now()
        this.durationMs = ChronoUnit.MILLIS.between(startedAt, finishedAt)
    }

    /**
     * 부분 성공 처리
     */
    fun partial(
        itemsFound: Int,
        itemsSaved: Int,
        itemsUpdated: Int = 0,
        itemsTranslated: Int = 0,
        errorMessage: String
    ) {
        this.status = CrawlStatus.PARTIAL
        this.itemsFound = itemsFound
        this.itemsSaved = itemsSaved
        this.itemsUpdated = itemsUpdated
        this.itemsTranslated = itemsTranslated
        this.errorMessage = errorMessage
        this.finishedAt = LocalDateTime.now()
        this.durationMs = ChronoUnit.MILLIS.between(startedAt, finishedAt)
    }

    /**
     * 실행 중 여부 확인
     */
    fun isRunning(): Boolean {
        return status == CrawlStatus.RUNNING
    }

    /**
     * 성공 여부 확인
     */
    fun isSuccessful(): Boolean {
        return status == CrawlStatus.SUCCESS
    }

    /**
     * 소요 시간 (초) 반환
     */
    fun getDurationSeconds(): Long? {
        return durationMs?.let { it / 1000 }
    }

    companion object {
        /**
         * 새 크롤링 이력 시작
         */
        fun start(source: CrawlSource): CrawlHistory {
            return CrawlHistory(source = source)
        }
    }
}
