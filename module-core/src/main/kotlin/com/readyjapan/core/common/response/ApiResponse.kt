package com.readyjapan.core.common.response

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean = true,
    val data: T? = null,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                message = message
            )
        }

        fun success(message: String): ApiResponse<Unit> {
            return ApiResponse(
                success = true,
                data = null,
                message = message
            )
        }

        fun <T> empty(): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = null
            )
        }
    }
}
