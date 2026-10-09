// 서비스들이 공유하는 빌드 규칙. 루트 빌드가 included build로 포함한다.
rootProject.name = "build-logic"

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    // 버전은 저장소 루트의 카탈로그 한 곳에서만 관리한다.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
