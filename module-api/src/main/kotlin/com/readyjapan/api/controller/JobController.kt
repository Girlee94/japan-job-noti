package com.readyjapan.api.controller

import com.readyjapan.api.controller.dto.JobPostingResponse
import com.readyjapan.core.common.exception.EntityNotFoundException
import com.readyjapan.core.common.response.ApiResponse
import com.readyjapan.core.domain.entity.enums.PostingStatus
import com.readyjapan.core.domain.repository.JobPostingRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Job Postings", description = "채용공고 조회 API")
@RestController
@RequestMapping("/api/v1/jobs")
class JobController(
    private val jobPostingRepository: JobPostingRepository
) {

    @Operation(summary = "채용공고 목록 조회", description = "최근 채용공고 목록을 조회합니다.")
    @GetMapping
    fun getJobs(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "ACTIVE") status: PostingStatus
    ): ApiResponse<List<JobPostingResponse>> {
        val jobs = jobPostingRepository.findRecentByStatus(
            status = status,
            limit = limit.coerceIn(1, 100)
        )
        return ApiResponse.success(jobs.map { JobPostingResponse.from(it) })
    }

    @Operation(summary = "채용공고 상세 조회", description = "채용공고 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    fun getJob(@PathVariable id: Long): ApiResponse<JobPostingResponse> {
        val job = jobPostingRepository.findById(id)
            ?: throw EntityNotFoundException("JobPosting", id)
        return ApiResponse.success(JobPostingResponse.detail(job))
    }
}
