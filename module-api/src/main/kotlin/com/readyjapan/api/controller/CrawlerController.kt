package com.readyjapan.api.controller

import com.readyjapan.api.controller.dto.CommunityPostResponse
import com.readyjapan.core.common.response.ApiResponse
import com.readyjapan.core.domain.repository.CommunityPostRepository
import com.readyjapan.infrastructure.crawler.qiita.QiitaCrawlerService
import com.readyjapan.infrastructure.crawler.reddit.RedditCrawlerService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

@Tag(name = "Crawler", description = "크롤링 관리 API")
@RestController
@RequestMapping("/api/crawler")
class CrawlerController(
    private val redditCrawlerService: RedditCrawlerService,
    private val qiitaCrawlerService: QiitaCrawlerService,
    private val communityPostRepository: CommunityPostRepository
) {
    private val crawlInProgress = AtomicBoolean(false)
    private val qiitaCrawlInProgress = AtomicBoolean(false)

    @Operation(summary = "수동 크롤링 실행", description = "Reddit 크롤링을 수동으로 실행합니다.")
    @PostMapping("/reddit/run")
    fun runRedditCrawl(): Callable<ApiResponse<CrawlResultResponse>> {
        if (!crawlInProgress.compareAndSet(false, true)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "크롤링이 이미 실행 중입니다.")
        }

        return Callable {
            try {
                val histories = redditCrawlerService.crawlAllSources()
                val result = CrawlResultResponse(
                    sourcesProcessed = histories.size,
                    totalFound = histories.sumOf { it.itemsFound },
                    totalSaved = histories.sumOf { it.itemsSaved },
                    totalUpdated = histories.sumOf { it.itemsUpdated },
                    failedSources = histories.count { !it.isSuccessful() }
                )
                ApiResponse.success(result, "크롤링이 완료되었습니다.")
            } catch (e: Exception) {
                logger.error(e) { "Reddit 크롤링 중 오류 발생" }
                ApiResponse.error("크롤링 중 오류가 발생했습니다: ${e.message}")
            } finally {
                crawlInProgress.set(false)
            }
        }
    }

    @Operation(summary = "Qiita 수동 크롤링 실행", description = "Qiita 크롤링을 수동으로 실행합니다.")
    @PostMapping("/qiita/run")
    fun runQiitaCrawl(): Callable<ApiResponse<CrawlResultResponse>> {
        if (!qiitaCrawlInProgress.compareAndSet(false, true)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Qiita 크롤링이 이미 실행 중입니다.")
        }

        return Callable {
            try {
                val histories = qiitaCrawlerService.crawlAllSources()
                val result = CrawlResultResponse(
                    sourcesProcessed = histories.size,
                    totalFound = histories.sumOf { it.itemsFound },
                    totalSaved = histories.sumOf { it.itemsSaved },
                    totalUpdated = histories.sumOf { it.itemsUpdated },
                    failedSources = histories.count { !it.isSuccessful() }
                )
                ApiResponse.success(result, "Qiita 크롤링이 완료되었습니다.")
            } catch (e: Exception) {
                logger.error(e) { "Qiita 크롤링 중 오류 발생" }
                ApiResponse.error("Qiita 크롤링 중 오류가 발생했습니다: ${e.message}")
            } finally {
                qiitaCrawlInProgress.set(false)
            }
        }
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

}
