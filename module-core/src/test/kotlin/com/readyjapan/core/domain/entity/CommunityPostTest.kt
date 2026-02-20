package com.readyjapan.core.domain.entity

import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.Sentiment
import com.readyjapan.core.domain.entity.enums.SourceType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class CommunityPostTest : BehaviorSpec({

    fun createSource(): CrawlSource = CrawlSource(
        id = 1L,
        name = "test-source",
        url = "https://reddit.com/r/japanlife",
        sourceType = SourceType.COMMUNITY,
        platform = CommunityPlatform.REDDIT
    )

    fun createPost(
        language: String = "ja",
        title: String? = "テスト投稿",
        titleTranslated: String? = null,
        content: String = "テスト内容です",
        contentTranslated: String? = null,
        sentiment: Sentiment? = null,
        likeCount: Int = 0,
        commentCount: Int = 0
    ): CommunityPost = CommunityPost(
        source = createSource(),
        externalId = "test123",
        platform = CommunityPlatform.REDDIT,
        title = title,
        titleTranslated = titleTranslated,
        content = content,
        contentTranslated = contentTranslated,
        originalUrl = "https://reddit.com/r/japanlife/test123",
        sentiment = sentiment,
        language = language,
        likeCount = likeCount,
        commentCount = commentCount,
        publishedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
    )

    Given("needsTranslation") {
        When("일본어이고 번역이 없을 때") {
            Then("true를 반환한다") {
                val post = createPost(language = "ja", contentTranslated = null)
                post.needsTranslation() shouldBe true
            }
        }
        When("이미 번역이 있을 때") {
            Then("false를 반환한다") {
                val post = createPost(language = "ja", contentTranslated = "번역된 내용")
                post.needsTranslation() shouldBe false
            }
        }
        When("영어일 때") {
            Then("false를 반환한다") {
                val post = createPost(language = "en")
                post.needsTranslation() shouldBe false
            }
        }
    }

    Given("needsSentimentAnalysis") {
        When("감정 분석이 없을 때") {
            Then("true를 반환한다") {
                val post = createPost(sentiment = null)
                post.needsSentimentAnalysis() shouldBe true
            }
        }
        When("감정 분석이 있을 때") {
            Then("false를 반환한다") {
                val post = createPost(sentiment = Sentiment.POSITIVE)
                post.needsSentimentAnalysis() shouldBe false
            }
        }
    }

    Given("isPopular") {
        When("좋아요 10개 이상일 때") {
            Then("true를 반환한다") {
                val post = createPost(likeCount = 10)
                post.isPopular() shouldBe true
            }
        }
        When("좋아요 10개 미만일 때") {
            Then("false를 반환한다") {
                val post = createPost(likeCount = 9)
                post.isPopular() shouldBe false
            }
        }
    }

    Given("getDisplayTitle") {
        When("번역 제목 존재 시") {
            Then("번역 제목을 반환한다") {
                val post = createPost(title = "原文タイトル", titleTranslated = "번역된 제목")
                post.getDisplayTitle() shouldBe "번역된 제목"
            }
        }
        When("번역 제목 없을 시") {
            Then("원문 제목을 반환한다") {
                val post = createPost(title = "原文タイトル", titleTranslated = null)
                post.getDisplayTitle() shouldBe "原文タイトル"
            }
        }
    }

    Given("getDisplayContent") {
        When("번역 내용 존재 시") {
            Then("번역 내용을 반환한다") {
                val post = createPost(content = "原文内容", contentTranslated = "번역된 내용")
                post.getDisplayContent() shouldBe "번역된 내용"
            }
        }
        When("번역 내용 없을 시") {
            Then("원문 내용을 반환한다") {
                val post = createPost(content = "原文内容", contentTranslated = null)
                post.getDisplayContent() shouldBe "原文内容"
            }
        }
    }

    Given("updateStats") {
        When("통계 업데이트 시") {
            Then("정상 반영된다") {
                val post = createPost()
                post.updateStats(likeCount = 100, commentCount = 50, shareCount = 10)
                post.likeCount shouldBe 100
                post.commentCount shouldBe 50
                post.shareCount shouldBe 10
            }
        }
    }

    Given("applyTranslation") {
        When("번역 적용 시") {
            Then("정상 반영된다") {
                val post = createPost()
                post.applyTranslation("번역 제목", "번역 내용")
                post.titleTranslated shouldBe "번역 제목"
                post.contentTranslated shouldBe "번역 내용"
            }
        }
    }

    Given("applySentiment") {
        When("감정 분석 적용 시") {
            Then("감정이 설정된다") {
                val post = createPost(sentiment = null)
                post.applySentiment(Sentiment.NEGATIVE)
                post.sentiment shouldBe Sentiment.NEGATIVE
            }
        }
    }
})
