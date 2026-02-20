package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.core.domain.entity.enums.SummaryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface JpaDailySummaryRepository : JpaRepository<DailySummary, Long> {

    fun findBySummaryDate(date: LocalDate): DailySummary?

    fun findByStatus(status: SummaryStatus): List<DailySummary>

    @Query(
        value = "SELECT * FROM daily_summaries ORDER BY summary_date DESC LIMIT :limit",
        nativeQuery = true
    )
    fun findRecentSummaries(@Param("limit") limit: Int): List<DailySummary>

    @Query("SELECT d FROM DailySummary d WHERE d.summaryDate BETWEEN :startDate AND :endDate ORDER BY d.summaryDate DESC")
    fun findBySummaryDateBetween(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<DailySummary>

    @Query("SELECT d FROM DailySummary d ORDER BY d.summaryDate DESC LIMIT 1")
    fun findLatest(): DailySummary?

    fun existsBySummaryDate(date: LocalDate): Boolean
}
