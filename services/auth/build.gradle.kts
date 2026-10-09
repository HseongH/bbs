plugins {
    id("bbs.spring-boot-conventions")
}

group = "com.board"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":libs:internal-token"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.session.data.redis)

    implementation(libs.jspecify)

    developmentOnly(libs.spring.boot.devtools)
    developmentOnly(libs.spring.boot.docker.compose)

    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.redis)
}

tasks.test {
    // 통합 테스트가 개발 환경과 같은 컨테이너 이미지를 쓰도록 compose 파일을 알려 준다. 작업 디렉터리에 기대지 않는다.
    systemProperty(
        "bbs.compose-file",
        rootProject.layout.projectDirectory
            .file("deploy/compose.yaml")
            .asFile.absolutePath,
    )
}
