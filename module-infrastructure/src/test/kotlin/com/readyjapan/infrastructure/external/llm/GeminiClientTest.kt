package com.readyjapan.infrastructure.external.llm

import com.readyjapan.infrastructure.external.llm.dto.GeminiCandidate
import com.readyjapan.infrastructure.external.llm.dto.GeminiGenerateResponse
import com.readyjapan.infrastructure.external.llm.dto.GeminiResponseContent
import com.readyjapan.infrastructure.external.llm.dto.GeminiResponsePart
import com.readyjapan.infrastructure.external.llm.dto.GeminiUsageMetadata
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class GeminiClientTest : BehaviorSpec({

    val llmProperties = LlmProperties(
        provider = "gemini",
        apiKey = "test-api-key",
        model = "gemini-2.5-flash",
        enabled = true,
        timeoutSeconds = 5,
        maxTokens = 2000,
        temperature = 0.3
    )

    fun createMockedClient(responseMono: Mono<GeminiGenerateResponse>): GeminiClient {
        val requestBodyUriSpec = mockk<WebClient.RequestBodyUriSpec>()
        val requestBodySpec = mockk<WebClient.RequestBodySpec>()
        val requestHeadersSpec = mockk<WebClient.RequestHeadersSpec<*>>()
        val responseSpec = mockk<WebClient.ResponseSpec>()

        every { requestBodyUriSpec.uri(any<String>()) } returns requestBodySpec
        every { requestBodySpec.header(any(), any()) } returns requestBodySpec
        every { requestBodySpec.bodyValue(any()) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(any<ParameterizedTypeReference<GeminiGenerateResponse>>()) } returns responseMono

        val webClient = mockk<WebClient>()
        every { webClient.post() } returns requestBodyUriSpec

        val webClientBuilder = mockk<WebClient.Builder>()
        every { webClientBuilder.baseUrl(any()) } returns webClientBuilder
        every { webClientBuilder.defaultHeader(any(), any()) } returns webClientBuilder
        every { webClientBuilder.build() } returns webClient

        return GeminiClient(llmProperties, webClientBuilder)
    }

    fun successResponse(text: String, totalTokens: Int = 100): GeminiGenerateResponse =
        GeminiGenerateResponse(
            candidates = listOf(
                GeminiCandidate(
                    content = GeminiResponseContent(
                        parts = listOf(GeminiResponsePart(text = text)),
                        role = "model"
                    )
                )
            ),
            usageMetadata = GeminiUsageMetadata(
                promptTokenCount = 50,
                candidatesTokenCount = 50,
                totalTokenCount = totalTokens
            )
        )

    Given("chatCompletion") {
        When("정상 응답 시") {
            Then("응답 텍스트를 반환한다") {
                val response = successResponse("번역된 텍스트입니다.")
                val client = createMockedClient(Mono.just(response))

                val result = client.chatCompletion(
                    systemPrompt = "시스템 프롬프트",
                    userPrompt = "사용자 프롬프트"
                )

                result shouldBe "번역된 텍스트입니다."
            }
        }

        When("candidates가 빈 리스트일 때") {
            Then("null을 반환한다") {
                val response = GeminiGenerateResponse(candidates = emptyList())
                val client = createMockedClient(Mono.just(response))

                val result = client.chatCompletion(
                    systemPrompt = "시스템 프롬프트",
                    userPrompt = "사용자 프롬프트"
                )

                result.shouldBeNull()
            }
        }

        When("candidates가 null일 때") {
            Then("null을 반환한다") {
                val response = GeminiGenerateResponse(candidates = null)
                val client = createMockedClient(Mono.just(response))

                val result = client.chatCompletion(
                    systemPrompt = "시스템 프롬프트",
                    userPrompt = "사용자 프롬프트"
                )

                result.shouldBeNull()
            }
        }

        When("content.parts가 null일 때") {
            Then("null을 반환한다") {
                val response = GeminiGenerateResponse(
                    candidates = listOf(
                        GeminiCandidate(
                            content = GeminiResponseContent(parts = null, role = "model")
                        )
                    )
                )
                val client = createMockedClient(Mono.just(response))

                val result = client.chatCompletion(
                    systemPrompt = "시스템 프롬프트",
                    userPrompt = "사용자 프롬프트"
                )

                result.shouldBeNull()
            }
        }

        When("API 호출 중 예외 발생 시") {
            Then("null을 반환한다") {
                val client = createMockedClient(
                    Mono.error(RuntimeException("Connection refused"))
                )

                val result = client.chatCompletion(
                    systemPrompt = "시스템 프롬프트",
                    userPrompt = "사용자 프롬프트"
                )

                result.shouldBeNull()
            }
        }

        When("타임아웃 발생 시") {
            Then("null을 반환한다") {
                val client = createMockedClient(
                    Mono.error(java.util.concurrent.TimeoutException("Timed out"))
                )

                val result = client.chatCompletion(
                    systemPrompt = "시스템 프롬프트",
                    userPrompt = "사용자 프롬프트"
                )

                result.shouldBeNull()
            }
        }

        When("LLM이 비활성화일 때") {
            Then("null을 반환한다") {
                val disabledProperties = llmProperties.copy(enabled = false)
                val webClientBuilder = mockk<WebClient.Builder>()
                every { webClientBuilder.baseUrl(any()) } returns webClientBuilder
                every { webClientBuilder.defaultHeader(any(), any()) } returns webClientBuilder
                every { webClientBuilder.build() } returns mockk()

                val client = GeminiClient(disabledProperties, webClientBuilder)

                val result = client.chatCompletion(
                    systemPrompt = "시스템 프롬프트",
                    userPrompt = "사용자 프롬프트"
                )

                result.shouldBeNull()
            }
        }

        When("temperature와 maxTokens를 지정할 때") {
            Then("정상 응답을 반환한다") {
                val response = successResponse("커스텀 설정 응답")
                val client = createMockedClient(Mono.just(response))

                val result = client.chatCompletion(
                    systemPrompt = "시스템 프롬프트",
                    userPrompt = "사용자 프롬프트",
                    temperature = 0.7,
                    maxTokens = 500
                )

                result shouldBe "커스텀 설정 응답"
            }
        }
    }
})
