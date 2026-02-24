package com.readyjapan.infrastructure.crawler.reddit

import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.core.domain.repository.CrawlSourceRepository
import com.readyjapan.infrastructure.crawler.reddit.dto.RedditPostData
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalDateTime

class RedditPostPersistenceServiceTest : BehaviorSpec({

    val communityPostRepository = mockk<CommunityPostRepository>()
    val crawlSourceRepository = mockk<CrawlSourceRepository>()
    val persistenceService = RedditPostPersistenceService(communityPostRepository, crawlSourceRepository)

    beforeEach {
        clearMocks(communityPostRepository, crawlSourceRepository)
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
        numComments: Int = 3
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
        created = Instant.now().epochSecond.toDouble(),
        createdUtc = Instant.now().epochSecond.toDouble(),
        linkFlairText = null,
        over18 = false,
        spoiler = false,
        stickied = false,
        locked = false,
        removed = null,
        isSelf = true
    )

    Given("saveCrawledPosts") {
        When("새 게시물 저장 시") {
            Then("saved가 1이고 updated가 0이다") {
                val source = createSource()
                val postData = createRedditPostData(id = "new1")

                every {
                    communityPostRepository.findAllBySourceIdAndExternalIdIn(source.id, listOf("new1"))
                } returns emptyList()
                every { communityPostRepository.saveAll(any<List<CommunityPost>>()) } answers { firstArg() }
                every { crawlSourceRepository.save(any()) } answers { firstArg() }

                val (saved, updated) = persistenceService.saveCrawledPosts(source, listOf(postData))

                saved shouldBe 1
                updated shouldBe 0
                verify(exactly = 1) { communityPostRepository.saveAll(any<List<CommunityPost>>()) }
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
                    communityPostRepository.findAllBySourceIdAndExternalIdIn(source.id, listOf("existing1"))
                } returns listOf(existingPost)
                every { communityPostRepository.saveAll(any<List<CommunityPost>>()) } answers { firstArg() }
                every { crawlSourceRepository.save(any()) } answers { firstArg() }

                val (saved, updated) = persistenceService.saveCrawledPosts(source, listOf(postData))

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
                    communityPostRepository.findAllBySourceIdAndExternalIdIn(source.id, listOf("same1"))
                } returns listOf(existingPost)
                every { crawlSourceRepository.save(any()) } answers { firstArg() }

                val (saved, updated) = persistenceService.saveCrawledPosts(source, listOf(postData))

                saved shouldBe 0
                updated shouldBe 0
            }
        }
        When("빈 게시물 리스트 저장 시") {
            Then("(0, 0)을 반환하고 소스의 lastCrawledAt이 갱신된다") {
                val source = createSource()

                every { crawlSourceRepository.save(any()) } answers { firstArg() }

                val (saved, updated) = persistenceService.saveCrawledPosts(source, emptyList())

                saved shouldBe 0
                updated shouldBe 0
                verify(exactly = 1) { crawlSourceRepository.save(any()) }
                verify(exactly = 0) { communityPostRepository.findAllBySourceIdAndExternalIdIn(any(), any()) }
            }
        }
    }
})
