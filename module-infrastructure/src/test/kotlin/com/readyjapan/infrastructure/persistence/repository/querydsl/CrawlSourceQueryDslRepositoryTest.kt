package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.infrastructure.persistence.config.QueryDslConfig
import com.readyjapan.infrastructure.persistence.config.TestcontainersConfig
import com.readyjapan.infrastructure.persistence.repository.JpaCrawlSourceRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.extensions.spring.SpringExtension
import jakarta.persistence.EntityManager
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@DataJpaTest
@Import(QueryDslConfig::class, TestcontainersConfig::class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
@Transactional
class CrawlSourceQueryDslRepositoryTest(
    private val repository: JpaCrawlSourceRepository,
    private val em: EntityManager
) : BehaviorSpec({

    extensions(SpringExtension)

    fun setupMixedSources() {
        val enabledCommunity = TestFixtures.crawlSource(
            name = "Reddit Japan",
            sourceType = SourceType.COMMUNITY,
            platform = CommunityPlatform.REDDIT,
            enabled = true
        )
        val enabledJob = TestFixtures.crawlSource(
            name = "Indeed Japan",
            sourceType = SourceType.JOB_SITE,
            enabled = true
        )
        val disabledSource = TestFixtures.crawlSource(
            name = "Disabled Source",
            sourceType = SourceType.NEWS_SITE,
            enabled = false
        )
        em.persist(enabledCommunity)
        em.persist(enabledJob)
        em.persist(disabledSource)
        em.flush()
        em.clear()
    }

    Given("활성/비활성 CrawlSource 필터링") {
        When("findAllEnabled를 호출하면") {
            Then("활성화된 소스만 반환된다") {
                setupMixedSources()
                val result = repository.findAllEnabled()
                result shouldHaveSize 2
                result.map { it.name } shouldContainExactlyInAnyOrder listOf("Reddit Japan", "Indeed Japan")
            }
        }

        When("findEnabledBySourceType으로 COMMUNITY를 조회하면") {
            Then("활성화된 COMMUNITY 타입만 반환된다") {
                setupMixedSources()
                val result = repository.findEnabledBySourceType(SourceType.COMMUNITY)
                result shouldHaveSize 1
                result.first().name shouldBe "Reddit Japan"
            }
        }

        When("findEnabledBySourceType으로 NEWS_SITE를 조회하면 (비활성만 존재)") {
            Then("빈 결과가 반환된다") {
                setupMixedSources()
                val result = repository.findEnabledBySourceType(SourceType.NEWS_SITE)
                result.shouldBeEmpty()
            }
        }

        When("findEnabledBySourceTypeAndPlatform으로 COMMUNITY + REDDIT을 조회하면") {
            Then("해당 소스만 반환된다") {
                setupMixedSources()
                val result = repository.findEnabledBySourceTypeAndPlatform(
                    SourceType.COMMUNITY,
                    CommunityPlatform.REDDIT
                )
                result shouldHaveSize 1
                result.first().platform shouldBe CommunityPlatform.REDDIT
            }
        }

        When("findEnabledBySourceTypeAndPlatform으로 COMMUNITY + QIITA를 조회하면") {
            Then("빈 결과가 반환된다") {
                setupMixedSources()
                val result = repository.findEnabledBySourceTypeAndPlatform(
                    SourceType.COMMUNITY,
                    CommunityPlatform.QIITA
                )
                result.shouldBeEmpty()
            }
        }
    }
})
