plugins {
    id("bbs.spring-boot-conventions")
}

group = "com.board"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)

    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.session.data.redis)

    implementation(libs.spring.boot.flyway)
    runtimeOnly(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)

    implementation(libs.querydsl.jpa)
    annotationProcessor(variantOf(libs.querydsl.apt) { classifier("jakarta") })
    annotationProcessor(libs.jakarta.annotation.api)
    annotationProcessor(libs.jakarta.persistence.api)

    implementation(libs.jspecify)

    implementation(libs.springdoc.openapi.webmvc.ui)

    developmentOnly(libs.spring.boot.devtools)
    developmentOnly(libs.spring.boot.docker.compose)

    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.redis)
    testImplementation(libs.archunit.junit5)
}

// Gradle 스크립트의 포맷. Java 소스의 규칙은 bbs.java-conventions에 있다.
spotless {
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint()
    }
}

// 훅을 복사하지 않고 저장소의 hooks/를 그대로 쓰게 한다. 훅을 고치면 바로 반영되고 워크트리에서도 동작한다.
val gitMetadata: File = layout.projectDirectory.file(".git").asFile

tasks.register<Exec>("installGitHooks") {
    description = "Git이 저장소의 hooks/ 디렉터리를 훅 경로로 쓰게 한다"

    onlyIf { gitMetadata.exists() }

    commandLine("git", "config", "core.hooksPath", "hooks")
}

tasks.build {
    dependsOn("installGitHooks")
}
