package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.enums.PostingStatus
import java.time.LocalDateTime

interface JobPostingQueryDslRepository {

    fun findBySourceIdAndExternalId(sourceId: Long, externalId: String): JobPosting?

    fun findAllByCreatedAtAfter(dateTime: LocalDateTime): List<JobPosting>

    fun findAllByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<JobPosting>

    fun findAllNeedingTranslation(): List<JobPosting>

    fun findRecentByStatus(status: PostingStatus, limit: Int): List<JobPosting>

    fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Long

    fun existsBySourceIdAndExternalId(sourceId: Long, externalId: String): Boolean
}
