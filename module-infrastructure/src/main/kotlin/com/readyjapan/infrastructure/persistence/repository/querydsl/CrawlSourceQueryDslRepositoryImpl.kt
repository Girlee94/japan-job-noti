package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.querydsl.jpa.impl.JPAQueryFactory
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.QCrawlSource.crawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType

class CrawlSourceQueryDslRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : CrawlSourceQueryDslRepository {

    override fun findAllEnabled(): List<CrawlSource> {
        return queryFactory
            .selectFrom(crawlSource)
            .where(crawlSource.enabled.isTrue)
            .fetch()
    }

    override fun findEnabledBySourceType(sourceType: SourceType): List<CrawlSource> {
        return queryFactory
            .selectFrom(crawlSource)
            .where(
                crawlSource.enabled.isTrue,
                crawlSource.sourceType.eq(sourceType)
            )
            .fetch()
    }

    override fun findEnabledBySourceTypeAndPlatform(
        sourceType: SourceType,
        platform: CommunityPlatform
    ): List<CrawlSource> {
        return queryFactory
            .selectFrom(crawlSource)
            .where(
                crawlSource.enabled.isTrue,
                crawlSource.sourceType.eq(sourceType),
                crawlSource.platform.eq(platform)
            )
            .fetch()
    }
}
