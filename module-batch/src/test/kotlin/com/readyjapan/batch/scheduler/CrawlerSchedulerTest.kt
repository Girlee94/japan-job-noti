package com.readyjapan.batch.scheduler

import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.CrawlStatus
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.infrastructure.crawler.qiita.QiitaCrawlerService
import com.readyjapan.infrastructure.crawler.reddit.RedditCrawlerService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CrawlerSchedulerTest : BehaviorSpec({

    val redditCrawlerService = mockk<RedditCrawlerService>()
    val qiitaCrawlerService = mockk<QiitaCrawlerService>()
    val crawlerScheduler = CrawlerScheduler(redditCrawlerService, qiitaCrawlerService)

    beforeEach { clearMocks(redditCrawlerService, qiitaCrawlerService) }

    fun createSource(): CrawlSource = CrawlSource(
        id = 1L,
        name = "r/japanlife",
        url = "https://reddit.com/r/japanlife",
        sourceType = SourceType.COMMUNITY,
        platform = CommunityPlatform.REDDIT
    )

    fun createHistory(
        status: CrawlStatus = CrawlStatus.SUCCESS,
        itemsFound: Int = 10,
        itemsSaved: Int = 8,
        itemsUpdated: Int = 2
    ): CrawlHistory {
        val history = CrawlHistory.start(createSource())
        when (status) {
            CrawlStatus.SUCCESS -> history.complete(itemsFound, itemsSaved, itemsUpdated)
            CrawlStatus.FAILED -> history.fail("Test error")
            else -> {}
        }
        return history
    }

    Given("manualCrawl") {
        When("정상 집계 시") {
            Then("집계 결과를 반환한다") {
                val redditHistories = listOf(
                    createHistory(CrawlStatus.SUCCESS, itemsFound = 10, itemsSaved = 8, itemsUpdated = 2),
                    createHistory(CrawlStatus.SUCCESS, itemsFound = 5, itemsSaved = 3, itemsUpdated = 1)
                )
                every { redditCrawlerService.crawlAllSources() } returns redditHistories
                every { qiitaCrawlerService.crawlAllSources() } returns emptyList()

                val result = crawlerScheduler.manualCrawl()

                result.sourcesProcessed shouldBe 2
                result.totalFound shouldBe 15
                result.totalSaved shouldBe 11
                result.totalUpdated shouldBe 3
                result.failedSources shouldBe 0
            }
        }
        When("실패 소스 포함 시") {
            Then("failedSources를 카운트한다") {
                val histories = listOf(
                    createHistory(CrawlStatus.SUCCESS, itemsFound = 10, itemsSaved = 8, itemsUpdated = 2),
                    createHistory(CrawlStatus.FAILED)
                )
                every { redditCrawlerService.crawlAllSources() } returns histories
                every { qiitaCrawlerService.crawlAllSources() } returns emptyList()

                val result = crawlerScheduler.manualCrawl()

                result.sourcesProcessed shouldBe 2
                result.failedSources shouldBe 1
            }
        }
    }

    Given("scheduledRedditCrawl") {
        When("예외 발생 시") {
            Then("예외가 전파되지 않는다") {
                every { redditCrawlerService.crawlAllSources() } throws RuntimeException("Unexpected error")

                crawlerScheduler.scheduledRedditCrawl()

                verify(exactly = 1) { redditCrawlerService.crawlAllSources() }
            }
        }
    }
})
