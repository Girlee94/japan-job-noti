package com.readyjapan.core.domain.repository

import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.enums.CrawlStatus
import java.time.LocalDateTime

/**
 * 크롤링 이력 리포지토리 인터페이스
 */
interface CrawlHistoryRepository {
    fun findById(id: Long): CrawlHistory?
    fun findBySourceId(sourceId: Long): List<CrawlHistory>
    fun findRecentBySourceId(sourceId: Long, limit: Int): List<CrawlHistory>
    fun findByStatus(status: CrawlStatus): List<CrawlHistory>
    fun findByStartedAtBetween(start: LocalDateTime, end: LocalDateTime): List<CrawlHistory>
    fun findLatestBySourceId(sourceId: Long): CrawlHistory?
    fun save(crawlHistory: CrawlHistory): CrawlHistory
    fun deleteById(id: Long)
    fun deleteOlderThan(dateTime: LocalDateTime): Int
}
