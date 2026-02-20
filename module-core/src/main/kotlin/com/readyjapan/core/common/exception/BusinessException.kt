package com.readyjapan.core.common.exception

/**
 * 비즈니스 예외의 기본 클래스
 * 모든 도메인 예외는 이 클래스를 상속받아야 합니다.
 */
sealed class BusinessException(
    val errorCode: String,
    override val message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 엔티티를 찾을 수 없을 때 발생하는 예외
 */
class EntityNotFoundException(
    entityName: String,
    identifier: Any
) : BusinessException(
    errorCode = "ENTITY_NOT_FOUND",
    message = "$entityName not found with identifier: $identifier"
)

/**
 * 외부 API 호출 실패 시 발생하는 예외
 */
class ExternalApiException(
    apiName: String,
    reason: String? = null,
    cause: Throwable? = null
) : BusinessException(
    errorCode = "EXTERNAL_API_ERROR",
    message = "External API call failed: $apiName${reason?.let { " - $it" } ?: ""}",
    cause = cause
)

/**
 * 크롤링 실패 시 발생하는 예외
 */
class CrawlingException(
    source: String,
    reason: String,
    cause: Throwable? = null
) : BusinessException(
    errorCode = "CRAWLING_ERROR",
    message = "Crawling failed for $source: $reason",
    cause = cause
)

/**
 * 유효하지 않은 요청 시 발생하는 예외
 */
class InvalidRequestException(
    reason: String
) : BusinessException(
    errorCode = "INVALID_REQUEST",
    message = reason
)

/**
 * 중복 데이터 발생 시 예외
 */
class DuplicateEntityException(
    entityName: String,
    identifier: Any
) : BusinessException(
    errorCode = "DUPLICATE_ENTITY",
    message = "$entityName already exists with identifier: $identifier"
)
