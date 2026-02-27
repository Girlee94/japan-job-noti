package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.core.domain.entity.enums.SummaryStatus
import com.readyjapan.infrastructure.persistence.repository.querydsl.DailySummaryQueryDslRepository
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface JpaDailySummaryRepository : JpaRepository<DailySummary, Long>, DailySummaryQueryDslRepository {

    fun findBySummaryDate(date: LocalDate): DailySummary?

    fun findByStatus(status: SummaryStatus): List<DailySummary>

    fun existsBySummaryDate(date: LocalDate): Boolean
}
