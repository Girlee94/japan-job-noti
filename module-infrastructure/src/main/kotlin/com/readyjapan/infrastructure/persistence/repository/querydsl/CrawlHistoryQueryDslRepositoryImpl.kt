package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.querydsl.jpa.impl.JPAQueryFactory
import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.QCrawlHistory.crawlHistory
import java.time.LocalDateTime

class CrawlHistoryQueryDslRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : CrawlHistoryQueryDslRepository {

    override fun findBySourceId(sourceId: Long): List<CrawlHistory> {
        return queryFactory
            .selectFrom(crawlHistory)
            .where(crawlHistory.source.id.eq(sourceId))
            .orderBy(crawlHistory.startedAt.desc())
            .fetch()
    }

    override fun findRecentBySourceId(sourceId: Long, limit: Int): List<CrawlHistory> {
        return queryFactory
            .selectFrom(crawlHistory)
            .where(crawlHistory.source.id.eq(sourceId))
            .orderBy(crawlHistory.startedAt.desc())
            .limit(limit.toLong())
            .fetch()
    }

    override fun findByStartedAtBetween(start: LocalDateTime, end: LocalDateTime): List<CrawlHistory> {
        return queryFactory
            .selectFrom(crawlHistory)
            .where(crawlHistory.startedAt.between(start, end))
            .orderBy(crawlHistory.startedAt.desc())
            .fetch()
    }

    override fun findLatestBySourceId(sourceId: Long): CrawlHistory? {
        return queryFactory
            .selectFrom(crawlHistory)
            .where(crawlHistory.source.id.eq(sourceId))
            .orderBy(crawlHistory.startedAt.desc())
            .limit(1)
            .fetchOne()
    }

    override fun deleteOlderThan(dateTime: LocalDateTime): Long {
        return queryFactory
            .delete(crawlHistory)
            .where(crawlHistory.startedAt.before(dateTime))
            .execute()
    }
}
