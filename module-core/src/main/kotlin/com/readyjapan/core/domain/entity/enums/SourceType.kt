package com.readyjapan.core.domain.entity.enums

/**
 * 크롤링 소스 유형
 */
enum class SourceType {
    /** 채용 사이트 (Indeed, Wantedly 등) */
    JOB_SITE,

    /** 뉴스 사이트 (IT Media, TechCrunch Japan 등) */
    NEWS_SITE,

    /** 커뮤니티 (Reddit, Qiita 등) */
    COMMUNITY,

    /** 외부 API */
    API
}
