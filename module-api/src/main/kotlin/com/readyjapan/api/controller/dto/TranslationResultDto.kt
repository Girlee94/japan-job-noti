package com.readyjapan.api.controller.dto

import com.readyjapan.infrastructure.orchestration.result.TranslationBatchResult

data class TranslationResultDto(
    val jobPostingsTranslated: Int,
    val newsArticlesTranslated: Int,
    val communityPostsTranslated: Int
) {
    val totalTranslated: Int get() = jobPostingsTranslated + newsArticlesTranslated + communityPostsTranslated

    companion object {
        fun from(result: TranslationBatchResult): TranslationResultDto =
            TranslationResultDto(
                jobPostingsTranslated = result.jobPostingsTranslated,
                newsArticlesTranslated = result.newsArticlesTranslated,
                communityPostsTranslated = result.communityPostsTranslated
            )
    }
}
