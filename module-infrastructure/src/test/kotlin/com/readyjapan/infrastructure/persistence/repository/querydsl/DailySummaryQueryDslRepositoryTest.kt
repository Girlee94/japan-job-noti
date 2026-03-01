package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.readyjapan.infrastructure.persistence.config.QueryDslConfig
import com.readyjapan.infrastructure.persistence.config.TestcontainersConfig
import com.readyjapan.infrastructure.persistence.repository.JpaDailySummaryRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.extensions.spring.SpringExtension
import jakarta.persistence.EntityManager
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@DataJpaTest
@Import(QueryDslConfig::class, TestcontainersConfig::class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
@Transactional
class DailySummaryQueryDslRepositoryTest(
    private val repository: JpaDailySummaryRepository,
    private val em: EntityManager
) : BehaviorSpec({

    extensions(SpringExtension)

    val baseDate = LocalDate.of(2025, 3, 1)

    fun setupSummaries() {
        em.persist(TestFixtures.dailySummary(summaryDate = baseDate.minusDays(2), summaryContent = "이틀 전 요약"))
        em.persist(TestFixtures.dailySummary(summaryDate = baseDate.minusDays(1), summaryContent = "어제 요약"))
        em.persist(TestFixtures.dailySummary(summaryDate = baseDate, summaryContent = "오늘 요약"))
        em.flush()
        em.clear()
    }

    Given("DailySummary 조회") {
        When("findRecentSummaries(limit=2)를 호출하면") {
            Then("최신 2개가 날짜 내림차순으로 반환된다") {
                setupSummaries()
                val result = repository.findRecentSummaries(2)
                result shouldHaveSize 2
                result[0].summaryDate shouldBe baseDate
                result[1].summaryDate shouldBe baseDate.minusDays(1)
            }
        }

        When("findBySummaryDateBetween으로 범위 조회하면") {
            Then("범위 내 요약만 날짜 내림차순으로 반환된다") {
                setupSummaries()
                val result = repository.findBySummaryDateBetween(baseDate.minusDays(1), baseDate)
                result shouldHaveSize 2
                result[0].summaryDate shouldBe baseDate
                result[1].summaryDate shouldBe baseDate.minusDays(1)
            }
        }

        When("findLatest를 호출하면") {
            Then("가장 최신 요약이 반환된다") {
                setupSummaries()
                val result = repository.findLatest()
                result.shouldNotBeNull()
                result.summaryDate shouldBe baseDate
                result.summaryContent shouldBe "오늘 요약"
            }
        }
    }

    Given("DailySummary가 없을 때") {
        When("findLatest를 호출하면") {
            Then("null이 반환된다") {
                val result = repository.findLatest()
                result.shouldBeNull()
            }
        }

        When("findRecentSummaries를 호출하면") {
            Then("빈 리스트가 반환된다") {
                val result = repository.findRecentSummaries(5)
                result shouldHaveSize 0
            }
        }
    }
})
