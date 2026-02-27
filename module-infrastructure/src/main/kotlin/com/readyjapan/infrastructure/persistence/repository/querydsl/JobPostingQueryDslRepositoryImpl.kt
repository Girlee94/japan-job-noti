package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.querydsl.jpa.impl.JPAQueryFactory
import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.QJobPosting.jobPosting
import com.readyjapan.core.domain.entity.enums.PostingStatus
import java.time.LocalDateTime

class JobPostingQueryDslRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : JobPostingQueryDslRepository {

    override fun findBySourceIdAndExternalId(sourceId: Long, externalId: String): JobPosting? {
        return queryFactory
            .selectFrom(jobPosting)
            .where(
                jobPosting.source.id.eq(sourceId),
                jobPosting.externalId.eq(externalId)
            )
            .fetchOne()
    }

    override fun findAllByCreatedAtAfter(dateTime: LocalDateTime): List<JobPosting> {
        return queryFactory
            .selectFrom(jobPosting)
            .where(jobPosting.createdAt.after(dateTime))
            .orderBy(jobPosting.createdAt.desc())
            .fetch()
    }

    override fun findAllByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<JobPosting> {
        return queryFactory
            .selectFrom(jobPosting)
            .where(jobPosting.createdAt.between(start, end))
            .orderBy(jobPosting.createdAt.desc())
            .fetch()
    }

    override fun findAllNeedingTranslation(): List<JobPosting> {
        return queryFactory
            .selectFrom(jobPosting)
            .where(
                jobPosting.language.eq("ja"),
                jobPosting.titleTranslated.isNull
            )
            .fetch()
    }

    override fun findRecentByStatus(status: PostingStatus, limit: Int): List<JobPosting> {
        return queryFactory
            .selectFrom(jobPosting)
            .where(jobPosting.status.eq(status))
            .orderBy(jobPosting.createdAt.desc())
            .limit(limit.toLong())
            .fetch()
    }

    override fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Long {
        return queryFactory
            .select(jobPosting.count())
            .from(jobPosting)
            .where(jobPosting.createdAt.between(start, end))
            .fetchOne() ?: 0L
    }

    override fun existsBySourceIdAndExternalId(sourceId: Long, externalId: String): Boolean {
        return queryFactory
            .selectOne()
            .from(jobPosting)
            .where(
                jobPosting.source.id.eq(sourceId),
                jobPosting.externalId.eq(externalId)
            )
            .fetchFirst() != null
    }
}
