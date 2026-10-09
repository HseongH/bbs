---
doc_id: MEM-SDS
title: 회원·인증 설계 명세서
version: 1.3.0
status: In Review
owner: HseongH
reviewers: []
approved_date:
last_updated: 2026-10-09
related: [PRJ-SDS 1.3.0, MEM-SRS 1.1.0, MEM-QA 1.3.0, PRJ-CS 1.0.0]
---

# 회원·인증 설계 명세서

> 인증 흐름의 전체 그림과 보안 설정(URL 접근 규칙, CSRF)은 [프로젝트 SDS §7](../../project/sds.md#7-보안-설계)에 있다. 이 문서는 `com.board.bbs.member` 패키지와 화면의 `core/auth`를 다룬다.
>
> **이 문서가 다루지 않는 것:** 메서드 시그니처(코드가 기준), 요청·응답 필드(OpenAPI 문서가 기준), 테스트 목록([QA 체크리스트](qa-checklist.md)가 기준). 작성 기준은 [문서 체계 §8](../../README.md#8-sds-작성-기준)에 있다.

## 1. 설계 개요

회원 기능은 **외부 신원(Keycloak `sub`)과 내부 식별자(`MemberId`)를 연결하는 경계**다. 다른 기능은 Keycloak이나 OIDC를 전혀 모르고 `MemberId`만 사용한다. 이 연결은 두 지점에서 일어난다.

| 시점 | 담당 | 하는 일 |
|---|---|---|
| 로그인할 때 | `BbsOidcUserService` | 회원이 없으면 만들고, realm 역할을 권한으로 매핑한다 |
| API 요청마다 | `CurrentMemberArgumentResolver` | 세션의 `sub`로 회원을 찾아 `@CurrentMember MemberId` 파라미터를 채운다 |

`common`의 보안 설정은 OIDC 사용자 서비스를 **인터페이스 타입으로** 주입받는다. 그래서 `common`은 `member`에 의존하지 않는다 ([ADR-0009](../../project/adr/0009-feature-boundaries-via-events.md)).

## 2. 구성 요소

### 2.1 도메인 (`member.domain`)

| 요소 | 종류 | 책임 |
|---|---|---|
| `Member` | 애그리게이트 루트 | 외부 신원(`subject`), 닉네임, 이메일을 보유한다 |
| `MemberId` | 값 객체 | 회원 식별자. **다른 모든 기능이 작성자 식별자로 사용한다** |
| `Nickname` | 값 객체 | 닉네임 규칙. 외부에서 받은 이름은 거부하지 않고 최대 길이로 자르는 생성 경로가 따로 있다 ([SRS §2](srs.md#2-데이터-항목)) |

닉네임을 만드는 경로가 둘인 이유: 사용자가 직접 입력하는 값은 규칙을 어기면 거부해야 하지만, Keycloak에서 받은 이름은 사용자가 고칠 수 없으므로 거부하면 로그인 자체가 막힌다 (MEM-OPEN-02 해결).

### 2.2 애플리케이션 (`member.application`)

| 요소 | 종류 | 책임 |
|---|---|---|
| `MemberService` | 서비스 | 회원 프로비저닝(있으면 기존 회원, 없으면 생성), `subject`나 식별자로 회원 조회 |
| `MemberRepository` | 아웃바운드 포트 | 회원 조회와 "없을 때만 저장". 동시에 같은 사용자를 저장해도 실패하지 않는다 |

인바운드 포트는 두지 않는다. 인증 어댑터 두 개와 컨트롤러가 모두 `MemberService`를 직접 사용한다 ([ADR-0010](../../project/adr/0010-drop-inbound-ports.md)).

### 2.3 어댑터 (`member.adapter`)

| 요소 | 위치 | 책임 |
|---|---|---|
| `BbsOidcUserService` | `in/security` | OIDC 사용자 로드 → 회원 프로비저닝 → realm 역할 매핑 |
| `CurrentMemberArgumentResolver`, `CurrentMemberWebConfig` | `in/web` | `@CurrentMember` 파라미터 해석과 등록 |
| `MemberController` | `in/web` | 내 정보 조회 |
| `MemberPersistenceAdapter` | `out/persistence` | `MemberRepository` 구현. 중복 판정은 데이터베이스가 한다 |

`@CurrentMember` 어노테이션 자체는 모든 기능의 컨트롤러가 쓰므로 `common`에 있다.

## 3. 처리 흐름

### 3.1 로그인 (MEM-FR-001, 002, 003)

1. 스프링 보안이 인가 코드를 토큰으로 교환하고 ID 토큰을 검증한다.
2. `BbsOidcUserService`가 토큰에서 `sub`, 사용자 이름, 이메일을 꺼낸다. 사용자 이름이 없으면 `sub`를, 이메일이 없으면 `sub`로 만든 대체 주소를 쓴다.
3. 회원을 프로비저닝한다 (하나의 트랜잭션).
   - `sub`로 찾으면 기존 회원을 쓴다. **닉네임과 이메일은 갱신하지 않는다** ([SRS MEM-OPEN-01](srs.md#6-미결-사항)).
   - 없으면 "없을 때만 저장"한다. 같은 사용자가 동시에 처음 로그인해서 다른 요청이 먼저 저장했다면, 그 회원을 그대로 쓴다.
4. realm 역할을 `ROLE_` 접두사의 권한으로 매핑해 인증 정보에 더한다.

3단계의 "없을 때만 저장"은 데이터베이스의 유니크 제약과 `ON CONFLICT DO NOTHING`, 이어지는 재조회로 구현한다. 조회 후 저장 방식은 동시 요청 두 개가 함께 조회를 통과해서 한쪽이 제약 위반으로 실패했다 ([ADR-0005](../../project/adr/0005-database-decides-duplicates.md)와 같은 방식, MEM-NFR-001).

### 3.2 현재 회원 해석 (MEM-FR-006)

| 상황 | 결과 |
|---|---|
| 로그인 상태이고 로컬 회원이 있음 | 회원 식별자 |
| 로그인 상태인데 로컬 회원이 없음 | `404 MEMBER_NOT_FOUND` |
| 미인증, 파라미터가 필수 | `401 UNAUTHENTICATED` |
| 미인증, 파라미터가 선택 | `null` |

요청마다 `sub`로 회원을 조회한다. 세션에 회원 식별자를 저장하지 않는 이유는, 세션 직렬화 형식에 도메인 타입을 넣지 않고 회원 데이터의 변경을 즉시 반영하기 위해서다.

## 4. 인터페이스 설계

엔드포인트 목록은 [SRS §5](srs.md#5-인터페이스)에, 응답 필드는 OpenAPI 문서에 있다. 회원 기능이 쓰는 오류 코드는 `UNAUTHENTICATED`, `MEMBER_NOT_FOUND`다.

## 5. 데이터 설계

[프로젝트 SDS §5](../../project/sds.md#5-데이터-관점)의 `member` 테이블을 사용한다. `subject`의 유니크 제약이 같은 사용자의 중복 생성을 최종적으로 막고, 동시 최초 로그인에서 중복을 판정하는 기준이 된다.

## 6. 화면 설계 (`frontend/src/app/core/auth`)

| 요소 | 책임 |
|---|---|
| 현재 회원 스토어 | 내 정보 조회 결과를 보유한다. `401`은 "비로그인"으로 해석하고 로그인으로 이동하지 않는다. 조회 완료를 기다릴 수 있게 한다 |
| 인증 인터셉터 | `401`을 받으면 현재 경로를 기억하고 로그인으로 이동한다. 로그인 여부 확인 요청은 제외한다 |
| 인증 가드 | 회원 조회가 끝날 때까지 기다린 뒤, 미인증이면 로그인으로 이동한다 |
| 앱 시작 (`main.ts`) | 기억해 둔 경로를 한 번 꺼내 로그인 전 위치로 돌아간다 |

브라우저 저장소 접근이 막힌 환경(시크릿 모드 등)에서는 복귀 경로 없이 로그인만 진행한다.

## 7. 설계 결정

| 결정 | 이유 | 대안과 기각 이유 |
|---|---|---|
| 최초 로그인 시 자동 생성 (JIT provisioning) | 별도 가입 절차 없이 Keycloak 계정만으로 사용 | 가입 화면: 같은 정보를 두 번 입력하게 된다 |
| 다른 기능에는 `MemberId`만 노출 | 인증 방식이 바뀌어도 다른 기능이 영향을 받지 않는다 | `OidcUser`를 컨트롤러에서 직접 사용: 모든 기능이 OIDC에 묶인다 |
| 회원 생성의 중복 판정을 데이터베이스에 맡김 | 동시 최초 로그인이 모두 성공한다 | 조회 후 저장: 동시 요청 중 일부가 제약 위반으로 실패 (MEM-OPEN-03) |
| 외부 이름이 길면 잘라서 저장 | 사용자가 고칠 수 없는 값 때문에 로그인이 막히지 않는다 | 로그인 거부: MEM-OPEN-02의 원인. 제한 늘리기: 화면 표시 규칙이 함께 바뀐다 |
| 요청마다 `subject`로 회원 조회 | 세션에 도메인 타입을 넣지 않는다 | 세션에 회원 식별자 캐시: 조회 1회를 줄이지만 세션 직렬화와 무효화 문제가 생긴다 |
| 로그인 시 닉네임·이메일을 갱신하지 않음 | 구현 단순화 | 매 로그인 동기화: [SRS MEM-OPEN-01](srs.md#6-미결-사항)로 남겨 둠 |

## 8. 요구사항 대응표

요구사항을 어떤 설계 요소가 맡는지 보여 준다. 검증하는 테스트는 [QA 체크리스트](qa-checklist.md)에 있다.

| 요구사항 | 설계 요소 |
|---|---|
| MEM-FR-001, 003 | 보안 설정의 OIDC 로그인, `BbsOidcUserService` (§3.1) |
| MEM-FR-002, MEM-NFR-001 | `MemberService`, `MemberRepository`의 "없을 때만 저장", `Nickname` (§3.1) |
| MEM-FR-004 | 보안 설정의 로그아웃 처리 |
| MEM-FR-005 | `MemberController`, `MemberService` |
| MEM-FR-006 | `CurrentMemberArgumentResolver` (§3.2) |
| MEM-FR-020~023 | `core/auth`, `main.ts` (§6) |

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|---|---|---|---|
| 1.0.0 | 2026-10-09 | 최초 작성 (`main` e96a878 기준으로 역작성) | HseongH |
| 1.1.0 | 2026-10-09 | 인바운드 포트 제거와 저장소 포트 통합 반영 (ADR-0010). 현재 회원 해석이 서비스를 거치도록 바뀐 점 반영 | HseongH |
| 1.2.0 | 2026-10-09 | 회원 저장을 `saveIfAbsent`로 바꿔 동시 최초 로그인을 처리하고, 긴 닉네임을 자르는 `Nickname.truncating` 추가 | HseongH |
| 1.3.0 | 2026-10-09 | 세밀도 조정: 메서드 시그니처, 의사 코드, 응답 예시, 테스트 목록을 빼고 책임·흐름·결정 중심으로 재작성. 결함 수정(MEM-OPEN-02, 03)의 설계 결정을 §7에 추가 | HseongH |
