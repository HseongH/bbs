// 저장소 전체의 작업만 둔다. 서비스의 빌드는 services/ 아래 각 빌드 스크립트와 build-logic의 컨벤션 플러그인이 맡는다.
plugins {
    base
    alias(libs.plugins.spotless)
}

// Gradle 스크립트의 포맷. Java 소스의 규칙은 build-logic의 bbs.java-conventions에 있다.
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
