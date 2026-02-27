package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.readyjapan.core.domain.entity.CrawlHistory
import java.time.LocalDateTime

interface CrawlHistoryQueryDslRepository {

    fun findBySourceId(sourceId: Long): List<CrawlHistory>

    fun findRecentBySourceId(sourceId: Long, limit: Int): List<CrawlHistory>

    fun findByStartedAtBetween(start: LocalDateTime, end: LocalDateTime): List<CrawlHistory>

    fun findLatestBySourceId(sourceId: Long): CrawlHistory?

    fun deleteOlderThan(dateTime: LocalDateTime): Long
}
