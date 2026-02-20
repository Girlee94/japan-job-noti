package com.readyjapan.infrastructure.crawler.qiita

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Qiita API 설정
 * 인증 없이 시간당 60회, access token 사용 시 1000회 요청 가능
 */
@ConfigurationProperties(prefix = "app.qiita")
data class QiitaProperties(
    /** API 활성화 여부 */
    val enabled: Boolean = true,

    /** Qiita Access Token (선택, 있으면 시간당 1000회) */
    val accessToken: String = "",

    /** 검색 대상 태그 목록 */
    val tags: List<String> = listOf("日本", "就職", "転職", "キャリア"),

    /** 페이지당 조회 개수 (최대 100) */
    val perPage: Int = 20,

    /** 요청 간 딜레이 (밀리초) - Rate Limit 방지 */
    val requestDelayMs: Long = 2000
)
