package com.readyjapan.infrastructure.persistence.repository

import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.enums.PostingStatus
import com.readyjapan.infrastructure.persistence.repository.querydsl.JobPostingQueryDslRepository
import org.springframework.data.jpa.repository.JpaRepository

interface JpaJobPostingRepository : JpaRepository<JobPosting, Long>, JobPostingQueryDslRepository {

    fun findAllByStatus(status: PostingStatus): List<JobPosting>
}
