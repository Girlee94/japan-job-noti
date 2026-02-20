package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.enums.CrawlStatus
import com.readyjapan.core.domain.repository.CrawlHistoryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface JpaCrawlHistoryRepository : JpaRepository<CrawlHistory, Long>, CrawlHistoryRepository {

    @Query("SELECT h FROM CrawlHistory h WHERE h.source.id = :sourceId ORDER BY h.startedAt DESC")
    override fun findBySourceId(@Param("sourceId") sourceId: Long): List<CrawlHistory>

    @Query("SELECT h FROM CrawlHistory h WHERE h.source.id = :sourceId ORDER BY h.startedAt DESC")
    fun findBySourceIdOrderByStartedAtDesc(
        @Param("sourceId") sourceId: Long,
        pageable: PageRequest
    ): List<CrawlHistory>

    override fun findRecentBySourceId(sourceId: Long, limit: Int): List<CrawlHistory> {
        return findBySourceIdOrderByStartedAtDesc(sourceId, PageRequest.of(0, limit))
    }

    override fun findByStatus(status: CrawlStatus): List<CrawlHistory>

    @Query("SELECT h FROM CrawlHistory h WHERE h.startedAt BETWEEN :start AND :end ORDER BY h.startedAt DESC")
    override fun findByStartedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): List<CrawlHistory>

    @Query("SELECT h FROM CrawlHistory h WHERE h.source.id = :sourceId ORDER BY h.startedAt DESC LIMIT 1")
    override fun findLatestBySourceId(@Param("sourceId") sourceId: Long): CrawlHistory?

    @Modifying
    @Query("DELETE FROM CrawlHistory h WHERE h.startedAt < :dateTime")
    override fun deleteOlderThan(@Param("dateTime") dateTime: LocalDateTime): Int
}
