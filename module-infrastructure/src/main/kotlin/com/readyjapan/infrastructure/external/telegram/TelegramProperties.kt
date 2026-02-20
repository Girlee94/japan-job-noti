package com.readyjapan.infrastructure.external.telegram

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.telegram")
data class TelegramProperties(
    val botToken: String = "",
    val chatId: String = "",
    val enabled: Boolean = true
)
