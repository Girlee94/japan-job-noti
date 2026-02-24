package com.readyjapan.infrastructure.crawler.qiita

import com.readyjapan.core.domain.entity.CrawlHistory
import com.readyjapan.core.domain.entity.CrawlSource
import com.readyjapan.core.domain.entity.enums.CommunityPlatform
import com.readyjapan.core.domain.entity.enums.CrawlStatus
import com.readyjapan.core.domain.entity.enums.SourceType
import com.readyjapan.core.domain.repository.CrawlHistoryRepository
import com.readyjapan.core.domain.repository.CrawlSourceRepository
import com.readyjapan.infrastructure.crawler.config.CrawlerConfig
import com.readyjapan.infrastructure.crawler.qiita.dto.QiitaItemResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.ZoneOffset

class QiitaCrawlerServiceTest : BehaviorSpec({

    val qiitaApiClient = mockk<QiitaApiClient>()
    val qiitaProperties = QiitaProperties(
        enabled = true,
        tags = listOf("日本", "就職"),
        perPage = 20,
        requestDelayMs = 0 // 테스트에서는 딜레이 제거
    )
    val crawlSourceRepository = mockk<CrawlSourceRepository>()
    val crawlHistoryRepository = mockk<CrawlHistoryRepository>()
    val qiitaItemPersistenceService = mockk<QiitaItemPersistenceService>()
    val crawlerConfig = CrawlerConfig()
    val qiitaCrawlerService = QiitaCrawlerService(
        qiitaApiClient, qiitaProperties, crawlSourceRepository,
        crawlHistoryRepository, qiitaItemPersistenceService, crawlerConfig
    )

    beforeEach {
        clearMocks(qiitaApiClient, crawlSourceRepository, crawlHistoryRepository, qiitaItemPersistenceService)
    }

    fun createSource(): CrawlSource = CrawlSource(
        id = 1L,
        name = "Qiita 일본 취업",
        url = "https://qiita.com",
        sourceType = SourceType.COMMUNITY,
        platform = CommunityPlatform.QIITA,
        config = """{"tags": ["日本", "就職"]}"""
    )

    fun createQiitaItem(
        id: String,
        createdAt: String = OffsetDateTime.now(ZoneOffset.ofHours(9)).toString()
    ): QiitaItemResponse = QiitaItemResponse(
        id = id,
        title = "テスト記事 $id",
        body = "テスト本文",
        url = "https://qiita.com/items/$id",
        createdAt = createdAt,
        updatedAt = null,
        tags = emptyList(),
        likesCount = 5,
        commentsCount = 2,
        user = null
    )

    Given("crawlSource - 날짜 필터") {
        When("24시간 이내 기사와 이전 기사가 혼재할 때") {
            Then("client-side 필터가 오래된 기사를 제외한다") {
                val source = createSource()
                val now = OffsetDateTime.now(ZoneOffset.ofHours(9))
                val twoDaysAgo = now.minusDays(2)

                val freshItem = createQiitaItem(id = "fresh1", createdAt = now.toString())
                val staleItem = createQiitaItem(id = "stale1", createdAt = twoDaysAgo.toString())

                val historySlot = slot<CrawlHistory>()
                every { crawlHistoryRepository.save(capture(historySlot)) } answers { historySlot.captured }
                every { qiitaApiClient.isEnabled() } returns true
                every {
                    qiitaApiClient.getItemsByTag(any(), any(), any(), any())
                } returns Mono.just(listOf(freshItem, staleItem))
                every {
                    qiitaItemPersistenceService.saveCrawledItems(any(), any())
                } answers {
                    val items = secondArg<List<QiitaItemResponse>>()
                    Pair(items.size, 0)
                }

                val result = qiitaCrawlerService.crawlSource(source)

                result.status shouldBe CrawlStatus.SUCCESS
                result.itemsFound shouldBe 1
                result.itemsSaved shouldBe 1

                verify {
                    qiitaItemPersistenceService.saveCrawledItems(any(), match { it.size == 1 })
                }
            }
        }

        When("createdAt 파싱 실패 시") {
            Then("데이터 손실 방지를 위해 기본 포함한다") {
                val source = createSource()
                val now = OffsetDateTime.now(ZoneOffset.ofHours(9))

                val normalItem = createQiitaItem(id = "normal1", createdAt = now.toString())
                val badDateItem = createQiitaItem(id = "bad1", createdAt = "invalid-date-format")

                val historySlot = slot<CrawlHistory>()
                every { crawlHistoryRepository.save(capture(historySlot)) } answers { historySlot.captured }
                every { qiitaApiClient.isEnabled() } returns true
                every {
                    qiitaApiClient.getItemsByTag(any(), any(), any(), any())
                } returns Mono.just(listOf(normalItem, badDateItem))
                every {
                    qiitaItemPersistenceService.saveCrawledItems(any(), any())
                } answers {
                    val items = secondArg<List<QiitaItemResponse>>()
                    Pair(items.size, 0)
                }

                val result = qiitaCrawlerService.crawlSource(source)

                result.status shouldBe CrawlStatus.SUCCESS
                result.itemsFound shouldBe 2
                result.itemsSaved shouldBe 2

                verify {
                    qiitaItemPersistenceService.saveCrawledItems(any(), match { it.size == 2 })
                }
            }
        }
    }
})
