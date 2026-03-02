package com.readyjapan.api.controller

import com.readyjapan.core.common.exception.EntityNotFoundException
import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CommunityPostRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class PostControllerTest : BehaviorSpec({

    val communityPostRepository = mockk<CommunityPostRepository>()
    val postController = PostController(communityPostRepository)

    beforeEach {
        clearMocks(communityPostRepository)
    }

    fun createSource(): CrawlSource = CrawlSource(
        id = 1L,
        name = "r/japanlife",
        url = "https://reddit.com/r/japanlife",
        sourceType = SourceType.COMMUNITY,
        platform = CommunityPlatform.REDDIT
    )

    fun createPost(id: Long = 1L, platform: CommunityPlatform = CommunityPlatform.REDDIT): CommunityPost =
        CommunityPost(
            id = id,
            source = createSource(),
            externalId = "post$id",
            platform = platform,
            title = "Test Post $id",
            content = "Test content for post $id",
            originalUrl = "https://reddit.com/r/japanlife/post$id",
            likeCount = 15,
            commentCount = 3,
            publishedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
        )

    Given("getPosts") {
        When("platform 파라미터 없이 조회 시") {
            Then("최근 게시글 목록을 반환한다") {
                val posts = listOf(createPost(1L), createPost(2L))
                every { communityPostRepository.findRecentPosts(20) } returns posts

                val response = postController.getPosts(20, null)

                response.success shouldBe true
                response.data!! shouldHaveSize 2
                response.data!![0].id shouldBe 1L
            }
        }
        When("platform 파라미터로 필터링 시") {
            Then("해당 플랫폼의 게시글을 반환한다") {
                val posts = listOf(createPost(1L, CommunityPlatform.QIITA))
                every { communityPostRepository.findAllByPlatform(CommunityPlatform.QIITA) } returns posts

                val response = postController.getPosts(20, CommunityPlatform.QIITA)

                response.success shouldBe true
                response.data!! shouldHaveSize 1
            }
        }
        When("limit이 100 초과 시") {
            Then("100으로 클램핑된다") {
                every { communityPostRepository.findRecentPosts(100) } returns emptyList()

                postController.getPosts(200, null)
            }
        }
    }

    Given("getPost") {
        When("존재하는 ID로 조회 시") {
            Then("게시글 상세 정보를 반환한다") {
                val post = createPost(1L)
                every { communityPostRepository.findById(1L) } returns post

                val response = postController.getPost(1L)

                response.success shouldBe true
                response.data!!.id shouldBe 1L
                response.data!!.content shouldNotBe null
            }
        }
        When("존재하지 않는 ID로 조회 시") {
            Then("EntityNotFoundException이 발생한다") {
                every { communityPostRepository.findById(999L) } returns null

                shouldThrow<EntityNotFoundException> {
                    postController.getPost(999L)
                }
            }
        }
    }

    Given("getPopularPosts") {
        When("인기 게시글 조회 시") {
            Then("좋아요 기준 이상의 게시글을 반환한다") {
                val posts = listOf(createPost(1L))
                every { communityPostRepository.findPopularPosts(10, 20) } returns posts

                val response = postController.getPopularPosts(20, 10)

                response.success shouldBe true
                response.data!! shouldHaveSize 1
            }
        }
    }
})
