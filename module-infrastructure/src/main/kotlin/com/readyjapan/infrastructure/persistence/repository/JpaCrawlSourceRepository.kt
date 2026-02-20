package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CrawlSourceRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface JpaCrawlSourceRepository : JpaRepository<CrawlSource, Long>, CrawlSourceRepository {

    @Query("SELECT c FROM CrawlSource c WHERE c.enabled = true")
    override fun findAllEnabled(): List<CrawlSource>

    override fun findBySourceType(sourceType: SourceType): List<CrawlSource>

    @Query("SELECT c FROM CrawlSource c WHERE c.enabled = true AND c.sourceType = :sourceType")
    override fun findEnabledBySourceType(sourceType: SourceType): List<CrawlSource>
}
