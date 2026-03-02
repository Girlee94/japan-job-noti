package com.readyjapan.api.controller

import com.readyjapan.api.controller.dto.CommunityPostResponse
import com.readyjapan.core.common.exception.EntityNotFoundException
import com.readyjapan.core.common.response.ApiResponse
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.repository.CommunityPostRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Community Posts", description = "커뮤니티 게시글 조회 API")
@RestController
@RequestMapping("/api/v1/posts")
class PostController(
    private val communityPostRepository: CommunityPostRepository
) {

    @Operation(summary = "게시글 목록 조회", description = "최근 커뮤니티 게시글 목록을 조회합니다.")
    @GetMapping
    fun getPosts(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) platform: CommunityPlatform?
    ): ApiResponse<List<CommunityPostResponse>> {
        val safeLimit = limit.coerceIn(1, 100)
        val posts = if (platform != null) {
            communityPostRepository.findAllByPlatform(platform).take(safeLimit)
        } else {
            communityPostRepository.findRecentPosts(safeLimit)
        }
        return ApiResponse.success(posts.map { CommunityPostResponse.from(it) })
    }

    @Operation(summary = "게시글 상세 조회", description = "커뮤니티 게시글 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    fun getPost(@PathVariable id: Long): ApiResponse<CommunityPostResponse> {
        val post = communityPostRepository.findById(id)
            ?: throw EntityNotFoundException("CommunityPost", id)
        return ApiResponse.success(CommunityPostResponse.detail(post))
    }

    @Operation(summary = "인기 게시글 조회", description = "좋아요가 많은 인기 게시글을 조회합니다.")
    @GetMapping("/popular")
    fun getPopularPosts(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "10") minLikes: Int
    ): ApiResponse<List<CommunityPostResponse>> {
        val posts = communityPostRepository.findPopularPosts(
            minLikes = minLikes.coerceAtLeast(0),
            limit = limit.coerceIn(1, 100)
        )
        return ApiResponse.success(posts.map { CommunityPostResponse.from(it) })
    }
}
