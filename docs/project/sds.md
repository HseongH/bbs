---
doc_id: PRJ-SDS
title: 게시판(bbs) 프로젝트 설계 명세서
version: 1.0.0
status: In Review
owner: HseongH
reviewers: []
approved_date:
last_updated: 2026-10-09
related: [PRJ-SRS 1.0.0, PRJ-QA 1.0.0]
---

# 게시판(bbs) 프로젝트 설계 명세서

> IEEE 1016의 설계 관점(context, composition, dependency, information, interface, interaction) 중 **프로젝트 전체에 해당하는 부분**을 담는다. 아키텍처 기술(ISO/IEC/IEEE 42010)도 이 문서에 포함했다. 기능별 상세 설계는 각 기능 SDS에 있다.

## 1. 개요

### 1.1 설계 목표

[프로젝트 SRS](srs.md)의 공통 요구사항 가운데 설계에 가장 큰 영향을 준 것은 다음 세 가지다.

| 설계 목표 | 근거 요구사항 | 설계 수단 |
|---|---|---|
| 규칙에 우회 경로가 없다 | COM-NFR-005 | 규칙과 권한을 도메인 객체 안에 둔다 |
| 구조가 시간이 지나도 무너지지 않는다 | COM-NFR-030, 031 | 헥사고날 아키텍처 + ArchUnit + 빌드 게이트 |
| 동시 요청에서 데이터가 정확하다 | COM-NFR-010, 011 | 원자적 UPDATE, DB 제약, Redis `SETNX` |

### 1.2 관심사와 해당 절

| 관심사 | 관련 이해관계자 | 다루는 절 |
|---|---|---|
| 시스템이 외부와 어떻게 연결되는가 | 개발자, 운영자 | §3 컨텍스트 |
| 코드를 어떻게 나누고 무엇이 무엇에 의존하는가 | 개발자 | §4 구성과 의존 |
| 데이터를 어디에 어떤 형태로 저장하는가 | 개발자 | §5 데이터 |
| 기능 사이에 어떻게 협력하는가 | 개발자 | §6 기능 간 상호작용 |
| 인증과 보안이 어떻게 동작하는가 | 개발자, 보안 검토자 | §7 보안 |
| 어떻게 실행하고 검증하는가 | 개발자 | §8 실행 환경, §9 품질 게이트 |

## 2. 기술 스택

| 영역 | 선택 | 비고 |
|---|---|---|
| 언어·런타임 | Java 25 (Temurin) | 가상 스레드 활성화 |
| 프레임워크 | Spring Boot 4.1.1 | Spring Framework 7, Jakarta EE 11 |
| 빌드 | Gradle (Kotlin DSL) | 버전은 `.sdkmanrc`, wrapper로 고정 |
| 영속성 | Spring Data JPA (Hibernate 7) | 엔티티는 어댑터 전용 ([ADR-0002](adr/0002-separate-domain-and-jpa-entity.md)) |
| 동적 쿼리 | `io.github.openfeign.querydsl` 7.0 | [ADR-0006](adr/0006-querydsl-openfeign-fork.md) |
| 스키마 관리 | Flyway | [ADR-0007](adr/0007-flyway-single-source-of-schema.md) |
| DB | PostgreSQL 17 | |
| 세션·캐시 | Redis 7, Spring Session | [ADR-0008](adr/0008-oidc-bff-and-redis-session.md) |
| 인증 | Spring Security OAuth2 Client + Keycloak 26 | |
| API 문서 | springdoc-openapi | |
| null 안정성 | JSpecify + NullAway (Error Prone) | |
| 화면 | Angular 22, TypeScript 6, Tailwind CSS 4 | |
| API 타입 생성 | openapi-typescript | |
| 테스트 | JUnit 5, AssertJ, Testcontainers, ArchUnit / Vitest, Playwright | |

## 3. 컨텍스트 관점

```
             ┌──────────────────────── 동일 오리진 ────────────────────────┐
 사용자 ──▶  │  Angular SPA  ──/api, /oauth2, /login, /logout──▶  bbs 앱    │
             └─────────────────────────────────────────────────────────────┘
                                                                  │
                     ┌───────────────────────┬────────────────────┼──────────────────┐
                     ▼                       ▼                    ▼                  ▼
                PostgreSQL               Redis                Keycloak          Actuator
              (게시글·댓글·회원)   (세션, 조회수 중복 키)   (OIDC 로그인, 역할)   (health, metrics)
```

| 외부 요소 | 역할 | 연결 방식 |
|---|---|---|
| Keycloak | 사용자 계정, 로그인, 역할(`USER`, `ADMIN`) | OIDC Authorization Code (BFF) |
| PostgreSQL | 영속 데이터 | JDBC (JPA, QueryDSL, 네이티브 쿼리) |
| Redis | HTTP 세션, 조회수 중복 판정 키 | Spring Session, `StringRedisTemplate` |
| 브라우저 | 화면 | 세션 쿠키 + CSRF 쿠키 (`XSRF-TOKEN`) |

## 4. 구성과 의존 관점

### 4.1 최상위 구성

```
com.board.bbs
├── common/     공유 커널: 설정, 오류 규약, 보안 어노테이션, 페이지 응답
├── member/     회원·인증
├── post/       게시글, 좋아요, 조회수
└── comment/    댓글
```

`common`은 어떤 기능에도 의존하지 않는다. 기능 패키지는 `common`을 사용할 수 있다.

### 4.2 기능 내부 구성 (헥사고날)

모든 기능은 같은 계층 구조를 따른다 ([ADR-0001](adr/0001-hexagonal-architecture-enforced-by-tests.md)).

```
<feature>/
├── domain/                  순수 Java. 애그리게이트, 값 객체, 도메인 이벤트
├── application/
│   ├── port/in/             유스케이스 인터페이스 (외부에 공개하는 API)
│   ├── port/out/            영속성·부가 기능 포트 (기능 내부 구현)
│   └── service/             유스케이스 구현, 트랜잭션 경계
└── adapter/
    ├── in/web/              REST 컨트롤러, 요청·응답 record
    ├── in/event/            다른 기능의 도메인 이벤트 수신 (필요한 기능만)
    ├── in/security/         인증 연동 (member만)
    ├── out/persistence/     JPA 엔티티, 리포지토리, 매퍼, 영속성 어댑터
    └── out/redis/           Redis 어댑터 (필요한 기능만)
```

### 4.3 의존 규칙

규칙은 문서가 아니라 테스트로 강제한다. 아래 표의 규칙을 어기면 `./gradlew check`가 실패한다.

**계층 규칙** (`HexagonalArchitectureTest`)

| 규칙 | 이유 |
|---|---|
| 도메인은 `org.springframework`, `jakarta`, `com.querydsl`을 참조하지 않는다 | 도메인 테스트가 스프링 없이 밀리초 단위로 돈다 |
| 도메인과 애플리케이션은 어댑터를 참조하지 않는다 | 기술을 바꿔도 규칙이 영향을 받지 않는다 |
| 인바운드 어댑터는 아웃바운드 어댑터를 참조하지 않는다 | 컨트롤러가 리포지토리를 직접 호출하는 지름길을 막는다 |
| `@Transactional`은 `application.service`에만 둔다 | 트랜잭션 경계를 한 곳에서만 찾으면 된다 |
| JPA 엔티티는 `adapter.out.persistence`에만, 컨트롤러는 `adapter.in.web`에만 둔다 | 타입의 위치만 보고 역할을 알 수 있다 |
| 아웃바운드 포트는 인터페이스다 | 애플리케이션이 구현이 아니라 계약에 의존한다 |
| 모든 패키지는 `@NullMarked`다 | NullAway가 모든 코드의 null 계약을 검사한다 |

**기능 경계 규칙** (`FeatureBoundaryTest`, [ADR-0009](adr/0009-feature-boundaries-via-events.md))

| 규칙 | 이유 |
|---|---|
| 최상위 패키지(`common`, `member`, `post`, `comment`) 사이에 순환이 없다 | 서로를 아는 두 기능은 따로 떼어 내거나 따로 이해할 수 없다 |
| 다른 기능의 아웃바운드 포트(`application.port.out`)와 어댑터(`adapter`)에 의존하지 않는다 | 다른 기능에는 도메인과 인바운드 포트(유스케이스)로만 접근한다 |
| 예외: 이름이 `*QueryRepository`인 읽기 전용 저장소는 다른 기능의 QueryDSL 메타모델(`Q*JpaEntity`)을 조인할 수 있다 | 목록에 작성자 닉네임을 붙이는 읽기 쿼리를 한 번에 실행하기 위해서다 |

### 4.4 현재 기능 사이의 의존

```
            ┌────────────── PostDeleted 이벤트 ──────────────┐
            │                                                ▼
   post ────┘                                             comment
     ▲                                                       │
     │  GetPostUseCase (게시글 존재 확인)                      │
     └───────────────────────────────────────────────────────┘

   post, comment ──▶ member.domain.MemberId (작성자 식별자)
   post (PostQueryRepository) ──▶ QMemberJpaEntity (닉네임 조인, 허용된 예외)
   모든 기능 ──▶ common
```

## 5. 데이터 관점

### 5.1 스키마

스키마의 유일한 출처는 `src/main/resources/db/migration/`의 Flyway 스크립트다. JPA는 `ddl-auto: validate`로 검증만 한다.

```
member (1) ───< post (N)         post.author_id → member.id
member (1) ───< comment (N)      comment.author_id → member.id
post   (1) ───< comment (N)      comment.post_id → post.id
comment(1) ───< comment (N)      comment.parent_comment_id → comment.id (깊이 1까지)
post   (1) ───< post_like (N) >─── (1) member
```

| 테이블 | 주요 컬럼 | 제약·인덱스 | 마이그레이션 |
|---|---|---|---|
| `member` | `subject`, `nickname(50)`, `email` | `uk_member_subject UNIQUE(subject)` | V1 |
| `post` | `title(100)`, `content(10000)`, `author_id`, `view_count`, `like_count`, `deleted_at` | 부분 인덱스 `idx_post_active_created_at (created_at DESC, id DESC) WHERE deleted_at IS NULL` | V2 |
| `comment` | `post_id`, `author_id`, `body(1000)`, `parent_comment_id`, `depth`, `deleted_at` | `ck_comment_depth CHECK (depth BETWEEN 0 AND 1)`, 부분 인덱스 `idx_comment_active_by_post (post_id, created_at, id) WHERE deleted_at IS NULL` | V3 |
| `post_like` | `post_id`, `member_id` | `uk_post_like UNIQUE(post_id, member_id)` | V4 |

모든 테이블은 `created_at`을 가지며, `post_like`를 제외한 테이블은 `updated_at`도 가진다. 시각은 `TIMESTAMPTZ`로 저장한다.

### 5.2 Redis 키

| 키 | 값 | TTL | 용도 | 소유 기능 |
|---|---|---|---|---|
| `spring:session:*` | 세션 데이터 | 30분 (마지막 요청 기준) | HTTP 세션 | 공통 |
| `post:view:{postId}:{viewerKey}` | `"1"` | 24시간 | 조회수 중복 판정 | post |

### 5.3 도메인 모델과 영속성 모델의 분리

도메인 객체와 JPA 엔티티는 별개의 클래스이며, 영속성 어댑터의 매퍼가 둘을 변환한다 ([ADR-0002](adr/0002-separate-domain-and-jpa-entity.md)). 도메인 객체는 `write()`(신규 생성, 식별자 없음)와 `restore()`(영속 상태 복원) 두 가지 팩토리 메서드만 가진다.

## 6. 기능 간 상호작용

| 상호작용 | 방식 | 트랜잭션 | 근거 |
|---|---|---|---|
| 게시글 삭제 시 댓글 삭제 | `post`가 `PostDeleted` 이벤트 발행 → `comment`의 `PostDeletedListener`가 동기로 수신 | 같은 트랜잭션. 게시글 삭제가 롤백되면 댓글 삭제도 롤백된다 | [ADR-0009](adr/0009-feature-boundaries-via-events.md) |
| 댓글 작성 시 게시글 존재 확인 | `comment`가 `post`의 공개 유스케이스 `GetPostUseCase` 호출 | 같은 트랜잭션 | [ADR-0009](adr/0009-feature-boundaries-via-events.md) |
| 현재 회원 식별 | `member`의 `CurrentMemberArgumentResolver`가 `@CurrentMember MemberId` 파라미터를 채운다 | 해당 없음 | 기능 컨트롤러는 인증 방식을 알 필요가 없다 |

## 7. 보안 설계

### 7.1 인증 흐름 (BFF 패턴)

```
1. 브라우저 → /oauth2/authorization/keycloak
2. bbs → Keycloak 로그인 화면으로 리다이렉트
3. 로그인 성공 → bbs가 Authorization Code로 토큰 교환 (토큰은 서버에만 존재)
4. BbsOidcUserService: 회원 자동 생성(최초 1회) + realm 역할을 ROLE_* 권한으로 매핑
5. 세션 생성 → Redis 저장 → 브라우저에는 세션 쿠키만 전달
```

브라우저는 액세스 토큰을 보관하지 않는다. 토큰 탈취 위험을 서버 쪽으로 옮기는 대신, CSRF 방어가 필요해진다.

### 7.2 접근 제어 계층

| 계층 | 담당 | 판단하는 것 |
|---|---|---|
| URL | `SecurityConfig` | 인증 여부 (`GET /api/posts/**`, `/api/comments/**`는 공개, 나머지 `/api/**`는 인증 필요) |
| 파라미터 | `CurrentMemberArgumentResolver` | 현재 회원 식별자. 필수인데 미인증이면 `401` |
| 도메인 | `Post`, `Comment` | 작성자 여부, 관리자 여부, 삭제 여부 |

관리자 여부는 컨트롤러가 `Authentication`의 권한(`ROLE_ADMIN`)에서 읽어 도메인 메서드에 `boolean`으로 전달한다. 도메인은 스프링 보안 타입을 모른다.

### 7.3 CSRF

- 토큰 저장소: `CookieCsrfTokenRepository.withHttpOnlyFalse()` (SPA가 쿠키를 읽어 헤더로 보낸다)
- 조회 요청에도 토큰 쿠키를 즉시 발급하도록 `CsrfTokenRequestAttributeHandler`의 지연 로딩을 끈다.

### 7.4 오류 처리

`GlobalExceptionHandler`가 모든 예외를 ProblemDetail로 변환한다.

| 예외 | 변환 결과 |
|---|---|
| `BusinessException` | `ErrorCode`의 상태와 코드. 메시지는 예외 메시지 |
| `AccessDeniedException` | `403 ACCESS_DENIED` |
| `MethodArgumentNotValidException` | `400 INVALID_REQUEST` + `errors` 필드 |
| 스프링 MVC 표준 예외 | 원래 상태 코드 유지, `code`는 HTTP 상태 이름 |
| 그 밖의 모든 예외 | `500 INTERNAL_ERROR`, 서버 로그에만 상세 기록 |
| 인증 실패 (필터 단계) | `SecurityConfig`의 진입점이 `401 UNAUTHENTICATED` ProblemDetail을 직접 작성 |

`BusinessException`은 예상된 실패이므로 스택트레이스를 수집하지 않는다.

## 8. 화면(SPA) 구성

```
frontend/src/app/
├── core/
│   ├── api/       OpenAPI에서 생성한 타입(schema.d.ts), ProblemDetail 해석
│   └── auth/      인증 인터셉터(401 → 로그인), 라우트 가드, 현재 회원 스토어
├── features/
│   ├── post/      post-api.service, post.store, pages, components
│   └── comment/   comment-api.service, comment.store, components
├── shared/ui/     공용 UI
└── app.routes.ts
```

- 기능마다 HTTP 호출만 담당하는 `*-api.service.ts`와 상태·무효화를 담당하는 `*.store.ts`로 나눈다. 컴포넌트는 스토어만 주입받는다.
- 개발 서버는 `/api`, `/oauth2`, `/login`, `/logout`을 백엔드로 프록시해서 동일 오리진을 만든다.
- 백엔드 OpenAPI 문서에서 타입을 생성하므로, API 계약이 바뀌면 프론트엔드 타입 검사가 실패한다 (COM-NFR-032).

| 경로 | 화면 | 인증 |
|---|---|---|
| `/` | 게시글 목록 (검색, 페이지) | 불필요 |
| `/posts/new` | 게시글 작성 | 필요 (`authGuard`) |
| `/posts/:postId` | 게시글 상세 (댓글, 좋아요) | 불필요 |
| `/posts/:postId/edit` | 게시글 수정 | 필요 (`authGuard`) |
| `**` | 찾을 수 없음 | 불필요 |

## 9. 실행 환경과 품질 게이트

### 9.1 로컬 실행

| 구성 요소 | 실행 방법 | 주소 |
|---|---|---|
| PostgreSQL, Redis, Keycloak | `docker compose up -d` | Keycloak 콘솔 `localhost:8081` |
| 백엔드 | `./gradlew bootRun` | `localhost:8080` |
| 화면 | `cd frontend && pnpm dev` | `localhost:5173` |

Keycloak realm은 `docker/keycloak/bbs-realm.json`으로 자동 구성된다. 시험 계정은 `tester`(USER)와 `admin-user`(USER, ADMIN)이다.

### 9.2 품질 게이트

| 단계 | 명령 | 포함 검사 |
|---|---|---|
| 커밋 전 | Git hook (`hooks/pre-commit`) | 포맷, Checkstyle, 프론트엔드 변경 시 프론트엔드 검사 |
| 커밋 메시지 | Git hook (`hooks/commit-msg`) | `type(scope): subject` 형식 |
| 백엔드 빌드 | `./gradlew check` | Spotless, Checkstyle, Error Prone·NullAway, 단위·통합·아키텍처 테스트, JaCoCo |
| 프론트엔드 | `pnpm verify` | ESLint, Prettier, 타입 검사, 단위 테스트 |
| E2E | `pnpm e2e` | Playwright (백엔드와 컨테이너 필요) |
| CI | `.github/workflows/backend.yml` | push(main), PR에서 `./gradlew check` |

상세 기준은 [공통 QA 기준](qa-standards.md)에 있다.

## 10. 아키텍처 결정 목록

| ADR | 제목 |
|---|---|
| [0001](adr/0001-hexagonal-architecture-enforced-by-tests.md) | 헥사고날 아키텍처를 쓰고 테스트로 강제한다 |
| [0002](adr/0002-separate-domain-and-jpa-entity.md) | 도메인 모델과 JPA 엔티티를 분리한다 |
| [0003](adr/0003-authorization-in-domain.md) | 소유권 검사는 도메인에 둔다 |
| [0004](adr/0004-atomic-counter-update.md) | 카운터는 원자적 UPDATE로만 바꾼다 |
| [0005](adr/0005-database-decides-duplicates.md) | 중복 판정은 저장소에 맡긴다 |
| [0006](adr/0006-querydsl-openfeign-fork.md) | QueryDSL은 openfeign 포크를 쓴다 |
| [0007](adr/0007-flyway-single-source-of-schema.md) | 스키마는 Flyway가 유일한 출처다 |
| [0008](adr/0008-oidc-bff-and-redis-session.md) | 인증은 OIDC BFF 방식, 세션은 Redis에 둔다 |
| [0009](adr/0009-feature-boundaries-via-events.md) | 기능 사이의 의존은 이벤트와 공개 유스케이스로 한정한다 |

## 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|---|---|---|---|
| 1.0.0 | 2026-10-09 | 최초 작성 (`main` e96a878 기준으로 역작성) | HseongH |
