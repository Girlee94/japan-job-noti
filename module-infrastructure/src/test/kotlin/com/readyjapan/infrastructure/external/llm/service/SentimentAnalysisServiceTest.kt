package com.readyjapan.infrastructure.external.llm.service

import com.readyjapan.core.domain.entity.enums.Sentiment
import com.readyjapan.infrastructure.external.llm.OpenAiClient
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class SentimentAnalysisServiceTest : BehaviorSpec({

    val openAiClient = mockk<OpenAiClient>()
    val sentimentAnalysisService = SentimentAnalysisService(openAiClient)

    beforeEach { clearMocks(openAiClient) }

    Given("analyze") {
        When("빈 텍스트 입력 시") {
            Then("NEUTRAL을 반환한다") {
                val result = sentimentAnalysisService.analyze("   ")

                result.sentiment shouldBe Sentiment.NEUTRAL
                verify(exactly = 0) { openAiClient.chatCompletion(any(), any(), any(), any()) }
            }
        }
        When("POSITIVE 응답 시") {
            Then("POSITIVE를 파싱한다") {
                every {
                    openAiClient.chatCompletion(any(), any(), any(), any())
                } returns "POSITIVE\n채용 증가 소식입니다."

                val result = sentimentAnalysisService.analyze("좋은 소식입니다")

                result.sentiment shouldBe Sentiment.POSITIVE
                result.reason shouldBe "채용 증가 소식입니다."
            }
        }
        When("NEGATIVE 응답 시") {
            Then("NEGATIVE를 파싱한다") {
                every {
                    openAiClient.chatCompletion(any(), any(), any(), any())
                } returns "NEGATIVE\n취업 시장이 어렵습니다."

                val result = sentimentAnalysisService.analyze("어려운 상황입니다")

                result.sentiment shouldBe Sentiment.NEGATIVE
            }
        }
        When("NEUTRAL 응답 시") {
            Then("NEUTRAL을 파싱한다") {
                every {
                    openAiClient.chatCompletion(any(), any(), any(), any())
                } returns "NEUTRAL\n단순 정보 전달입니다."

                val result = sentimentAnalysisService.analyze("정보입니다")

                result.sentiment shouldBe Sentiment.NEUTRAL
            }
        }
        When("null 응답 시") {
            Then("NEUTRAL과 '응답 없음'을 반환한다") {
                every {
                    openAiClient.chatCompletion(any(), any(), any(), any())
                } returns null

                val result = sentimentAnalysisService.analyze("테스트")

                result.sentiment shouldBe Sentiment.NEUTRAL
                result.reason shouldBe "응답 없음"
            }
        }
    }

    Given("analyzeWithContext") {
        When("제목만 있을 때") {
            Then("정상 분석한다") {
                every {
                    openAiClient.chatCompletion(any(), any(), any(), any())
                } returns "POSITIVE\n좋은 소식"

                val result = sentimentAnalysisService.analyzeWithContext("좋은 뉴스", null)

                result.sentiment shouldBe Sentiment.POSITIVE
            }
        }
        When("제목과 내용 모두 있을 때") {
            Then("정상 분석한다") {
                every {
                    openAiClient.chatCompletion(any(), any(), any(), any())
                } returns "NEGATIVE\n부정적 내용"

                val result = sentimentAnalysisService.analyzeWithContext("뉴스 제목", "부정적인 내용입니다")

                result.sentiment shouldBe Sentiment.NEGATIVE
            }
        }
    }

    Given("analyzeBatch") {
        When("빈 리스트 입력 시") {
            Then("빈 리스트를 반환한다") {
                sentimentAnalysisService.analyzeBatch(emptyList()).shouldBeEmpty()
            }
        }
        When("단일 항목 시") {
            Then("analyze에 위임한다") {
                every {
                    openAiClient.chatCompletion(any(), any(), any(), any())
                } returns "POSITIVE\n좋은 소식"

                val result = sentimentAnalysisService.analyzeBatch(listOf("좋은 소식입니다"))

                result shouldHaveSize 1
                result[0].sentiment shouldBe Sentiment.POSITIVE
            }
        }
        When("여러 항목 정상 파싱 시") {
            Then("파싱된 감정 리스트를 반환한다") {
                every {
                    openAiClient.chatCompletion(any(), any(), any(), any())
                } returns "[1] POSITIVE: 채용 증가\n[2] NEGATIVE: 경기 침체\n[3] NEUTRAL: 정보 전달"

                val result = sentimentAnalysisService.analyzeBatch(listOf("텍스트1", "텍스트2", "텍스트3"))

                result shouldHaveSize 3
                result[0].sentiment shouldBe Sentiment.POSITIVE
                result[1].sentiment shouldBe Sentiment.NEGATIVE
                result[2].sentiment shouldBe Sentiment.NEUTRAL
            }
        }
        When("null 응답 시") {
            Then("모두 NEUTRAL을 반환한다") {
                every {
                    openAiClient.chatCompletion(any(), any(), any(), any())
                } returns null

                val result = sentimentAnalysisService.analyzeBatch(listOf("텍스트1", "텍스트2"))

                result shouldHaveSize 2
                result.forEach { it.sentiment shouldBe Sentiment.NEUTRAL }
            }
        }
    }
})
