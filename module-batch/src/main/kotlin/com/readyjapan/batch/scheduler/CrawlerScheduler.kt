package com.readyjapan.batch.scheduler

import com.readyjapan.infrastructure.crawler.reddit.RedditCrawlerService
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
    private val redditCrawlerService: RedditCrawlerService
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
        } catch (e: Exception) {
            logger.error(e) { "Scheduled Reddit crawl failed" }
        }
    }

    /**
     * 수동 크롤링 실행 (테스트용)
     */
    fun manualCrawl(): CrawlResult {
        logger.info { "Starting manual Reddit crawl" }

        val histories = redditCrawlerService.crawlAllSources()

        return CrawlResult(
            sourcesProcessed = histories.size,
            totalFound = histories.sumOf { it.itemsFound },
            totalSaved = histories.sumOf { it.itemsSaved },
            totalUpdated = histories.sumOf { it.itemsUpdated },
            failedSources = histories.count { !it.isSuccessful() }
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
