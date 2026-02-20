package com.readyjapan.infrastructure.persistence.repository.adapter

import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CrawlSourceRepository
import com.readyjapan.infrastructure.persistence.repository.JpaCrawlSourceRepository
import org.springframework.stereotype.Repository

@Repository
class CrawlSourceRepositoryAdapter(
    private val jpa: JpaCrawlSourceRepository
) : CrawlSourceRepository {

    override fun findById(id: Long): CrawlSource? = jpa.findById(id).orElse(null)

    override fun findAll(): List<CrawlSource> = jpa.findAll()

    override fun findAllEnabled(): List<CrawlSource> = jpa.findAllEnabled()

    override fun findBySourceType(sourceType: SourceType): List<CrawlSource> =
        jpa.findBySourceType(sourceType)

    override fun findEnabledBySourceType(sourceType: SourceType): List<CrawlSource> =
        jpa.findEnabledBySourceType(sourceType)

    override fun save(crawlSource: CrawlSource): CrawlSource = jpa.save(crawlSource)

    override fun deleteById(id: Long) = jpa.deleteById(id)
}
