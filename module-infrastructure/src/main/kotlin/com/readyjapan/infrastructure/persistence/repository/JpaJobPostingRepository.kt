package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.enums.PostingStatus
import com.readyjapan.core.domain.repository.JobPostingRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface JpaJobPostingRepository : JpaRepository<JobPosting, Long>, JobPostingRepository {

    @Query("SELECT j FROM JobPosting j WHERE j.source.id = :sourceId AND j.externalId = :externalId")
    override fun findBySourceIdAndExternalId(
        @Param("sourceId") sourceId: Long,
        @Param("externalId") externalId: String
    ): JobPosting?

    override fun findAllByStatus(status: PostingStatus): List<JobPosting>

    @Query("SELECT j FROM JobPosting j WHERE j.createdAt > :dateTime ORDER BY j.createdAt DESC")
    override fun findAllByCreatedAtAfter(@Param("dateTime") dateTime: LocalDateTime): List<JobPosting>

    @Query("SELECT j FROM JobPosting j WHERE j.language = 'ja' AND j.titleTranslated IS NULL")
    override fun findAllNeedingTranslation(): List<JobPosting>

    @Query(
        "SELECT j FROM JobPosting j WHERE j.status = :status ORDER BY j.createdAt DESC"
    )
    fun findByStatusOrderByCreatedAtDesc(
        @Param("status") status: PostingStatus,
        pageable: PageRequest
    ): List<JobPosting>

    override fun findRecentByStatus(status: PostingStatus, limit: Int): List<JobPosting> {
        return findByStatusOrderByCreatedAtDesc(status, PageRequest.of(0, limit))
    }

    @Query("SELECT COUNT(j) FROM JobPosting j WHERE j.createdAt BETWEEN :start AND :end")
    override fun countByCreatedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Int

    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END FROM JobPosting j WHERE j.source.id = :sourceId AND j.externalId = :externalId")
    override fun existsBySourceIdAndExternalId(
        @Param("sourceId") sourceId: Long,
        @Param("externalId") externalId: String
    ): Boolean
}
