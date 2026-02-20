package com.readyjapan.infrastructure.persistence.repository.adapter

import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.core.domain.entity.enums.SummaryStatus
import com.readyjapan.core.domain.repository.DailySummaryRepository
import com.readyjapan.infrastructure.persistence.repository.JpaDailySummaryRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class DailySummaryRepositoryAdapter(
    private val jpa: JpaDailySummaryRepository
) : DailySummaryRepository {

    override fun findById(id: Long): DailySummary? = jpa.findById(id).orElse(null)

    override fun findBySummaryDate(date: LocalDate): DailySummary? =
        jpa.findBySummaryDate(date)

    override fun findByStatus(status: SummaryStatus): List<DailySummary> =
        jpa.findByStatus(status)

    override fun findRecentSummaries(limit: Int): List<DailySummary> =
        jpa.findRecentSummaries(limit)

    override fun findBySummaryDateBetween(startDate: LocalDate, endDate: LocalDate): List<DailySummary> =
        jpa.findBySummaryDateBetween(startDate, endDate)

    override fun findLatest(): DailySummary? = jpa.findLatest()

    override fun existsBySummaryDate(date: LocalDate): Boolean =
        jpa.existsBySummaryDate(date)

    override fun save(dailySummary: DailySummary): DailySummary = jpa.save(dailySummary)

    override fun deleteById(id: Long) = jpa.deleteById(id)
}
