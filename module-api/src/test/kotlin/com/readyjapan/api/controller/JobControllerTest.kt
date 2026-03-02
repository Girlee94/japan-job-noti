package com.readyjapan.api.controller

import com.readyjapan.core.common.exception.EntityNotFoundException
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.JobPosting
import com.readyjapan.core.domain.entity.enums.PostingStatus
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.JobPostingRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class JobControllerTest : BehaviorSpec({

    val jobPostingRepository = mockk<JobPostingRepository>()
    val jobController = JobController(jobPostingRepository)

    beforeEach {
        clearMocks(jobPostingRepository)
    }

    fun createSource(): CrawlSource = CrawlSource(
        id = 1L,
        name = "Indeed Japan",
        url = "https://jp.indeed.com",
        sourceType = SourceType.JOB_SITE
    )

    fun createJob(id: Long = 1L): JobPosting = JobPosting(
        id = id,
        source = createSource(),
        externalId = "job$id",
        title = "Software Engineer $id",
        companyName = "Tech Corp",
        location = "Tokyo",
        description = "Job description for position $id",
        originalUrl = "https://jp.indeed.com/job$id"
    )

    Given("getJobs") {
        When("기본 상태(ACTIVE)로 조회 시") {
            Then("활성 채용공고 목록을 반환한다") {
                val jobs = listOf(createJob(1L), createJob(2L))
                every { jobPostingRepository.findRecentByStatus(PostingStatus.ACTIVE, 20) } returns jobs

                val response = jobController.getJobs(20, PostingStatus.ACTIVE)

                response.success shouldBe true
                response.data!! shouldHaveSize 2
                response.data!![0].title shouldBe "Software Engineer 1"
            }
        }
        When("EXPIRED 상태로 필터링 시") {
            Then("만료된 채용공고를 반환한다") {
                every { jobPostingRepository.findRecentByStatus(PostingStatus.EXPIRED, 20) } returns emptyList()

                val response = jobController.getJobs(20, PostingStatus.EXPIRED)

                response.success shouldBe true
                response.data!! shouldHaveSize 0
            }
        }
    }

    Given("getJob") {
        When("존재하는 ID로 조회 시") {
            Then("채용공고 상세 정보를 반환한다") {
                val job = createJob(1L)
                every { jobPostingRepository.findById(1L) } returns job

                val response = jobController.getJob(1L)

                response.success shouldBe true
                response.data!!.id shouldBe 1L
                response.data!!.companyName shouldBe "Tech Corp"
            }
        }
        When("존재하지 않는 ID로 조회 시") {
            Then("EntityNotFoundException이 발생한다") {
                every { jobPostingRepository.findById(999L) } returns null

                shouldThrow<EntityNotFoundException> {
                    jobController.getJob(999L)
                }
            }
        }
    }
})
