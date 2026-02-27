package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType

interface CrawlSourceQueryDslRepository {

    fun findAllEnabled(): List<CrawlSource>

    fun findEnabledBySourceType(sourceType: SourceType): List<CrawlSource>

    fun findEnabledBySourceTypeAndPlatform(
        sourceType: SourceType,
        platform: CommunityPlatform
    ): List<CrawlSource>
}
