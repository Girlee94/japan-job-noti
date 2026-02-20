package com.readyjapan.infrastructure.external.llm.service

import com.readyjapan.infrastructure.external.llm.OpenAiClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 일본어 → 한국어 번역 서비스
 */
@Service
class TranslationService(
    private val openAiClient: OpenAiClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SYSTEM_PROMPT = """당신은 일본어를 한국어로 번역하는 전문 번역가입니다.
다음 규칙을 따라 번역해주세요:

1. 자연스러운 한국어로 번역합니다.
2. IT/기술 용어는 한국에서 일반적으로 사용하는 표현으로 번역합니다.
3. 회사명, 서비스명 등 고유명사는 그대로 유지합니다.
4. 번역문만 출력하고, 추가 설명은 하지 않습니다.
5. 원문이 일본어가 아닌 경우(영어 등), 그대로 한국어로 번역합니다."""
    }

    /**
     * 텍스트 번역
     */
    fun translate(text: String): String? {
        if (text.isBlank()) {
            return null
        }

        log.debug("Translating text: ${text.take(50)}...")

        return openAiClient.chatCompletion(
            systemPrompt = SYSTEM_PROMPT,
            userPrompt = text,
            temperature = 0.1 // 번역은 일관성을 위해 낮은 temperature 사용
        )
    }

    /**
     * 제목과 내용을 함께 번역
     */
    fun translateTitleAndContent(title: String, content: String?): TranslationResult {
        val translatedTitle = translate(title)
        val translatedContent = content?.let { translate(it) }

        return TranslationResult(
            translatedTitle = translatedTitle,
            translatedContent = translatedContent
        )
    }

    /**
     * 여러 텍스트를 일괄 번역 (비용 절감을 위해 하나의 요청으로 처리)
     */
    fun translateBatch(texts: List<String>): List<String?> {
        if (texts.isEmpty()) {
            return emptyList()
        }

        if (texts.size == 1) {
            return listOf(translate(texts.first()))
        }

        val numberedTexts = texts.mapIndexed { index, text ->
            "[${index + 1}] $text"
        }.joinToString("\n\n")

        val batchSystemPrompt = """당신은 일본어를 한국어로 번역하는 전문 번역가입니다.
아래에 번호가 붙은 여러 텍스트가 있습니다. 각각을 번역해주세요.

규칙:
1. 각 번역은 "[번호] 번역문" 형식으로 출력합니다.
2. 자연스러운 한국어로 번역합니다.
3. IT/기술 용어는 한국에서 일반적으로 사용하는 표현으로 번역합니다.
4. 회사명, 서비스명 등 고유명사는 그대로 유지합니다."""

        val response = openAiClient.chatCompletion(
            systemPrompt = batchSystemPrompt,
            userPrompt = numberedTexts,
            temperature = 0.1,
            maxTokens = 3000
        )

        return if (response != null) {
            parseBatchResponse(response, texts.size)
        } else {
            texts.map { null }
        }
    }

    private fun parseBatchResponse(response: String, expectedCount: Int): List<String?> {
        val results = mutableListOf<String?>()
        val lines = response.split("\n")

        for (i in 1..expectedCount) {
            val pattern = "\\[$i\\]\\s*(.+)".toRegex()
            val match = lines.firstNotNullOfOrNull { line ->
                pattern.find(line)?.groupValues?.get(1)
            }
            results.add(match?.trim())
        }

        return results
    }
}

data class TranslationResult(
    val translatedTitle: String?,
    val translatedContent: String?
)
