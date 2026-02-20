package com.readyjapan.core.domain.entity.enums

/**
 * 크롤링 상태
 */
enum class CrawlStatus {
    /** 실행 중 */
    RUNNING,

    /** 성공 */
    SUCCESS,

    /** 실패 */
    FAILED,

    /** 부분 성공 */
    PARTIAL
}
