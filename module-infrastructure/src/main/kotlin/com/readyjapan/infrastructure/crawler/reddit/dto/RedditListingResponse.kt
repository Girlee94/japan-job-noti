package com.readyjapan.infrastructure.crawler.reddit.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Reddit API Listing 응답 (게시물 목록)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditListingResponse(
    val kind: String,
    val data: RedditListingData
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditListingData(
    val after: String?,
    val before: String?,
    val children: List<RedditPostWrapper>,
    val dist: Int?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditPostWrapper(
    val kind: String,
    val data: RedditPostData
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RedditPostData(
    /** 게시물 고유 ID */
    val id: String,

    /** 게시물 이름 (t3_xxx 형식) */
    val name: String,

    /** 제목 */
    val title: String,

    /** 본문 (self post인 경우) */
    val selftext: String?,

    /** 작성자 */
    val author: String,

    /** 서브레딧 이름 */
    val subreddit: String,

    /** 서브레딧 표시명 (r/xxx 형식) */
    @JsonProperty("subreddit_name_prefixed")
    val subredditNamePrefixed: String,

    /** 게시물 URL */
    val url: String,

    /** 퍼마링크 */
    val permalink: String,

    /** 업보트 수 */
    val ups: Int,

    /** 다운보트 수 */
    val downs: Int,

    /** 점수 (ups - downs) */
    val score: Int,

    /** 업보트 비율 */
    @JsonProperty("upvote_ratio")
    val upvoteRatio: Double,

    /** 댓글 수 */
    @JsonProperty("num_comments")
    val numComments: Int,

    /** 생성 시각 (Unix timestamp) */
    val created: Double,

    /** 생성 시각 UTC (Unix timestamp) */
    @JsonProperty("created_utc")
    val createdUtc: Double,

    /** 플레어 텍스트 */
    @JsonProperty("link_flair_text")
    val linkFlairText: String?,

    /** NSFW 여부 */
    @JsonProperty("over_18")
    val over18: Boolean,

    /** 스포일러 여부 */
    val spoiler: Boolean,

    /** 고정 게시물 여부 */
    val stickied: Boolean,

    /** 잠금 여부 */
    val locked: Boolean,

    /** 삭제 여부 */
    val removed: Boolean? = false,

    /** 외부 링크 여부 */
    @JsonProperty("is_self")
    val isSelf: Boolean
) {
    /**
     * 전체 Reddit URL 반환
     */
    fun getFullUrl(): String = "https://www.reddit.com$permalink"

    /**
     * 작성자 프로필 URL 반환
     */
    fun getAuthorProfileUrl(): String = "https://www.reddit.com/user/$author"

    /**
     * 태그 목록 반환 (JSON 배열 문자열)
     */
    fun getTagsJson(): String? {
        return linkFlairText?.let { "[\"$it\"]" }
    }
}
