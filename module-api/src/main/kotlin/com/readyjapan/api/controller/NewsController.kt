package com.readyjapan.api.controller

import com.readyjapan.api.controller.dto.NewsArticleResponse
import com.readyjapan.core.common.exception.EntityNotFoundException
import com.readyjapan.core.common.response.ApiResponse
import com.readyjapan.core.domain.repository.NewsArticleRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "News Articles", description = "뉴스 기사 조회 API")
@RestController
@RequestMapping("/api/v1/news")
class NewsController(
    private val newsArticleRepository: NewsArticleRepository
) {

    @Operation(summary = "뉴스 기사 목록 조회", description = "최근 뉴스 기사 목록을 조회합니다.")
    @GetMapping
    fun getNews(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) category: String?
    ): ApiResponse<List<NewsArticleResponse>> {
        val safeLimit = limit.coerceIn(1, 100)
        val articles = if (category != null) {
            newsArticleRepository.findByCategory(category).take(safeLimit)
        } else {
            newsArticleRepository.findRecentArticles(safeLimit)
        }
        return ApiResponse.success(articles.map { NewsArticleResponse.from(it) })
    }

    @Operation(summary = "뉴스 기사 상세 조회", description = "뉴스 기사 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    fun getNewsArticle(@PathVariable id: Long): ApiResponse<NewsArticleResponse> {
        val article = newsArticleRepository.findById(id)
            ?: throw EntityNotFoundException("NewsArticle", id)
        return ApiResponse.success(NewsArticleResponse.from(article))
    }
}
