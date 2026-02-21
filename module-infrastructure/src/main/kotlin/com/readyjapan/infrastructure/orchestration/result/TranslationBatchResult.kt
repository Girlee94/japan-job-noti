package com.readyjapan.infrastructure.orchestration.result

data class TranslationBatchResult(
    val jobPostingsTranslated: Int,
    val newsArticlesTranslated: Int,
    val communityPostsTranslated: Int
) {
    val totalTranslated: Int
        get() = jobPostingsTranslated + newsArticlesTranslated + communityPostsTranslated
}
