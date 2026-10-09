---
doc_id: PRJ-QC
title: 게시판(bbs) 프로젝트 QA 체크리스트
version: 1.0.0
status: Draft
owner: HseongH
reviewers: []
approved_date:
last_updated: 2026-10-09
related: [PRJ-QA 1.3.0, PRJ-SRS 1.5.0, PRJ-SDS 1.6.0]
---

# 게시판(bbs) 프로젝트 QA 체크리스트

> 작성 규칙과 판정 기준은 [공통 QA 기준](qa-standards.md)을 따른다. 이 문서는 **어느 기능에도 속하지 않는 공통 요구사항**(빌드, CI, 개발 환경)만 검증한다. 기능의 동작으로 확인하는 공통 요구사항(예: COM-NFR-002의 401 응답)은 지금처럼 해당 기능의 QA 체크리스트가 검증한다.

## 1. 수행 정보

| 항목 | 값 |
|---|---|
| 대상 커밋 | 아직 수행하지 않음 |
| 수행일 | |
| 자동 검증 | |
| 수동 검증 | |

## 2. 테스트 항목

### 2.1 Java 빌드 규칙

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-COM-001 | COM-NFR-031, 034 | board에 포맷을 어긴 Java 코드를 임시로 넣고 `./gradlew :services:board:check` | 포맷 검사에서 빌드 실패 | 명령 실행 후 원복 | N/T |
| TC-COM-002 | COM-NFR-031, 034 | board에 `@Nullable` 값을 검사 없이 역참조하는 코드를 임시로 넣고 컴파일 | NullAway 오류로 컴파일 실패 | 명령 실행 후 원복 | N/T |
| TC-COM-003 | COM-NFR-031, 034 | 전환 전 `main`과 전환 후 브랜치에서 각각 전체 검증 실행 | 테스트 수가 같고 실패 0. Checkstyle, Error Prone, 커버리지 검증 작업이 전환 후에도 실행됨 | `./gradlew check` 결과 비교 | N/T |
| TC-COM-004 | COM-NFR-034 | board의 빌드 스크립트 검토 | 품질 규칙(Checkstyle, Error Prone, Spotless, JaCoCo 설정)이 없고 컨벤션 플러그인만 적용 | 구성 검토 | N/T |

### 2.2 CI

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-COM-005 | COM-NFR-033 | board 파일을 바꾼 PR | `board` 워크플로가 실행되어 통과 | PR의 CI 실행 기록 | N/T |
| TC-COM-006 | COM-NFR-033 | web 파일을 바꾼 PR | `web` 워크플로가 실행되어 통과 | PR의 CI 실행 기록 | N/T |
| TC-COM-007 | COM-NFR-033 | 워크플로의 실행 조건 검토 | `board`의 조건에 Java 공통 빌드 파일과 `deploy/compose.yaml`이 있고, `web`의 조건에 board 경로가 없음 | 구성 검토 | N/T |

### 2.3 의존성 갱신

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-COM-008 | COM-NFR-035 | Dependabot 설정 검토 | gradle(`/`), npm(`/services/web`), github-actions(`/`), docker-compose(`/deploy`)가 모두 등록됨 | 구성 검토 | N/T |

### 2.4 개발 환경

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-COM-009 | COM-CON-004 | `.env` 없이 `docker compose -f deploy/compose.yaml up -d` | PostgreSQL, Valkey, Keycloak이 모두 정상 상태 | 명령 실행, `docker compose ps` | N/T |
| TC-COM-010 | COM-NFR-006 | 기본 설정으로 실행한 컨테이너의 포트 확인 | 모든 포트가 `127.0.0.1`에만 열림 | `docker compose ps` | N/T |
| TC-COM-011 | COM-NFR-006 | 바인딩 주소와 DB 비밀번호를 환경 변수로 지정하고 구성 확인 | 지정한 값이 구성에 반영됨 | `docker compose config` | N/T |
| TC-COM-012 | COM-CON-006 | realm 파일 검토, 개발 환경에서 시험 계정으로 로그인 | realm 파일에 사용자가 없음. `tester`로 로그인 성공 | 구성 검토, E2E `auth.setup` | N/T |
| TC-COM-013 | COM-CON-004 | `./gradlew :services:board:bootRun` | compose 서비스가 자동으로 뜨고 `/actuator/health`가 `UP` | 명령 실행 | N/T |
| TC-COM-014 | COM-NFR-031 | 통합 테스트의 컨테이너 이미지 출처 | `deploy/compose.yaml`의 이미지를 읽음 | `ComposeImagesTest` | N/T |
| TC-COM-015 | COM-NFR-032 | board 실행 중 `services/web`에서 타입 재생성 | 생성 파일과 타입 검사가 전환 전과 같음 | `pnpm gen:api` 후 `pnpm typecheck` | N/T |

## 3. 추적 요약

| 요구사항 | TC | 자동화 |
|---|---|---|
| COM-NFR-006 | 010, 011 | 없음 (명령 확인) |
| COM-NFR-031 | 001~003, 014 | 부분 |
| COM-NFR-032 | 015 | 부분 (명령 확인) |
| COM-NFR-033 | 005~007 | CI 기록 |
| COM-NFR-034 | 001~004 | 부분 |
| COM-NFR-035 | 008 | 없음 (구성 검토) |
| COM-CON-004 | 009, 013 | 없음 (명령 확인) |
| COM-CON-006 | 012 | E2E |

## 4. 결과 요약과 후속 조치

- 집계: 전체 15건 중 Pass 0, Fail 0, N/T 15 (구현 전 작성).

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|---|---|---|---|
| 1.0.0 | 2026-10-09 | 최초 작성: 모노레포 전환의 공통 요구사항 항목 (ADR-0014). 판정은 구현 후 채운다 | HseongH |
