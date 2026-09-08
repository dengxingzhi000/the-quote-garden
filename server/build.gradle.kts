plugins {
    id("java")
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.6"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core:11.20.3")
    implementation("org.flywaydb:flyway-database-postgresql:11.20.3")
    implementation("org.postgresql:postgresql")
    implementation("org.jsoup:jsoup:1.18.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.test {
    useJUnitPlatform()
}
