package com.readyjapan.api.controller

import com.readyjapan.api.controller.dto.CrawlSourceResponse
import com.readyjapan.api.controller.dto.DashboardStatsResponse
import com.readyjapan.api.service.DashboardService
import com.readyjapan.core.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Dashboard", description = "대시보드 API")
@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController(
    private val dashboardService: DashboardService
) {

    @Operation(summary = "대시보드 통계 조회", description = "오늘 수집 현황과 최신 요약 정보를 조회합니다.")
    @GetMapping("/stats")
    fun getStats(): ApiResponse<DashboardStatsResponse> {
        return ApiResponse.success(dashboardService.getStats())
    }

    @Operation(summary = "크롤 소스 목록 조회", description = "크롤링 소스 목록과 마지막 크롤링 시간을 조회합니다.")
    @GetMapping("/sources")
    fun getSources(): ApiResponse<List<CrawlSourceResponse>> {
        return ApiResponse.success(dashboardService.getSources())
    }
}
