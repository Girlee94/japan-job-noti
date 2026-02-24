package com.readyjapan.infrastructure.crawler.reddit

import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.CrawlStatus
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CrawlHistoryRepository
import com.readyjapan.core.domain.repository.CrawlSourceRepository
import com.readyjapan.infrastructure.crawler.config.CrawlerConfig
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
import java.time.Instant

class RedditCrawlerServiceTest : BehaviorSpec({

    val redditApiClient = mockk<RedditApiClient>()
    val crawlSourceRepository = mockk<CrawlSourceRepository>()
    val crawlHistoryRepository = mockk<CrawlHistoryRepository>()
    val redditPostPersistenceService = mockk<RedditPostPersistenceService>()
    val crawlerConfig = CrawlerConfig()
    val redditCrawlerService = RedditCrawlerService(
        redditApiClient, crawlSourceRepository, crawlHistoryRepository,
        redditPostPersistenceService, crawlerConfig
    )

    beforeEach {
        clearMocks(redditApiClient, crawlSourceRepository, crawlHistoryRepository, redditPostPersistenceService)
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
        removed: Boolean? = null,
        createdUtc: Double = Instant.now().epochSecond.toDouble()
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
        created = createdUtc,
        createdUtc = createdUtc,
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
                    redditPostPersistenceService.saveCrawledPosts(any(), any())
                } returns Pair(1, 0)

                val result = redditCrawlerService.crawlSource(source)

                result.status shouldBe CrawlStatus.SUCCESS
                result.itemsFound shouldBe 1
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
        When("24시간 이전 게시물이 포함된 경우") {
            Then("오래된 게시물은 필터링되고 최신 게시물만 수집된다") {
                val source = createSource()
                val now = Instant.now().epochSecond.toDouble()
                val twoDaysAgo = (Instant.now().epochSecond - 48 * 3600).toDouble()

                val freshPost = createRedditPostData(id = "fresh1", createdUtc = now)
                val stalePost = createRedditPostData(id = "stale1", createdUtc = twoDaysAgo)
                val response = createListingResponse(listOf(freshPost, stalePost))
                val historySlot = slot<CrawlHistory>()

                every { crawlHistoryRepository.save(capture(historySlot)) } answers { historySlot.captured }
                every {
                    redditApiClient.getSubredditPosts(any(), any(), any(), any())
                } returns Mono.just(response)
                every {
                    redditPostPersistenceService.saveCrawledPosts(any(), match { it.size == 1 })
                } returns Pair(1, 0)

                val result = redditCrawlerService.crawlSource(source)

                result.status shouldBe CrawlStatus.SUCCESS
                result.itemsFound shouldBe 1
                result.itemsSaved shouldBe 1

                verify {
                    redditPostPersistenceService.saveCrawledPosts(any(), match { posts ->
                        posts.size == 1 && posts[0].id == "fresh1"
                    })
                }
            }
        }
        When("cutoff 경계 근처의 게시물인 경우") {
            Then("경계 직후 게시물은 포함되고 경계 이전 게시물은 제외된다") {
                val source = createSource()
                val cutoffEpoch = Instant.now().epochSecond - crawlerConfig.freshnessHours * 3600
                // 경계 10초 후 (확실히 포함됨)
                val nearPost = createRedditPostData(id = "near1", createdUtc = (cutoffEpoch + 10).toDouble())
                // 경계 1분 전 (확실히 제외됨)
                val beforePost = createRedditPostData(id = "before1", createdUtc = (cutoffEpoch - 60).toDouble())
                val response = createListingResponse(listOf(nearPost, beforePost))
                val historySlot = slot<CrawlHistory>()

                every { crawlHistoryRepository.save(capture(historySlot)) } answers { historySlot.captured }
                every {
                    redditApiClient.getSubredditPosts(any(), any(), any(), any())
                } returns Mono.just(response)
                every {
                    redditPostPersistenceService.saveCrawledPosts(any(), any())
                } returns Pair(1, 0)

                val result = redditCrawlerService.crawlSource(source)

                result.status shouldBe CrawlStatus.SUCCESS
                result.itemsFound shouldBe 1

                verify {
                    redditPostPersistenceService.saveCrawledPosts(any(), match { posts ->
                        posts.size == 1 && posts[0].id == "near1"
                    })
                }
            }
        }
        When("모든 게시물이 필터링된 경우") {
            Then("빈 리스트로 PersistenceService를 호출한다") {
                val source = createSource()
                val twoDaysAgo = (Instant.now().epochSecond - 48 * 3600).toDouble()

                val stalePost = createRedditPostData(id = "stale1", createdUtc = twoDaysAgo)
                val response = createListingResponse(listOf(stalePost))
                val historySlot = slot<CrawlHistory>()

                every { crawlHistoryRepository.save(capture(historySlot)) } answers { historySlot.captured }
                every {
                    redditApiClient.getSubredditPosts(any(), any(), any(), any())
                } returns Mono.just(response)
                every {
                    redditPostPersistenceService.saveCrawledPosts(any(), any())
                } returns Pair(0, 0)

                val result = redditCrawlerService.crawlSource(source)

                result.status shouldBe CrawlStatus.SUCCESS
                result.itemsFound shouldBe 0
                result.itemsSaved shouldBe 0

                verify {
                    redditPostPersistenceService.saveCrawledPosts(any(), match { it.isEmpty() })
                }
            }
        }
    }
})
