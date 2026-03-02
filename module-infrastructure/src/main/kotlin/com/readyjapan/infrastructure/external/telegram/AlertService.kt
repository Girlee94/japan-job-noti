package com.readyjapan.infrastructure.external.telegram

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

@Service
class AlertService(
    private val telegramClient: TelegramClient
) {
    private val lastAlertTimes = ConcurrentHashMap<String, Instant>()

    companion object {
        private const val COOLDOWN_MINUTES = 30L
        private const val MAX_DETAIL_LENGTH = 300
    }

    fun sendAlert(alertKey: String, title: String, detail: String? = null) {
        if (isInCooldown(alertKey)) {
            logger.debug { "알림 쿨다운 중 (key=$alertKey), 전송 생략" }
            return
        }

        val message = buildMessage(title, detail)
        val sent = telegramClient.sendMessageSync(message)

        if (sent) {
            lastAlertTimes[alertKey] = Instant.now()
            logger.info { "장애 알림 전송 완료: $alertKey" }
        } else {
            logger.warn { "장애 알림 전송 실패: $alertKey" }
        }
    }

    private fun isInCooldown(alertKey: String): Boolean {
        val lastTime = lastAlertTimes[alertKey] ?: return false
        return Instant.now().isBefore(lastTime.plusSeconds(COOLDOWN_MINUTES * 60))
    }

    private fun buildMessage(title: String, detail: String?): String {
        val sb = StringBuilder()
        sb.appendLine("*[Alert] ${escapeMarkdown(title)}*")
        if (!detail.isNullOrBlank()) {
            val truncated = if (detail.length > MAX_DETAIL_LENGTH) {
                detail.take(MAX_DETAIL_LENGTH) + "..."
            } else {
                detail
            }
            sb.appendLine()
            sb.appendLine(escapeMarkdown(truncated))
        }
        return sb.toString().trim()
    }

    private fun escapeMarkdown(text: String): String {
        return text.replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("`", "\\`")
    }
}
