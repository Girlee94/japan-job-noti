package com.readyjapan.infrastructure.external.llm

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * LLM 설정 활성화
 */
@Configuration
@EnableConfigurationProperties(LlmProperties::class)
class LlmConfig
