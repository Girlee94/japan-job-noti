package com.readyjapan.batch.scheduler

import com.readyjapan.infrastructure.crawler.qiita.QiitaCrawlerService
import com.readyjapan.infrastructure.crawler.reddit.RedditCrawlerService
import com.readyjapan.infrastructure.external.telegram.AlertService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * 크롤링 스케줄러
 * 정해진 시간에 자동으로 크롤링을 실행합니다.
 */
@Component
class CrawlerScheduler(
    private val redditCrawlerService: RedditCrawlerService,
    private val qiitaCrawlerService: QiitaCrawlerService,
    private val alertService: AlertService
) {
    /**
     * Reddit 크롤링 (매일 오전 8시, 오후 6시)
     * JST 기준
     */
    @Scheduled(cron = "0 0 8,18 * * *", zone = "Asia/Tokyo")
    fun scheduledRedditCrawl() {
        logger.info { "Starting scheduled Reddit crawl" }

        try {
            val histories = redditCrawlerService.crawlAllSources()

            val totalFound = histories.sumOf { it.itemsFound }
            val totalSaved = histories.sumOf { it.itemsSaved }
            val totalUpdated = histories.sumOf { it.itemsUpdated }

            logger.info {
                "Scheduled Reddit crawl completed: " +
                        "sources=${histories.size}, " +
                        "found=$totalFound, " +
                        "saved=$totalSaved, " +
                        "updated=$totalUpdated"
            }
            val failedSources = histories.filter { !it.isSuccessful() }
            if (failedSources.isNotEmpty()) {
                alertService.sendAlert(
                    "reddit-crawl-partial",
                    "Reddit 크롤링 부분 실패",
                    "${failedSources.size}개 소스 실패 (전체 ${histories.size}개)"
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Scheduled Reddit crawl failed" }
            alertService.sendAlert("reddit-crawl", "Reddit 크롤링 전체 실패", e.message ?: e::class.simpleName)
        }
    }

    /**
     * Qiita 크롤링 (매일 오전 9시, 오후 7시)
     * JST 기준
     */
    @Scheduled(cron = "0 0 9,19 * * *", zone = "Asia/Tokyo")
    fun scheduledQiitaCrawl() {
        logger.info { "Starting scheduled Qiita crawl" }

        try {
            val histories = qiitaCrawlerService.crawlAllSources()

            val totalFound = histories.sumOf { it.itemsFound }
            val totalSaved = histories.sumOf { it.itemsSaved }
            val totalUpdated = histories.sumOf { it.itemsUpdated }

            logger.info {
                "Scheduled Qiita crawl completed: " +
                        "sources=${histories.size}, " +
                        "found=$totalFound, " +
                        "saved=$totalSaved, " +
                        "updated=$totalUpdated"
            }
            val failedSources = histories.filter { !it.isSuccessful() }
            if (failedSources.isNotEmpty()) {
                alertService.sendAlert(
                    "qiita-crawl-partial",
                    "Qiita 크롤링 부분 실패",
                    "${failedSources.size}개 소스 실패 (전체 ${histories.size}개)"
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Scheduled Qiita crawl failed" }
            alertService.sendAlert("qiita-crawl", "Qiita 크롤링 전체 실패", e.message ?: e::class.simpleName)
        }
    }

    /**
     * 수동 크롤링 실행 (테스트용)
     */
    fun manualCrawl(): CrawlResult {
        logger.info { "Starting manual crawl (Reddit + Qiita)" }

        val redditHistories = try {
            redditCrawlerService.crawlAllSources()
        } catch (e: Exception) {
            logger.error(e) { "Reddit crawl failed during manual crawl" }
            emptyList()
        }
        val qiitaHistories = try {
            qiitaCrawlerService.crawlAllSources()
        } catch (e: Exception) {
            logger.error(e) { "Qiita crawl failed during manual crawl" }
            emptyList()
        }
        val allHistories = redditHistories + qiitaHistories

        return CrawlResult(
            sourcesProcessed = allHistories.size,
            totalFound = allHistories.sumOf { it.itemsFound },
            totalSaved = allHistories.sumOf { it.itemsSaved },
            totalUpdated = allHistories.sumOf { it.itemsUpdated },
            failedSources = allHistories.count { !it.isSuccessful() }
        )
    }

    data class CrawlResult(
        val sourcesProcessed: Int,
        val totalFound: Int,
        val totalSaved: Int,
        val totalUpdated: Int,
        val failedSources: Int
    )
}
