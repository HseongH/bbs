---
doc_id: MEM-SDS
title: 회원·인증 설계 명세서
version: 1.2.0
status: In Review
owner: HseongH
reviewers: []
approved_date:
last_updated: 2026-10-09
related: [PRJ-SDS 1.2.0, MEM-SRS 1.1.0, MEM-QA 1.2.0]
---

# 회원·인증 설계 명세서

> 인증 흐름의 전체 그림과 보안 설정(URL 접근 규칙, CSRF)은 [프로젝트 SDS §7](../../project/sds.md#7-보안-설계)에 있다. 이 문서는 `com.board.bbs.member` 패키지와 화면의 `core/auth`를 다룬다.

## 1. 설계 개요

회원 기능은 **외부 신원(Keycloak `sub`)과 내부 식별자(`MemberId`)를 연결하는 경계**다. 다른 기능은 Keycloak이나 OIDC를 전혀 모르고 `MemberId`만 사용한다. 이 연결은 두 지점에서 일어난다.

| 시점 | 담당 | 하는 일 |
|---|---|---|
| 로그인할 때 | `BbsOidcUserService` | 회원이 없으면 만들고, realm 역할을 권한으로 매핑한다 |
| API 요청마다 | `CurrentMemberArgumentResolver` | 세션의 `sub`로 `MemberService`에서 회원을 찾아 `@CurrentMember MemberId`를 채운다 |

`SecurityConfig`(`common`)는 `OAuth2UserService<OidcUserRequest, OidcUser>` 인터페이스 타입으로 `BbsOidcUserService`를 주입받는다. 그래서 `common`은 `member`에 의존하지 않는다 ([ADR-0009](../../project/adr/0009-feature-boundaries-via-events.md)).

## 2. 구성 요소

### 2.1 도메인 (`member.domain`)

| 요소 | 종류 | 책임 |
|---|---|---|
| `Member` | 애그리게이트 루트 | `subject`, 닉네임, 이메일 보유. `provision()`으로 생성, `changeNickname()` (현재 호출하는 곳 없음) |
| `MemberId` | 값 객체 | 1 이상의 식별자. **다른 모든 기능이 작성자 식별자로 사용한다** |
| `Nickname` | 값 객체 | 공백 제거 후 1~50자. `truncating()`은 외부에서 받은 이름을 50자로 자른다 (두 개의 `char`로 된 문자는 가르지 않음) |

### 2.2 애플리케이션 (`member.application`)

| 요소 | 종류 | 책임 |
|---|---|---|
| `MemberService` | 서비스 | `provision(subject, nickname, email) → MemberId`: 있으면 기존 식별자, 없으면 생성. `getIdBySubject(subject) → MemberId`, `getById(MemberId) → Member`: 없으면 `MEMBER_NOT_FOUND` |
| `MemberRepository` | 아웃바운드 포트 | `findBySubject`, `loadById` (없으면 `MEMBER_NOT_FOUND`), `saveIfAbsent` (같은 `subject`가 있으면 기존 회원 반환. 어댑터는 `INSERT ... ON CONFLICT (subject) DO NOTHING` 후 조회) |

인바운드 포트는 두지 않는다. 인증 어댑터 두 개와 컨트롤러가 모두 `MemberService`를 직접 사용한다 ([ADR-0010](../../project/adr/0010-drop-inbound-ports.md)).

### 2.3 어댑터 (`member.adapter`)

| 요소 | 위치 | 책임 |
|---|---|---|
| `BbsOidcUserService` | `in/security` | OIDC 사용자 로드 → 회원 프로비저닝 → realm 역할 매핑 |
| `CurrentMemberArgumentResolver` | `in/web` | `@CurrentMember MemberId` 파라미터 해석 |
| `CurrentMemberWebConfig` | `in/web` | 위 해석기를 스프링 MVC에 등록 |
| `MemberController` | `in/web` | `GET /api/members/me` |
| `MemberResponse` | `in/web/dto` | `{id, nickname, email}` |
| `MemberJpaEntity`, `MemberJpaRepository`, `MemberMapper`, `MemberPersistenceAdapter` | `out/persistence` | 영속성 |

`@CurrentMember` 어노테이션 자체는 모든 기능의 컨트롤러가 쓰므로 `common.security`에 있다.

## 3. 처리 흐름

### 3.1 로그인 (MEM-FR-001, 002, 003)

```
Spring Security oauth2Login → BbsOidcUserService.loadUser(request)
  ├─ super.loadUser()                                  ID 토큰 검증, UserInfo 조회
  ├─ subject  = oidcUser.sub
  │  nickname = preferred_username ?: sub
  │  email    = email ?: "{sub}@unknown.local"
  ├─ MemberService.provision(subject, nickname, email)           [트랜잭션]
  │    findBySubject(subject) 있음 → 기존 MemberId (정보 갱신 안 함)
  │                         없음 → saveIfAbsent(Member.provision(subject, Nickname.truncating(nickname), email))
  │                                동시에 다른 요청이 먼저 저장했으면 그 회원을 반환
  └─ 권한 = 기존 권한 + realm_access.roles.map("ROLE_" + it)
     → DefaultOidcUser(권한, idToken, userInfo, nameAttribute="preferred_username")
```

### 3.2 현재 회원 해석 (MEM-FR-006)

```
CurrentMemberArgumentResolver.resolveArgument(@CurrentMember(required) MemberId)
  principal is OidcUser → MemberService.getIdBySubject(sub)       [읽기 전용 트랜잭션]
                            있음 → MemberId
                            없음 → MEMBER_NOT_FOUND (404)
  그 밖 (미인증)         → required ? UNAUTHENTICATED (401) : null
```

요청마다 `subject`로 회원을 조회한다. 세션에 `MemberId`를 저장하지 않는 이유는, 세션 직렬화 형식에 도메인 타입을 넣지 않고 회원 데이터의 변경(삭제 등)을 즉시 반영하기 위해서다.

## 4. 인터페이스 설계

```jsonc
// GET /api/members/me → MemberResponse
{ "id": 7, "nickname": "tester", "email": "tester@example.com" }
```

| 상황 | 상태 | code |
|---|---|---|
| 미인증 | 401 | `UNAUTHENTICATED` |
| 인증되었으나 로컬 회원 없음 | 404 | `MEMBER_NOT_FOUND` |

## 5. 데이터 설계

[프로젝트 SDS §5](../../project/sds.md#5-데이터-관점)의 `member` 테이블(`V1__create_member.sql`)을 사용한다. `uk_member_subject`가 같은 사용자의 중복 생성을 최종적으로 막는다.

## 6. 화면 설계 (`frontend/src/app/core/auth`)

| 요소 | 책임 |
|---|---|
| `current-member.store.ts` | `/api/members/me` 결과 보유. `401`은 "비로그인"으로 해석하고 로그인으로 이동하지 않는다 (`SKIP_LOGIN_REDIRECT`). `whenSettled()`로 조회 완료를 기다릴 수 있다 |
| `auth.interceptor.ts` | `401`을 받으면 현재 경로를 `sessionStorage`에 저장하고 로그인으로 이동 |
| `auth.guard.ts` | 회원 조회가 끝날 때까지 기다린 뒤, 미인증이면 로그인으로 이동 |
| `app.ts` | 상단 영역에 로그인 상태 표시 |
| `main.ts` | 앱 시작 시 저장된 경로를 한 번 꺼내(`takeRedirectPath`) 로그인 전 위치로 복귀 |

`sessionStorage` 접근이 막힌 환경(시크릿 모드 등)에서는 복귀 경로 없이 로그인만 진행한다.

## 7. 설계 결정

| 결정 | 이유 | 대안과 기각 이유 |
|---|---|---|
| 최초 로그인 시 자동 생성 (JIT provisioning) | 별도 가입 절차 없이 Keycloak 계정만으로 사용 | 가입 화면: 같은 정보를 두 번 입력하게 된다 |
| 다른 기능에는 `MemberId`만 노출 | 인증 방식이 바뀌어도 다른 기능이 영향을 받지 않는다 | `OidcUser`를 컨트롤러에서 직접 사용: 모든 기능이 OIDC에 묶인다 |
| 요청마다 `subject`로 회원 조회 | 세션에 도메인 타입을 넣지 않는다 | 세션에 `MemberId` 캐시: 조회 1회를 줄이지만 세션 직렬화와 무효화 문제가 생긴다 |
| 로그인 시 닉네임·이메일을 갱신하지 않음 | 구현 단순화 | 매 로그인 동기화: [SRS MEM-OPEN-01](srs.md#6-미결-사항)로 남겨 둠 |

## 8. 요구사항 대응표

| 요구사항 | 설계 요소 | 자동 테스트 |
|---|---|---|
| MEM-FR-001 | `SecurityConfig.oauth2Login`, `BbsOidcUserService` | 없음 (E2E `auth.setup`이 실제 로그인 수행) |
| MEM-FR-002 | `MemberService.provision`, `Nickname.truncating`, `uk_member_subject` | `MemberServiceTest`, `MemberPersistenceAdapterTest`, `MemberTest` |
| MEM-NFR-001 | `MemberRepository.saveIfAbsent` (`ON CONFLICT`) | `MemberProvisioningConcurrencyTest` |
| MEM-FR-003 | `BbsOidcUserService.realmRoles` | 없음 |
| MEM-FR-004 | `SecurityConfig.logout` | `SecurityCsrfTest` |
| MEM-FR-005 | `MemberController`, `MemberService.getById` | `MemberControllerTest`, `MemberServiceTest` |
| MEM-FR-006 | `CurrentMemberArgumentResolver` | `MemberControllerTest`, 각 기능의 컨트롤러 테스트 (간접) |
| MEM-FR-020~023 | `core/auth`, `app.ts`, `main.ts` | `app.spec`, `auth.interceptor.spec`, `auth.guard.spec`, `current-member.store.spec`, E2E `logout.spec` |

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|---|---|---|---|
| 1.0.0 | 2026-10-09 | 최초 작성 (`main` e96a878 기준으로 역작성) | HseongH |
| 1.1.0 | 2026-10-09 | 인바운드 포트 제거와 저장소 포트 통합 반영 (ADR-0010). 현재 회원 해석이 서비스를 거치도록 바뀐 점 반영 | HseongH |
| 1.2.0 | 2026-10-09 | 회원 저장을 `saveIfAbsent`로 바꿔 동시 최초 로그인을 처리하고, 긴 닉네임을 자르는 `Nickname.truncating` 추가 | HseongH |
