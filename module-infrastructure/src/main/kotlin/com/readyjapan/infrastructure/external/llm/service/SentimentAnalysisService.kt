package com.readyjapan.infrastructure.external.llm.service

import com.readyjapan.core.domain.entity.enums.Sentiment
import com.readyjapan.infrastructure.external.llm.OpenAiClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 감정 분석 서비스
 */
@Service
class SentimentAnalysisService(
    private val openAiClient: OpenAiClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SYSTEM_PROMPT = """당신은 일본 IT 취업 관련 텍스트의 감정을 분석하는 전문가입니다.

텍스트를 읽고 다음 중 하나로 분류해주세요:
- POSITIVE: 긍정적인 내용 (좋은 소식, 성공 사례, 희망적인 전망 등)
- NEGATIVE: 부정적인 내용 (불만, 어려움, 부정적인 전망 등)
- NEUTRAL: 중립적인 내용 (단순 정보 전달, 질문, 객관적 사실 등)

응답 형식:
1. 첫 줄에 감정 분류 결과만 출력 (POSITIVE, NEGATIVE, NEUTRAL 중 하나)
2. 두 번째 줄부터 간단한 분석 이유 (1-2문장)

예시:
POSITIVE
일본 IT 기업의 채용 증가와 연봉 인상에 대한 긍정적인 소식입니다."""
    }

    /**
     * 단일 텍스트 감정 분석
     */
    fun analyze(text: String): SentimentResult {
        if (text.isBlank()) {
            return SentimentResult(Sentiment.NEUTRAL, "빈 텍스트입니다.")
        }

        log.debug("Analyzing sentiment for: ${text.take(50)}...")

        val response = openAiClient.chatCompletion(
            systemPrompt = SYSTEM_PROMPT,
            userPrompt = text,
            temperature = 0.1
        )

        return parseResponse(response)
    }

    /**
     * 제목과 내용을 종합하여 감정 분석
     */
    fun analyzeWithContext(title: String, content: String?): SentimentResult {
        val combinedText = if (content.isNullOrBlank()) {
            "제목: $title"
        } else {
            "제목: $title\n\n내용: $content"
        }

        return analyze(combinedText)
    }

    /**
     * 여러 텍스트 일괄 감정 분석
     */
    fun analyzeBatch(texts: List<String>): List<SentimentResult> {
        if (texts.isEmpty()) {
            return emptyList()
        }

        if (texts.size == 1) {
            return listOf(analyze(texts.first()))
        }

        val numberedTexts = texts.mapIndexed { index, text ->
            "[${index + 1}] $text"
        }.joinToString("\n\n---\n\n")

        val batchSystemPrompt = """당신은 일본 IT 취업 관련 텍스트의 감정을 분석하는 전문가입니다.

아래에 번호가 붙은 여러 텍스트가 있습니다. 각각의 감정을 분석해주세요.

감정 분류:
- POSITIVE: 긍정적인 내용
- NEGATIVE: 부정적인 내용
- NEUTRAL: 중립적인 내용

응답 형식 (각 텍스트마다):
[번호] 감정분류: 간단한 이유

예시:
[1] POSITIVE: 채용 증가 소식
[2] NEUTRAL: 단순 정보 질문
[3] NEGATIVE: 취업 어려움 호소"""

        val response = openAiClient.chatCompletion(
            systemPrompt = batchSystemPrompt,
            userPrompt = numberedTexts,
            temperature = 0.1,
            maxTokens = 2000
        )

        return if (response != null) {
            parseBatchResponse(response, texts.size)
        } else {
            texts.map { SentimentResult(Sentiment.NEUTRAL, "분석 실패") }
        }
    }

    private fun parseResponse(response: String?): SentimentResult {
        if (response.isNullOrBlank()) {
            return SentimentResult(Sentiment.NEUTRAL, "응답 없음")
        }

        val lines = response.trim().split("\n", limit = 2)
        val sentimentStr = lines.firstOrNull()?.trim()?.uppercase() ?: "NEUTRAL"
        val reason = lines.getOrNull(1)?.trim() ?: ""

        val sentiment = when {
            sentimentStr.contains("POSITIVE") -> Sentiment.POSITIVE
            sentimentStr.contains("NEGATIVE") -> Sentiment.NEGATIVE
            else -> Sentiment.NEUTRAL
        }

        return SentimentResult(sentiment, reason)
    }

    private fun parseBatchResponse(response: String, expectedCount: Int): List<SentimentResult> {
        val results = mutableListOf<SentimentResult>()
        val lines = response.split("\n")

        for (i in 1..expectedCount) {
            val pattern = "\\[$i\\]\\s*(POSITIVE|NEGATIVE|NEUTRAL)[:\\s]*(.*)".toRegex(RegexOption.IGNORE_CASE)
            val match = lines.firstNotNullOfOrNull { line ->
                pattern.find(line)
            }

            if (match != null) {
                val sentiment = when (match.groupValues[1].uppercase()) {
                    "POSITIVE" -> Sentiment.POSITIVE
                    "NEGATIVE" -> Sentiment.NEGATIVE
                    else -> Sentiment.NEUTRAL
                }
                val reason = match.groupValues[2].trim()
                results.add(SentimentResult(sentiment, reason))
            } else {
                results.add(SentimentResult(Sentiment.NEUTRAL, "파싱 실패"))
            }
        }

        return results
    }
}

data class SentimentResult(
    val sentiment: Sentiment,
    val reason: String
)
