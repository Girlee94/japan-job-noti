package com.readyjapan.core.domain.entity.enums

/**
 * 일간 요약 상태
 */
enum class SummaryStatus {
    /** 생성됨 (미발송) */
    DRAFT,

    /** 발송 완료 */
    SENT,

    /** 발송 실패 */
    FAILED
}
