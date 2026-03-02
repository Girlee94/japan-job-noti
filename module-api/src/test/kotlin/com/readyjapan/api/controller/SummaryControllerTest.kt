package com.readyjapan.api.controller

import com.readyjapan.core.common.exception.EntityNotFoundException
import com.readyjapan.core.domain.entity.DailySummary
import com.readyjapan.core.domain.repository.DailySummaryRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class SummaryControllerTest : BehaviorSpec({

    val dailySummaryRepository = mockk<DailySummaryRepository>()
    val summaryController = SummaryController(dailySummaryRepository)

    beforeEach {
        clearMocks(dailySummaryRepository)
    }

    fun createSummary(id: Long = 1L, date: LocalDate = LocalDate.of(2026, 1, 1)): DailySummary = DailySummary(
        id = id,
        summaryDate = date,
        summaryContent = "오늘의 일본 IT 취업 동향 요약입니다.",
        jobPostingCount = 5,
        newsArticleCount = 10,
        communityPostCount = 15
    )

    Given("getSummaries") {
        When("목록 조회 시") {
            Then("최근 일간 요약 목록을 반환한다") {
                val summaries = listOf(
                    createSummary(1L, LocalDate.of(2026, 1, 2)),
                    createSummary(2L, LocalDate.of(2026, 1, 1))
                )
                every { dailySummaryRepository.findRecentSummaries(10) } returns summaries

                val response = summaryController.getSummaries(10)

                response.success shouldBe true
                response.data!! shouldHaveSize 2
            }
        }
    }

    Given("getLatestSummary") {
        When("최신 요약이 존재할 때") {
            Then("최신 일간 요약을 반환한다") {
                val summary = createSummary(1L)
                every { dailySummaryRepository.findLatest() } returns summary

                val response = summaryController.getLatestSummary()

                response.success shouldBe true
                response.data!!.id shouldBe 1L
                response.data!!.totalCount shouldBe 30
            }
        }
        When("요약이 없을 때") {
            Then("EntityNotFoundException이 발생한다") {
                every { dailySummaryRepository.findLatest() } returns null

                shouldThrow<EntityNotFoundException> {
                    summaryController.getLatestSummary()
                }
            }
        }
    }

    Given("getSummaryByDate") {
        When("존재하는 날짜로 조회 시") {
            Then("해당 날짜의 요약을 반환한다") {
                val date = LocalDate.of(2026, 1, 1)
                val summary = createSummary(1L, date)
                every { dailySummaryRepository.findBySummaryDate(date) } returns summary

                val response = summaryController.getSummaryByDate(date)

                response.success shouldBe true
                response.data!!.summaryDate shouldBe "2026-01-01"
            }
        }
        When("존재하지 않는 날짜로 조회 시") {
            Then("EntityNotFoundException이 발생한다") {
                val date = LocalDate.of(2025, 12, 31)
                every { dailySummaryRepository.findBySummaryDate(date) } returns null

                shouldThrow<EntityNotFoundException> {
                    summaryController.getSummaryByDate(date)
                }
            }
        }
    }
})
