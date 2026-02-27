package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.readyjapan.core.domain.entity.DailySummary
import java.time.LocalDate

interface DailySummaryQueryDslRepository {

    fun findRecentSummaries(limit: Int): List<DailySummary>

    fun findBySummaryDateBetween(startDate: LocalDate, endDate: LocalDate): List<DailySummary>

    fun findLatest(): DailySummary?
}
