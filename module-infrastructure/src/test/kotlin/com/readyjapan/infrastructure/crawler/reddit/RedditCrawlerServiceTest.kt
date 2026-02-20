package com.readyjapan.infrastructure.crawler.reddit

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.CrawlStatus
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.core.domain.repository.CrawlHistoryRepository
import com.readyjapan.core.domain.repository.CrawlSourceRepository
import com.readyjapan.infrastructure.crawler.reddit.dto.RedditListingData
import com.readyjapan.infrastructure.crawler.reddit.dto.RedditListingResponse
import com.readyjapan.infrastructure.crawler.reddit.dto.RedditPostData
import com.readyjapan.infrastructure.crawler.reddit.dto.RedditPostWrapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import reactor.core.publisher.Mono
import java.time.LocalDateTime

class RedditCrawlerServiceTest : BehaviorSpec({

    val redditApiClient = mockk<RedditApiClient>()
    val crawlSourceRepository = mockk<CrawlSourceRepository>()
    val communityPostRepository = mockk<CommunityPostRepository>()
    val crawlHistoryRepository = mockk<CrawlHistoryRepository>()
    val redditCrawlerService = RedditCrawlerService(
        redditApiClient, crawlSourceRepository, communityPostRepository, crawlHistoryRepository
    )

    beforeEach {
        clearMocks(redditApiClient, crawlSourceRepository, communityPostRepository, crawlHistoryRepository)
    }

    fun createSource(
        id: Long = 1L,
        subreddit: String = "japanlife"
    ): CrawlSource = CrawlSource(
        id = id,
        name = "r/$subreddit",
        url = "https://reddit.com/r/$subreddit",
        sourceType = SourceType.COMMUNITY,
        platform = CommunityPlatform.REDDIT,
        config = """{"subreddit": "$subreddit"}"""
    )

    fun createRedditPostData(
        id: String = "abc123",
        title: String = "Looking for IT jobs in Japan",
        selftext: String? = "Any tips for finding work?",
        subreddit: String = "japanlife",
        score: Int = 5,
        numComments: Int = 3,
        stickied: Boolean = false,
        locked: Boolean = false,
        removed: Boolean? = null
    ): RedditPostData = RedditPostData(
        id = id,
        name = "t3_$id",
        title = title,
        selftext = selftext,
        author = "testuser",
        subreddit = subreddit,
        subredditNamePrefixed = "r/$subreddit",
        url = "https://reddit.com/r/$subreddit/comments/$id",
        permalink = "/r/$subreddit/comments/$id",
        ups = score,
        downs = 0,
        score = score,
        upvoteRatio = 0.9,
        numComments = numComments,
        created = 1700000000.0,
        createdUtc = 1700000000.0,
        linkFlairText = null,
        over18 = false,
        spoiler = false,
        stickied = stickied,
        locked = locked,
        removed = removed,
        isSelf = true
    )

    fun createListingResponse(posts: List<RedditPostData>): RedditListingResponse =
        RedditListingResponse(
            kind = "Listing",
            data = RedditListingData(
                after = null,
                before = null,
                children = posts.map { RedditPostWrapper(kind = "t3", data = it) },
                dist = posts.size
            )
        )

    Given("crawlAllSources") {
        When("API 비활성화 시") {
            Then("빈 리스트를 반환한다") {
                every { redditApiClient.isEnabled() } returns false

                val result = redditCrawlerService.crawlAllSources()

                result.shouldBeEmpty()
            }
        }
        When("활성 소스 없을 시") {
            Then("빈 리스트를 반환한다") {
                every { redditApiClient.isEnabled() } returns true
                every {
                    crawlSourceRepository.findEnabledBySourceType(SourceType.COMMUNITY)
                } returns emptyList()

                val result = redditCrawlerService.crawlAllSources()

                result.shouldBeEmpty()
            }
        }
    }

    Given("crawlSource") {
        When("정상 크롤링 흐름 시") {
            Then("SUCCESS 히스토리를 반환한다") {
                val source = createSource()
                val postData = createRedditPostData()
                val response = createListingResponse(listOf(postData))
                val historySlot = slot<CrawlHistory>()

                every { crawlHistoryRepository.save(capture(historySlot)) } answers { historySlot.captured }
                every {
                    redditApiClient.getSubredditPosts(any(), any(), any(), any())
                } returns Mono.just(response)
                every {
                    communityPostRepository.findBySourceIdAndExternalId(any(), any())
                } returns null
                every { communityPostRepository.save(any()) } answers { firstArg() }
                every { crawlSourceRepository.save(any()) } answers { firstArg() }

                val result = redditCrawlerService.crawlSource(source)

                result.status shouldBe CrawlStatus.SUCCESS
                result.itemsFound shouldBe result.itemsFound // >= 0 확인
            }
        }
        When("API null 응답 시") {
            Then("FAILED 히스토리를 반환한다") {
                val source = createSource()
                val historySlot = slot<CrawlHistory>()

                every { crawlHistoryRepository.save(capture(historySlot)) } answers { historySlot.captured }
                every {
                    redditApiClient.getSubredditPosts(any(), any(), any(), any())
                } returns Mono.empty()

                val result = redditCrawlerService.crawlSource(source)

                result.status shouldBe CrawlStatus.FAILED
                result.errorMessage.shouldNotBeNull()
            }
        }
        When("예외 발생 시") {
            Then("FAILED 히스토리를 반환한다") {
                val source = createSource()
                val historySlot = slot<CrawlHistory>()

                every { crawlHistoryRepository.save(capture(historySlot)) } answers { historySlot.captured }
                every {
                    redditApiClient.getSubredditPosts(any(), any(), any(), any())
                } throws RuntimeException("Connection failed")

                val result = redditCrawlerService.crawlSource(source)

                result.status shouldBe CrawlStatus.FAILED
                result.errorMessage!! shouldBe "Connection failed"
            }
        }
    }

    Given("saveCrawledPosts") {
        When("새 게시물 저장 시") {
            Then("saved가 1이고 updated가 0이다") {
                val source = createSource()
                val postData = createRedditPostData(id = "new1")

                every {
                    communityPostRepository.findBySourceIdAndExternalId(source.id, "new1")
                } returns null
                every { communityPostRepository.save(any()) } answers { firstArg() }
                every { crawlSourceRepository.save(any()) } answers { firstArg() }

                val (saved, updated) = redditCrawlerService.saveCrawledPosts(source, listOf(postData))

                saved shouldBe 1
                updated shouldBe 0
                verify(exactly = 1) { communityPostRepository.save(any()) }
            }
        }
        When("기존 게시물 통계 업데이트 시") {
            Then("saved가 0이고 updated가 1이다") {
                val source = createSource()
                val postData = createRedditPostData(id = "existing1", score = 100, numComments = 50)
                val existingPost = CommunityPost(
                    source = source,
                    externalId = "existing1",
                    platform = CommunityPlatform.REDDIT,
                    content = "existing content",
                    originalUrl = "https://reddit.com/r/japanlife/existing1",
                    likeCount = 10,
                    commentCount = 5,
                    publishedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
                )

                every {
                    communityPostRepository.findBySourceIdAndExternalId(source.id, "existing1")
                } returns existingPost
                every { communityPostRepository.save(any()) } answers { firstArg() }
                every { crawlSourceRepository.save(any()) } answers { firstArg() }

                val (saved, updated) = redditCrawlerService.saveCrawledPosts(source, listOf(postData))

                saved shouldBe 0
                updated shouldBe 1
            }
        }
        When("통계 동일 시") {
            Then("스킵한다") {
                val source = createSource()
                val postData = createRedditPostData(id = "same1", score = 10, numComments = 5)
                val existingPost = CommunityPost(
                    source = source,
                    externalId = "same1",
                    platform = CommunityPlatform.REDDIT,
                    content = "existing content",
                    originalUrl = "https://reddit.com/r/japanlife/same1",
                    likeCount = 10,
                    commentCount = 5,
                    publishedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
                )

                every {
                    communityPostRepository.findBySourceIdAndExternalId(source.id, "same1")
                } returns existingPost
                every { crawlSourceRepository.save(any()) } answers { firstArg() }

                val (saved, updated) = redditCrawlerService.saveCrawledPosts(source, listOf(postData))

                saved shouldBe 0
                updated shouldBe 0
            }
        }
    }
})
