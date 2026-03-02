package com.readyjapan.api.controller

import com.readyjapan.api.controller.dto.DailySummaryResponse
import com.readyjapan.core.common.exception.EntityNotFoundException
import com.readyjapan.core.common.response.ApiResponse
import com.readyjapan.core.domain.repository.DailySummaryRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@Tag(name = "Daily Summaries", description = "일간 요약 조회 API")
@RestController
@RequestMapping("/api/v1/summaries")
class SummaryController(
    private val dailySummaryRepository: DailySummaryRepository
) {

    @Operation(summary = "일간 요약 목록 조회", description = "최근 일간 요약 목록을 조회합니다.")
    @GetMapping
    fun getSummaries(
        @RequestParam(defaultValue = "10") limit: Int
    ): ApiResponse<List<DailySummaryResponse>> {
        val summaries = dailySummaryRepository.findRecentSummaries(limit.coerceIn(1, 50))
        return ApiResponse.success(summaries.map { DailySummaryResponse.from(it) })
    }

    @Operation(summary = "최신 일간 요약 조회", description = "가장 최근 일간 요약을 조회합니다.")
    @GetMapping("/latest")
    fun getLatestSummary(): ApiResponse<DailySummaryResponse> {
        val summary = dailySummaryRepository.findLatest()
            ?: throw EntityNotFoundException("DailySummary", "latest")
        return ApiResponse.success(DailySummaryResponse.detail(summary))
    }

    @Operation(summary = "날짜별 일간 요약 조회", description = "특정 날짜의 일간 요약을 조회합니다.")
    @GetMapping("/{date}")
    fun getSummaryByDate(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ApiResponse<DailySummaryResponse> {
        val summary = dailySummaryRepository.findBySummaryDate(date)
            ?: throw EntityNotFoundException("DailySummary", date)
        return ApiResponse.success(DailySummaryResponse.detail(summary))
    }
}
