package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.infrastructure.persistence.repository.querydsl.CrawlSourceQueryDslRepository
import org.springframework.data.jpa.repository.JpaRepository

interface JpaCrawlSourceRepository : JpaRepository<CrawlSource, Long>, CrawlSourceQueryDslRepository {

    fun findBySourceType(sourceType: SourceType): List<CrawlSource>
}
