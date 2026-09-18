plugins {
    java
    checkstyle
    jacoco
    id("com.diffplug.spotless") version "8.10.2"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.board"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

// com.querydsl:querydsl-jpa 5.1.0은 Hibernate 6 기준이라 Spring Boot 4.1에서 동작하지 않는다.
// Hibernate 7을 대상으로 빌드된 openfeign 포크를 사용한다.
val querydslVersion = "7.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.session:spring-session-data-redis")

    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    implementation("io.github.openfeign.querydsl:querydsl-jpa:$querydslVersion")
    annotationProcessor("io.github.openfeign.querydsl:querydsl-apt:$querydslVersion:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    // Testcontainers 2.x부터 모든 모듈 아티팩트에 testcontainers- 접두사가 붙는다.
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("com.redis:testcontainers-redis")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

checkstyle {
    toolVersion = "14.1.0"
}

spotless {
    java {
        target("src/**/*.java")
        targetExclude("**/generated/**")

        googleJavaFormat("1.30.0")

        formatAnnotations()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint()
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Checkstyle>().configureEach {
    javaLauncher =
        javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(25)
        }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// 커버리지 집계에서 제외한다: QueryDSL 생성 클래스, 설정 클래스, 부트 진입점.
val coverageExclusions =
    listOf(
        "**/Q*.class",
        "**/config/**",
        "**/BbsApplication.class",
    )

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) { exclude(coverageExclusions) }
            },
        ),
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) { exclude(coverageExclusions) }
            },
        ),
    )
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
        // 도메인과 애플리케이션 계층은 이 프로젝트의 핵심이므로 더 높은 기준을 적용한다.
        rule {
            element = "PACKAGE"
            includes = listOf("com.board.bbs.*.domain", "com.board.bbs.*.application.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.register<Copy>("installGitHooks") {
    description = "Git 훅을 .git/hooks에 설치한다"

    onlyIf { file("${rootProject.projectDir}/.git/hooks").isDirectory }

    from(layout.projectDirectory.dir("hooks"))
    into(layout.projectDirectory.dir(".git/hooks"))

    filePermissions { unix("0755") }
}

// 클론 직후 빌드만 해도 훅이 설치되도록 한다.
tasks.build {
    dependsOn("installGitHooks")
}
