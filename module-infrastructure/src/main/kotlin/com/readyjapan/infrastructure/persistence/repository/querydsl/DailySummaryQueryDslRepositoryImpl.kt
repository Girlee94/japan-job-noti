package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.querydsl.jpa.impl.JPAQueryFactory
import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.core.domain.entity.QDailySummary.dailySummary
import java.time.LocalDate

class DailySummaryQueryDslRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : DailySummaryQueryDslRepository {

    override fun findRecentSummaries(limit: Int): List<DailySummary> {
        return queryFactory
            .selectFrom(dailySummary)
            .orderBy(dailySummary.summaryDate.desc())
            .limit(limit.toLong())
            .fetch()
    }

    override fun findBySummaryDateBetween(startDate: LocalDate, endDate: LocalDate): List<DailySummary> {
        return queryFactory
            .selectFrom(dailySummary)
            .where(dailySummary.summaryDate.between(startDate, endDate))
            .orderBy(dailySummary.summaryDate.desc())
            .fetch()
    }

    override fun findLatest(): DailySummary? {
        return queryFactory
            .selectFrom(dailySummary)
            .orderBy(dailySummary.summaryDate.desc())
            .limit(1)
            .fetchOne()
    }
}
