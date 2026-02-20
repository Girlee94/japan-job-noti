package com.readyjapan.api.controller

import com.readyjapan.core.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Tag(name = "Health", description = "서버 상태 확인 API")
@RestController
@RequestMapping("/api/health")
class HealthController {

    @Operation(summary = "서버 상태 확인", description = "서버가 정상적으로 동작하는지 확인합니다.")
    @GetMapping
    fun health(): ApiResponse<HealthResponse> {
        return ApiResponse.success(
            data = HealthResponse(
                status = "UP",
                timestamp = LocalDateTime.now()
            )
        )
    }

    data class HealthResponse(
        val status: String,
        val timestamp: LocalDateTime
    )
}
