package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.enums.PostingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface JpaJobPostingRepository : JpaRepository<JobPosting, Long> {

    @Query("SELECT j FROM JobPosting j WHERE j.source.id = :sourceId AND j.externalId = :externalId")
    fun findBySourceIdAndExternalId(
        @Param("sourceId") sourceId: Long,
        @Param("externalId") externalId: String
    ): JobPosting?

    fun findAllByStatus(status: PostingStatus): List<JobPosting>

    @Query("SELECT j FROM JobPosting j WHERE j.createdAt > :dateTime ORDER BY j.createdAt DESC")
    fun findAllByCreatedAtAfter(@Param("dateTime") dateTime: LocalDateTime): List<JobPosting>

    @Query("SELECT j FROM JobPosting j WHERE j.createdAt BETWEEN :start AND :end ORDER BY j.createdAt DESC")
    fun findAllByCreatedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): List<JobPosting>

    @Query("SELECT j FROM JobPosting j WHERE j.language = 'ja' AND j.titleTranslated IS NULL")
    fun findAllNeedingTranslation(): List<JobPosting>

    @Query(
        value = "SELECT * FROM job_postings WHERE status = :status ORDER BY created_at DESC LIMIT :limit",
        nativeQuery = true
    )
    fun findRecentByStatus(
        @Param("status") status: PostingStatus,
        @Param("limit") limit: Int
    ): List<JobPosting>

    @Query("SELECT COUNT(j) FROM JobPosting j WHERE j.createdAt BETWEEN :start AND :end")
    fun countByCreatedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Int

    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END FROM JobPosting j WHERE j.source.id = :sourceId AND j.externalId = :externalId")
    fun existsBySourceIdAndExternalId(
        @Param("sourceId") sourceId: Long,
        @Param("externalId") externalId: String
    ): Boolean
}
