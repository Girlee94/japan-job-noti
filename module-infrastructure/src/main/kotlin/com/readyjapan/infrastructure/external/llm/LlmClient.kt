package com.readyjapan.infrastructure.external.llm

/**
 * LLM API 클라이언트 인터페이스
 * Provider 교체를 용이하게 하기 위한 추상화 계층
 */
interface LlmClient {

    /**
     * Chat Completion API 호출
     *
     * @param systemPrompt 시스템 프롬프트
     * @param userPrompt 사용자 프롬프트
     * @param temperature 창의성 조절 (0.0 ~ 2.0)
     * @param maxTokens 최대 토큰 수
     * @return 응답 텍스트, 실패 시 null
     */
    fun chatCompletion(
        systemPrompt: String,
        userPrompt: String,
        temperature: Double? = null,
        maxTokens: Int? = null
    ): String?
}
