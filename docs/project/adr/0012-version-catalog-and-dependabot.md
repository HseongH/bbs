# ADR-0012. 의존성 버전은 version catalog 한 곳에서 관리하고 Dependabot으로 갱신한다

- 상태: Accepted
- 일자: 2026-10-08
- 관련: PR #8, COM-NFR-031

## 맥락

- 의존성과 도구(Checkstyle, google-java-format)의 버전이 빌드 스크립트 곳곳에 흩어져 있었다.
- 통합 테스트의 Testcontainers 이미지 태그가 `compose.yaml`과 따로 적혀 있어, 개발 환경과 테스트 환경의 버전이 어긋날 수 있었다.
- 1인 개발이라 의존성 업데이트를 사람이 챙기기 어렵다.

## 결정

- 버전은 `gradle/libs.versions.toml` 한 곳에서 관리한다. 스프링 부트 BOM이 관리하는 의존성은 버전 없이 등록하고, 직접 정하는 버전만 `[versions]`에 둔다. 도구도 라이브러리로 등록해 업데이트 대상에 포함한다.
- Dependabot(`.github/dependabot.yml`)이 매주 Gradle 의존성(minor·patch 묶음), GitHub Actions(묶음), compose 이미지의 업데이트 PR을 올린다. CI의 `./gradlew check`가 그 PR을 검증한다.
- 통합 테스트는 컨테이너 이미지를 `compose.yaml`에서 읽는다.
- Git 훅은 `.git/hooks`로 복사하지 않고 `core.hooksPath`로 저장소의 `hooks/`를 가리킨다. 고친 훅이 바로 반영되고 worktree에서도 동작한다.

## 검토한 대안

| 대안 | 기각 이유 |
|---|---|
| `build.gradle.kts`의 `extra` 속성으로 버전 관리 | Dependabot이 인식하지 못한다 |
| Renovate | 기능은 더 많지만 GitHub 기본 기능인 Dependabot으로 충분하다 |

## 결과

- 좋은 점: 버전 변경 지점이 하나다. 업데이트 PR이 자동으로 올라오고 CI가 검증한다. 개발 환경과 테스트 환경의 이미지 버전이 일치한다.
- 나쁜 점: 매주 업데이트 PR을 검토하고 merge하는 일이 생긴다.
