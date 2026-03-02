package com.readyjapan.infrastructure.external.llm

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.concurrent.TimeoutException

private val logger = KotlinLogging.logger {}

// NOTE: Thread.sleep으로 스케줄러 스레드를 블로킹합니다 (최대 1s+2s=3초/호출).
// 기존 LLM 클라이언트가 .block() 사용하므로 동기 재시도가 적합합니다.
fun <T> retryOnTransientError(
    maxRetries: Int = 2,
    initialDelayMs: Long = 1000,
    operationName: String = "LLM API",
    operation: () -> T?
): T? {
    require(maxRetries >= 0) { "maxRetries must be >= 0" }
    require(initialDelayMs > 0) { "initialDelayMs must be > 0" }

    for (attempt in 0..maxRetries) {
        try {
            return operation()
        } catch (e: Exception) {
            if (attempt < maxRetries && isTransientError(e)) {
                val delayMs = initialDelayMs * (1L shl attempt)
                logger.warn(e) {
                    "$operationName 호출 실패 (시도 ${attempt + 1}/${maxRetries + 1}), " +
                        "${delayMs}ms 후 재시도: ${e.message}"
                }
                try {
                    Thread.sleep(delayMs)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw ie
                }
            } else {
                throw e
            }
        }
    }
    // 컴파일러를 위한 코드 — 루프는 항상 return 또는 throw로 종료됨
    throw IllegalStateException("Unreachable")
}

private fun isTransientError(e: Exception): Boolean {
    if (e is WebClientResponseException) {
        return e.statusCode.is5xxServerError || e.statusCode.value() == 429
    }
    if (e is TimeoutException || e.cause is TimeoutException) {
        return true
    }
    if (e is IllegalStateException && e.message?.contains("Timeout") == true) {
        return true
    }
    return false
}
