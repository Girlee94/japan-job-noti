package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.infrastructure.persistence.config.QueryDslConfig
import com.readyjapan.infrastructure.persistence.config.TestcontainersConfig
import com.readyjapan.infrastructure.persistence.repository.JpaCrawlHistoryRepository
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
import java.time.LocalDateTime

@DataJpaTest
@Import(QueryDslConfig::class, TestcontainersConfig::class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
@Transactional
class CrawlHistoryQueryDslRepositoryTest(
    private val repository: JpaCrawlHistoryRepository,
    private val em: EntityManager
) : BehaviorSpec({

    extensions(SpringExtension)

    val now = LocalDateTime.of(2025, 3, 1, 12, 0)

    fun persistSource(name: String = "Source A"): CrawlSource {
        val source = TestFixtures.crawlSource(name = name)
        em.persist(source)
        em.flush()
        return source
    }

    Given("CrawlHistory 소스별 조회") {
        When("findBySourceId로 조회하면") {
            Then("해당 소스의 이력만 startedAt 내림차순으로 반환된다") {
                val source = persistSource()
                val other = persistSource("Source B")
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = now.minusHours(3)))
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = now.minusHours(1)))
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = now))
                em.persist(TestFixtures.crawlHistory(source = other, startedAt = now))
                em.flush()
                em.clear()

                val result = repository.findBySourceId(source.id)
                result shouldHaveSize 3
                result[0].startedAt shouldBe now
                result[1].startedAt shouldBe now.minusHours(1)
                result[2].startedAt shouldBe now.minusHours(3)
            }
        }

        When("findRecentBySourceId(limit=2)로 조회하면") {
            Then("최신 2개만 반환된다") {
                val source = persistSource()
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = now.minusHours(3)))
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = now.minusHours(1)))
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = now))
                em.flush()
                em.clear()

                val result = repository.findRecentBySourceId(source.id, 2)
                result shouldHaveSize 2
                result[0].startedAt shouldBe now
                result[1].startedAt shouldBe now.minusHours(1)
            }
        }

        When("findLatestBySourceId로 조회하면") {
            Then("가장 최신 이력이 반환된다") {
                val source = persistSource()
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = now.minusHours(2)))
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = now))
                em.flush()
                em.clear()

                val result = repository.findLatestBySourceId(source.id)
                result.shouldNotBeNull()
                result.startedAt shouldBe now
            }
        }

        When("findLatestBySourceId로 없는 소스를 조회하면") {
            Then("null이 반환된다") {
                val result = repository.findLatestBySourceId(99999L)
                result.shouldBeNull()
            }
        }
    }

    Given("CrawlHistory 시간 범위 조회") {
        When("findByStartedAtBetween으로 범위 조회하면") {
            Then("범위 내 이력만 반환된다") {
                val source = persistSource()
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = now.minusHours(5)))
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = now.minusHours(1)))
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = now))
                em.flush()
                em.clear()

                val result = repository.findByStartedAtBetween(now.minusHours(2), now)
                result shouldHaveSize 2
                result.all { it.startedAt >= now.minusHours(2) && it.startedAt <= now } shouldBe true
            }
        }
    }

    Given("CrawlHistory 삭제") {
        When("deleteOlderThan을 호출하면") {
            Then("기준 시점 이전 레코드가 삭제되고 삭제 건수가 반환된다") {
                val source = persistSource()
                val cutoff = now
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = cutoff.minusDays(2)))
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = cutoff.minusDays(1)))
                em.persist(TestFixtures.crawlHistory(source = source, startedAt = cutoff.plusHours(1)))
                em.flush()
                em.clear()

                val deletedCount = repository.deleteOlderThan(cutoff)
                em.flush()
                em.clear()

                deletedCount shouldBe 2
                repository.findAll() shouldHaveSize 1
            }
        }
    }
})
