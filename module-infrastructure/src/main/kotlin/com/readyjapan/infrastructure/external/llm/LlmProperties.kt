package com.readyjapan.infrastructure.external.llm

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * LLM API 설정
 */
@ConfigurationProperties(prefix = "app.llm")
data class LlmProperties(
    /** LLM 제공자 (openai, anthropic) */
    val provider: String = "openai",

    /** API Key */
    val apiKey: String = "",

    /** 사용할 모델 */
    val model: String = "gpt-4o-mini",

    /** API 활성화 여부 */
    val enabled: Boolean = true,

    /** 요청 타임아웃 (초) */
    val timeoutSeconds: Long = 60,

    /** 최대 토큰 수 */
    val maxTokens: Int = 2000,

    /** Temperature (창의성 조절, 0.0 ~ 2.0) */
    val temperature: Double = 0.3
)
