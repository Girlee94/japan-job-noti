package com.readyjapan.core.domain.entity

import com.readyjapan.core.domain.entity.enums.SourceType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class NewsArticleTest : BehaviorSpec({

    fun createSource(): CrawlSource = CrawlSource(
        id = 1L,
        name = "test-news-site",
        url = "https://example.com/news",
        sourceType = SourceType.NEWS_SITE
    )

    fun createArticle(
        language: String = "ja",
        title: String = "日本のIT業界ニュース",
        titleTranslated: String? = null,
        summary: String? = "要約テスト",
        summaryTranslated: String? = null,
        content: String? = "本文テスト",
        contentTranslated: String? = null
    ): NewsArticle = NewsArticle(
        source = createSource(),
        externalId = "news123",
        title = title,
        titleTranslated = titleTranslated,
        summary = summary,
        summaryTranslated = summaryTranslated,
        content = content,
        contentTranslated = contentTranslated,
        originalUrl = "https://example.com/news/123",
        language = language
    )

    Given("needsTranslation") {
        When("일본어이고 번역 없을 때") {
            Then("true를 반환한다") {
                val article = createArticle(language = "ja", titleTranslated = null)
                article.needsTranslation() shouldBe true
            }
        }
        When("이미 번역 있을 때") {
            Then("false를 반환한다") {
                val article = createArticle(language = "ja", titleTranslated = "일본 IT 뉴스")
                article.needsTranslation() shouldBe false
            }
        }
        When("영어일 때") {
            Then("false를 반환한다") {
                val article = createArticle(language = "en")
                article.needsTranslation() shouldBe false
            }
        }
    }

    Given("applyTranslation") {
        When("번역 적용 시") {
            Then("정상 반영된다") {
                val article = createArticle()
                article.applyTranslation(
                    titleTranslated = "번역 제목",
                    summaryTranslated = "번역 요약",
                    contentTranslated = "번역 내용"
                )

                article.titleTranslated shouldBe "번역 제목"
                article.summaryTranslated shouldBe "번역 요약"
                article.contentTranslated shouldBe "번역 내용"
            }
        }
    }

    Given("getDisplayTitle") {
        When("번역 제목 존재 시") {
            Then("번역 제목을 반환한다") {
                val article = createArticle(title = "原文タイトル", titleTranslated = "번역된 제목")
                article.getDisplayTitle() shouldBe "번역된 제목"
            }
        }
        When("번역 제목 없을 시") {
            Then("원문 제목을 반환한다") {
                val article = createArticle(title = "原文タイトル", titleTranslated = null)
                article.getDisplayTitle() shouldBe "原文タイトル"
            }
        }
    }

    Given("getDisplaySummary") {
        When("번역 요약 존재 시") {
            Then("번역 요약을 반환한다") {
                val article = createArticle(summary = "原文要約", summaryTranslated = "번역된 요약")
                article.getDisplaySummary() shouldBe "번역된 요약"
            }
        }
        When("번역 요약 없을 시") {
            Then("원문 요약을 반환한다") {
                val article = createArticle(summary = "原文要約", summaryTranslated = null)
                article.getDisplaySummary() shouldBe "原文要約"
            }
        }
        When("요약 자체가 없을 시") {
            Then("null을 반환한다") {
                val article = createArticle(summary = null, summaryTranslated = null)
                article.getDisplaySummary().shouldBeNull()
            }
        }
    }
})
