---
doc_id: PRJ-SRS
title: 게시판(bbs) 프로젝트 요구사항 명세서
version: 1.5.0
status: In Review
owner: HseongH
reviewers: []
approved_date:
last_updated: 2026-10-09
related: [PRJ-CHARTER 1.0.0, PRJ-SDS 1.6.0, PRJ-QA 1.3.0, PRJ-QC 1.2.0]
---

# 게시판(bbs) 프로젝트 요구사항 명세서

> ISO/IEC/IEEE 29148의 SRS 구성을 따르되, 기능별 상세 요구사항은 [기능 SRS](../README.md#12-기능별-문서)로 분리했다. 이 문서는 **모든 기능에 공통으로 적용되는 요구사항**과 **기능 목록**만 담는다.

## 1. 개요

### 1.1 목적

이 문서는 게시판 시스템 전체가 만족해야 하는 요구사항을 정의한다. 기능 SRS는 이 문서의 공통 요구사항을 상속하며, 공통 요구사항과 다른 점만 기술한다.

### 1.2 범위

[Project Charter](charter.md) §3의 범위를 따른다. 시스템은 로그인한 사용자가 게시글과 댓글을 작성하고, 누구나 그것을 읽을 수 있는 웹 게시판이다.

### 1.3 용어

| 용어 | 정의 |
|---|---|
| 회원 | Keycloak에 계정이 있고 한 번 이상 로그인해서 로컬 DB에 생성된 사용자 |
| 비회원 | 로그인하지 않은 방문자 |
| 작성자 | 해당 게시글 또는 댓글을 작성한 회원 |
| 관리자 | Keycloak realm 역할 `ADMIN`을 가진 회원 |
| 원댓글 | 게시글에 직접 단 댓글 (깊이 0) |
| 대댓글 | 원댓글에 단 답글 (깊이 1) |
| 소프트 삭제 | 행을 지우지 않고 삭제 시각(`deleted_at`)만 기록하는 삭제 방식 |
| ProblemDetail | RFC 9457이 정의하는 HTTP API 오류 응답 형식 |
| 서비스 | 따로 빌드하고 배포하는 단위. 저장소의 `services/` 아래에 하나씩 둔다 (현재 `board`, `web`) |

### 1.4 참고 문서

- [Project Charter](charter.md)
- [프로젝트 SDS](sds.md)
- 개발 과정 기록: [백엔드 설계](../superpowers/specs/2026-09-18-bbs-design.md), [프론트엔드 설계](../superpowers/specs/2026-09-21-bbs-frontend-design.md), [Angular 전환 설계](../superpowers/specs/2026-09-21-angular-migration-design.md)

## 2. 전체 설명

### 2.1 시스템 구성 관점

```
브라우저 (Angular SPA, web 서비스)
   │  동일 오리진, 세션 쿠키 + CSRF 쿠키
   ▼
bbs 애플리케이션 (board 서비스, Spring Boot)  ──OIDC──▶  Keycloak
   │                    │
   ▼                    ▼
PostgreSQL            Valkey (Redis 호환: 세션, 조회수 중복 판정)
```

### 2.2 사용자 특성

| 사용자 | 할 수 있는 일 |
|---|---|
| 비회원 | 게시글 목록·상세, 댓글 목록 조회 |
| 회원 | 비회원 기능 + 게시글·댓글 작성, 본인 글·댓글 수정·삭제, 좋아요 |
| 관리자 | 회원 기능 + 다른 사람의 게시글·댓글 삭제 (수정은 불가) |

### 2.3 가정과 의존

- 사용자 계정은 Keycloak이 관리한다. 시스템은 회원 가입, 비밀번호 변경 기능을 제공하지 않는다.
- Keycloak의 `sub` 값은 사용자마다 고유하고 변하지 않는다고 가정한다.
- 시간은 서버의 UTC 시각(`Instant`)을 기준으로 기록한다.

## 3. 기능 목록

| 접두사 | 기능 | 개요 | 상세 문서 |
|---|---|---|---|
| `MEM` | 회원·인증 | Keycloak 로그인, 회원 자동 생성, 역할 매핑, 내 정보 조회, 로그아웃 | [SRS](../features/member/srs.md) |
| `PST` | 게시글 | 작성, 조회, 수정, 삭제, 목록, 검색, 조회수, 좋아요 | [SRS](../features/post/srs.md) |
| `CMT` | 댓글 | 작성, 목록, 수정, 삭제, 대댓글 1단계 | [SRS](../features/comment/srs.md) |

### 3.1 권한 매트릭스

모든 기능의 권한 규칙을 한눈에 확인하기 위한 요약이다. 상세 규칙과 근거는 각 기능 SRS에 있다.

| 행위 | 비회원 | 회원 | 작성자 | 관리자 |
|---|:-:|:-:|:-:|:-:|
| 게시글 목록·상세 조회 | O | O | O | O |
| 게시글 작성 | X | O | - | O |
| 게시글 수정 | X | X | O | X (본인 글만 O) |
| 게시글 삭제 | X | X | O | O |
| 좋아요·좋아요 취소 | X | O | O | O |
| 댓글 목록 조회 | O | O | O | O |
| 댓글 작성 | X | O | - | O |
| 댓글 수정 | X | X | O | X (본인 댓글만 O) |
| 댓글 삭제 | X | X | O | O |
| 내 정보 조회 | X | O | - | O |

## 4. 공통 비기능 요구사항

모든 기능에 적용된다. "검증" 열은 요구사항을 만족하는지 확인하는 방법이며, 자동 테스트가 있으면 테스트 이름을 적는다.

### 4.1 보안

| ID | 요구사항 | 검증 |
|---|---|---|
| COM-NFR-001 | 인증은 외부 IdP(Keycloak, OIDC)에 위임한다. 시스템은 비밀번호를 저장하거나 처리하지 않는다. | 설계 검토 ([ADR-0008](adr/0008-oidc-bff-and-redis-session.md)) |
| COM-NFR-002 | 인증이 필요한 API에 미인증 요청이 오면 로그인 페이지로 리다이렉트하지 않고 `401` ProblemDetail을 반환한다. 이 응답도 다른 오류 응답과 같은 필드(COM-IF-003)를 갖고, 요청 경로에 어떤 문자가 있어도 올바른 JSON이어야 한다. | `MemberControllerTest#미인증_요청은_401을_반환한다`, `SpaForwardingTest#미인증_API_요청은_여전히_401이다`, `UnauthenticatedResponseTest` |
| COM-NFR-003 | 상태를 바꾸는 요청(POST, PATCH, DELETE)은 CSRF 토큰이 없으면 거부한다. 조회 요청은 토큰 없이 허용하고 토큰 쿠키를 발급한다. | `SecurityCsrfTest#토큰_없는_변경_요청은_거부된다`, `CsrfCookieIssuanceTest#조회_요청은_토큰이_필요없고_토큰_쿠키를_내려준다` |
| COM-NFR-004 | 오류 응답에 스택트레이스, 예외 클래스명, SQL 같은 내부 정보를 담지 않는다. 예상하지 못한 예외는 `500 INTERNAL_ERROR`로 변환하고 서버 로그에만 기록한다. | `GlobalExceptionHandlerTest` |
| COM-NFR-005 | 소유권(작성자 여부) 검사는 도메인 객체 안에서 수행해서, 어떤 호출 경로로도 우회할 수 없어야 한다. | `PostTest`, `CommentTest`의 권한 테스트 ([ADR-0003](adr/0003-authorization-in-domain.md)) |
| COM-NFR-006 | 개발 환경의 컨테이너 포트는 기본적으로 루프백 주소(`127.0.0.1`)에만 열린다. 접속 정보와 바인딩 주소는 저장소의 파일을 고치지 않고 환경 변수로 바꿀 수 있다. | [프로젝트 QA](qa-checklist.md) TC-COM-010, 011 |

### 4.2 데이터 무결성과 동시성

| ID | 요구사항 | 검증 |
|---|---|---|
| COM-NFR-010 | 여러 요청이 동시에 같은 카운터(조회수, 좋아요 수)를 바꿔도 값을 잃지 않는다. | `PostLikeConcurrencyTest`, `PostCounterPreservationTest` ([ADR-0004](adr/0004-atomic-counter-update.md)) |
| COM-NFR-011 | 중복이 허용되지 않는 데이터(같은 회원의 같은 글 좋아요)는 데이터베이스 제약으로 최종 판정한다. | `PostLikeConcurrencyTest#동시에_좋아요를_눌러도_한_번만_반영된다` ([ADR-0005](adr/0005-database-decides-duplicates.md)) |
| COM-NFR-012 | 게시글과 댓글은 소프트 삭제한다. 삭제된 데이터는 조회·수정·삭제 대상이 되지 않으며 `404`로 응답한다. 예외: 살아 있는 대댓글이 있는 삭제된 원댓글은 댓글 목록에 자리만 남기고, 본문과 작성자는 응답에 담지 않는다 (CMT-FR-009). | `PostPersistenceAdapterTest#삭제된_게시글은_읽을_수_없다`, `CommentPersistenceAdapterTest#삭제된_댓글은_읽을_수_없다` |
| COM-NFR-013 | 도메인 규칙 중 데이터 손상으로 이어지는 규칙은 데이터베이스 제약으로 한 번 더 막는다. (댓글 깊이 `CHECK`, 좋아요 `UNIQUE`, 회원 `subject` `UNIQUE`) | 마이그레이션 스크립트 검토 |
| COM-NFR-014 | 목록 조회는 정렬 키에 식별자를 포함해서, 페이지를 넘길 때 행이 중복되거나 누락되지 않아야 한다. | `PostQueryRepositoryTest#페이지_크기를_넘으면_다음_페이지로_넘어간다`, `CommentPersistenceAdapterTest#원댓글_목록은_작성_순서이고_원댓글_수로_페이지를_나눈다` |

### 4.3 확장성과 운영

| ID | 요구사항 | 검증 |
|---|---|---|
| COM-NFR-020 | 애플리케이션 인스턴스는 상태를 갖지 않는다. 세션은 Valkey(Redis 호환)에 저장하며 30분 동안 요청이 없으면 만료된다. | 설정 검토 (`application.yml`의 `spring.session`) |
| COM-NFR-021 | 상태 확인(`/actuator/health`), 정보(`/actuator/info`), 지표(`/actuator/metrics`)를 노출한다. health와 info는 인증 없이 접근할 수 있고, 지표를 비롯한 나머지 액추에이터 경로는 관리자만 접근할 수 있다. | `ActuatorAccessTest`, `SpaForwardingTest#액추에이터_경로는_포워딩되지_않는다` |
| COM-NFR-022 | 종료 신호를 받으면 처리 중인 요청을 끝낸 뒤 종료한다 (graceful shutdown). | 설정 검토 |
| COM-NFR-023 | 로그에 trace ID와 span ID를 포함해서 요청 단위로 추적할 수 있어야 한다. | 설정 검토 (`logging.pattern.correlation`) |

### 4.4 유지보수성

| ID | 요구사항 | 검증 |
|---|---|---|
| COM-NFR-030 | 계층 사이의 의존 방향과 기능 사이의 경계를 자동 테스트로 강제한다. | `HexagonalArchitectureTest`, `FeatureBoundaryTest` |
| COM-NFR-031 | 코드 포맷, 정적 분석, null 안정성, 테스트, 커버리지 기준 중 하나라도 위반하면 빌드가 실패한다. | `./gradlew check`, 서비스별 CI (`.github/workflows/`), [프로젝트 QA](qa-checklist.md) TC-COM-001~003 |
| COM-NFR-032 | 백엔드 API가 바뀌면 프론트엔드 타입 검사가 실패해야 한다 (API 계약의 컴파일 시점 검증). | `pnpm gen:api` 후 `pnpm typecheck`, [프로젝트 QA](qa-checklist.md) TC-COM-015 |
| COM-NFR-033 | 모든 서비스는 변경이 PR에 올라오면 그 서비스의 검증이 자동으로 실행된다. Java 서비스 공통 빌드 파일이 바뀌면 모든 Java 서비스가 검증된다. 관계없는 서비스의 검증은 실행하지 않는다. | [프로젝트 QA](qa-checklist.md) TC-COM-005~007 ([ADR-0014](adr/0014-monorepo-with-gradle-convention-plugins.md)) |
| COM-NFR-034 | 모든 Java 서비스는 공통 빌드 규칙으로 같은 품질 기준(COM-NFR-031)을 적용받는다. 서비스의 빌드 스크립트에 품질 규칙을 복사하지 않는다. | [프로젝트 QA](qa-checklist.md) TC-COM-001~004 ([ADR-0014](adr/0014-monorepo-with-gradle-convention-plugins.md)) |
| COM-NFR-035 | 모든 서비스의 의존성(Gradle, npm, GitHub Actions, 컨테이너 이미지)은 자동 갱신 PR로 관리한다. | [프로젝트 QA](qa-checklist.md) TC-COM-008 ([ADR-0012](adr/0012-version-catalog-and-dependabot.md), [ADR-0014](adr/0014-monorepo-with-gradle-convention-plugins.md)) |

### 4.5 성능 (미정)

성능 목표는 아직 정의하지 않았다. §7 미결 사항을 참고한다. 현재는 성능에 영향을 주는 설계 원칙만 지킨다.

- 목록 조회는 본문을 제외한 별도 프로젝션으로 읽고, 작성자 닉네임은 조인으로 함께 읽는다 (N+1 방지).
- 게시글·댓글 목록은 부분 인덱스(`WHERE deleted_at IS NULL`)를 사용한다.

## 5. 공통 인터페이스 요구사항

| ID | 요구사항 | 검증 |
|---|---|---|
| COM-IF-001 | API는 `/api` 아래에 JSON으로 제공한다. 요청·응답 본문의 시각은 ISO-8601 UTC 문자열이다. | 컨트롤러 테스트 |
| COM-IF-002 | 자원을 만들면 `201 Created`와 `Location` 헤더를, 수정·삭제·좋아요는 `204 No Content`를 반환한다. | `PostControllerTest#게시글을_작성하면_201과_위치를_반환한다` |
| COM-IF-003 | 모든 오류는 RFC 9457 ProblemDetail 형식(`application/problem+json`)으로 반환한다. 표준 필드에 더해 `code` 확장 필드를 포함한다. `type`은 `urn:bbs:error:<code 소문자>`이다. | `GlobalExceptionHandlerTest#비즈니스_예외는_ProblemDetail_형식으로_변환된다` |
| COM-IF-004 | 요청 값 검증에 실패하면 `400 INVALID_REQUEST`와 함께 `errors` 확장 필드에 `{필드명: 메시지}`를 담는다. 경로 변수나 요청 본문의 식별자가 1보다 작은 경우도 같은 형식으로 응답한다. | `PostControllerTest#제목이_비면_400과_필드_오류를_반환한다`, `PostControllerTest#식별자가_1보다_작으면_400이다`, `CommentControllerTest#식별자가_1보다_작으면_400이다`, `CommentControllerTest#부모_댓글_식별자가_1보다_작으면_400이다` |
| COM-IF-005 | 목록 API는 오프셋 페이징을 사용한다. 요청은 `page`(0부터), `size`(기본 20) 쿼리 파라미터이고, 응답은 `{content, page, size, totalElements, totalPages, last}`이다. | `OpenApiDocumentTest#페이지_정보는_개별_파라미터로_평탄화된다` |
| COM-IF-006 | OpenAPI 문서(`/v3/api-docs`, `/swagger-ui.html`)를 제공한다. 응답 필드 중 null이 될 수 없는 필드는 `required`로 표시하고, 서버가 채우는 인자(현재 회원 등)는 문서에 노출하지 않는다. | `OpenApiDocumentTest` |
| COM-IF-007 | 로그인은 `/oauth2/authorization/keycloak`에서 시작하고, 로그아웃은 `POST /logout`(CSRF 토큰 필요)이며 `204`를 반환한다. | `SecurityCsrfTest#로그아웃은_토큰과_함께_POST하면_성공한다` |
| COM-IF-008 | `/api`, `/actuator`, `/oauth2`, `/login`, `/logout`, Swagger 경로가 아닌 GET 요청은 SPA 진입점(`index.html`)을 반환한다. 존재하지 않는 API 경로는 화면을 반환하지 않는다. | `SpaForwardingTest` |

### 5.1 오류 코드 목록

모든 기능이 공유하는 단일 목록이다 (`common.error.ErrorCode`). 새 오류 코드는 이 표와 코드에 함께 추가한다.

| code | HTTP | 기본 메시지 | 사용 기능 |
|---|---|---|---|
| `INVALID_REQUEST` | 400 | 요청 값이 올바르지 않습니다. | 공통 |
| `UNAUTHENTICATED` | 401 | 인증이 필요합니다. | 공통 |
| `ACCESS_DENIED` | 403 | 권한이 없습니다. | 공통 |
| `MEMBER_NOT_FOUND` | 404 | 회원을 찾을 수 없습니다. | MEM |
| `POST_NOT_FOUND` | 404 | 게시글을 찾을 수 없습니다. | PST, CMT |
| `COMMENT_NOT_FOUND` | 404 | 댓글을 찾을 수 없습니다. | CMT |
| `COMMENT_DEPTH_EXCEEDED` | 400 | 대댓글에는 답글을 달 수 없습니다. | CMT |
| `ALREADY_LIKED` | 409 | 이미 좋아요한 게시글입니다. | PST |
| `NOT_LIKED` | 409 | 좋아요하지 않은 게시글입니다. | PST |
| `INTERNAL_ERROR` | 500 | 서버 오류가 발생했습니다. | 공통 |

## 6. 제약 조건

| ID | 제약 |
|---|---|
| COM-CON-001 | 백엔드는 Java 25, Spring Boot 4.1 계열을 사용한다. |
| COM-CON-002 | 데이터 저장소는 PostgreSQL, 세션과 캐시성 데이터는 Redis 프로토콜 호환 저장소(Valkey)를 사용한다. 라이선스 검토가 필요 없는 구현을 쓴다 ([ADR-0013](adr/0013-valkey-instead-of-redis.md)). |
| COM-CON-003 | 화면은 백엔드와 동일 오리진에서 제공한다. CORS를 허용하지 않는다. |
| COM-CON-004 | 로컬 실행에 필요한 외부 시스템(PostgreSQL, Valkey, Keycloak)은 `docker compose -f deploy/compose.yaml up -d` 한 번으로 준비되어야 한다. 환경 변수 파일(`.env`)이 없어도 개발 기본값으로 동작한다. |
| COM-CON-005 | 데이터베이스 스키마는 Flyway 마이그레이션으로만 변경한다. |
| COM-CON-006 | 시험용 사용자 계정은 Keycloak realm 구조와 분리해서 두고, 개발 환경에서만 가져온다. |

## 7. 미결 사항

| 번호 | 내용 | 영향 | 결정 필요 시점 |
|---|---|---|---|
| OPEN-01 | 응답 시간, 동시 사용자 수 같은 성능 목표가 없다. | 성능 회귀를 판단할 기준이 없다. | 부하 테스트를 도입할 때 |
| OPEN-02 | 지원 브라우저 범위가 정의되지 않았다. | 화면 QA의 대상 환경이 모호하다. | 화면 QA 체계를 만들 때 |
| OPEN-03 | 역할 수준 인가를 하지 않는다. Keycloak에 로그인한 사용자는 `USER` 역할이 없어도 회원 기능을 사용할 수 있다. | 역할 없는 계정이 생기면 의도하지 않은 접근이 가능하다. | 계정 종류가 늘어날 때 |
| OPEN-04 | **해결됨 (1.1.0).** `/actuator/metrics`가 인증 없이 열려 있었다. 보안 설정의 `GET /**` 허용 규칙(SPA 화면 경로용)이 액추에이터 경로에도 적용되었기 때문이다. 나머지 액추에이터 경로를 관리자 전용으로 막고 COM-NFR-021에 반영했다. | - | - |

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|---|---|---|---|
| 1.0.0 | 2026-10-09 | 최초 작성 (구현 완료 시점 기준으로 역작성) | HseongH |
| 1.1.0 | 2026-10-09 | OPEN-04 해결: 지표를 비롯한 나머지 액추에이터 경로를 관리자 전용으로 제한하고 COM-NFR-021 갱신 | HseongH |
| 1.2.0 | 2026-10-09 | Valkey 전환 반영 (COM-CON-002, COM-NFR-020, PR #15) | HseongH |
| 1.3.0 | 2026-10-09 | 원댓글 단위 댓글 목록 반영: COM-NFR-012에 삭제된 원댓글 자리 표시 예외(CMT-FR-009) 추가, COM-NFR-014의 검증 테스트를 원댓글 목록 테스트로 교체 | HseongH |
| 1.4.0 | 2026-10-09 | 코드 리뷰 결함 수정 반영: COM-IF-004에 1보다 작은 식별자(경로 변수, 요청 본문) 추가, COM-NFR-002에 401 응답의 형식 조건 추가 | HseongH |
| 1.5.0 | 2026-10-09 | 모노레포 전환 반영 ([ADR-0014](adr/0014-monorepo-with-gradle-convention-plugins.md)): 용어 "서비스" 추가, 시스템 구성 관점에 서비스 이름 병기, COM-NFR-006·033·034·035와 COM-CON-006 추가, COM-CON-004·COM-NFR-031·032의 실행 경로와 검증 수단 갱신. 기능에 속하지 않는 공통 요구사항의 검증은 [프로젝트 QA 체크리스트](qa-checklist.md)로 연결 | HseongH |
