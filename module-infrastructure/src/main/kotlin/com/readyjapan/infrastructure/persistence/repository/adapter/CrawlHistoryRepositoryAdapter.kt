package com.readyjapan.infrastructure.persistence.repository.adapter

import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.enums.CrawlStatus
import com.readyjapan.core.domain.repository.CrawlHistoryRepository
import com.readyjapan.infrastructure.persistence.repository.JpaCrawlHistoryRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CrawlHistoryRepositoryAdapter(
    private val jpa: JpaCrawlHistoryRepository
) : CrawlHistoryRepository {

    override fun findById(id: Long): CrawlHistory? = jpa.findById(id).orElse(null)

    override fun findBySourceId(sourceId: Long): List<CrawlHistory> =
        jpa.findBySourceId(sourceId)

    override fun findRecentBySourceId(sourceId: Long, limit: Int): List<CrawlHistory> =
        jpa.findRecentBySourceId(sourceId, limit)

    override fun findByStatus(status: CrawlStatus): List<CrawlHistory> =
        jpa.findByStatus(status)

    override fun findByStartedAtBetween(start: LocalDateTime, end: LocalDateTime): List<CrawlHistory> =
        jpa.findByStartedAtBetween(start, end)

    override fun findLatestBySourceId(sourceId: Long): CrawlHistory? =
        jpa.findLatestBySourceId(sourceId)

    override fun save(crawlHistory: CrawlHistory): CrawlHistory =
        jpa.save(crawlHistory)

    override fun deleteById(id: Long) = jpa.deleteById(id)

    override fun deleteOlderThan(dateTime: LocalDateTime): Int =
        jpa.deleteOlderThan(dateTime)
}
