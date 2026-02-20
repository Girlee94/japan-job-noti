package com.readyjapan.infrastructure.crawler.qiita.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Qiita API v2 아이템 응답
 * GET /api/v2/items
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class QiitaItemResponse(
    /** 기사 고유 ID */
    val id: String,

    /** 기사 제목 */
    val title: String,

    /** 기사 본문 (Markdown) */
    val body: String?,

    /** 기사 URL */
    val url: String,

    /** 작성일시 (ISO 8601) */
    @JsonProperty("created_at")
    val createdAt: String,

    /** 수정일시 (ISO 8601) */
    @JsonProperty("updated_at")
    val updatedAt: String?,

    /** 태그 목록 */
    val tags: List<QiitaTag> = emptyList(),

    /** LGTM 수 */
    @JsonProperty("likes_count")
    val likesCount: Int = 0,

    /** 댓글 수 */
    @JsonProperty("comments_count")
    val commentsCount: Int = 0,

    /** 작성자 정보 */
    val user: QiitaUser?
) {
    /**
     * 태그를 JSON 배열 문자열로 변환
     */
    fun getTagsJson(): String? {
        if (tags.isEmpty()) return null
        return tags.joinToString(prefix = "[", postfix = "]") { "\"${it.name}\"" }
    }

    /**
     * 작성자 프로필 URL 반환
     */
    fun getAuthorProfileUrl(): String? {
        return user?.id?.let { "https://qiita.com/$it" }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class QiitaTag(
    val name: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class QiitaUser(
    val id: String,

    val name: String?,

    @JsonProperty("profile_image_url")
    val profileImageUrl: String?
)
