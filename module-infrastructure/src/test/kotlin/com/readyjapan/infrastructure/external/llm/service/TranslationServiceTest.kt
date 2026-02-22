package com.readyjapan.infrastructure.external.llm.service

import com.readyjapan.infrastructure.external.llm.LlmClient
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class TranslationServiceTest : BehaviorSpec({

    val llmClient = mockk<LlmClient>()
    val translationService = TranslationService(llmClient)

    beforeEach { clearMocks(llmClient) }

    Given("translate") {
        When("빈 텍스트 입력 시") {
            Then("null을 반환한다") {
                val result = translationService.translate("   ")

                result.shouldBeNull()
                verify(exactly = 0) { llmClient.chatCompletion(any(), any(), any(), any()) }
            }
        }
        When("정상 번역 시") {
            Then("번역 결과를 반환한다") {
                every {
                    llmClient.chatCompletion(any(), eq("日本のIT企業で働きたい"), any(), any())
                } returns "일본 IT 기업에서 일하고 싶다"

                val result = translationService.translate("日本のIT企業で働きたい")

                result shouldBe "일본 IT 기업에서 일하고 싶다"
            }
        }
        When("LLM null 응답 시") {
            Then("null을 반환한다") {
                every {
                    llmClient.chatCompletion(any(), eq("テスト"), any(), any())
                } returns null

                val result = translationService.translate("テスト")

                result.shouldBeNull()
            }
        }
    }

    Given("translateTitleAndContent") {
        When("제목과 내용 번역 시") {
            Then("번역된 제목과 내용을 반환한다") {
                every {
                    llmClient.chatCompletion(any(), eq("タイトル"), any(), any())
                } returns "제목"
                every {
                    llmClient.chatCompletion(any(), eq("内容テスト"), any(), any())
                } returns "내용 테스트"

                val result = translationService.translateTitleAndContent("タイトル", "内容テスト")

                result.translatedTitle shouldBe "제목"
                result.translatedContent shouldBe "내용 테스트"
            }
        }
        When("content가 null일 때") {
            Then("translatedContent가 null이다") {
                every {
                    llmClient.chatCompletion(any(), eq("タイトル"), any(), any())
                } returns "제목"

                val result = translationService.translateTitleAndContent("タイトル", null)

                result.translatedTitle shouldBe "제목"
                result.translatedContent.shouldBeNull()
            }
        }
    }

    Given("translateBatch") {
        When("빈 리스트 입력 시") {
            Then("빈 리스트를 반환한다") {
                translationService.translateBatch(emptyList()).shouldBeEmpty()
            }
        }
        When("단일 항목 시") {
            Then("translate에 위임한다") {
                every {
                    llmClient.chatCompletion(any(), eq("テスト"), any(), any())
                } returns "테스트"

                val result = translationService.translateBatch(listOf("テスト"))

                result shouldHaveSize 1
                result[0] shouldBe "테스트"
            }
        }
        When("여러 항목 정상 파싱 시") {
            Then("파싱된 번역 리스트를 반환한다") {
                every {
                    llmClient.chatCompletion(any(), any(), any(), any())
                } returns "[1] 테스트1\n[2] 테스트2"

                val result = translationService.translateBatch(listOf("テスト1", "テスト2"))

                result shouldHaveSize 2
                result[0] shouldBe "테스트1"
                result[1] shouldBe "테스트2"
            }
        }
        When("LLM null 응답 시") {
            Then("null 리스트를 반환한다") {
                every {
                    llmClient.chatCompletion(any(), any(), any(), any())
                } returns null

                val result = translationService.translateBatch(listOf("テスト1", "テスト2"))

                result shouldHaveSize 2
                result.forEach { it.shouldBeNull() }
            }
        }
    }
})
