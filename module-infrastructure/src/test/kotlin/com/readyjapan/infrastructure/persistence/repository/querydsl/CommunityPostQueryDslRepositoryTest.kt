package com.readyjapan.infrastructure.persistence.repository.querydsl

import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.Sentiment
import com.readyjapan.infrastructure.persistence.config.QueryDslConfig
import com.readyjapan.infrastructure.persistence.config.TestcontainersConfig
import com.readyjapan.infrastructure.persistence.repository.JpaCommunityPostRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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
class CommunityPostQueryDslRepositoryTest(
    private val repository: JpaCommunityPostRepository,
    private val em: EntityManager
) : BehaviorSpec({

    extensions(SpringExtension)

    val now = LocalDateTime.of(2025, 3, 1, 12, 0)

    fun persistSource(): CrawlSource {
        val source = TestFixtures.crawlSource(name = "Reddit Community")
        em.persist(source)
        em.flush()
        return source
    }

    fun setupPosts(): CrawlSource {
        val source = persistSource()
        val post1 = TestFixtures.communityPost(
            source = source, externalId = "post-1", content = "일본어 게시글",
            language = "ja", sentiment = Sentiment.POSITIVE, likeCount = 15,
            publishedAt = now.minusHours(3)
        )
        val post2 = TestFixtures.communityPost(
            source = source, externalId = "post-2", content = "번역 필요 게시글",
            language = "ja", sentiment = null, contentTranslated = null, likeCount = 5,
            publishedAt = now.minusHours(2)
        )
        val post3 = TestFixtures.communityPost(
            source = source, externalId = "post-3", content = "이미 번역된 게시글",
            language = "ja", sentiment = Sentiment.NEGATIVE,
            contentTranslated = "Translated content", likeCount = 20,
            publishedAt = now.minusHours(1)
        )
        val post4 = TestFixtures.communityPost(
            source = source, externalId = "post-4", content = "English post",
            language = "en", sentiment = Sentiment.NEUTRAL, likeCount = 3,
            publishedAt = now
        )
        em.persist(post1)
        em.persist(post2)
        em.persist(post3)
        em.persist(post4)
        em.flush()

        em.updateCreatedAt("community_posts", post1.id, now.minusHours(3))
        em.updateCreatedAt("community_posts", post2.id, now.minusHours(2))
        em.updateCreatedAt("community_posts", post3.id, now.minusHours(1))
        em.updateCreatedAt("community_posts", post4.id, now)
        return source
    }

    Given("CommunityPost 단건/복수 조회") {
        When("findBySourceIdAndExternalId로 조회하면") {
            Then("존재하는 게시글이 반환된다") {
                val source = setupPosts()
                val result = repository.findBySourceIdAndExternalId(source.id, "post-1")
                result.shouldNotBeNull()
                result.content shouldBe "일본어 게시글"
            }

            Then("없는 게시글은 null이 반환된다") {
                val source = setupPosts()
                val result = repository.findBySourceIdAndExternalId(source.id, "nonexistent")
                result.shouldBeNull()
            }
        }

        When("findAllBySourceIdAndExternalIdIn으로 IN절 조회하면") {
            Then("존재하는 게시글만 반환된다") {
                val source = setupPosts()
                val result = repository.findAllBySourceIdAndExternalIdIn(
                    source.id, listOf("post-1", "post-3", "nonexistent")
                )
                result shouldHaveSize 2
                result.map { it.externalId } shouldContainExactlyInAnyOrder listOf("post-1", "post-3")
            }
        }
    }

    Given("CommunityPost 시간 기반 조회") {
        When("findAllByCreatedAtAfter로 조회하면") {
            Then("기준 시점 이후 게시글이 createdAt 내림차순으로 반환된다") {
                setupPosts()
                val result = repository.findAllByCreatedAtAfter(now.minusHours(2).minusMinutes(1))
                result shouldHaveSize 3
                result[0].externalId shouldBe "post-4"
                result[1].externalId shouldBe "post-3"
                result[2].externalId shouldBe "post-2"
            }
        }

        When("findAllByCreatedAtBetween으로 범위 조회하면") {
            Then("범위 내 게시글이 반환된다") {
                setupPosts()
                val result = repository.findAllByCreatedAtBetween(now.minusHours(2), now.minusHours(1))
                result shouldHaveSize 2
            }
        }
    }

    Given("CommunityPost 번역/감정분석 필터") {
        When("findAllNeedingTranslation을 호출하면") {
            Then("language=ja이고 contentTranslated=null인 게시글만 반환된다") {
                setupPosts()
                val result = repository.findAllNeedingTranslation()
                result shouldHaveSize 2
                result.map { it.externalId } shouldContainExactlyInAnyOrder listOf("post-1", "post-2")
            }
        }

        When("findAllNeedingSentimentAnalysis를 호출하면") {
            Then("sentiment=null인 게시글만 반환된다") {
                setupPosts()
                val result = repository.findAllNeedingSentimentAnalysis()
                result shouldHaveSize 1
                result.first().externalId shouldBe "post-2"
            }
        }
    }

    Given("CommunityPost 인기/최신 게시글 조회") {
        When("findPopularPosts로 minLikes=10 조회하면") {
            Then("likeCount >= 10인 게시글이 likeCount 내림차순으로 반환된다") {
                setupPosts()
                val result = repository.findPopularPosts(minLikes = 10, limit = 10)
                result shouldHaveSize 2
                result[0].likeCount shouldBe 20
                result[1].likeCount shouldBe 15
            }

            Then("limit만큼만 반환된다") {
                setupPosts()
                val result = repository.findPopularPosts(minLikes = 10, limit = 1)
                result shouldHaveSize 1
                result.first().likeCount shouldBe 20
            }
        }

        When("findRecentPosts를 호출하면") {
            Then("publishedAt DESC 순으로 반환된다") {
                setupPosts()
                val result = repository.findRecentPosts(10)
                result shouldHaveSize 4
                result[0].publishedAt shouldBe now
                result[1].publishedAt shouldBe now.minusHours(1)
                result[2].publishedAt shouldBe now.minusHours(2)
                result[3].publishedAt shouldBe now.minusHours(3)
            }
        }
    }

    Given("CommunityPost 카운트/존재 여부") {
        When("countByCreatedAtBetween으로 전체 범위를 세면") {
            Then("전체 건수가 반환된다") {
                setupPosts()
                val result = repository.countByCreatedAtBetween(now.minusDays(1), now.plusDays(1))
                result shouldBe 4
            }
        }

        When("countBySentiment으로 POSITIVE를 세면") {
            Then("POSITIVE 건수가 반환된다") {
                setupPosts()
                val result = repository.countBySentiment(Sentiment.POSITIVE)
                result shouldBe 1
            }
        }

        When("existsBySourceIdAndExternalId로 확인하면") {
            Then("존재하는 경우 true, 없는 경우 false") {
                val source = setupPosts()
                repository.existsBySourceIdAndExternalId(source.id, "post-1") shouldBe true
                repository.existsBySourceIdAndExternalId(source.id, "nonexistent") shouldBe false
            }
        }
    }

    Given("CommunityPost가 없을 때") {
        When("findAllNeedingTranslation을 호출하면") {
            Then("빈 리스트가 반환된다") {
                persistSource()
                val result = repository.findAllNeedingTranslation()
                result.shouldBeEmpty()
            }
        }

        When("countBySentiment을 호출하면") {
            Then("0이 반환된다") {
                val result = repository.countBySentiment(Sentiment.POSITIVE)
                result shouldBe 0
            }
        }

        When("findPopularPosts를 호출하면") {
            Then("빈 리스트가 반환된다") {
                val result = repository.findPopularPosts(minLikes = 10, limit = 10)
                result.shouldBeEmpty()
            }
        }
    }
})
