// auth와 board가 함께 쓰는 내부 토큰의 계약. 발급과 검증 규칙을 한 벌만 둔다 (ADR-0016).
plugins {
    id("bbs.java-conventions")
    `java-library`
}

dependencies {
    api(platform(libs.spring.boot.dependencies))
    api(libs.spring.security.oauth2.jose)
    implementation(libs.jspecify)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
