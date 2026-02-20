package com.readyjapan.core.domain.repository

import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.core.domain.entity.enums.SummaryStatus
import java.time.LocalDate

/**
 * 일간 요약 리포지토리 인터페이스
 */
interface DailySummaryRepository {
    fun findById(id: Long): DailySummary?
    fun findBySummaryDate(date: LocalDate): DailySummary?
    fun findByStatus(status: SummaryStatus): List<DailySummary>
    fun findRecentSummaries(limit: Int): List<DailySummary>
    fun findBySummaryDateBetween(startDate: LocalDate, endDate: LocalDate): List<DailySummary>
    fun findLatest(): DailySummary?
    fun existsBySummaryDate(date: LocalDate): Boolean
    fun save(dailySummary: DailySummary): DailySummary
    fun deleteById(id: Long)
}
