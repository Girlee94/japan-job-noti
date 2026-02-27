package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.QCommunityPost.communityPost
import com.readyjapan.core.domain.entity.enums.Sentiment
import java.time.LocalDateTime

class CommunityPostQueryDslRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : CommunityPostQueryDslRepository {

    override fun findBySourceIdAndExternalId(sourceId: Long, externalId: String): CommunityPost? {
        return queryFactory
            .selectFrom(communityPost)
            .where(
                communityPost.source.id.eq(sourceId),
                communityPost.externalId.eq(externalId)
            )
            .fetchOne()
    }

    override fun findAllBySourceIdAndExternalIdIn(sourceId: Long, externalIds: List<String>): List<CommunityPost> {
        return queryFactory
            .selectFrom(communityPost)
            .where(
                communityPost.source.id.eq(sourceId),
                communityPost.externalId.`in`(externalIds)
            )
            .fetch()
    }

    override fun findAllByCreatedAtAfter(dateTime: LocalDateTime): List<CommunityPost> {
        return queryFactory
            .selectFrom(communityPost)
            .where(communityPost.createdAt.after(dateTime))
            .orderBy(communityPost.createdAt.desc())
            .fetch()
    }

    override fun findAllByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<CommunityPost> {
        return queryFactory
            .selectFrom(communityPost)
            .where(communityPost.createdAt.between(start, end))
            .orderBy(communityPost.createdAt.desc())
            .fetch()
    }

    override fun findAllNeedingTranslation(): List<CommunityPost> {
        return queryFactory
            .selectFrom(communityPost)
            .where(
                communityPost.language.eq("ja"),
                communityPost.contentTranslated.isNull
            )
            .fetch()
    }

    override fun findAllNeedingSentimentAnalysis(): List<CommunityPost> {
        return queryFactory
            .selectFrom(communityPost)
            .where(communityPost.sentiment.isNull)
            .fetch()
    }

    override fun findPopularPosts(minLikes: Int, limit: Int): List<CommunityPost> {
        return queryFactory
            .selectFrom(communityPost)
            .where(communityPost.likeCount.goe(minLikes))
            .orderBy(communityPost.likeCount.desc())
            .limit(limit.toLong())
            .fetch()
    }

    override fun findRecentPosts(limit: Int): List<CommunityPost> {
        return queryFactory
            .selectFrom(communityPost)
            .orderBy(
                OrderSpecifier<LocalDateTime>(
                    Order.DESC,
                    communityPost.publishedAt,
                    OrderSpecifier.NullHandling.NullsLast
                )
            )
            .limit(limit.toLong())
            .fetch()
    }

    override fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Long {
        return queryFactory
            .select(communityPost.count())
            .from(communityPost)
            .where(communityPost.createdAt.between(start, end))
            .fetchOne() ?: 0L
    }

    override fun countBySentiment(sentiment: Sentiment): Long {
        return queryFactory
            .select(communityPost.count())
            .from(communityPost)
            .where(communityPost.sentiment.eq(sentiment))
            .fetchOne() ?: 0L
    }

    override fun existsBySourceIdAndExternalId(sourceId: Long, externalId: String): Boolean {
        return queryFactory
            .selectOne()
            .from(communityPost)
            .where(
                communityPost.source.id.eq(sourceId),
                communityPost.externalId.eq(externalId)
            )
            .fetchFirst() != null
    }
}
