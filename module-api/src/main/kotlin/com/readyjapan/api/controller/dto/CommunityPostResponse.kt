package com.readyjapan.api.controller.dto

import com.readyjapan.core.domain.entity.CommunityPost

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

        fun detail(post: CommunityPost): CommunityPostResponse {
            return CommunityPostResponse(
                id = post.id,
                platform = post.platform.name,
                title = post.getDisplayTitle(),
                content = post.getDisplayContent(),
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
