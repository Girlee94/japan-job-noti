package com.readyjapan.core.domain.repository

import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.enums.PostingStatus
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 채용 공고 리포지토리 인터페이스
 */
interface JobPostingRepository {
    fun findById(id: Long): JobPosting?
    fun findBySourceIdAndExternalId(sourceId: Long, externalId: String): JobPosting?
    fun findAllByStatus(status: PostingStatus): List<JobPosting>
    fun findAllByCreatedAtAfter(dateTime: LocalDateTime): List<JobPosting>
    fun findAllNeedingTranslation(): List<JobPosting>
    fun findRecentByStatus(status: PostingStatus, limit: Int): List<JobPosting>
    fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Int
    fun save(jobPosting: JobPosting): JobPosting
    fun saveAll(jobPostings: List<JobPosting>): List<JobPosting>
    fun deleteById(id: Long)
    fun existsBySourceIdAndExternalId(sourceId: Long, externalId: String): Boolean
}
