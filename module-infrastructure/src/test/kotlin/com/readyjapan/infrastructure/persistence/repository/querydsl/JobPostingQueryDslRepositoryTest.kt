package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.PostingStatus
import com.readyjapan.infrastructure.persistence.config.QueryDslConfig
import com.readyjapan.infrastructure.persistence.config.TestcontainersConfig
import com.readyjapan.infrastructure.persistence.repository.JpaJobPostingRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
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
class JobPostingQueryDslRepositoryTest(
    private val repository: JpaJobPostingRepository,
    private val em: EntityManager
) : BehaviorSpec({

    extensions(SpringExtension)

    val now = LocalDateTime.of(2025, 3, 1, 12, 0)

    fun persistSource(): CrawlSource {
        val source = TestFixtures.crawlSource(name = "Job Source")
        em.persist(source)
        em.flush()
        return source
    }

    fun setupJobData(): CrawlSource {
        val source = persistSource()
        val job1 = TestFixtures.jobPosting(source = source, externalId = "job-1", title = "Kotlin Engineer")
        val job2 = TestFixtures.jobPosting(source = source, externalId = "job-2", title = "Java Developer")
        val job3 = TestFixtures.jobPosting(
            source = source, externalId = "job-3", title = "Translated Job",
            language = "ja", titleTranslated = "번역된 제목"
        )
        val expiredJob = TestFixtures.jobPosting(
            source = source, externalId = "job-4", title = "Expired Position",
            status = PostingStatus.EXPIRED
        )
        em.persist(job1)
        em.persist(job2)
        em.persist(job3)
        em.persist(expiredJob)
        em.flush()

        em.updateCreatedAt("job_postings", job1.id, now.minusHours(3))
        em.updateCreatedAt("job_postings", job2.id, now.minusHours(2))
        em.updateCreatedAt("job_postings", job3.id, now.minusHours(1))
        em.updateCreatedAt("job_postings", expiredJob.id, now)
        return source
    }

    Given("JobPosting 조회") {
        When("findBySourceIdAndExternalId로 조회하면") {
            Then("존재하는 공고가 반환된다") {
                val source = setupJobData()
                val result = repository.findBySourceIdAndExternalId(source.id, "job-1")
                result.shouldNotBeNull()
                result.title shouldBe "Kotlin Engineer"
            }

            Then("없는 공고는 null이 반환된다") {
                val source = setupJobData()
                val result = repository.findBySourceIdAndExternalId(source.id, "nonexistent")
                result.shouldBeNull()
            }
        }

        When("findAllByCreatedAtAfter로 조회하면") {
            Then("기준 시점 이후 공고가 createdAt 내림차순으로 반환된다") {
                setupJobData()
                val result = repository.findAllByCreatedAtAfter(now.minusHours(2).minusMinutes(1))
                result shouldHaveSize 3
                result[0].externalId shouldBe "job-4"
                result[1].externalId shouldBe "job-3"
                result[2].externalId shouldBe "job-2"
            }
        }

        When("findAllByCreatedAtBetween으로 범위 조회하면") {
            Then("범위 내 공고가 반환된다") {
                setupJobData()
                val result = repository.findAllByCreatedAtBetween(now.minusHours(2), now.minusHours(1))
                result shouldHaveSize 2
            }
        }

        When("findAllNeedingTranslation을 호출하면") {
            Then("language=ja이고 titleTranslated=null인 공고만 반환된다") {
                setupJobData()
                val result = repository.findAllNeedingTranslation()
                result shouldHaveSize 3
                result.none { it.externalId == "job-3" } shouldBe true
            }
        }

        When("findRecentByStatus로 ACTIVE 공고를 조회하면") {
            Then("ACTIVE 상태의 최신 2개만 반환된다") {
                setupJobData()
                val result = repository.findRecentByStatus(PostingStatus.ACTIVE, 2)
                result shouldHaveSize 2
                result.all { it.status == PostingStatus.ACTIVE } shouldBe true
            }
        }

        When("countByCreatedAtBetween으로 전체 범위를 세면") {
            Then("전체 건수가 반환된다") {
                setupJobData()
                val result = repository.countByCreatedAtBetween(now.minusDays(1), now.plusDays(1))
                result shouldBe 4
            }
        }

        When("existsBySourceIdAndExternalId로 존재 여부를 확인하면") {
            Then("존재하는 경우 true, 없는 경우 false") {
                val source = setupJobData()
                repository.existsBySourceIdAndExternalId(source.id, "job-1") shouldBe true
                repository.existsBySourceIdAndExternalId(source.id, "nonexistent") shouldBe false
            }
        }
    }

    Given("JobPosting이 없을 때") {
        When("findAllNeedingTranslation을 호출하면") {
            Then("빈 리스트가 반환된다") {
                persistSource()
                val result = repository.findAllNeedingTranslation()
                result.shouldBeEmpty()
            }
        }

        When("countByCreatedAtBetween을 호출하면") {
            Then("0이 반환된다") {
                val result = repository.countByCreatedAtBetween(now.minusDays(1), now.plusDays(1))
                result shouldBe 0
            }
        }
    }
})
