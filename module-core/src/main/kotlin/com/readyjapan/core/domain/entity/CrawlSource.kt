package com.readyjapan.core.domain.entity

import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 크롤링 소스 엔티티
 * 데이터를 수집할 웹사이트/API 정보를 관리합니다.
 */
@Entity
@Table(name = "crawl_sources")
class CrawlSource(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 500)
    var url: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    var sourceType: SourceType,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var platform: CommunityPlatform? = null,

    @Column(name = "cron_expression", length = 50)
    var cronExpression: String = "0 0 8 * * *",

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(columnDefinition = "jsonb")
    var config: String? = null,

    @Column(name = "last_crawled_at")
    var lastCrawledAt: LocalDateTime? = null
) : BaseEntity() {

    /**
     * 소스 활성화
     */
    fun enable() {
        this.enabled = true
    }

    /**
     * 소스 비활성화
     */
    fun disable() {
        this.enabled = false
    }

    /**
     * 마지막 크롤링 시각 업데이트
     */
    fun updateLastCrawledAt() {
        this.lastCrawledAt = LocalDateTime.now()
    }

    /**
     * 크롤링 가능 여부 확인
     */
    fun isCrawlable(): Boolean {
        return enabled
    }
}
