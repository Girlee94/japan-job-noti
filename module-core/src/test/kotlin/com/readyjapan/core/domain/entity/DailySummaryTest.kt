package com.readyjapan.core.domain.entity

import com.readyjapan.core.domain.entity.enums.SummaryStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.ZoneId

class DailySummaryTest : BehaviorSpec({

    fun createSummary(
        summaryDate: LocalDate = LocalDate.of(2026, 1, 15),
        summaryContent: String = "테스트 요약 내용",
        jobPostingCount: Int = 5,
        newsArticleCount: Int = 3,
        communityPostCount: Int = 10,
        status: SummaryStatus = SummaryStatus.DRAFT
    ): DailySummary = DailySummary(
        summaryDate = summaryDate,
        summaryContent = summaryContent,
        jobPostingCount = jobPostingCount,
        newsArticleCount = newsArticleCount,
        communityPostCount = communityPostCount,
        status = status
    )

    Given("markAsSent") {
        When("전송 완료 시") {
            Then("SENT 상태와 sentAt이 설정된다") {
                val summary = createSummary()
                summary.markAsSent()

                summary.status shouldBe SummaryStatus.SENT
                summary.sentAt.shouldNotBeNull()
            }
        }
    }

    Given("markAsFailed") {
        When("전송 실패 시") {
            Then("FAILED 상태가 된다") {
                val summary = createSummary()
                summary.markAsFailed()

                summary.status shouldBe SummaryStatus.FAILED
            }
        }
    }

    Given("isSent") {
        When("SENT 상태일 때") {
            Then("true를 반환한다") {
                val summary = createSummary()
                summary.markAsSent()
                summary.isSent() shouldBe true
            }
        }
        When("DRAFT 상태일 때") {
            Then("false를 반환한다") {
                val summary = createSummary()
                summary.isSent() shouldBe false
            }
        }
    }

    Given("getTotalCount") {
        When("모든 카운트 합산 시") {
            Then("정확한 합계를 반환한다") {
                val summary = createSummary(
                    jobPostingCount = 5,
                    newsArticleCount = 3,
                    communityPostCount = 10
                )
                summary.getTotalCount() shouldBe 18
            }
        }
        When("모든 카운트가 0일 때") {
            Then("0을 반환한다") {
                val summary = createSummary(
                    jobPostingCount = 0,
                    newsArticleCount = 0,
                    communityPostCount = 0
                )
                summary.getTotalCount() shouldBe 0
            }
        }
    }

    Given("createForDate") {
        When("지정된 날짜로 생성 시") {
            Then("해당 날짜와 내용으로 요약이 생성된다") {
                val date = LocalDate.of(2026, 2, 20)
                val content = "2월 20일 요약"

                val summary = DailySummary.createForDate(date, content)

                summary.summaryDate shouldBe date
                summary.summaryContent shouldBe content
                summary.status shouldBe SummaryStatus.DRAFT
            }
        }
    }

    Given("createForToday") {
        When("오늘 날짜로 생성 시") {
            Then("JST 기준 오늘 날짜로 요약이 생성된다") {
                val content = "오늘의 요약"
                val todayJst = LocalDate.now(ZoneId.of("Asia/Tokyo"))

                val summary = DailySummary.createForToday(content)

                summary.summaryDate shouldBe todayJst
                summary.summaryContent shouldBe content
            }
        }
    }

    Given("updateSummary") {
        When("요약 내용 업데이트 시") {
            Then("정상 반영된다") {
                val summary = createSummary()
                summary.updateSummary(
                    summaryContent = "업데이트된 요약",
                    trendingTopics = """["AI", "취업"]""",
                    keyHighlights = """["하이라이트1"]"""
                )

                summary.summaryContent shouldBe "업데이트된 요약"
                summary.trendingTopics shouldBe """["AI", "취업"]"""
                summary.keyHighlights shouldBe """["하이라이트1"]"""
            }
        }
    }

    Given("updateCounts") {
        When("카운트 업데이트 시") {
            Then("정상 반영된다") {
                val summary = createSummary(
                    jobPostingCount = 0,
                    newsArticleCount = 0,
                    communityPostCount = 0
                )
                summary.updateCounts(
                    jobPostingCount = 10,
                    newsArticleCount = 5,
                    communityPostCount = 20
                )

                summary.jobPostingCount shouldBe 10
                summary.newsArticleCount shouldBe 5
                summary.communityPostCount shouldBe 20
            }
        }
    }
})
