package com.readyjapan.infrastructure.persistence.repository.adapter

import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.enums.PostingStatus
import com.readyjapan.core.domain.repository.JobPostingRepository
import com.readyjapan.infrastructure.persistence.repository.JpaJobPostingRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class JobPostingRepositoryAdapter(
    private val jpa: JpaJobPostingRepository
) : JobPostingRepository {

    override fun findById(id: Long): JobPosting? = jpa.findById(id).orElse(null)

    override fun findBySourceIdAndExternalId(sourceId: Long, externalId: String): JobPosting? =
        jpa.findBySourceIdAndExternalId(sourceId, externalId)

    override fun findAllByStatus(status: PostingStatus): List<JobPosting> =
        jpa.findAllByStatus(status)

    override fun findAllByCreatedAtAfter(dateTime: LocalDateTime): List<JobPosting> =
        jpa.findAllByCreatedAtAfter(dateTime)

    override fun findAllNeedingTranslation(): List<JobPosting> =
        jpa.findAllNeedingTranslation()

    override fun findRecentByStatus(status: PostingStatus, limit: Int): List<JobPosting> =
        jpa.findRecentByStatus(status, limit)

    override fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Int =
        jpa.countByCreatedAtBetween(start, end)

    override fun save(jobPosting: JobPosting): JobPosting = jpa.save(jobPosting)

    override fun saveAll(jobPostings: List<JobPosting>): List<JobPosting> =
        jpa.saveAll(jobPostings)

    override fun deleteById(id: Long) = jpa.deleteById(id)

    override fun existsBySourceIdAndExternalId(sourceId: Long, externalId: String): Boolean =
        jpa.existsBySourceIdAndExternalId(sourceId, externalId)
}
