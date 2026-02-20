package com.readyjapan.core.domain.entity

import com.readyjapan.core.domain.entity.enums.EmploymentType
import com.readyjapan.core.domain.entity.enums.PostingStatus
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 채용 공고 엔티티
 * 크롤링으로 수집된 채용 공고 정보를 저장합니다.
 */
@Entity
@Table(
    name = "job_postings",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_job_postings_source_external",
            columnNames = ["source_id", "external_id"]
        )
    ],
    indexes = [
        Index(name = "idx_job_postings_status", columnList = "status"),
        Index(name = "idx_job_postings_created", columnList = "created_at DESC"),
        Index(name = "idx_job_postings_posted", columnList = "posted_at DESC"),
        Index(name = "idx_job_postings_company", columnList = "company_name")
    ]
)
class JobPosting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    val source: CrawlSource,

    @Column(name = "external_id", nullable = false, length = 200)
    val externalId: String,

    @Column(nullable = false, length = 500)
    var title: String,

    @Column(name = "title_translated", length = 500)
    var titleTranslated: String? = null,

    @Column(name = "company_name", nullable = false, length = 200)
    var companyName: String,

    @Column(length = 100)
    var location: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 20)
    var employmentType: EmploymentType? = null,

    @Column(length = 200)
    var salary: String? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "description_translated", columnDefinition = "TEXT")
    var descriptionTranslated: String? = null,

    @Column(columnDefinition = "TEXT")
    var requirements: String? = null,

    @Column(name = "requirements_translated", columnDefinition = "TEXT")
    var requirementsTranslated: String? = null,

    @Column(name = "original_url", nullable = false, length = 1000)
    val originalUrl: String,

    @Column(nullable = false, length = 10)
    var language: String = "ja",

    @Column(name = "posted_at")
    var postedAt: LocalDate? = null,

    @Column(name = "expires_at")
    var expiresAt: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PostingStatus = PostingStatus.ACTIVE
) : BaseEntity() {

    /**
     * 공고 만료 처리
     */
    fun expire() {
        this.status = PostingStatus.EXPIRED
    }

    /**
     * 공고 삭제 처리
     */
    fun delete() {
        this.status = PostingStatus.DELETED
    }

    /**
     * 번역 적용
     */
    fun applyTranslation(
        titleTranslated: String?,
        descriptionTranslated: String?,
        requirementsTranslated: String?
    ) {
        this.titleTranslated = titleTranslated
        this.descriptionTranslated = descriptionTranslated
        this.requirementsTranslated = requirementsTranslated
    }

    /**
     * 번역 필요 여부 확인
     */
    fun needsTranslation(): Boolean {
        return language == "ja" && titleTranslated == null
    }

    /**
     * 활성 상태 여부 확인
     */
    fun isActive(): Boolean {
        return status == PostingStatus.ACTIVE
    }
}
