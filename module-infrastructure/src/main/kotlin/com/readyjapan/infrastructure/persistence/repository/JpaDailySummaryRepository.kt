package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.core.domain.entity.enums.SummaryStatus
import com.readyjapan.core.domain.repository.DailySummaryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface JpaDailySummaryRepository : JpaRepository<DailySummary, Long>, DailySummaryRepository {

    override fun findBySummaryDate(date: LocalDate): DailySummary?

    override fun findByStatus(status: SummaryStatus): List<DailySummary>

    @Query("SELECT d FROM DailySummary d ORDER BY d.summaryDate DESC")
    fun findAllOrderBySummaryDateDesc(pageable: PageRequest): List<DailySummary>

    override fun findRecentSummaries(limit: Int): List<DailySummary> {
        return findAllOrderBySummaryDateDesc(PageRequest.of(0, limit))
    }

    @Query("SELECT d FROM DailySummary d WHERE d.summaryDate BETWEEN :startDate AND :endDate ORDER BY d.summaryDate DESC")
    override fun findBySummaryDateBetween(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<DailySummary>

    @Query("SELECT d FROM DailySummary d ORDER BY d.summaryDate DESC LIMIT 1")
    override fun findLatest(): DailySummary?

    override fun existsBySummaryDate(date: LocalDate): Boolean
}
