package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.Sentiment
import com.readyjapan.infrastructure.persistence.repository.querydsl.CommunityPostQueryDslRepository
import org.springframework.data.jpa.repository.JpaRepository

interface JpaCommunityPostRepository : JpaRepository<CommunityPost, Long>, CommunityPostQueryDslRepository {

    fun findAllByPlatform(platform: CommunityPlatform): List<CommunityPost>

    fun findBySentiment(sentiment: Sentiment): List<CommunityPost>
}
