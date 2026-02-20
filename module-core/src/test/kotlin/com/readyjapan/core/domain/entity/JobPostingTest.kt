package com.readyjapan.core.domain.entity

import com.readyjapan.core.domain.entity.enums.PostingStatus
import com.readyjapan.core.domain.entity.enums.SourceType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class JobPostingTest : BehaviorSpec({

    fun createSource(): CrawlSource = CrawlSource(
        id = 1L,
        name = "test-job-site",
        url = "https://example.com/jobs",
        sourceType = SourceType.JOB_SITE
    )

    fun createJobPosting(
        language: String = "ja",
        titleTranslated: String? = null,
        status: PostingStatus = PostingStatus.ACTIVE
    ): JobPosting = JobPosting(
        source = createSource(),
        externalId = "job123",
        title = "エンジニア募集",
        companyName = "テスト株式会社",
        originalUrl = "https://example.com/jobs/123",
        language = language,
        titleTranslated = titleTranslated,
        status = status
    )

    Given("expire") {
        When("만료 처리 시") {
            Then("EXPIRED 상태가 된다") {
                val job = createJobPosting()
                job.expire()
                job.status shouldBe PostingStatus.EXPIRED
            }
        }
    }

    Given("delete") {
        When("삭제 처리 시") {
            Then("DELETED 상태가 된다") {
                val job = createJobPosting()
                job.delete()
                job.status shouldBe PostingStatus.DELETED
            }
        }
    }

    Given("isActive") {
        When("ACTIVE 상태일 때") {
            Then("true를 반환한다") {
                val job = createJobPosting(status = PostingStatus.ACTIVE)
                job.isActive() shouldBe true
            }
        }
        When("EXPIRED 상태일 때") {
            Then("false를 반환한다") {
                val job = createJobPosting(status = PostingStatus.EXPIRED)
                job.isActive() shouldBe false
            }
        }
        When("DELETED 상태일 때") {
            Then("false를 반환한다") {
                val job = createJobPosting(status = PostingStatus.DELETED)
                job.isActive() shouldBe false
            }
        }
    }

    Given("needsTranslation") {
        When("일본어이고 번역 없을 때") {
            Then("true를 반환한다") {
                val job = createJobPosting(language = "ja", titleTranslated = null)
                job.needsTranslation() shouldBe true
            }
        }
        When("이미 번역 있을 때") {
            Then("false를 반환한다") {
                val job = createJobPosting(language = "ja", titleTranslated = "엔지니어 모집")
                job.needsTranslation() shouldBe false
            }
        }
        When("영어일 때") {
            Then("false를 반환한다") {
                val job = createJobPosting(language = "en")
                job.needsTranslation() shouldBe false
            }
        }
    }

    Given("applyTranslation") {
        When("번역 적용 시") {
            Then("정상 반영된다") {
                val job = createJobPosting()
                job.applyTranslation(
                    titleTranslated = "엔지니어 모집",
                    descriptionTranslated = "상세 설명 번역",
                    requirementsTranslated = "요구사항 번역"
                )

                job.titleTranslated shouldBe "엔지니어 모집"
                job.descriptionTranslated shouldBe "상세 설명 번역"
                job.requirementsTranslated shouldBe "요구사항 번역"
            }
        }
    }
})
