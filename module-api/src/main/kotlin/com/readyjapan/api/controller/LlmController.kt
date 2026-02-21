package com.readyjapan.api.controller

import com.readyjapan.api.controller.dto.DailySummaryResultDto
import com.readyjapan.api.controller.dto.SentimentAnalysisResultDto
import com.readyjapan.api.controller.dto.TranslationResultDto
import com.readyjapan.core.common.response.ApiResponse
import com.readyjapan.infrastructure.orchestration.DailySummaryOrchestrationService
import com.readyjapan.infrastructure.orchestration.SentimentOrchestrationService
import com.readyjapan.infrastructure.orchestration.TranslationOrchestrationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.concurrent.Callable

private val logger = KotlinLogging.logger {}

/**
 * LLM 서비스 수동 트리거 컨트롤러
 * 모든 엔드포인트는 Callable로 래핑하여 서블릿 스레드 블로킹을 방지
 */
@RestController
@RequestMapping("/api/v1/llm")
class LlmController(
    private val translationOrchestrationService: TranslationOrchestrationService,
    private val sentimentOrchestrationService: SentimentOrchestrationService,
    private val dailySummaryOrchestrationService: DailySummaryOrchestrationService
) {

    /**
     * 번역 수동 트리거
     */
    @PostMapping("/translate")
    fun triggerTranslation(): Callable<ApiResponse<TranslationResultDto>> {
        logger.info { "Manual translation triggered" }
        return Callable {
            try {
                val result = translationOrchestrationService.translatePendingContent()
                ApiResponse.success(TranslationResultDto.from(result))
            } catch (e: Exception) {
                logger.warn(e) { "Translation failed" }
                ApiResponse.error("번역 처리 중 오류가 발생했습니다")
            }
        }
    }

    /**
     * 감정 분석 수동 트리거
     */
    @PostMapping("/sentiment")
    fun triggerSentimentAnalysis(): Callable<ApiResponse<SentimentAnalysisResultDto>> {
        logger.info { "Manual sentiment analysis triggered" }
        return Callable {
            try {
                val result = sentimentOrchestrationService.analyzePendingSentiments()
                ApiResponse.success(SentimentAnalysisResultDto.from(result))
            } catch (e: Exception) {
                logger.warn(e) { "Sentiment analysis failed" }
                ApiResponse.error("감정 분석 처리 중 오류가 발생했습니다")
            }
        }
    }

    /**
     * 일일 요약 수동 생성
     */
    @PostMapping("/summary")
    fun generateDailySummary(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?
    ): Callable<ApiResponse<DailySummaryResultDto>> {
        val targetDate = date ?: LocalDate.now().minusDays(1)
        logger.info { "Manual summary generation for: $targetDate" }
        return Callable {
            try {
                val result = dailySummaryOrchestrationService.generateDailySummary(targetDate, skipIfExists = false)
                ApiResponse.success(DailySummaryResultDto.from(result))
            } catch (e: Exception) {
                logger.warn(e) { "Daily summary generation failed for: $targetDate" }
                ApiResponse.error("일일 요약 생성 중 오류가 발생했습니다")
            }
        }
    }

    /**
     * 일일 요약 생성 및 텔레그램 전송
     */
    @PostMapping("/summary/send")
    fun generateAndSendDailySummary(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?
    ): Callable<ApiResponse<DailySummaryResultDto>> {
        val targetDate = date ?: LocalDate.now().minusDays(1)
        logger.info { "Manual summary generation and send for: $targetDate" }
        return Callable {
            try {
                val result = dailySummaryOrchestrationService.generateAndSendDailySummary(targetDate, skipIfExists = false)
                ApiResponse.success(DailySummaryResultDto.from(result))
            } catch (e: Exception) {
                logger.warn(e) { "Daily summary generation and send failed for: $targetDate" }
                ApiResponse.error("일일 요약 생성 및 전송 중 오류가 발생했습니다")
            }
        }
    }
}
