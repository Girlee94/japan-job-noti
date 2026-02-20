package com.readyjapan.infrastructure.persistence.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = ["com.readyjapan.core.domain.entity"])
@EnableJpaRepositories(basePackages = ["com.readyjapan.infrastructure.persistence.repository"])
class JpaConfig
