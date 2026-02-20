package com.readyjapan.core.domain.entity

import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.CrawlStatus
import com.readyjapan.core.domain.entity.enums.SourceType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class CrawlHistoryTest : BehaviorSpec({

    fun createSource(): CrawlSource = CrawlSource(
        id = 1L,
        name = "test-source",
        url = "https://reddit.com/r/japanlife",
        sourceType = SourceType.COMMUNITY,
        platform = CommunityPlatform.REDDIT
    )

    Given("start") {
        When("새 히스토리 생성 시") {
            Then("RUNNING 상태이다") {
                val history = CrawlHistory.start(createSource())
                history.status shouldBe CrawlStatus.RUNNING
                history.source shouldBe history.source
                history.itemsFound shouldBe 0
                history.itemsSaved shouldBe 0
            }
        }
    }

    Given("complete") {
        When("크롤링 완료 시") {
            Then("SUCCESS 상태로 전이된다") {
                val history = CrawlHistory.start(createSource())
                history.complete(itemsFound = 10, itemsSaved = 8, itemsUpdated = 2)

                history.status shouldBe CrawlStatus.SUCCESS
                history.itemsFound shouldBe 10
                history.itemsSaved shouldBe 8
                history.itemsUpdated shouldBe 2
                history.finishedAt.shouldNotBeNull()
                history.durationMs.shouldNotBeNull()
                history.durationMs!! shouldBeGreaterThanOrEqual 0L
            }
        }
    }

    Given("fail") {
        When("크롤링 실패 시") {
            Then("FAILED 상태로 전이된다") {
                val history = CrawlHistory.start(createSource())
                history.fail("Connection timeout")

                history.status shouldBe CrawlStatus.FAILED
                history.errorMessage shouldBe "Connection timeout"
                history.finishedAt.shouldNotBeNull()
                history.durationMs.shouldNotBeNull()
            }
        }
    }

    Given("partial") {
        When("부분 완료 시") {
            Then("PARTIAL 상태로 전이된다") {
                val history = CrawlHistory.start(createSource())
                history.partial(
                    itemsFound = 10,
                    itemsSaved = 5,
                    itemsUpdated = 1,
                    errorMessage = "Some items failed"
                )

                history.status shouldBe CrawlStatus.PARTIAL
                history.itemsFound shouldBe 10
                history.itemsSaved shouldBe 5
                history.itemsUpdated shouldBe 1
                history.errorMessage shouldBe "Some items failed"
                history.finishedAt.shouldNotBeNull()
            }
        }
    }

    Given("isRunning") {
        When("RUNNING 상태일 때") {
            Then("true를 반환한다") {
                val history = CrawlHistory.start(createSource())
                history.isRunning() shouldBe true
            }
        }
        When("SUCCESS 상태일 때") {
            Then("false를 반환한다") {
                val history = CrawlHistory.start(createSource())
                history.complete(itemsFound = 0, itemsSaved = 0)
                history.isRunning() shouldBe false
            }
        }
    }

    Given("isSuccessful") {
        When("SUCCESS 상태일 때") {
            Then("true를 반환한다") {
                val history = CrawlHistory.start(createSource())
                history.complete(itemsFound = 5, itemsSaved = 5)
                history.isSuccessful() shouldBe true
            }
        }
        When("FAILED 상태일 때") {
            Then("false를 반환한다") {
                val history = CrawlHistory.start(createSource())
                history.fail("Error")
                history.isSuccessful() shouldBe false
            }
        }
    }

    Given("getDurationSeconds") {
        When("durationMs 설정 시") {
            Then("초 단위를 반환한다") {
                val history = CrawlHistory.start(createSource())
                history.complete(itemsFound = 1, itemsSaved = 1)

                history.getDurationSeconds().shouldNotBeNull() shouldBeGreaterThanOrEqual 0L
            }
        }
        When("durationMs가 null일 때") {
            Then("null을 반환한다") {
                val history = CrawlHistory.start(createSource())
                history.getDurationSeconds().shouldBeNull()
            }
        }
    }
})
