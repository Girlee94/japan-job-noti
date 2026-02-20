package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface JpaCrawlSourceRepository : JpaRepository<CrawlSource, Long> {

    @Query("SELECT c FROM CrawlSource c WHERE c.enabled = true")
    fun findAllEnabled(): List<CrawlSource>

    fun findBySourceType(sourceType: SourceType): List<CrawlSource>

    @Query("SELECT c FROM CrawlSource c WHERE c.enabled = true AND c.sourceType = :sourceType")
    fun findEnabledBySourceType(sourceType: SourceType): List<CrawlSource>

    @Query("SELECT c FROM CrawlSource c WHERE c.enabled = true AND c.sourceType = :sourceType AND c.platform = :platform")
    fun findEnabledBySourceTypeAndPlatform(
        @Param("sourceType") sourceType: SourceType,
        @Param("platform") platform: CommunityPlatform
    ): List<CrawlSource>
}
