package com.readyjapan.infrastructure.crawler.reddit.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Reddit OAuth2 토큰 응답
 */
data class RedditAuthResponse(
    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("token_type")
    val tokenType: String,

    @JsonProperty("expires_in")
    val expiresIn: Long,

    val scope: String
)
