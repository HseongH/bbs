# ADR-0014. 저장소를 배포 단위별 모노레포로 나누고 Java 빌드 규칙은 컨벤션 플러그인으로 공유한다

- 상태: Accepted
- 일자: 2026-10-09 (PR #28)
- 관련: COM-NFR-006, COM-NFR-031~035, COM-CON-004, COM-CON-006 (PRJ-SRS 1.5.0), PRJ-SDS 1.6.0 §9·§10, [ADR-0012](0012-version-catalog-and-dependabot.md)

## 맥락

- 저장소 루트가 곧 백엔드 Gradle 프로젝트이고, 화면(Angular)은 `frontend/`에, 개발용 컨테이너 구성은 루트의 `compose.yaml`과 `docker/`에 흩어져 있다.
- 앞으로 인증·인가를 맡는 `auth` 서비스와 진입점 프록시(Traefik)를 추가해 서비스를 나눌 계획이다. 지금 구조에서는 두 번째 Java 서비스를 둘 자리가 없고, 빌드 규칙(툴체인, Error Prone·NullAway, Checkstyle, 포맷, 커버리지 기준)이 백엔드 빌드 스크립트 하나에 묶여 있어 다른 서비스가 같은 기준을 얻으려면 복사해야 한다.
- CI는 백엔드만 검증하고(`paths-ignore: frontend/**`), 화면의 `pnpm verify`는 PR에서 실행되지 않는다. 화면 의존성은 Dependabot 대상도 아니다.
- 개발용 compose 파일에 접속 정보가 그대로 적혀 있고 포트가 모든 네트워크 인터페이스에 열린다. Keycloak realm 파일에 realm 구조와 시험 사용자가 섞여 있다.

## 결정

### 구조

배포 단위(서비스)를 기준으로 나눈다. 기술(프론트엔드·백엔드)로 나누지 않는다.

```
services/board/   게시판 서비스 (현재 백엔드)
services/web/     화면 (현재 frontend/)
build-logic/      Java 서비스 공통 빌드 규칙 (Gradle included build)
deploy/           실행 환경 구성 (compose.yaml, .env.example, keycloak/)
config/checkstyle 모든 Java 서비스가 공유하는 Checkstyle 규칙
gradle/           래퍼, 버전 카탈로그
```

- **내용이 없는 모듈이나 디렉터리는 만들지 않는다.** `services/auth`, `libs/`, `deploy/traefik/`은 첫 코드와 함께 만든다. 빈 골격은 "있지만 아무것도 하지 않는 구조"가 되어 처음 읽는 사람을 오해하게 하고, 아직 하지 않은 설계를 미리 정하게 만든다.
- 파일은 `git mv`로 옮겨 이력을 보존한다. Java 패키지, 애플리케이션 이름, API는 바꾸지 않는다.

### Gradle

- 루트에 Gradle 빌드 하나를 두고 Java 서비스를 서브프로젝트(`:services:board`)로 등록한다. 래퍼, 버전 카탈로그, 저장소 선언은 루트에 하나씩만 둔다.
- 공통 빌드 규칙은 `build-logic`의 사전 컴파일된 컨벤션 플러그인으로 둔다.
  - `bbs.java-conventions`: 툴체인, 컴파일 옵션, Error Prone·NullAway, Checkstyle, Spotless, JaCoCo와 커버리지 기준
  - `bbs.spring-boot-conventions`: 위 플러그인, Spring Boot와 의존성 관리 플러그인, 공통 테스트 의존성
- 서비스의 빌드 스크립트에는 그 서비스만의 의존성과 설정만 남는다. 새 서비스는 플러그인 하나를 적용해 같은 품질 기준을 얻는다.
- 커버리지 기준의 패키지 패턴은 서비스 이름에 묶이지 않는 형태(`*.domain`, `*.application.*`)로 둔다.
- 루트 빌드 스크립트는 저장소 전체의 작업(Git 훅 설치, Gradle 스크립트 포맷 검사)만 맡는다.

### 실행 환경 (`deploy/`)

- compose의 접속 정보는 `${변수:-개발 기본값}` 형태로 두고 변수 목록은 `.env.example`에 적는다. `.env` 없이도 바로 실행된다.
- 포트는 기본적으로 `127.0.0.1`에만 연다. 다른 기기에서 접속해야 할 때를 위해 바인딩 주소를 변수로 바꿀 수 있게 한다.
- Keycloak 가져오기 파일을 realm 구조(`bbs-realm.json`)와 개발용 시험 사용자(`dev/bbs-users-0.json`)로 나눈다. 운영 환경을 만들 때 사용자 파일만 빼면 된다.
- 서비스는 compose 파일을 상대 경로나 빌드가 넘겨주는 절대 경로로 찾는다. 테스트는 작업 디렉터리에 기대지 않도록 Gradle이 넘겨주는 경로를 쓴다.

### CI와 의존성 갱신

- 워크플로는 서비스마다 하나씩 두고 **포함 목록(`paths`)**으로 실행 조건을 건다. 서비스가 늘어나도 다른 서비스의 변경이 관계없는 워크플로를 실행하지 않는다.
- Java 서비스의 워크플로는 자기 디렉터리에 더해 공통 빌드 파일(`build-logic/`, `gradle/`, `config/`, 루트 Gradle 파일)과 테스트가 읽는 `deploy/compose.yaml`이 바뀔 때도 실행한다.
- 화면 워크플로를 추가해 PR에서 `pnpm verify`를 실행한다.
- Dependabot은 화면의 npm 의존성도 갱신한다(minor·patch 묶음). compose 이미지는 `deploy/`에서 찾는다.

## 검토한 대안

| 대안 | 기각 이유 |
|---|---|
| 서비스마다 독립된 Gradle 빌드 (자체 래퍼·settings, `build-logic`만 공유) | 저장소를 나누기는 쉽지만 래퍼와 설정이 서비스 수만큼 중복되고, 카탈로그 공유에 별도 설정이 필요하며, IDE에서 프로젝트를 여러 개 열어야 한다 |
| 루트 빌드 스크립트의 `subprojects { }`로 공통 설정 주입 | Gradle이 권하지 않는 프로젝트 간 설정이다. 서브프로젝트만 읽어서는 설정의 출처를 알 수 없고 구성 캐시와도 맞지 않는다 |
| `frontend/`, `backend/`처럼 기술로 나누기 | 서비스가 둘 이상이 되면 `backend/` 아래에 다시 서비스를 나눠야 한다. 배포·CI·소유 단위는 기술이 아니라 서비스다 |
| `auth`, `libs` 골격을 미리 만들기 | 구조가 완성된 모습은 먼저 보이지만, 단계 2의 설계(의존성, 패키지)를 근거 없이 미리 정하게 된다 |
| 서비스마다 저장소를 나누기 (polyrepo) | 1인 프로젝트에서 저장소 간 버전 맞추기와 변경 동기화 비용이 크다. 한 PR로 여러 서비스를 함께 바꿀 수 없다 |

## 결과

- 좋은 점: 새 Java 서비스가 빌드 규칙 한 줄로 같은 품질 기준을 얻는다. 모든 서비스가 PR에서 검증되고 의존성이 갱신된다. 실행 환경 구성이 한 디렉터리에 모이고, 접속 정보와 시험 사용자가 구성에서 분리된다.
- 나쁜 점: 빌드 구조를 이해하려면 컨벤션 플러그인과 included build를 알아야 한다. 경로 포함 목록은 공통 파일을 빠뜨리면 검증이 누락되므로, 공통 파일이 늘면 목록을 함께 고쳐야 한다. Dependabot PR이 매주 하나 더 생긴다.
- 재검토 조건: Java 서비스가 늘어 전체 빌드 시간이 문제가 될 때(빌드 캐시, 영향받은 프로젝트만 빌드하는 방식 검토). 운영 배포를 시작해 `deploy/`가 환경별 구성을 가져야 할 때. 쿠버네티스로 옮길 때.

### 구현에서 확인한 사항

- 개발 환경의 Keycloak에 상태 검사를 둔다. 없으면 `up --wait`와 board의 compose 연동이 Keycloak 준비 전에 넘어가서, 컨테이너가 없는 상태의 첫 실행이 실패한다.
- 다른 기기에서 로그인할 때는 Keycloak 포트만 연다(`KEYCLOAK_BIND_ADDRESS`). 공통 바인딩 주소를 열면 기본 비밀번호의 DB와 인증 없는 Valkey까지 노출된다.
- 서비스 단위 CI 명령에는 루트가 맡는 Gradle 스크립트 포맷 검사(`:spotlessCheck`)를 함께 넣는다. 빠뜨리면 전환 전 CI에 있던 검사가 조용히 사라진다.
- compose 파일은 이력 보존을 위해 이동과 내용 변경을 다른 커밋으로 나눴다. 프로젝트 이름은 `bbs`로 고정한다.
