---
doc_id: MEM-QA
title: 회원·인증 QA 체크리스트
version: 1.6.0
status: In Review
owner: HseongH
reviewers: []
approved_date:
last_updated: 2026-10-10
related: [PRJ-QA 1.4.0, MEM-SRS 1.3.0, MEM-SDS 2.0.0]
---

# 회원·인증 QA 체크리스트

> 작성 규칙과 판정 기준은 [공통 QA 기준](../../project/qa-standards.md)을 따른다. 공통 보안 요구사항(COM-NFR-002, 003, 007~009, 021)의 항목도 이 기능에서 함께 검증한다. 이 기능은 auth와 board에 걸쳐 있으므로 검증 수단에 서비스를 함께 적는다.

## 1. 수행 정보

| 항목 | 값 |
|---|---|
| 대상 커밋 | `feature/auth-service` 브랜치의 7ad3046 (이전 수행: `fix/review-defects`, e96a878, f46a99c, 85cce67) |
| 수행일 | 2026-10-10 |
| 백엔드 자동 검증 | 루트에서 `./gradlew test --rerun check` 성공 (최종 리뷰 반영 후 라이브러리 8개, auth 54개, board 140개. 실패 0, 오류 0, 건너뜀 0. 이 문서가 인용한 테스트가 모두 이번 실행 결과에 Pass로 있는 것을 대조함) |
| 화면 자동 검증 | `pnpm verify` 성공 (린트, 타입 검사 통과, 테스트 파일 12개·테스트 42개 통과). board 실행 중 `pnpm gen:api` 후 생성 타입 변화 없음 |
| E2E | `pnpm e2e` 성공 (Chromium, 9개 통과: 로그인 준비, 글·댓글·좋아요, 검색 URL 유지, 진입점 5개, 로그아웃). `deploy/compose.yaml`의 컨테이너(진입점 포함)와 auth·board의 `bootRun`을 띄우고 진입점 `localhost:8000`으로 2026-10-10 수행 |
| 수동 검증 | 실행하지 않음 (N/T) |

## 2. 테스트 항목

### 2.1 인증과 회원 생성

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-MEM-001 | MEM-FR-001 | 진입점으로 접속해 시험 계정(`tester`)으로 실제 Keycloak 로그인 | 상단에 닉네임 표시 | E2E `auth.setup: tester로 로그인한다` (진입점 경유) | Pass |
| TC-MEM-002 | MEM-FR-002 | 처음 보는 사용자 식별자로 프로비저닝 | 회원 생성 | board `MemberServiceTest#처음_보는_사용자는_회원이_생성된다` | Pass |
| TC-MEM-003 | MEM-FR-002 | 같은 사용자 식별자로 다시 프로비저닝 | 중복 생성 없음, 같은 식별자 | board `MemberServiceTest#이미_가입한_회원은_중복_생성되지_않는다` | Pass |
| TC-MEM-004 | MEM-FR-002 | 신규 회원 객체 | 식별자 없음 | `MemberTest#신규_회원은_식별자가_없는_상태로_생성된다` | Pass |
| TC-MEM-005 | MEM-FR-002 | 닉네임 경계값: 공백, 51자 | 거부 | `MemberTest#닉네임은_비어있을_수_없다`, `#닉네임은_50자를_넘을_수_없다` | Pass |
| TC-MEM-006 | MEM-FR-002 | 회원 식별자 0 이하 | 거부 | `MemberTest#회원_식별자는_양수여야_한다` | Pass |
| TC-MEM-007 | MEM-FR-002 | `subject`로 회원 찾기, 식별자로 읽기 | 저장한 값과 같음 | `MemberPersistenceAdapterTest#사용자_식별자로_회원을_찾을_수_있다`, `#저장한_회원을_식별자로_읽을_수_있다` | Pass |
| TC-MEM-008 | MEM-FR-003 | `ADMIN` 역할을 담은 내부 토큰으로 요청 | board에서 `ROLE_ADMIN` 권한을 가짐 | board `InternalTokenAuthenticationConverterTest#역할은_ROLE_접두사_권한이_된다`, `PostControllerTest#관리자는_다른_사람의_글을_삭제할_수_있다`(ADMIN 역할 토큰) | Pass |
| TC-MEM-009 | MEM-FR-004, COM-NFR-003 | CSRF 토큰과 함께 `POST /logout` | `204`, 세션 무효화. 이후 ForwardAuth 판정에 토큰이 붙지 않음 | auth `LogoutTest#로그아웃은_토큰과_함께_POST하면_204다`, E2E `logout.spec: 로그아웃하면 비로그인 상태로 돌아간다`(로그아웃 뒤 화면이 비로그인으로 돌아옴. 토큰이 붙지 않는 것을 헤더로 직접 확인하는 자동 테스트는 없음) | Pass |
| TC-MEM-010 | COM-NFR-003 | 로그인 상태에서 CSRF 토큰 없이 상태 변경 요청을 ForwardAuth로 판정 (원래 메서드 `POST`) | `403 ACCESS_DENIED` ProblemDetail | auth `ForwardAuthTest#로그인했어도_CSRF_토큰_없는_쓰기는_403이다` | Pass |
| TC-MEM-011 | COM-NFR-003 | 조회 요청을 ForwardAuth로 판정 (원래 메서드 `GET`) | 토큰 없이 허용, 응답에 `XSRF-TOKEN` 쿠키 | auth `ForwardAuthTest#조회는_CSRF_쿠키를_발급한다`. 진입점 경유 전달은 `curl -si localhost:8000/api/posts`의 `Set-Cookie: XSRF-TOKEN`으로 확인 | Pass |
| TC-MEM-012 | MEM-FR-002 | 50자를 넘는 닉네임으로 처음 프로비저닝 (회귀, MEM-OPEN-02) | 회원 생성 성공, 닉네임은 50자로 잘림. 두 개의 `char`로 된 문자는 가르지 않음 | board `MemberServiceTest#닉네임이_50자를_넘어도_회원을_만들_수_있다`, `MemberTest#외부에서_받은_닉네임이_50자를_넘으면_50자로_자른다`, `#닉네임을_자를_때_두_단위로_된_문자를_가르지_않는다` | Pass |
| TC-MEM-013 | MEM-NFR-001 | 같은 사용자의 첫 요청 16개가 동시에 도착 (회귀, MEM-OPEN-03) | 모두 성공, 같은 식별자, 회원 1명 | board `MemberProvisioningConcurrencyTest#같은_사용자의_첫_요청이_동시에_와도_모두_성공하고_회원은_하나다` | Pass |
| TC-MEM-014 | COM-NFR-021 | board에 토큰 없이 `/actuator/health`, `/actuator/info` 조회 | `200` | board `ActuatorAccessTest#상태_확인과_정보는_인증_없이_볼_수_있다` | Pass |
| TC-MEM-015 | COM-NFR-021 | board에 토큰 없이 `/actuator/metrics` 조회 (회귀, OPEN-04) | `401` | board `ActuatorAccessTest#지표는_인증_없이_볼_수_없다` | Pass |
| TC-MEM-016 | COM-NFR-021 | board에 `USER` 역할 토큰으로 `/actuator/metrics` 조회 (회귀, OPEN-04) | `403 ACCESS_DENIED` ProblemDetail | board `ActuatorAccessTest#지표는_일반_회원이_볼_수_없다` | Pass |
| TC-MEM-017 | COM-NFR-021 | board에 `ADMIN` 역할 토큰으로 `/actuator/metrics` 조회 | `200` | board `ActuatorAccessTest#지표는_관리자가_볼_수_있다` | Pass |
| TC-MEM-018 | MEM-FR-002 | Keycloak 사용자 이름·이메일이 없거나 공백뿐인 사용자로 로그인 (회귀) | auth의 사용자 정보에서 닉네임은 `sub`, 이메일은 `{sub}@unknown.local` | auth `LoginUsersTest#사용자_이름이나_이메일이_공백뿐이면_subject로_대신한다`, `#사용자_이름이나_이메일이_없으면_subject로_대신한다` | Pass |
| TC-MEM-019 | COM-NFR-002, COM-IF-003 | board에 토큰 없이 인증이 필요한 API 요청, 경로에 따옴표가 있는 요청 (회귀) | `401`, ProblemDetail 필드가 모두 있고 올바른 JSON. `instance`는 허용되지 않는 문자만 인코딩 | board `UnauthenticatedResponseTest`(잘못된 토큰으로 진입점 응답 확인), `ProblemDetailsTest` | Pass |

### 2.2 회원 정보와 현재 회원

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-MEM-020 | MEM-FR-005 | 내부 토큰으로 내 정보 조회 | 식별자, 닉네임, 이메일 | board `MemberControllerTest#로그인한_회원의_정보를_반환한다` | Pass |
| TC-MEM-021 | MEM-FR-005, COM-NFR-002 | 토큰 없이 board에 내 정보 조회 | `401 UNAUTHENTICATED` (리다이렉트 아님) | board `MemberControllerTest#미인증_요청은_401을_반환한다` | Pass |
| TC-MEM-022 | MEM-FR-006 | 처음 보는 사용자의 내부 토큰으로 `@CurrentMember`가 필수인 API 호출 | 토큰의 클레임으로 회원이 생기고 요청 성공 (1.4.x까지는 `404 MEMBER_NOT_FOUND`) | board `MemberControllerTest#처음_보는_사용자는_첫_요청에서_회원이_생긴다` | Pass |
| TC-MEM-023 | MEM-FR-006 | 선택적 `@CurrentMember`에 비로그인 요청 | `null`로 처리되어 요청 성공 | `PostControllerTest#목록은_비로그인으로도_조회할_수_있다`(간접), 상세 조회 비로그인 경로는 자동 테스트 없음 | N/T |
| TC-MEM-024 | COM-NFR-002 | 진입점으로 비로그인 상태에서 인증이 필요한 API 요청 | 화면이 아니라 auth의 `401` ProblemDetail | E2E `gateway.spec: 비로그인 쓰기는 진입점에서 401 ProblemDetail이다` | Pass |

### 2.3 auth 판정과 내부 토큰

| TC | 요구사항 | 시나리오 | 기대 결과 | 검증 수단 | 판정 |
|---|---|---|---|---|---|
| TC-MEM-040 | COM-NFR-002 | 비로그인 상태의 인증 필요 요청을 ForwardAuth로 판정, 원래 경로에 따옴표 포함 | `401 UNAUTHENTICATED` ProblemDetail. `instance`는 원래 요청의 경로 | auth `ForwardAuthTest#비로그인_쓰기는_401_ProblemDetail이다` | Pass |
| TC-MEM-041 | COM-NFR-002 | CSRF 실패로 거부된 요청의 응답 | `403 ACCESS_DENIED` ProblemDetail, board와 같은 필드 | auth `ForwardAuthTest#로그인했어도_CSRF_토큰_없는_쓰기는_403이다`, `LogoutTest#토큰_없는_로그아웃은_403_ProblemDetail이다` | Pass |
| TC-MEM-042 | COM-NFR-007 | 다른 키로 서명한 토큰으로 board API 요청 | `401 UNAUTHENTICATED` | board `InternalTokenValidationTest#다른_키로_서명한_토큰은_401이다`, 라이브러리 `InternalTokenContractTest#다른_키로_서명한_토큰은_거부한다` | Pass |
| TC-MEM-043 | COM-NFR-007 | 만료된 토큰으로 board API 요청 | `401 UNAUTHENTICATED` | board `InternalTokenValidationTest#만료된_토큰은_401이다`, 라이브러리 `InternalTokenContractTest#만료된_토큰은_거부한다`, `#만료된_지_얼마_안_된_토큰도_거부한다`, `#만료_시각이_없는_토큰은_거부한다` | Pass |
| TC-MEM-044 | COM-NFR-007 | 발급자나 대상(`aud`)이 다른 토큰으로 board API 요청 | `401 UNAUTHENTICATED` | board `InternalTokenValidationTest#발급자나_대상이_다른_토큰은_401이다`, 라이브러리 `InternalTokenContractTest#발급자가_다른_토큰은_거부한다`, `#대상이_다른_토큰은_거부한다` | Pass |
| TC-MEM-045 | COM-NFR-008 | 로그인 상태의 요청을 ForwardAuth로 판정 | `200`과 `Authorization: Bearer`. 토큰의 클레임은 발급자, 대상 `bbs`, `sub`, `nickname`, `email`, `roles`이고 만료는 발급 후 60초 | auth `ForwardAuthTest#로그인한_공개_조회는_내부_토큰을_붙여_통과한다`, 라이브러리 `InternalTokenContractTest#발급한_토큰을_검증하면_같은_사용자가_나온다` | Pass |
| TC-MEM-046 | COM-NFR-008, MEM-FR-003 | realm 역할이 `USER`, `ADMIN`, `offline_access`, `default-roles-bbs`인 사용자로 로그인 | 세션과 내부 토큰의 역할은 `USER`, `ADMIN`뿐 | auth `BbsOidcUserServiceTest#realm_역할_중_bbs가_정의한_역할만_권한이_된다`, `LoginUsersTest#역할_권한만_역할로_옮긴다` | Pass |
| TC-MEM-047 | COM-NFR-008 | 서명 키 설정 없이 `local`이 아닌 프로필로 auth 시작 | 시작 실패. `local` 프로필에서는 임시 키로 시작하고 경고 기록 | auth `SigningKeyConfigTest`(5개: 키 없음, local 임시 키, PEM 로드, 2048비트 미만 거부, PKCS#8이 아닌 형식 거부) | Pass |
| TC-MEM-048 | COM-NFR-009 | 원래 경로가 `/api/posts/%2e%2e/members/me`, `/api/posts%2F1`, `/api/posts;x=1`, `/api/posts/..%5C` 같은 모호한 값 | `400 INVALID_REQUEST`, 판정하지 않음 | auth `ForwardedRequestTest#모호한_경로는_거부한다`(14가지, 인코딩된 세미콜론 포함), `#표준_대문자_메서드가_아니면_거부한다`, `ForwardAuthTest#모호한_원래_경로는_400이다`, `#전달_헤더_없이_직접_호출하면_400이다` | Pass |
| TC-MEM-049 | COM-NFR-021 | auth에 비로그인으로 `/actuator/health`, `/actuator/metrics` 조회 | health는 `200`, metrics는 `401` | auth `ActuatorAccessTest`(4개) | Pass |
| TC-MEM-050 | MEM-FR-001, COM-NFR-007 | 비로그인 상태의 공개 조회(`GET /api/posts`)와 로그인 상태의 공개 조회를 ForwardAuth로 판정 | 둘 다 `200`. 비로그인은 `Authorization` 없음, 로그인은 내부 토큰 있음 | auth `ForwardAuthTest#비로그인_공개_조회는_토큰_없이_통과한다`, `#로그인한_공개_조회는_내부_토큰을_붙여_통과한다`, `#세션이_없어진_브라우저의_공개_조회는_200이다` | Pass |
| TC-MEM-051 | COM-NFR-007 | auth가 발급한 토큰을 board의 검증 설정으로 검증 (서비스 간 계약) | 검증 성공, board가 같은 사용자 식별자·닉네임·이메일·권한을 읽음 | 라이브러리 `InternalTokenContractTest#발급한_토큰을_검증하면_같은_사용자가_나온다`(board가 쓰는 검증기로 검증), board `InternalTokenAuthenticationConverterTest`(권한 변환). 실제 연동은 E2E 9개 | Pass |
| TC-MEM-052 | COM-NFR-007 | ForwardAuth로 `/api`가 아닌 원래 경로를 판정 | 거부 (진입점 설정 오류로 간주) | auth `ForwardAuthTest#api가_아닌_원래_경로는_거부한다` | Pass |

### 2.4 화면

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
| MEM-FR-001 | 001, 050 | E2E, auth 테스트 |
| MEM-FR-002 | 002~007, 012, 018, 022 | 완전 |
| MEM-FR-003 | 008, 046 | 완전 (1.4.x까지는 없음) |
| MEM-FR-004 | 009 | 완전 |
| MEM-FR-005 | 020, 021 | 완전 |
| MEM-FR-006 | 022, 023 | 부분 |
| MEM-FR-020~023 | 030~039 | 화면 단위 테스트 대부분, 로그아웃 흐름은 E2E. 로그인 후 원래 경로 복귀(038)는 자동 테스트 없음 |
| MEM-NFR-001 | 013 | 완전 |
| COM-NFR-021 | 014~017, 049 | 완전 |
| COM-NFR-002 | 019, 021, 024, 040, 041 | 완전 |
| COM-NFR-003 | 009~011 | 완전 |
| COM-NFR-007 | 042~044, 050~052 | 완전 |
| COM-NFR-008 | 045~047 | 완전 |
| COM-NFR-009 | 048 | 완전 |

## 4. 결과 요약과 후속 조치

- 집계: 전체 47건 중 Pass 45, Fail 0, N/T 2 (자동 테스트 없음 2: TC-MEM-023, 038).
- TC-MEM-040~052는 auth 분리(ADR-0016)로 추가한 항목이고, 근거 테스트가 auth로 옮겨 가거나 토큰 기반으로 바뀐 항목은 이번 실행으로 다시 판정했다. 바뀌지 않은 항목(도메인 단위 테스트, 화면 테스트)도 같은 실행에서 다시 확인했다.
- TC-MEM-009의 "로그아웃 뒤 ForwardAuth 판정에 토큰이 붙지 않음"은 E2E에서 화면이 비로그인으로 돌아오는 것으로만 확인했다. 세션 무효화 뒤의 판정을 직접 검사하는 auth 테스트를 추가할 후보다.
- E2E 2건(TC-MEM-001, 039)은 1.2.x까지 N/T였다. E2E 브라우저를 Firefox로 바꾸고 실행해 Pass로 판정했다.
- MEM-FR-003(역할 매핑)은 1.4.x까지 자동 테스트가 없었다. auth의 역할 걸러내기(TC-MEM-046)와 board의 권한 변환(TC-MEM-008)으로 나눠 자동화한다.
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
| 1.5.0 | 2026-10-10 | auth 서비스 분리 반영 (MEM-SRS 1.3.0, ADR-0016): auth 판정과 내부 토큰 항목 TC-MEM-040~052 추가. 근거 테스트가 auth로 옮겨 가거나 토큰 기반으로 바뀌는 항목의 시나리오를 고치고 N/T로 되돌림. TC-MEM-022는 "회원 없음"이 `404`에서 자동 생성으로 바뀜. 판정은 구현 후 채운다 | HseongH |
| 1.6.0 | 2026-10-10 | `feature/auth-service`에서 수행. TC-MEM-040~052와 근거가 바뀐 항목을 실제 실행 결과로 판정 (Pass 45, N/T 2) | HseongH |
