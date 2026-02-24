package com.readyjapan.infrastructure.crawler.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * 크롤러 공통 설정
 */
@Component
@ConfigurationProperties(prefix = "app.crawler")
data class CrawlerConfig(
    /** User-Agent 헤더 */
    val userAgent: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",

    /** 요청 타임아웃 (초) */
    val timeoutSeconds: Long = 30,

    /** 재시도 횟수 */
    val retryCount: Int = 3,

    /** 재시도 간격 (밀리초) */
    val retryDelayMs: Long = 1000,

    /** 게시물 수집 기준 시간 (시간 단위, 기본 24시간) */
    val freshnessHours: Long = 24
)
