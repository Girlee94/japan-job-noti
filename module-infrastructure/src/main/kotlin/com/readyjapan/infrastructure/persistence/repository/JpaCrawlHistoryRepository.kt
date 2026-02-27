package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.enums.CrawlStatus
import com.readyjapan.infrastructure.persistence.repository.querydsl.CrawlHistoryQueryDslRepository
import org.springframework.data.jpa.repository.JpaRepository

interface JpaCrawlHistoryRepository : JpaRepository<CrawlHistory, Long>, CrawlHistoryQueryDslRepository {

    fun findByStatus(status: CrawlStatus): List<CrawlHistory>
}
