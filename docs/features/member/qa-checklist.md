---
doc_id: MEM-QA
title: 회원·인증 QA 체크리스트
version: 1.4.1
status: In Review
owner: HseongH
reviewers: []
approved_date:
last_updated: 2026-10-09
related: [PRJ-QA 1.3.0, MEM-SRS 1.2.0, MEM-SDS 1.4.1]
---

# 회원·인증 QA 체크리스트

> 작성 규칙과 판정 기준은 [공통 QA 기준](../../project/qa-standards.md)을 따른다. 공통 보안 요구사항(COM-NFR-002, 003)의 항목도 이 기능에서 함께 검증한다.

## 1. 수행 정보

| 항목 | 값 |
|---|---|
| 대상 커밋 | `fix/review-defects` 브랜치 끝, 이 문서를 고친 커밋과 같은 코드 (이전 수행: e96a878, f46a99c, 85cce67) |
| 수행일 | 2026-10-09 |
| 백엔드 자동 검증 | `./gradlew check` 성공 (테스트 140개, 실패 0, 오류 0, 건너뜀 0. 이 문서가 인용한 테스트가 모두 이번 실행 결과에 Pass로 있는 것을 대조함) |
| 화면 자동 검증 | `pnpm verify` 성공 (린트, 타입 검사 통과, 테스트 파일 12개·테스트 42개 통과). `pnpm gen:api` 후 생성 타입 변화 없음 |
| E2E | `pnpm e2e` 성공 (Chromium, 4개 통과: 로그인 준비, 글·댓글·좋아요 흐름, 검색 URL 유지, 로그아웃). `main` 1fa1827에 E2E 브라우저를 Chromium으로 바꾸는 설정을 적용하고, `deploy/compose.yaml`의 컨테이너와 `./gradlew :services:board:bootRun`을 띄운 뒤 2026-10-09 수행 (이전 수행: Firefox, `main` 0b719d8) |
| 수동 검증 | 실행하지 않음 (N/T) |

## 2. 테스트 항목

### 2.1 인증과 회원 생성

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-MEM-001 | MEM-FR-001 | 시험 계정(`tester`)으로 실제 Keycloak 로그인 | 상단에 닉네임 표시 | E2E `auth.setup: tester로 로그인한다` | Pass |
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
| TC-MEM-012 | MEM-FR-002 | 50자를 넘는 Keycloak 이름으로 처음 로그인 (회귀, MEM-OPEN-02) | 로그인 성공, 닉네임은 50자로 잘림. 두 개의 `char`로 된 문자는 가르지 않음 | `MemberServiceTest#닉네임이_50자를_넘어도_로그인할_수_있다`, `MemberTest#외부에서_받은_닉네임이_50자를_넘으면_50자로_자른다`, `#닉네임을_자를_때_두_단위로_된_문자를_가르지_않는다` | Pass |
| TC-MEM-013 | MEM-NFR-001 | 같은 사용자가 동시에 16번 처음 로그인 (회귀, MEM-OPEN-03) | 모두 성공, 같은 식별자, 회원 1명 | `MemberProvisioningConcurrencyTest#같은_사용자가_동시에_처음_로그인해도_모두_성공하고_회원은_하나다` | Pass |
| TC-MEM-014 | COM-NFR-021 | 비로그인으로 `/actuator/health`, `/actuator/info` 조회 | `200` | `ActuatorAccessTest#상태_확인과_정보는_인증_없이_볼_수_있다` | Pass |
| TC-MEM-015 | COM-NFR-021 | 비로그인으로 `/actuator/metrics` 조회 (회귀, OPEN-04) | `401` | `ActuatorAccessTest#지표는_인증_없이_볼_수_없다` | Pass |
| TC-MEM-016 | COM-NFR-021 | 일반 회원이 `/actuator/metrics` 조회 (회귀, OPEN-04) | `403` | `ActuatorAccessTest#지표는_일반_회원이_볼_수_없다` | Pass |
| TC-MEM-017 | COM-NFR-021 | 관리자가 `/actuator/metrics` 조회 | `200` | `ActuatorAccessTest#지표는_관리자가_볼_수_있다` | Pass |
| TC-MEM-018 | MEM-FR-002 | Keycloak 사용자 이름·이메일이 공백뿐인 사용자로 처음 로그인 (회귀) | 닉네임은 `sub`, 이메일은 `{sub}@unknown.local` | `BbsOidcUserServiceTest#사용자_이름이나_이메일이_공백뿐이면_subject로_대신한다`, `#사용자_이름이나_이메일이_없으면_subject로_대신한다` | Pass |
| TC-MEM-019 | COM-NFR-002, COM-IF-003 | 미인증 API 요청, 경로에 따옴표가 있는 미인증 요청 (회귀) | `401`, ProblemDetail 필드가 모두 있고 올바른 JSON. `instance`는 허용되지 않는 문자만 인코딩 | `UnauthenticatedResponseTest`, `ProblemDetailsTest` | Pass |

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
| TC-MEM-039 | MEM-FR-023 | 로그아웃 | 비로그인 상태로 복귀 | E2E `logout.spec: 로그아웃하면 비로그인 상태로 돌아간다` | Pass |

## 3. 추적 요약

| 요구사항 | TC | 자동화 |
|---|---|---|
| MEM-FR-001 | 001 | E2E |
| MEM-FR-002 | 002~007, 012, 018 | 완전 |
| MEM-FR-003 | 008 | **없음** |
| MEM-FR-004 | 009 | 완전 |
| MEM-FR-005 | 020, 021 | 완전 |
| MEM-FR-006 | 022, 023 | 부분 |
| MEM-FR-020~023 | 030~039 | 화면 단위 테스트 대부분, 로그아웃 흐름은 E2E. 로그인 후 원래 경로 복귀(038)는 자동 테스트 없음 |
| MEM-NFR-001 | 013 | 완전 |
| COM-NFR-021 | 014~017 | 완전 |
| COM-NFR-002 | 019 | 완전 |

## 4. 결과 요약과 후속 조치

- 집계: 전체 34건 중 Pass 31, Fail 0, N/T 3 (자동 테스트 없음 3).
- E2E 2건(TC-MEM-001, 039)은 1.2.x까지 N/T였다. E2E 브라우저를 Firefox로 바꾸고 실행해 Pass로 판정했다.
- **MEM-FR-003(역할 매핑)에 자동 테스트가 없다.** 관리자 기능 전체가 이 매핑에 의존하므로 우선순위가 가장 높은 테스트 추가 후보다. `BbsOidcUserService`의 `realmRoles`를 가짜 `OidcUser`로 검증하는 단위 테스트를 제안한다.
- [SRS 미결 사항](srs.md#6-미결-사항) 중 MEM-OPEN-02, 03은 로그인 실패로 이어지는 결함으로 다시 분류해 1.1.0에서 고쳤고, 회귀 항목 TC-MEM-012, 013을 추가했다. MEM-OPEN-01, 04는 미정의 동작이므로 판정 대상에서 제외한다.

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|---|---|---|---|
| 1.0.0 | 2026-10-09 | 최초 작성 (`main` e96a878 기준 수행) | HseongH |
| 1.0.1 | 2026-10-09 | `main` f46a99c(포트 정리, Lombok 제거 반영)에서 재수행. 판정 변화 없음. 참조 테스트 이름 전수 확인 | HseongH |
| 1.1.0 | 2026-10-09 | MEM-OPEN-02, 03 해결에 따라 회귀 항목 TC-MEM-012, 013 추가 | HseongH |
| 1.2.0 | 2026-10-09 | 공통 보안 항목에 액추에이터 접근 회귀 항목 TC-MEM-014~017 추가 (OPEN-04 해결, PR #18). 수행 정보를 `main` 85cce67 재수행 결과로 정정 | HseongH |
| 1.3.0 | 2026-10-09 | E2E를 Firefox로 실행해 TC-MEM-001, 039를 N/T → Pass로 판정 | HseongH |
| 1.4.0 | 2026-10-09 | `fix/review-defects`에서 수행. 회귀 항목 TC-MEM-018(공백 사용자 이름), 019(401 응답 형식) 추가 | HseongH |
| 1.4.1 | 2026-10-09 | E2E 브라우저를 Chromium으로 바꾸고 재수행. TC-MEM-001, 039 판정 변화 없음 (Pass) | HseongH |
