---
doc_id: PRJ-QC
title: 게시판(bbs) 프로젝트 QA 체크리스트
version: 1.3.0
status: In Review
owner: HseongH
reviewers: []
approved_date:
last_updated: 2026-10-09
related: [PRJ-QA 1.4.0, PRJ-SRS 1.6.0, PRJ-SDS 1.7.0]
---

# 게시판(bbs) 프로젝트 QA 체크리스트

> 작성 규칙과 판정 기준은 [공통 QA 기준](qa-standards.md)을 따른다. 이 문서는 **어느 기능에도 속하지 않는 공통 요구사항**(빌드, CI, 개발 환경)만 검증한다. 기능의 동작으로 확인하는 공통 요구사항(예: COM-NFR-002의 401 응답)은 지금처럼 해당 기능의 QA 체크리스트가 검증한다.

## 1. 수행 정보

| 항목 | 값 |
|---|---|
| 대상 커밋 | `refactor/monorepo` 브랜치 끝, 이 문서를 고친 커밋과 같은 코드 |
| 수행일 | 2026-10-09 |
| 자동 검증 | 루트에서 `./gradlew test --rerun check` 성공 (테스트 140개, 실패 0. 전환 전 `main`과 같은 수). `services/web`에서 `pnpm verify` 성공 (테스트 파일 12개, 테스트 42개). `pnpm e2e` 성공 (Firefox, 4개) |
| 수동 검증 | 아래 각 항목의 명령. 위반 코드는 확인 후 원복함 |

## 2. 테스트 항목

### 2.1 Java 빌드 규칙

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-COM-001 | COM-NFR-031, 034 | board에 포맷을 어긴 Java 코드를 임시로 넣고 `./gradlew :services:board:check` | 포맷 검사에서 빌드 실패 | 명령 실행 후 원복: `BbsApplication.java`의 들여쓰기를 깨고 `./gradlew spotlessCheck` → `:spotlessJavaCheck` 실패. 규칙을 플러그인으로 옮긴 직후, board를 옮기기 전(루트가 곧 board였던 시점)에 실행했다 | Pass |
| TC-COM-002 | COM-NFR-031, 034 | board에 `@Nullable` 값을 검사 없이 역참조하는 코드를 임시로 넣고 컴파일 | NullAway 오류로 컴파일 실패 | 명령 실행 후 원복: board를 옮기기 전 `MemberService`에 `@Nullable` 지역 변수의 역참조를 넣고 `./gradlew compileJava` → `[NullAway] dereferenced expression 's' is @Nullable`로 실패 | Pass |
| TC-COM-003 | COM-NFR-031, 034 | 전환 전 `main`과 전환 후 브랜치에서 각각 전체 검증 실행 | 테스트 수가 같고 실패 0. Checkstyle, Error Prone, 커버리지 검증 작업이 전환 후에도 실행됨 | `./gradlew check` 결과 비교: `check --dry-run` 작업 목록 17개가 전환 전과 같음(서비스 경로 접두사 제외). CI의 board 명령은 `:services:board:check :spotlessCheck`로, 전환 전 CI가 실행하던 Gradle 스크립트 포맷 검사(`:spotlessKotlinGradleCheck`)를 포함함(최종 리뷰에서 누락을 발견해 수정). 테스트 140개 실패 0. 패키지 커버리지 기준을 임시로 99.9%로 올리면 `*.domain` 세 패키지가 걸림(일반화한 패턴이 적용됨) | Pass |
| TC-COM-004 | COM-NFR-034 | board의 빌드 스크립트 검토 | 품질 규칙(Checkstyle, Error Prone, Spotless, JaCoCo 설정)이 없고 컨벤션 플러그인만 적용 | 구성 검토: `grep -nE "checkstyle\|errorprone\|spotless\|jacoco" services/board/build.gradle.kts` 출력 없음 | Pass |

### 2.2 CI

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-COM-005 | COM-NFR-033 | board 파일을 바꾼 PR | `board` 워크플로가 실행되어 통과 | PR의 CI 실행 기록: PR #28(커밋 431fd7b)에서 `board` 워크플로 실행, `./gradlew :services:board:check :spotlessCheck` 작업 22개 성공 ([실행 기록](https://github.com/HseongH/bbs/actions/runs/37935282153)) | Pass |
| TC-COM-006 | COM-NFR-033 | web 파일을 바꾼 PR | `web` 워크플로가 실행되어 통과 | PR의 CI 실행 기록: PR #28(커밋 431fd7b)에서 `web` 워크플로 실행, `pnpm verify` 테스트 파일 12개·테스트 42개 통과 ([실행 기록](https://github.com/HseongH/bbs/actions/runs/37935282134)) | Pass |
| TC-COM-016 | COM-NFR-036 | `e2e` 워크플로를 추가한 PR | `e2e` 워크플로가 실행되어 E2E 4개 통과 | PR의 CI 실행 기록 | N/T |
| TC-COM-017 | COM-NFR-036 | `e2e` 워크플로의 실행 조건 검토 | board·web·`deploy/`와 board의 실행 결과를 바꾸는 공통 빌드 파일이 조건에 있음 | 구성 검토: `e2e`의 `paths`에 `services/board/**`, `services/web/**`, `deploy/**`, `build-logic/**`, `gradle/**`, 루트 Gradle 파일, 자기 파일 포함 | Pass |
| TC-COM-007 | COM-NFR-033 | 워크플로의 실행 조건 검토 | `board`의 조건에 Java 공통 빌드 파일과 `deploy/compose.yaml`이 있고, `web`의 조건에 board 경로가 없음 | 구성 검토: `board`의 `paths`에 `build-logic/**`, `gradle/**`, `config/**`, 루트 Gradle 파일, `gradlew`, `deploy/compose.yaml` 포함. `web`의 `paths`는 `services/web/**`와 자기 파일뿐 | Pass |

### 2.3 의존성 갱신

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-COM-008 | COM-NFR-035 | Dependabot 설정 검토 | gradle(`/`), npm(`/services/web`), github-actions(`/`), docker-compose(`/deploy`)가 모두 등록됨 | 구성 검토: `.github/dependabot.yml`에 gradle `/`, github-actions `/`, npm `/services/web`, docker-compose `/deploy` 등록 | Pass |

### 2.4 개발 환경

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-COM-009 | COM-CON-004 | `.env` 없이 `docker compose -f deploy/compose.yaml up -d` | PostgreSQL, Valkey, Keycloak이 모두 정상 상태 | 명령 실행, `docker compose ps`: `.env` 없이 `docker compose -f deploy/compose.yaml up -d --wait` → 세 서비스 모두 healthy | Pass |
| TC-COM-010 | COM-NFR-006 | 기본 설정으로 실행한 컨테이너의 포트 확인 | 모든 포트가 `127.0.0.1`에만 열림 | `docker compose ps`: 포트 `127.0.0.1:5432`, `127.0.0.1:6379`, `127.0.0.1:8081` | Pass |
| TC-COM-011 | COM-NFR-006 | 바인딩 주소와 DB 비밀번호를 환경 변수로 지정하고 구성 확인 | 지정한 값이 구성에 반영됨 | `docker compose config`: `BIND_ADDRESS=0.0.0.0 POSTGRES_PASSWORD=changed`로 `config` → `host_ip: 0.0.0.0` 3곳, `POSTGRES_PASSWORD: changed`. `KEYCLOAK_BIND_ADDRESS=0.0.0.0`만 주면 Keycloak만 `0.0.0.0`이고 PostgreSQL·Valkey는 `127.0.0.1`. `.env.example`을 그대로 복사한 `.env`(빈 `KEYCLOAK_BIND_ADDRESS`)로는 모두 `127.0.0.1` | Pass |
| TC-COM-012 | COM-CON-006 | realm 파일 검토, 개발 환경에서 시험 계정으로 로그인 | realm 파일에 사용자가 없음. `tester`로 로그인 성공 | 구성 검토, E2E `auth.setup`: realm 파일에 `users` 없음. 관리 API의 `users?username=tester` → 1명. E2E `tester로 로그인한다` 통과 | Pass |
| TC-COM-013 | COM-CON-004 | `./gradlew :services:board:bootRun` | compose 서비스가 자동으로 뜨고 `/actuator/health`가 `UP` | 명령 실행: 컨테이너가 없는 상태에서 `./gradlew :services:board:bootRun` → compose가 뜨고 `/actuator/health`가 `UP` | Pass |
| TC-COM-014 | COM-NFR-031 | 통합 테스트의 컨테이너 이미지 출처 | `deploy/compose.yaml`의 이미지를 읽음 | `ComposeImagesTest`: `ComposeImagesTest#저장소의_compose_파일에서_읽는다` 통과 | Pass |
| TC-COM-015 | COM-NFR-032 | board 실행 중 `services/web`에서 타입 재생성 | 생성 파일과 타입 검사가 전환 전과 같음 | `pnpm gen:api` 후 `pnpm typecheck`: board 실행 중 `gen:api` 후 Prettier 적용 → `schema.d.ts` 변경 없음, 타입 검사 통과 | Pass |

## 3. 추적 요약

| 요구사항 | TC | 자동화 |
|---|---|---|
| COM-NFR-006 | 010, 011 | 없음 (명령 확인) |
| COM-NFR-031 | 001~003, 014 | 부분 |
| COM-NFR-032 | 015 | 부분 (명령 확인) |
| COM-NFR-033 | 005~007 | CI 기록 |
| COM-NFR-034 | 001~004 | 부분 |
| COM-NFR-035 | 008 | 없음 (구성 검토) |
| COM-NFR-036 | 016, 017 | CI 기록 |
| COM-CON-004 | 009, 013 | 없음 (명령 확인) |
| COM-CON-006 | 012 | E2E |

## 4. 결과 요약과 후속 조치

- 집계: 전체 17건 중 Pass 16, Fail 0, N/T 1. TC-COM-016은 이 체크리스트를 고친 PR의 CI 실행 후 판정한다.
- TC-COM-005, 006은 두 서비스를 함께 바꾼 PR #28에서 판정했다. 한 서비스만 바꾼 PR에서 다른 워크플로가 실행되지 않는 것은 실행 조건 검토(TC-COM-007)로만 확인했으므로, 다음 단일 서비스 PR에서 실행 목록을 한 번 확인한다.
- 수행 중 발견한 결함: Keycloak에 상태 검사가 없어 `up --wait`와 board의 compose 연동이 Keycloak 준비 전에 넘어갔다. 컨테이너가 없는 상태에서 `bootRun`하면 issuer 조회에 실패해 COM-CON-004를 어겼다. 상태 검사를 추가해 고쳤고(TC-COM-009, 013의 근거 실행은 수정 후), 수정 전 재현(`--wait` 직후 조회 실패)과 수정 후 성공을 모두 확인했다.
- 자동 테스트 추가 후보: TC-COM-001, 002의 위반 코드 검사는 매번 수동이다. 빌드 규칙을 검사하는 Gradle TestKit 테스트로 바꿀 수 있다.

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|---|---|---|---|
| 1.0.0 | 2026-10-09 | 최초 작성: 모노레포 전환의 공통 요구사항 항목 (ADR-0014). 판정은 구현 후 채운다 | HseongH |
| 1.1.0 | 2026-10-09 | `refactor/monorepo`에서 수행. 최종 리뷰 반영(CI의 Gradle 스크립트 포맷 검사, Keycloak 전용 바인딩 주소, TC-COM-001·002 근거의 수행 시점 명시). TC-COM-001~004, 007~015 판정 (Pass 13). TC-COM-005, 006은 CI 실행 후 판정 | HseongH |
| 1.2.0 | 2026-10-09 | PR #28의 CI 실행 기록으로 TC-COM-005, 006 판정 (Pass 15) | HseongH |
| 1.3.0 | 2026-10-09 | `e2e` 워크플로 항목 TC-COM-016, 017 추가 (PRJ-SRS 1.6.0). TC-COM-017 판정, 016은 CI 실행 후 판정 | HseongH |
