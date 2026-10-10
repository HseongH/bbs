---
doc_id: PRJ-QA
title: 게시판(bbs) 공통 QA 기준
version: 1.4.0
status: In Review
owner: HseongH
reviewers: []
approved_date:
last_updated: 2026-10-09
related: [PRJ-SRS 1.7.0, PRJ-SDS 1.9.0, PRJ-QC 1.6.0]
---

# 게시판(bbs) 공통 QA 기준

> ISO/IEC/IEEE 29119-3의 테스트 계획(Test Plan) 가운데 **모든 기능에 공통으로 적용하는 부분**만 담았다. 기능별 테스트 항목과 결과는 각 기능의 QA 체크리스트에, 어느 기능에도 속하지 않는 공통 요구사항(빌드, CI, 개발 환경)의 항목과 결과는 [프로젝트 QA 체크리스트](qa-checklist.md)에 기록한다.

## 1. 완료 정의 (Definition of Done)

기능 하나를 "완료"라고 말하려면 아래를 모두 만족해야 한다. 하나라도 만족하지 못하면 미완료로 보고한다.

| 번호 | 조건 | 확인 방법 |
|---|---|---|
| DoD-1 | 기능 SRS의 모든 `FR`이 QA 체크리스트의 `TC`에 하나 이상 연결되어 있다 | 체크리스트의 추적 표 확인 |
| DoD-2 | 모든 `TC`의 판정이 `Pass`이거나, `Fail`·`Blocked`라면 사유와 후속 조치가 기록되어 있다 | 체크리스트 확인 |
| DoD-3 | 저장소 루트에서 `./gradlew check`가 성공한다 (모든 Java 서비스) | 명령 실행 결과 (종료 코드 0), PR의 서비스별 CI |
| DoD-4 | 화면 변경이 있으면 `services/web`에서 `pnpm verify`가 성공한다 | 명령 실행 결과, PR의 `web` CI |
| DoD-5 | 화면 흐름이 바뀌면 `pnpm e2e`가 성공한다 | 명령 실행 결과, PR의 `e2e` CI |
| DoD-6 | API가 바뀌었으면 `pnpm gen:api`로 타입을 다시 생성했고 타입 검사가 통과한다 | 생성 파일의 diff, `pnpm typecheck` |
| DoD-7 | 기능 SRS·SDS·QA 체크리스트의 버전과 `related`가 서로 맞는다. 공통 요구사항을 바꿨으면 프로젝트 SRS·SDS·QA 체크리스트도 같다 | 문서 머리말 확인 |
| DoD-8 | 새 설계 결정이 프로젝트 전체에 영향을 주면 ADR이 추가되었다 | `docs/project/adr/` 확인 |

**검증 원칙:** "통과할 것이다", "아마 될 것이다"는 판정이 아니다. 판정은 **이번에 실행한 명령의 출력**을 근거로 한다.

## 2. 테스트 수준

| 수준 | 도구 | 대상 | 스프링 컨텍스트 | 외부 시스템 |
|---|---|---|---|---|
| 도메인 단위 | JUnit 5, AssertJ | 불변식, 상태 전이, 권한 규칙 | 없음 | 없음 |
| 서비스 단위 | JUnit 5, 직접 만든 인메모리 대역 | 유스케이스 조율 | 없음 | 없음 |
| 웹 슬라이스 | `@WebMvcTest` | 오류 변환 규약 | 웹 계층만 | 없음 |
| 영속성 | `IntegrationTestBase`(`@SpringBootTest`) + Testcontainers | 매핑, 쿼리, 제약 조건 | 전체 | PostgreSQL, Valkey |
| 웹·통합 | `IntegrationTestBase` + MockMvc + `oidcLogin()` | 요청 검증, 인증, 오류 변환, 전체 흐름 | 전체 | PostgreSQL, Valkey |
| 동시성 | 가상 스레드로 동시 요청 | 카운터, 중복 판정 | 전체 | PostgreSQL, Valkey |
| 아키텍처 | ArchUnit | 계층·기능 의존 규칙 | 없음 | 없음 |
| 화면 단위 | Vitest + 생성된 API 타입 기반 목 | 컴포넌트, 스토어, 가드, 인터셉터 | - | 없음 |
| E2E | Playwright | 사용자 시나리오 | - | 전체 (백엔드, Keycloak 포함) |

규칙은 가능한 한 **가장 낮은 수준**에서 검증한다. 예를 들어 제목 길이 규칙은 도메인 단위 테스트로 검증하고, 웹 테스트에서는 "검증 실패가 400으로 변환되는지"만 확인한다.

## 3. 자동 품질 기준

| 항목 | 기준 | 위반 시 |
|---|---|---|
| 포맷 | google-java-format (Spotless), Prettier | 빌드 실패 |
| 정적 분석 | Checkstyle (서식을 뺀 이름·구조 규칙), Error Prone (경고도 실패로 처리), ESLint | 빌드 실패 |
| null 안정성 | NullAway (JSpecify 모드), 모든 패키지 `@NullMarked` | 컴파일 실패 |
| 아키텍처 | `HexagonalArchitectureTest`, `FeatureBoundaryTest` | 빌드 실패 |
| 라인 커버리지 (전체) | 80% 이상 | 빌드 실패 |
| 라인 커버리지 (`*.domain`, `*.application.*`) | 패키지마다 90% 이상 | 빌드 실패 |
| 커밋 메시지 | `type(scope): subject` | 커밋 거부 |

커버리지 측정에서 QueryDSL 생성 클래스(`Q*`), 설정 패키지(`**/config/**`), 애플리케이션 진입점은 제외한다.

## 4. 테스트 작성 규칙

1. **테스트 이름은 검증하는 규칙을 문장으로 적는다.** (`작성자가_아니면_수정할_수_없다`) 테스트 이름만 모아도 요구사항 목록처럼 읽혀야 한다.
2. **실패하는 테스트를 먼저 작성하고, 올바른 이유로 실패하는 것을 확인한 뒤에 구현한다.**
3. **버그를 고칠 때는 버그를 재현하는 테스트를 먼저 추가한다.** 그 테스트를 QA 체크리스트에 회귀 항목으로 기록한다.
4. **목은 꼭 필요할 때만 쓴다.** 데이터베이스 동작(제약, 원자적 UPDATE)은 Testcontainers의 실제 PostgreSQL로 검증한다.
5. **화면 테스트의 API 목은 생성된 API 타입을 사용한다.** 백엔드 계약이 바뀌면 목도 컴파일 오류가 나야 한다.

## 5. QA 체크리스트 작성 규칙

기능별 QA 체크리스트와 프로젝트 QA 체크리스트는 다음 열을 가진다.

| 열 | 내용 |
|---|---|
| TC | `TC-<기능>-NNN`. 프로젝트 QA 체크리스트는 `TC-COM-NNN` |
| 요구사항 | 검증하는 `FR`·`NFR` 식별자 |
| 시나리오 | 사전 조건 → 행위 |
| 기대 결과 | 관찰할 수 있는 결과 (상태 코드, 화면, 데이터) |
| 검증 수단 | 자동 테스트 이름, 실행한 명령, 또는 `수동` |
| 판정 | `Pass` / `Fail` / `Blocked` / `N/T`(미수행) |

- 자동 테스트로 검증하는 항목은 `./gradlew check` 실행 결과로 판정한다.
- 수동 항목은 수행 일자와 환경(브라우저, 계정)을 함께 기록한다.
- `Fail`은 지우지 않는다. 결함 번호나 후속 조치를 적고, 수정 후 같은 행의 판정을 바꾸며 변경 이력에 남긴다.

## 6. 테스트 환경

| 항목 | 값 |
|---|---|
| JDK | Temurin 25 |
| 데이터베이스 | Testcontainers PostgreSQL (`application-test.yml`) |
| Valkey | Testcontainers `RedisContainer`에 Valkey 이미지 사용 |
| 컨테이너 이미지 | 통합 테스트도 `deploy/compose.yaml`의 이미지 태그를 읽는다. 개발 환경과 테스트 환경의 버전이 같다 |
| 인증 | 테스트에서는 `oidcLogin()` 등 스프링 보안 테스트 지원으로 대체한다. 실제 Keycloak은 E2E에서만 사용한다 |
| E2E 계정 | `tester` / `tester` (USER), `admin-user` / `admin` (USER, ADMIN). 개발용 사용자 파일(`deploy/keycloak/dev/`)에서 가져온다 |

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|---|---|---|---|
| 1.0.0 | 2026-10-09 | 최초 작성 | HseongH |
| 1.1.0 | 2026-10-09 | 정적 분석 기준(Checkstyle 범위, Error Prone)과 테스트 이미지 출처 갱신 (PR #8) | HseongH |
| 1.2.0 | 2026-10-09 | 테스트 환경을 Valkey로 갱신 (PR #15) | HseongH |
| 1.3.0 | 2026-10-09 | 모노레포 전환 반영: 프로젝트 QA 체크리스트(`TC-COM`)의 범위 추가, DoD-3·4·7의 실행 위치와 CI, 테스트 환경의 compose 경로와 시험 계정 출처 갱신 (PRJ-SRS 1.5.0) | HseongH |
| 1.4.0 | 2026-10-09 | DoD-5의 확인 방법에 `e2e` CI 추가 (PRJ-SRS 1.6.0) | HseongH |
