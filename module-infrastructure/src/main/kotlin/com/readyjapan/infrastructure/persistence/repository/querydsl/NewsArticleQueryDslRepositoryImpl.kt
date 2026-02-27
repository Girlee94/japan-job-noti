package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import com.readyjapan.core.domain.entity.NewsArticle
import com.readyjapan.core.domain.entity.QNewsArticle.newsArticle
import java.time.LocalDateTime

class NewsArticleQueryDslRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : NewsArticleQueryDslRepository {

    override fun findBySourceIdAndExternalId(sourceId: Long, externalId: String): NewsArticle? {
        return queryFactory
            .selectFrom(newsArticle)
            .where(
                newsArticle.source.id.eq(sourceId),
                newsArticle.externalId.eq(externalId)
            )
            .fetchOne()
    }

    override fun findAllByCreatedAtAfter(dateTime: LocalDateTime): List<NewsArticle> {
        return queryFactory
            .selectFrom(newsArticle)
            .where(newsArticle.createdAt.after(dateTime))
            .orderBy(newsArticle.createdAt.desc())
            .fetch()
    }

    override fun findAllByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<NewsArticle> {
        return queryFactory
            .selectFrom(newsArticle)
            .where(newsArticle.createdAt.between(start, end))
            .orderBy(newsArticle.createdAt.desc())
            .fetch()
    }

    override fun findAllNeedingTranslation(): List<NewsArticle> {
        return queryFactory
            .selectFrom(newsArticle)
            .where(
                newsArticle.language.eq("ja"),
                newsArticle.titleTranslated.isNull
            )
            .fetch()
    }

    override fun findRecentArticles(limit: Int): List<NewsArticle> {
        return queryFactory
            .selectFrom(newsArticle)
            .orderBy(
                OrderSpecifier<LocalDateTime>(
                    Order.DESC,
                    newsArticle.publishedAt,
                    OrderSpecifier.NullHandling.NullsLast
                )
            )
            .limit(limit.toLong())
            .fetch()
    }

    override fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Long {
        return queryFactory
            .select(newsArticle.count())
            .from(newsArticle)
            .where(newsArticle.createdAt.between(start, end))
            .fetchOne() ?: 0L
    }

    override fun existsBySourceIdAndExternalId(sourceId: Long, externalId: String): Boolean {
        return queryFactory
            .selectOne()
            .from(newsArticle)
            .where(
                newsArticle.source.id.eq(sourceId),
                newsArticle.externalId.eq(externalId)
            )
            .fetchFirst() != null
    }
}
