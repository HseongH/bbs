pluginManagement {
    // 서비스 공통 빌드 규칙(컨벤션 플러그인)
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "bbs"
