package com.readyjapan.core.domain.repository

import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType

/**
 * 크롤링 소스 리포지토리 인터페이스
 */
interface CrawlSourceRepository {
    fun findById(id: Long): CrawlSource?
    fun findAll(): List<CrawlSource>
    fun findAllEnabled(): List<CrawlSource>
    fun findBySourceType(sourceType: SourceType): List<CrawlSource>
    fun findEnabledBySourceType(sourceType: SourceType): List<CrawlSource>
    fun findEnabledBySourceTypeAndPlatform(sourceType: SourceType, platform: CommunityPlatform): List<CrawlSource>
    fun save(crawlSource: CrawlSource): CrawlSource
    fun deleteById(id: Long)
}
