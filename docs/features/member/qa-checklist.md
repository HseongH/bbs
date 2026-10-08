---
doc_id: MEM-QA
title: 회원·인증 QA 체크리스트
version: 1.0.1
status: In Review
owner: HseongH
reviewers: []
approved_date:
last_updated: 2026-10-09
related: [PRJ-QA 1.1.0, MEM-SRS 1.0.0, MEM-SDS 1.1.0]
---

# 회원·인증 QA 체크리스트

> 작성 규칙과 판정 기준은 [공통 QA 기준](../../project/qa-standards.md)을 따른다. 공통 보안 요구사항(COM-NFR-002, 003)의 항목도 이 기능에서 함께 검증한다.

## 1. 수행 정보

| 항목 | 값 |
|---|---|
| 대상 커밋 | `main` f46a99c (최초 수행은 e96a878) |
| 수행일 | 2026-10-09 |
| 백엔드 자동 검증 | `./gradlew check` 성공 (테스트 103개, 실패 0, 오류 0, 건너뜀 0) |
| 화면 자동 검증 | `pnpm verify` 성공 (테스트 39개 통과) |
| E2E, 수동 검증 | 실행하지 않음 (N/T) |

## 2. 테스트 항목

### 2.1 인증과 회원 생성

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-MEM-001 | MEM-FR-001 | 시험 계정(`tester`)으로 실제 Keycloak 로그인 | 상단에 닉네임 표시 | E2E `auth.setup: tester로 로그인한다` | N/T |
| TC-MEM-002 | MEM-FR-002 | 처음 로그인 | 회원 생성 | `MemberServiceTest#처음_로그인하면_회원이_생성된다` | Pass |
| TC-MEM-003 | MEM-FR-002 | 같은 사용자가 다시 로그인 | 중복 생성 없음, 같은 식별자 | `MemberServiceTest#이미_가입한_회원은_중복_생성되지_않는다` | Pass |
| TC-MEM-004 | MEM-FR-002 | 신규 회원 객체 | 식별자 없음 | `MemberTest#신규_회원은_식별자가_없는_상태로_생성된다` | Pass |
| TC-MEM-005 | MEM-FR-002 | 닉네임 경계값: 공백, 51자 | 거부 | `MemberTest#닉네임은_비어있을_수_없다`, `#닉네임은_50자를_넘을_수_없다` | Pass |
| TC-MEM-006 | MEM-FR-002 | 회원 식별자 0 이하 | 거부 | `MemberTest#회원_식별자는_양수여야_한다` | Pass |
| TC-MEM-007 | MEM-FR-002 | `subject`로 회원 찾기, 식별자로 읽기 | 저장한 값과 같음 | `MemberPersistenceAdapterTest#사용자_식별자로_회원을_찾을_수_있다`, `#저장한_회원을_식별자로_읽을_수_있다` | Pass |
| TC-MEM-008 | MEM-FR-003 | `admin-user`로 로그인 후 다른 사람의 글 삭제 | `ROLE_ADMIN` 권한으로 삭제 성공 | 수동 (역할 매핑 자체의 자동 테스트 없음. 컨트롤러 테스트는 권한을 직접 주입) | N/T |
| TC-MEM-009 | MEM-FR-004, COM-NFR-003 | CSRF 토큰과 함께 `POST /logout` | `204` | `SecurityCsrfTest#로그아웃은_토큰과_함께_POST하면_성공한다` | Pass |
| TC-MEM-010 | COM-NFR-003 | CSRF 토큰 없이 상태 변경 요청 | 거부 (`403`) | `SecurityCsrfTest#토큰_없는_변경_요청은_거부된다` | Pass |
| TC-MEM-011 | COM-NFR-003 | 조회 요청 | 토큰 없이 허용, 토큰 쿠키 발급 | `CsrfCookieIssuanceTest#조회_요청은_토큰이_필요없고_토큰_쿠키를_내려준다` | Pass |

### 2.2 회원 정보와 현재 회원

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-MEM-020 | MEM-FR-005 | 로그인한 회원이 내 정보 조회 | 식별자, 닉네임, 이메일 | `MemberControllerTest#로그인한_회원의_정보를_반환한다` | Pass |
| TC-MEM-021 | MEM-FR-005, COM-NFR-002 | 비로그인으로 내 정보 조회 | `401 UNAUTHENTICATED` (리다이렉트 아님) | `MemberControllerTest#미인증_요청은_401을_반환한다` | Pass |
| TC-MEM-022 | MEM-FR-006 | 존재하지 않는 회원 조회 | `MEMBER_NOT_FOUND` | `MemberServiceTest#존재하지_않는_회원을_조회하면_예외가_발생한다`, `MemberPersistenceAdapterTest#존재하지_않는_회원을_읽으면_예외가_발생한다` | Pass |
| TC-MEM-023 | MEM-FR-006 | 선택적 `@CurrentMember`에 비로그인 요청 | `null`로 처리되어 요청 성공 | `PostControllerTest#목록은_비로그인으로도_조회할_수_있다`(간접), 상세 조회 비로그인 경로는 자동 테스트 없음 | N/T |
| TC-MEM-024 | COM-NFR-002 | 화면 경로로 미인증 API 요청 | 화면이 아니라 `401` | `SpaForwardingTest#미인증_API_요청은_여전히_401이다` | Pass |

### 2.3 화면

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-MEM-030 | MEM-FR-020 | 로그인 상태 | 닉네임, 로그아웃 표시 | `app.spec: 로그인 상태면 닉네임과 로그아웃을 보여준다` | Pass |
| TC-MEM-031 | MEM-FR-020 | 비로그인 상태 | 로그인 버튼 표시 | `app.spec: 비로그인이면 로그인 버튼을 보여준다` | Pass |
| TC-MEM-032 | MEM-FR-020 | 회원 조회 중 | 어느 쪽도 표시하지 않음 | `app.spec: 회원 조회가 끝나기 전에는 로그인도 닉네임도 보여주지 않는다` | Pass |
| TC-MEM-033 | MEM-FR-021 | API가 `401` 반환 | 현재 경로 저장 후 로그인 이동 | `auth.interceptor.spec: 401을 받으면 현재 경로를 기억하고 로그인으로 보낸다` | Pass |
| TC-MEM-034 | MEM-FR-021 | 로그인 확인 요청의 `401` | 로그인 이동 안 함 | `auth.interceptor.spec: 토큰이 붙은 요청은 401이어도…`, `current-member.store.spec: 미인증이면 null이고 로그인으로 이동하지 않는다` | Pass |
| TC-MEM-035 | MEM-FR-021 | `401`이 아닌 오류 | 개입하지 않음 | `auth.interceptor.spec: 401이 아닌 오류는 건드리지 않는다` | Pass |
| TC-MEM-036 | MEM-FR-022 | 로그인 상태로 보호 화면에 직접 진입 | 회원 조회를 기다린 뒤 통과 | `auth.guard.spec: 회원 조회가 끝날 때까지 기다린 뒤 통과시킨다` | Pass |
| TC-MEM-037 | MEM-FR-022 | 비로그인으로 보호 화면 진입 | 로그인으로 이동 | `auth.guard.spec: 미인증이면 막고 로그인으로 보낸다` | Pass |
| TC-MEM-038 | MEM-FR-021 | 로그인 후 원래 경로로 복귀 | 저장된 경로로 이동 | 자동 테스트 없음 (`main.ts`) | N/T |
| TC-MEM-039 | MEM-FR-023 | 로그아웃 | 비로그인 상태로 복귀 | E2E `logout.spec: 로그아웃하면 비로그인 상태로 돌아간다` | N/T |

## 3. 추적 요약

| 요구사항 | TC | 자동화 |
|---|---|---|
| MEM-FR-001 | 001 | E2E만 (이번 수행 미실행) |
| MEM-FR-002 | 002~007 | 완전 |
| MEM-FR-003 | 008 | **없음** |
| MEM-FR-004 | 009 | 완전 |
| MEM-FR-005 | 020, 021 | 완전 |
| MEM-FR-006 | 022, 023 | 부분 |
| MEM-FR-020~023 | 030~039 | 화면 단위 테스트 대부분, 복귀 경로·로그아웃 흐름은 미실행 |
| MEM-NFR-001 | - | 동시성 테스트 없음 (DB 제약으로 보장) |

## 4. 결과 요약과 후속 조치

- 집계: 전체 26건 중 Pass 21, Fail 0, N/T 5 (자동 테스트 없음 3, E2E 미실행 2).
- **MEM-FR-003(역할 매핑)에 자동 테스트가 없다.** 관리자 기능 전체가 이 매핑에 의존하므로 우선순위가 가장 높은 테스트 추가 후보다. `BbsOidcUserService`의 `realmRoles`를 가짜 `OidcUser`로 검증하는 단위 테스트를 제안한다.
- [SRS 미결 사항](srs.md#6-미결-사항) MEM-OPEN-01~03은 결함이 아니라 미정의 동작이므로 판정 대상에서 제외했다.

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|---|---|---|---|
| 1.0.0 | 2026-10-09 | 최초 작성 (`main` e96a878 기준 수행) | HseongH |
| 1.0.1 | 2026-10-09 | `main` f46a99c(포트 정리, Lombok 제거 반영)에서 재수행. 판정 변화 없음. 참조 테스트 이름 전수 확인 | HseongH |
