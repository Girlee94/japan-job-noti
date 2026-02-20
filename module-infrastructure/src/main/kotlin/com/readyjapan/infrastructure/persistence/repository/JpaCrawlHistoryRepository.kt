package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.enums.CrawlStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface JpaCrawlHistoryRepository : JpaRepository<CrawlHistory, Long> {

    @Query("SELECT h FROM CrawlHistory h WHERE h.source.id = :sourceId ORDER BY h.startedAt DESC")
    fun findBySourceId(@Param("sourceId") sourceId: Long): List<CrawlHistory>

    @Query(
        value = "SELECT * FROM crawl_histories WHERE source_id = :sourceId ORDER BY started_at DESC LIMIT :limit",
        nativeQuery = true
    )
    fun findRecentBySourceId(
        @Param("sourceId") sourceId: Long,
        @Param("limit") limit: Int
    ): List<CrawlHistory>

    fun findByStatus(status: CrawlStatus): List<CrawlHistory>

    @Query("SELECT h FROM CrawlHistory h WHERE h.startedAt BETWEEN :start AND :end ORDER BY h.startedAt DESC")
    fun findByStartedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): List<CrawlHistory>

    @Query("SELECT h FROM CrawlHistory h WHERE h.source.id = :sourceId ORDER BY h.startedAt DESC LIMIT 1")
    fun findLatestBySourceId(@Param("sourceId") sourceId: Long): CrawlHistory?

    @Modifying
    @Query("DELETE FROM CrawlHistory h WHERE h.startedAt < :dateTime")
    fun deleteOlderThan(@Param("dateTime") dateTime: LocalDateTime): Int
}
