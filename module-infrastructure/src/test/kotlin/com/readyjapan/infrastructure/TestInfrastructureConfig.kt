package com.readyjapan.infrastructure

import com.readyjapan.infrastructure.persistence.config.QueryDslConfig
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["com.readyjapan.core.domain.entity"])
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = ["com.readyjapan.infrastructure.persistence.repository"])
@Import(QueryDslConfig::class)
class TestInfrastructureConfig
