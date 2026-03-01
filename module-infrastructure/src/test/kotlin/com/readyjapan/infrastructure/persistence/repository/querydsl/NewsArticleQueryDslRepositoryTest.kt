package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.infrastructure.persistence.config.QueryDslConfig
import com.readyjapan.infrastructure.persistence.config.TestcontainersConfig
import com.readyjapan.infrastructure.persistence.repository.JpaNewsArticleRepository
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
class NewsArticleQueryDslRepositoryTest(
    private val repository: JpaNewsArticleRepository,
    private val em: EntityManager
) : BehaviorSpec({

    extensions(SpringExtension)

    val now = LocalDateTime.of(2025, 3, 1, 12, 0)

    fun persistSource(): CrawlSource {
        val source = TestFixtures.crawlSource(name = "News Source")
        em.persist(source)
        em.flush()
        return source
    }

    fun setupArticles(): CrawlSource {
        val source = persistSource()
        val article1 = TestFixtures.newsArticle(
            source = source, externalId = "news-1", title = "Japan IT News 1",
            publishedAt = now.minusHours(2)
        )
        val article2 = TestFixtures.newsArticle(
            source = source, externalId = "news-2", title = "Japan IT News 2",
            language = "ja", titleTranslated = "번역된 뉴스", publishedAt = now.minusHours(1)
        )
        val articleNullPublished = TestFixtures.newsArticle(
            source = source, externalId = "news-3", title = "No Publish Date",
            publishedAt = null
        )
        em.persist(article1)
        em.persist(article2)
        em.persist(articleNullPublished)
        em.flush()

        em.updateCreatedAt("news_articles", article1.id, now.minusHours(3))
        em.updateCreatedAt("news_articles", article2.id, now.minusHours(2))
        em.updateCreatedAt("news_articles", articleNullPublished.id, now.minusHours(1))
        return source
    }

    Given("NewsArticle 조회") {
        When("findBySourceIdAndExternalId로 조회하면") {
            Then("존재하는 기사가 반환된다") {
                val source = setupArticles()
                val result = repository.findBySourceIdAndExternalId(source.id, "news-1")
                result.shouldNotBeNull()
                result.title shouldBe "Japan IT News 1"
            }

            Then("없는 기사는 null이 반환된다") {
                val source = setupArticles()
                val result = repository.findBySourceIdAndExternalId(source.id, "nonexistent")
                result.shouldBeNull()
            }
        }

        When("findAllByCreatedAtAfter로 조회하면") {
            Then("기준 시점 이후 기사가 createdAt 내림차순으로 반환된다") {
                setupArticles()
                val result = repository.findAllByCreatedAtAfter(now.minusHours(2).minusMinutes(1))
                result shouldHaveSize 2
                result[0].externalId shouldBe "news-3"
                result[1].externalId shouldBe "news-2"
            }
        }

        When("findAllByCreatedAtBetween으로 범위 조회하면") {
            Then("범위 내 기사가 반환된다") {
                setupArticles()
                val result = repository.findAllByCreatedAtBetween(now.minusHours(3), now.minusHours(1))
                result shouldHaveSize 3
            }
        }

        When("findAllNeedingTranslation을 호출하면") {
            Then("language=ja이고 titleTranslated=null인 기사만 반환된다") {
                setupArticles()
                val result = repository.findAllNeedingTranslation()
                result shouldHaveSize 2
                result.none { it.externalId == "news-2" } shouldBe true
            }
        }

        When("findRecentArticles로 NULLS LAST 정렬을 검증하면") {
            Then("publishedAt DESC + NULLS LAST로 정렬된다") {
                setupArticles()
                val result = repository.findRecentArticles(10)
                result shouldHaveSize 3
                result[0].publishedAt shouldBe now.minusHours(1)
                result[1].publishedAt shouldBe now.minusHours(2)
                result[2].publishedAt.shouldBeNull()
            }

            Then("limit만큼만 반환된다") {
                setupArticles()
                val result = repository.findRecentArticles(2)
                result shouldHaveSize 2
            }
        }

        When("countByCreatedAtBetween으로 전체 범위를 세면") {
            Then("전체 건수가 반환된다") {
                setupArticles()
                val result = repository.countByCreatedAtBetween(now.minusDays(1), now.plusDays(1))
                result shouldBe 3
            }
        }

        When("existsBySourceIdAndExternalId로 존재 여부를 확인하면") {
            Then("존재하는 경우 true, 없는 경우 false") {
                val source = setupArticles()
                repository.existsBySourceIdAndExternalId(source.id, "news-1") shouldBe true
                repository.existsBySourceIdAndExternalId(source.id, "nonexistent") shouldBe false
            }
        }
    }

    Given("NewsArticle이 없을 때") {
        When("findRecentArticles를 호출하면") {
            Then("빈 리스트가 반환된다") {
                val result = repository.findRecentArticles(10)
                result.shouldBeEmpty()
            }
        }
    }
})
