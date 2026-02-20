plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":module-core"))
    implementation(project(":module-infrastructure"))

    // Spring Boot Starter
    implementation("org.springframework.boot:spring-boot-starter")

    // Spring Data JPA (트랜잭션 관리)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // PostgreSQL Driver
    runtimeOnly("org.postgresql:postgresql")

    // Scheduling (Spring Boot 기본 제공)
    // 복잡한 스케줄링이 필요하면 Quartz 추가 가능
    // implementation("org.springframework.boot:spring-boot-starter-quartz")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // Configuration Processor
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
