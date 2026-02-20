plugins {
    kotlin("plugin.jpa")
}

dependencies {
    // JPA (인터페이스만, 구현체는 infrastructure에서)
    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa")

    // Validation
    implementation("jakarta.validation:jakarta.validation-api")

    // Jackson (JSON 직렬화)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}

// JPA 엔티티를 위한 allOpen 설정
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
