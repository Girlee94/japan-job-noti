package com.readyjapan.core.common.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.readyjapan.core.common.exception.BusinessException
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val success: Boolean = false,
    val errorCode: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val details: Map<String, Any>? = null
) {
    companion object {
        fun of(exception: BusinessException): ErrorResponse {
            return ErrorResponse(
                errorCode = exception.errorCode,
                message = exception.message
            )
        }

        fun of(errorCode: String, message: String): ErrorResponse {
            return ErrorResponse(
                errorCode = errorCode,
                message = message
            )
        }

        fun of(errorCode: String, message: String, details: Map<String, Any>): ErrorResponse {
            return ErrorResponse(
                errorCode = errorCode,
                message = message,
                details = details
            )
        }
    }
}
