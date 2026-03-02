package com.readyjapan.api.controller.dto

import com.readyjapan.core.domain.entity.CrawlSource

data class CrawlSourceResponse(
    val id: Long,
    val name: String,
    val url: String,
    val sourceType: String,
    val platform: String?,
    val enabled: Boolean,
    val lastCrawledAt: String?,
    val createdAt: String
) {
    companion object {
        fun from(entity: CrawlSource): CrawlSourceResponse {
            return CrawlSourceResponse(
                id = entity.id,
                name = entity.name,
                url = entity.url,
                sourceType = entity.sourceType.name,
                platform = entity.platform?.name,
                enabled = entity.enabled,
                lastCrawledAt = entity.lastCrawledAt?.toString(),
                createdAt = entity.createdAt.toString()
            )
        }
    }
}
