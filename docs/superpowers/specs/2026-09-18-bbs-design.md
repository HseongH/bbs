# 게시판(bbs) 설계 문서

- 작성일: 2026-09-18
- 상태: 승인됨

## 1. 목적과 범위

Java 25 · Spring Boot 4.1.1 · Gradle 9.7.1 환경에서 **참고 구현으로 삼을 만한 품질**의 REST 게시판 API를 만든다.
목표는 기능을 채우는 것이 아니라, 아래를 코드로 증명하는 것이다.

- 도메인 로직이 프레임워크와 분리되어 있고, 그 분리가 **테스트로 강제**된다
- 모든 규칙(검증, 권한, 불변식)이 한 곳에 있고 우회 경로가 없다
- 빌드가 품질 기준(포맷·정적분석·커버리지·아키텍처)을 통과하지 못하면 실패한다

### 포함 기능

- 게시글: 작성, 조회, 수정, 삭제(소프트), 목록 페이징, 검색
- 댓글: 작성, 조회, 수정, 삭제 / 대댓글 1단계
- 조회수, 좋아요
- Keycloak(OIDC) 기반 인증, Redis 세션

### 제외 (YAGNI)

- 첨부파일 업로드
- 화면(서버 렌더링 또는 SPA) — 확인은 Swagger UI로 한다
- 관리자 백오피스, 알림, 검색 엔진 연동

## 2. 기술 스택

| 영역 | 선택 | 비고 |
|---|---|---|
| 언어/런타임 | Java 25 (Temurin 25.0.4 LTS) | 가상 스레드 활성화 |
| 프레임워크 | Spring Boot 4.1.1 | Spring Framework 7, Jakarta EE 11 |
| 빌드 | Gradle 9.7.1 (Kotlin DSL) | |
| 영속성 | Spring Data JPA (Hibernate 7) | |
| 동적 쿼리 | `io.github.openfeign.querydsl:querydsl-jpa:7.0` | **선택 근거는 아래 참조** |
| 스키마 | Flyway | `ddl-auto: validate` |
| DB | PostgreSQL | |
| 세션 | Spring Session Data Redis | 버전은 Boot BOM 관리 |
| 인증 | Spring Security OAuth2 Client + Keycloak | |
| 문서화 | springdoc-openapi 3.1.1 | |
| 보일러플레이트 | Lombok | 기존 `../ai` 컨벤션 유지 |
| 테스트 | JUnit 5, AssertJ, Testcontainers, ArchUnit 1.4.1 | |
| 품질 | Spotless(google-java-format), Checkstyle(Google Style), JaCoCo | |

### QueryDSL 선택 근거

`com.querydsl:querydsl-jpa`의 최신 버전은 5.1.0이며 Hibernate 6을 대상으로 빌드되어 있다.
Spring Boot 4.1은 Hibernate 7을 사용하므로 그대로 쓰면 런타임에 깨진다.
`io.github.openfeign.querydsl`(유지보수되는 포크)의 7.0은 Hibernate 7을 대상으로 빌드되어 있어 이쪽을 사용한다.

## 3. 아키텍처

### 3.1 구조

단일 Gradle 모듈 안에서 패키지로 헥사고날 경계를 표현한다.

```
com.board.bbs
├── common/
│   ├── config/        Security, Jpa, RedisSession, Querydsl, OpenApi
│   ├── error/         ErrorCode, BusinessException, GlobalExceptionHandler
│   └── support/       PageResponse 등 공통 타입
├── post/
│   ├── domain/            순수 Java. Spring/JPA/Jakarta 의존 없음
│   ├── application/
│   │   ├── port/in/       UseCase 인터페이스 (Command / Query 분리)
│   │   ├── port/out/      LoadPostPort, SavePostPort, SearchPostPort 등
│   │   └── service/       트랜잭션 경계는 여기에만 존재
│   └── adapter/
│       ├── in/web/            Controller, 요청·응답 record, WebMapper
│       └── out/persistence/   JpaEntity, JpaRepository, PersistenceAdapter, Mapper
├── comment/           (post와 동일 구조)
└── member/            Keycloak 사용자 ↔ 로컬 회원 프로비저닝
```

### 3.2 의존 규칙 (ArchUnit으로 강제)

- `domain`은 아무것도 참조하지 않는다. `org.springframework`, `jakarta.persistence` import 금지
- `application`은 `domain`만 참조한다. `adapter` 참조 금지
- `adapter.in`은 `adapter.out`을 참조할 수 없다 (반대도 동일)
- `@Transactional`은 `application.service`에만 존재한다
- 위반 시 빌드 실패

### 3.3 이 구조의 대가

도메인 객체와 JPA 엔티티가 분리되므로 매퍼 코드가 늘어난다. 이는 의도된 비용이며,
그 대가로 도메인 테스트가 스프링 컨텍스트 없이 밀리초 단위로 돌고 영속성 기술을 교체해도
도메인이 영향을 받지 않는다.

## 4. 도메인 모델

### 4.1 Post (애그리게이트 루트)

- 값 객체: `PostId`, `Title`(1~100자), `Content`(1~10,000자), `MemberId` — 모두 `record` + compact constructor 검증
- 불변식은 생성 시점에 보장하고, 이후 어떤 경로로도 위반 상태에 도달할 수 없다
- 수정·삭제 권한 검사는 `post.updateBy(memberId, ...)` / `post.deleteBy(memberId)` 안에서 수행한다.
  서비스는 조율만 하고 권한 판단을 하지 않는다
- 삭제는 소프트 삭제(`deletedAt`). 삭제된 게시글은 조회·수정 대상에서 제외된다

### 4.2 Comment (별도 애그리게이트)

- `PostId`로 게시글을 **ID 참조**한다 (객체 참조 금지 — 애그리게이트 경계 유지)
- 대댓글은 `parentCommentId` 1단계까지만 허용하며, 깊이 검증은 도메인에서 수행한다
- 게시글이 소프트 삭제되면 해당 댓글도 함께 소프트 삭제된다

### 4.3 좋아요

- `PostLike(postId, memberId)`에 **DB 유니크 제약**을 걸어 중복을 최종 방어한다
- `Post.likeCount`는 비정규화 컬럼이며 원자적 `UPDATE`로 증감한다 (낙관적 락 충돌 회피)

### 4.4 조회수

- 중복 조회 방지는 Redis TTL 키(회원 또는 세션 기준)로 처리한다
- 카운트는 원자적 `UPDATE`로 증가시킨다

## 5. 인증 · 인가

Keycloak을 OIDC 프로바이더로 두고 Spring Security `oauth2Login`(BFF 패턴)을 사용한다.
세션은 Spring Session Data Redis에 저장되어 애플리케이션 인스턴스는 무상태가 된다.

- 최초 로그인 시 Keycloak `sub`를 기준으로 로컬 `Member`를 자동 프로비저닝한다
- Keycloak realm role을 `ROLE_USER` / `ROLE_ADMIN`으로 매핑한다
- 역할 수준 인가만 `@PreAuthorize`로 처리하고, **소유권 검사는 도메인에서** 한다
- 인증되지 않은 요청에 대해 리다이렉트 대신 401 ProblemDetail을 반환한다 (API 전용이므로)

## 6. API 규약

- 에러 응답은 **RFC 9457 ProblemDetail** 단일 포맷으로 통일한다. `GlobalExceptionHandler`가
  전담하며 스택트레이스와 내부 메시지를 노출하지 않는다
- 페이징은 **오프셋 기반(페이지 번호)** 을 쓴다. 게시판 UX가 페이지 번호를 요구하기 때문이다.
  정렬 키를 고정하고 count 쿼리를 분리해 성능을 관리한다
- 요청·응답 DTO는 `record`이며 `jakarta.validation`으로 검증한다. 검증 메시지는 메시지 번들로 분리한다
- 도메인 객체는 절대 웹 계층에 노출하지 않는다
- springdoc-openapi로 `/swagger-ui.html`을 제공한다

### 엔드포인트

```
POST   /api/posts                     게시글 작성
GET    /api/posts                     목록 (페이징, 검색)
GET    /api/posts/{id}                단건 조회 (조회수 증가)
PATCH  /api/posts/{id}                수정 (작성자만)
DELETE /api/posts/{id}                삭제 (작성자 또는 관리자)
POST   /api/posts/{id}/likes          좋아요
DELETE /api/posts/{id}/likes          좋아요 취소
POST   /api/posts/{postId}/comments   댓글·대댓글 작성
GET    /api/posts/{postId}/comments   댓글 목록
PATCH  /api/comments/{id}             댓글 수정 (작성자만)
DELETE /api/comments/{id}             댓글 삭제 (작성자 또는 관리자)
GET    /api/members/me                내 정보
```

## 7. 영속성

- Flyway가 스키마의 유일한 출처다. 엔티티는 DDL을 생성하지 않는다 (`ddl-auto: validate`)
- JPA 엔티티는 어댑터 전용 타입이며 도메인 객체와 매퍼로 분리한다
- 엔티티에 `@Data`, Lombok `@EqualsAndHashCode`를 쓰지 않는다 (프록시·지연로딩과 충돌)
- N+1은 fetch join 또는 `@EntityGraph`로 차단하고, **쿼리 실행 횟수를 테스트로 검증**한다
- 동적 검색 조건은 QueryDSL로 조립한다

## 8. 테스트 전략

TDD로 진행한다 — 실패하는 테스트를 먼저 쓰고 구현한다.

| 층 | 도구 | 대상 |
|---|---|---|
| 도메인 단위 | JUnit 5 + AssertJ | 불변식, 상태 전이, 권한 규칙. 스프링 없음 |
| 웹 슬라이스 | `@WebMvcTest` | 요청 검증, 에러 매핑, 직렬화 |
| 영속성 슬라이스 | `@DataJpaTest` + Testcontainers | 매핑, 쿼리, 제약조건 |
| 통합 | `@SpringBootTest` + Testcontainers | postgres · redis · keycloak |
| 아키텍처 | ArchUnit | 3.2의 의존 규칙 |

JaCoCo 임계값: 전체 80%, `domain`·`application` 90%. 미달 시 빌드 실패.

## 9. 운영 · 관측

- Actuator: health, info, metrics
- MDC 기반 correlation ID를 포함한 구조화 로깅
- Graceful shutdown
- 가상 스레드 활성화 (`spring.threads.virtual.enabled=true`)

## 10. 로컬 개발 환경

`compose.yaml`에 postgres · redis · keycloak을 정의하고 `spring-boot-docker-compose`가
애플리케이션 실행 시 자동으로 기동하고 접속 정보를 주입한다.
Keycloak realm은 import 파일로 자동 구성하여, 클론 직후 `./gradlew bootRun`만으로 동작해야 한다.

## 11. 빌드 · 품질 도구

`../ai` 프로젝트의 구성을 이식한다.

- Spotless (google-java-format) + Checkstyle (Google Style, 539줄 설정 파일)
- `installGitHooks` 태스크 → `hooks/pre-commit`이 `spotlessCheck checkstyleMain` 실행
- Lombok, `-parameters`, UTF-8 인코딩

## 12. 구현 순서

커밋 메시지는 AngularJS 컨벤션을 따른다.

1. `chore: 빌드 및 코드 품질 도구 구성`
2. `chore: 로컬 개발 인프라 구성`
3. `feat: 공통 예외 처리 및 API 에러 응답 규약`
4. `feat: Keycloak 기반 인증 및 회원 프로비저닝`
5. `feat: 게시글 작성 및 조회`
6. `test: 헥사고날 의존 규칙 검증 추가`
7. `feat: 게시글 수정 및 삭제`
8. `feat: 게시글 목록 페이징 및 검색`
9. `feat: 댓글 및 대댓글`
10. `feat: 조회수 및 좋아요`
11. `docs: README 및 실행 가이드 작성`

아키텍처 검증을 첫 수직 슬라이스 직후로 앞당긴다. 마지막에 두면 이후 모든 코드가
규칙 위반을 누적한 뒤에야 드러난다.
