package com.readyjapan.infrastructure.crawler.reddit

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Reddit API 설정
 */
@ConfigurationProperties(prefix = "app.reddit")
data class RedditProperties(
    /** Reddit App Client ID */
    val clientId: String = "",

    /** Reddit App Client Secret */
    val clientSecret: String = "",

    /** User-Agent 헤더 (Reddit API 필수) */
    val userAgent: String = "ReadyJapan/1.0",

    /** API 활성화 여부 */
    val enabled: Boolean = true,

    /** 요청 간 딜레이 (밀리초) - Rate Limit 방지 */
    val requestDelayMs: Long = 1000,

    /** 기본 수집 개수 */
    val defaultLimit: Int = 50
)
