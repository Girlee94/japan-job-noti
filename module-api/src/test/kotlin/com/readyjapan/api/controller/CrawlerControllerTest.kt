package com.readyjapan.api.controller

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.infrastructure.crawler.qiita.QiitaCrawlerService
import com.readyjapan.infrastructure.crawler.reddit.RedditCrawlerService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

class CrawlerControllerTest : BehaviorSpec({

    val redditCrawlerService = mockk<RedditCrawlerService>()
    val qiitaCrawlerService = mockk<QiitaCrawlerService>()
    val communityPostRepository = mockk<CommunityPostRepository>()
    val crawlerController = CrawlerController(redditCrawlerService, qiitaCrawlerService, communityPostRepository)

    beforeEach {
        clearMocks(redditCrawlerService, qiitaCrawlerService, communityPostRepository)
    }

    fun createSource(): CrawlSource = CrawlSource(
        id = 1L,
        name = "r/japanlife",
        url = "https://reddit.com/r/japanlife",
        sourceType = SourceType.COMMUNITY,
        platform = CommunityPlatform.REDDIT
    )

    fun createPost(id: Long = 1L): CommunityPost = CommunityPost(
        id = id,
        source = createSource(),
        externalId = "post$id",
        platform = CommunityPlatform.REDDIT,
        title = "Test Post $id",
        content = "Test content for post $id",
        originalUrl = "https://reddit.com/r/japanlife/post$id",
        likeCount = 5,
        commentCount = 3,
        publishedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
    )

    Given("runRedditCrawl") {
        When("정상 크롤링 시") {
            Then("성공 응답을 반환한다") {
                val source = createSource()
                val history = CrawlHistory.start(source)
                history.complete(itemsFound = 10, itemsSaved = 8, itemsUpdated = 2)
                every { redditCrawlerService.crawlAllSources() } returns listOf(history)

                val response = crawlerController.runRedditCrawl()

                response.success shouldBe true
                response.data!!.sourcesProcessed shouldBe 1
                response.data!!.totalFound shouldBe 10
                response.data!!.totalSaved shouldBe 8
                response.data!!.totalUpdated shouldBe 2
                response.data!!.failedSources shouldBe 0
            }
        }
        When("동시 실행 시") {
            Then("409 예외가 발생한다") {
                every { redditCrawlerService.crawlAllSources() } answers {
                    Thread.sleep(100)
                    emptyList()
                }

                val thread = Thread { crawlerController.runRedditCrawl() }
                thread.start()
                Thread.sleep(20)

                shouldThrow<ResponseStatusException> {
                    crawlerController.runRedditCrawl()
                }

                thread.join()
            }
        }
    }

    Given("getRecentPosts") {
        When("정상 조회 시") {
            Then("게시물 리스트를 반환한다") {
                val posts = listOf(createPost(1L), createPost(2L))
                every { communityPostRepository.findRecentPosts(20) } returns posts

                val response = crawlerController.getRecentPosts(20)

                response.success shouldBe true
                response.data!! shouldHaveSize 2
                response.data!![0].id shouldBe 1L
            }
        }
        When("limit이 100 초과 시") {
            Then("100으로 클램핑된다") {
                every { communityPostRepository.findRecentPosts(100) } returns emptyList()

                crawlerController.getRecentPosts(200)

                // coerceIn(1, 100)에 의해 100으로 클램핑되어 호출
            }
        }
        When("limit이 0 이하 시") {
            Then("1로 클램핑된다") {
                every { communityPostRepository.findRecentPosts(1) } returns emptyList()

                crawlerController.getRecentPosts(0)

                // coerceIn(1, 100)에 의해 1로 클램핑되어 호출
            }
        }
    }
})
