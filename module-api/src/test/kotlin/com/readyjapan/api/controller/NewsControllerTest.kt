package com.readyjapan.api.controller

import com.readyjapan.core.common.exception.EntityNotFoundException
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.NewsArticle
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.NewsArticleRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class NewsControllerTest : BehaviorSpec({

    val newsArticleRepository = mockk<NewsArticleRepository>()
    val newsController = NewsController(newsArticleRepository)

    beforeEach {
        clearMocks(newsArticleRepository)
    }

    fun createSource(): CrawlSource = CrawlSource(
        id = 1L,
        name = "IT Media",
        url = "https://www.itmedia.co.jp",
        sourceType = SourceType.NEWS_SITE
    )

    fun createArticle(id: Long = 1L, category: String? = "IT"): NewsArticle = NewsArticle(
        id = id,
        source = createSource(),
        externalId = "article$id",
        title = "News Article $id",
        summary = "Summary for article $id",
        category = category,
        originalUrl = "https://www.itmedia.co.jp/article$id",
        publishedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
    )

    Given("getNews") {
        When("카테고리 없이 조회 시") {
            Then("최근 뉴스 기사 목록을 반환한다") {
                val articles = listOf(createArticle(1L), createArticle(2L))
                every { newsArticleRepository.findRecentArticles(20) } returns articles

                val response = newsController.getNews(20, null)

                response.success shouldBe true
                response.data!! shouldHaveSize 2
            }
        }
        When("카테고리로 필터링 시") {
            Then("해당 카테고리의 뉴스를 반환한다") {
                val articles = listOf(createArticle(1L, "IT"))
                every { newsArticleRepository.findByCategory("IT") } returns articles

                val response = newsController.getNews(20, "IT")

                response.success shouldBe true
                response.data!! shouldHaveSize 1
                response.data!![0].category shouldBe "IT"
            }
        }
    }

    Given("getNewsArticle") {
        When("존재하는 ID로 조회 시") {
            Then("뉴스 기사 상세 정보를 반환한다") {
                val article = createArticle(1L)
                every { newsArticleRepository.findById(1L) } returns article

                val response = newsController.getNewsArticle(1L)

                response.success shouldBe true
                response.data!!.id shouldBe 1L
                response.data!!.title shouldBe "News Article 1"
            }
        }
        When("존재하지 않는 ID로 조회 시") {
            Then("EntityNotFoundException이 발생한다") {
                every { newsArticleRepository.findById(999L) } returns null

                shouldThrow<EntityNotFoundException> {
                    newsController.getNewsArticle(999L)
                }
            }
        }
    }
})
