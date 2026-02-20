package com.readyjapan.api.controller

import com.readyjapan.core.common.response.ApiResponse
import com.readyjapan.core.domain.entity.CommunityPost
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.infrastructure.crawler.reddit.RedditCrawlerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Crawler", description = "크롤링 관리 API")
@RestController
@RequestMapping("/api/crawler")
class CrawlerController(
    private val redditCrawlerService: RedditCrawlerService,
    private val communityPostRepository: CommunityPostRepository
) {
    @Operation(summary = "수동 크롤링 실행", description = "Reddit 크롤링을 수동으로 실행합니다.")
    @PostMapping("/reddit/run")
    fun runRedditCrawl(): ApiResponse<CrawlResultResponse> {
        val histories = redditCrawlerService.crawlAllSources()

        val result = CrawlResultResponse(
            sourcesProcessed = histories.size,
            totalFound = histories.sumOf { it.itemsFound },
            totalSaved = histories.sumOf { it.itemsSaved },
            totalUpdated = histories.sumOf { it.itemsUpdated },
            failedSources = histories.count { !it.isSuccessful() }
        )

        return ApiResponse.success(result, "크롤링이 완료되었습니다.")
    }

    @Operation(summary = "최근 커뮤니티 글 조회", description = "최근 수집된 커뮤니티 글 목록을 조회합니다.")
    @GetMapping("/posts/recent")
    fun getRecentPosts(
        @RequestParam(defaultValue = "20") limit: Int
    ): ApiResponse<List<CommunityPostResponse>> {
        val posts = communityPostRepository.findRecentPosts(limit.coerceIn(1, 100))
        return ApiResponse.success(posts.map { CommunityPostResponse.from(it) })
    }

    data class CrawlResultResponse(
        val sourcesProcessed: Int,
        val totalFound: Int,
        val totalSaved: Int,
        val totalUpdated: Int,
        val failedSources: Int
    )

    data class CommunityPostResponse(
        val id: Long,
        val platform: String,
        val title: String?,
        val content: String,
        val author: String?,
        val originalUrl: String,
        val likeCount: Int,
        val commentCount: Int,
        val sentiment: String?,
        val language: String,
        val publishedAt: String,
        val createdAt: String
    ) {
        companion object {
            fun from(post: CommunityPost): CommunityPostResponse {
                return CommunityPostResponse(
                    id = post.id,
                    platform = post.platform.name,
                    title = post.getDisplayTitle(),
                    content = post.getDisplayContent().take(500),
                    author = post.author,
                    originalUrl = post.originalUrl,
                    likeCount = post.likeCount,
                    commentCount = post.commentCount,
                    sentiment = post.sentiment?.name,
                    language = post.language,
                    publishedAt = post.publishedAt.toString(),
                    createdAt = post.createdAt.toString()
                )
            }
        }
    }
}
