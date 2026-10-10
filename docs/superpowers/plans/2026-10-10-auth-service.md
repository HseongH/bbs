# auth 서비스와 진입점 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 인증을 `services/auth`로 분리하고, 진입점 Traefik이 ForwardAuth로 auth의 판정을 받은 뒤 auth가 서명한 내부 토큰을 붙여 board로 보내게 한다. board는 Keycloak에 연결하지 않는다.

**Architecture:** auth(Spring Boot)는 Keycloak OIDC 로그인, Valkey 세션, CSRF, 원래 요청 기준의 URL 규칙을 맡고 ForwardAuth 통과 시 RS256 내부 JWT(60초)를 발급한다. board는 OAuth2 Resource Server로 auth의 JWKS만 신뢰하고, 첫 인증된 요청에서 회원을 만든다. 토큰의 발급·검증 규칙은 `libs/internal-token` 하나에 두어 두 서비스가 같은 계약 코드를 쓴다.

**Tech Stack:** Java 25, Spring Boot 4.1 (Spring Security 7: oauth2-client, oauth2-resource-server, oauth2-jose), Spring Session Data Redis, Nimbus JOSE, Traefik 3, Testcontainers, Playwright.

**Spec:** [ADR-0016](../../project/adr/0016-auth-service-with-internal-token.md), [PRJ-SRS 1.7.0](../../project/srs.md), [PRJ-SDS 1.8.0 §7·§9·§10](../../project/sds.md), [PRJ-QC 1.5.0](../../project/qa-checklist.md), [MEM-SRS 1.3.0](../../features/member/srs.md), [MEM-SDS 2.0.0](../../features/member/sds.md), [MEM-QA 1.5.0](../../features/member/qa-checklist.md)

## Global Constraints

- COM-NFR-001: Keycloak에 연결하는 서비스는 auth 하나다. board의 설정·의존성·코드에 Keycloak 주소, OAuth2 Client, 세션이 없다 (TC-COM-018).
- COM-NFR-002, COM-IF-003: 401·403·400은 모두 ProblemDetail(`application/problem+json`), 필드 `type`(`urn:bbs:error:<code 소문자>`), `title`, `status`, `detail`, `instance`, `code`. auth와 board의 형식이 같다.
- COM-NFR-003: 상태를 바꾸는 요청은 CSRF 토큰이 없으면 `403`. 조회 요청은 토큰 없이 허용하고 `XSRF-TOKEN` 쿠키를 발급한다. 저장소는 `CookieCsrfTokenRepository.withHttpOnlyFalse()`, 지연 로딩 끔.
- COM-NFR-007: board는 내부 토큰으로만 사용자를 식별한다. 서명·만료·발급자·대상이 틀리면 `401`. 브라우저의 `Authorization` 헤더는 board에 도달하지 않는다.
- COM-NFR-008: 내부 토큰은 요청마다 발급, 수명 60초, 역할은 `USER`, `ADMIN`만. 서명 키 설정이 없으면 `local` 프로필이 아닌 환경에서 auth가 시작하지 않는다.
- COM-NFR-009: 원래 경로가 모호하면(인코딩된 `/`·`\`, `.`·`..` 경로 조각, `;`, 인코딩된 `%`, 제어 문자) auth가 `400 INVALID_REQUEST`.
- COM-NFR-020: 로그인 세션은 auth가 Valkey에 저장, 30분 무활동 만료. board는 세션을 쓰지 않는다.
- COM-NFR-021: 모든 Java 서비스는 `/actuator/health`, `/actuator/info`(공개)와 `/actuator/metrics`(관리자)를 노출한다. 진입점은 액추에이터와 JWKS 경로를 연결하지 않는다.
- COM-NFR-006: 모든 컨테이너 포트는 기본 `127.0.0.1`. 진입점 포트도 같다.
- COM-NFR-031, 034: 새 모듈(auth, internal-token)은 컨벤션 플러그인만 적용하고 품질 규칙을 복사하지 않는다. 모든 품질 게이트(포맷, Error Prone·NullAway, Checkstyle, 커버리지)를 통과한다.
- COM-NFR-033: 워크플로는 `paths` 포함 목록. 새 공통 경로 `libs/**`는 이 라이브러리를 쓰는 모든 Java 서비스 워크플로와 `e2e`에 들어간다.
- COM-NFR-036: `e2e`는 진입점·auth·board·화면을 띄우고 진입점 주소로 실행한다.
- COM-IF-007: 로그인 `/oauth2/authorization/keycloak`, 로그아웃 `POST /logout`(CSRF 필요) → `204`. 둘 다 진입점을 거쳐 auth가 처리한다.
- COM-IF-008: 진입점은 `/api`, `/oauth2`, `/login`, `/logout`이 아닌 요청을 web으로 보낸다. `/api` 아래의 없는 경로는 `404` ProblemDetail.
- COM-CON-003, 004: 브라우저는 진입점(`localhost:8000`) 하나로 접속. `docker compose -f deploy/compose.yaml up -d`가 진입점까지 준비하고 `.env` 없이 동작한다.
- 내부 토큰 값: `iss` = `urn:bbs:auth`, `aud` = `bbs`, 클레임 `sub`, `nickname`, `email`, `roles`(문자열 배열), 알고리즘 RS256, 헤더에 `kid`.
- 포트: auth `8082`, board `8080`, web `5173`, 진입점 `8000`, Keycloak `8081`.
- 코드 주석·테스트 이름은 기존 관례대로 한국어. 커밋 메시지는 `type(scope): subject`.

## Review Focus

1. Keycloak 사용자에게 `preferred_username`이 없음 → 로그인이 실패하지 않아야 한다. 지금 board의 `DefaultOidcUser(..., "preferred_username")`는 그 속성이 없으면 예외를 던진다. auth는 이름 속성으로 `sub`를 쓴다 (Task 3 테스트).
2. 세션이 만료된 브라우저가 공개 조회를 보냄 → 401이 아니라 `200`, 토큰 없음 (Task 4 테스트).
3. 원래 경로에 쿼리 문자열과 퍼센트 인코딩된 한글 검색어(`/api/posts?keyword=%ED%95%9C%EA%B8%80`, `?keyword=a/../b`)가 있음 → 모호한 경로로 거부하지 않아야 한다. 경로 검사와 규칙 매칭은 쿼리를 뺀 경로로 한다 (Task 4 테스트).
4. `X-Forwarded-Method`나 `X-Forwarded-Uri` 없이 auth의 ForwardAuth 경로를 직접 호출 → 토큰을 발급하지 않고 `400` (Task 4 테스트).
5. `roles` 클레임이 없거나 배열이 아닌 토큰 → board가 `500`이 아니라 역할 없는 사용자로 처리 (Task 1, Task 5 테스트).

---

## 파일 구조

```
libs/internal-token/                       auth와 board가 공유하는 내부 토큰 계약
  build.gradle.kts
  src/main/java/com/board/bbs/token/
    InternalTokens.java                    계약 상수 (발급자, 대상, 수명, 클레임 이름, 역할)
    InternalUser.java                      토큰이 나르는 사용자 (record) + Jwt에서 복원
    InternalTokenIssuer.java               서명 (auth가 사용)
    InternalTokenDecoders.java             검증기 생성 (board가 사용)
    package-info.java
  src/test/java/com/board/bbs/token/InternalTokenContractTest.java
services/auth/
  build.gradle.kts
  src/main/java/com/board/bbs/auth/
    AuthApplication.java
    config/SecurityConfig.java             로그인·로그아웃·CSRF·URL 규칙
    config/SigningKeyConfig.java           서명 키 로드·생성, 발급기 빈
    forward/ForwardedRequest.java          원래 메서드·경로 복원과 모호성 검사
    forward/ForwardedRequestMatcher.java   원래 요청 기준 RequestMatcher
    forward/ForwardedRequestFilter.java    모호한 전달 경로를 400으로 거부
    forward/ForwardAuthController.java     통과 시 내부 토큰 발급
    login/BbsOidcUserService.java          bbs 역할만 남김, 이름 속성 sub
    login/LoginUsers.java                  OidcUser → InternalUser (대체값 규칙)
    token/JwksController.java              /.well-known/jwks.json
    error/AuthErrorCode.java, error/ProblemResponses.java
  src/main/resources/application.yml, application-local.yml
  src/test/... (support/ComposeImages, IntegrationTestBase 포함)
services/board/  (축소: 세션·OIDC·CSRF·SPA 포워딩 제거, Resource Server 추가)
deploy/compose.yaml, deploy/.env.example, deploy/traefik/dynamic/routes.yml, deploy/keycloak/bbs-realm.json
services/web/angular.json, playwright.config.ts, e2e/gateway.spec.ts (proxy.conf.json 삭제)
.github/workflows/auth.yml (신규), board.yml, e2e.yml
```

---

### Task 1: 내부 토큰 계약 라이브러리 (`libs/internal-token`)

요구사항: COM-NFR-007, COM-NFR-008, TC-MEM-051 (계약), TC-MEM-042~045의 단위 수준 근거

**Files:**
- Create: `libs/internal-token/build.gradle.kts`, `libs/internal-token/src/main/java/com/board/bbs/token/{InternalTokens,InternalUser,InternalTokenIssuer,InternalTokenDecoders,package-info}.java`
- Create: `libs/internal-token/src/test/java/com/board/bbs/token/InternalTokenContractTest.java`
- Modify: `settings.gradle.kts` (`include(":libs:internal-token")`), `gradle/libs.versions.toml`
- Modify: `docs/project/sds.md` §7.6 끝 문단과 §10 구조 (공유 라이브러리 추가 이유)

**Interfaces:**
- Produces:
  - `InternalTokens`: `String ISSUER = "urn:bbs:auth"`, `String AUDIENCE = "bbs"`, `Duration LIFETIME = Duration.ofSeconds(60)`, `String NICKNAME = "nickname"`, `EMAIL = "email"`, `ROLES = "roles"`, `Set<String> KNOWN_ROLES = Set.of("USER", "ADMIN")`
  - `record InternalUser(String subject, String nickname, String email, Set<String> roles)`; `static InternalUser from(Jwt jwt)` — `roles`가 없거나 문자열 목록이 아니면 빈 집합
  - `InternalTokenIssuer(JWKSource<SecurityContext> keys, Clock clock)`; `String issue(InternalUser user)`
  - `InternalTokenDecoders.create(JWKSource<SecurityContext> keys) : JwtDecoder` — RS256만 허용, 검증기: 시각(기본 허용 오차), `iss == ISSUER`, `aud`에 `AUDIENCE` 포함

- [ ] **Step 1: 카탈로그와 빌드 구성**

`gradle/libs.versions.toml` `[libraries]`에 추가: `spring-boot-dependencies = { module = "org.springframework.boot:spring-boot-dependencies", version.ref = "spring-boot" }`, `spring-security-oauth2-jose = { module = "org.springframework.security:spring-security-oauth2-jose" }`, `spring-boot-starter-oauth2-resource-server = { module = "org.springframework.boot:spring-boot-starter-oauth2-resource-server" }`.

`libs/internal-token/build.gradle.kts`: 플러그인 `id("bbs.java-conventions")`, `` `java-library` ``. 의존성: `api(platform(libs.spring.boot.dependencies))`, `api(libs.spring.security.oauth2.jose)`, `implementation(libs.jspecify)`, `testImplementation(platform(libs.spring.boot.dependencies))`, `testImplementation(libs.spring.boot.starter.test)`, `testRuntimeOnly(libs.junit.platform.launcher)`. 품질 규칙은 적지 않는다(COM-NFR-034).

- [ ] **Step 2: 계약 테스트를 쓴다**

`InternalTokenContractTest` (고정 `Clock`, 테스트용 `RSAKeyGenerator(2048).keyID("test").generate()`):

```java
@Test void 발급한_토큰을_검증하면_같은_사용자가_나온다() {
  // issue(new InternalUser("sub-1", "닉네임", "a@b.c", Set.of("USER","ADMIN"))) → decoder.decode
  // InternalUser.from(jwt)가 같은 값, jwt.getIssuer()="urn:bbs:auth", aud=["bbs"],
  // exp - iat = 60초, 헤더 alg=RS256, kid="test"
}
@Test void 다른_키로_서명한_토큰은_거부한다()          // JwtException
@Test void 만료된_토큰은_거부한다()                    // 발급 Clock을 now-10분으로
@Test void 발급자가_다른_토큰은_거부한다()              // 테스트 안에서 NimbusJwtEncoder로 iss="urn:other" 서명
@Test void 대상이_다른_토큰은_거부한다()                // aud="other"
@Test void 역할_클레임이_없거나_목록이_아니면_역할이_없다() // roles 생략, roles="ADMIN"(문자열) → roles 빈 집합
```

- [ ] **Step 3: 실패 확인** — `./gradlew :libs:internal-token:test` → 컴파일 실패(클래스 없음).

- [ ] **Step 4: 구현** — `InternalTokenIssuer`는 `NimbusJwtEncoder(keys)`로 `JwsHeader.with(SignatureAlgorithm.RS256)`와 클레임(`iss`, `aud`=List.of(AUDIENCE), `sub`, `iat`, `exp`=iat+LIFETIME, `nickname`, `email`, `roles`=정렬된 목록)을 서명한다. `InternalTokenDecoders`는 `DefaultJWTProcessor`에 `JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keys)`를 넣고 `new NimbusJwtDecoder(processor)`에 `DelegatingOAuth2TokenValidator(JwtValidators.createDefaultWithIssuer(ISSUER), audience 검증기)`를 설정한다. `package-info.java`는 `@NullMarked`.

- [ ] **Step 5: 통과 확인** — `./gradlew :libs:internal-token:check` → 테스트 6개 통과, 커버리지 기준 통과.

- [ ] **Step 6: 문서** — 프로젝트 SDS §7.6 마지막 문단에 "내부 토큰의 계약(클레임 이름, 발급자·대상, 서명·검증 방식)은 두 서비스가 반드시 같아야 하므로 `libs/internal-token`에 한 벌만 둔다. 오류 응답 코드는 공유하지 않는다"를 더하고, §10 구조에 `libs/internal-token/` 행을 추가한다. 변경 이력 1.8.0 행에 함께 적는다(같은 PR 안의 미승인 버전이므로 버전은 그대로).

- [ ] **Step 7: Commit** — `feat(token): 내부 토큰 계약 라이브러리를 추가한다`

---

### Task 2: auth 서비스 골격, 서명 키, 공개키, 액추에이터

요구사항: COM-NFR-008(키), COM-NFR-021, COM-NFR-034, TC-MEM-047, 049, TC-COM-025

**Files:**
- Create: `services/auth/build.gradle.kts`, `AuthApplication.java`, `config/SigningKeyConfig.java`, `token/JwksController.java`, `config/SecurityConfig.java`(이 작업에서는 액추에이터·JWKS 규칙만), `error/AuthErrorCode.java`, `error/ProblemResponses.java`, 모든 패키지의 `package-info.java`(`@NullMarked`)
- Create: `src/main/resources/application.yml`, `application-local.yml`; `src/test/resources/application-test.yml`
- Create: `src/test/java/com/board/bbs/auth/support/{ComposeImages,IntegrationTestBase}.java` (board의 같은 파일을 복사. Valkey 컨테이너만 띄운다)
- Test: `config/SigningKeyConfigTest.java`, `token/JwksControllerTest.java`, `config/ActuatorAccessTest.java`
- Modify: `settings.gradle.kts` (`include(":services:auth")`)

**Interfaces:**
- Consumes: Task 1의 `InternalTokenIssuer`, `InternalTokens`
- Produces:
  - 빈 `RSAKey signingKey`, `JWKSource<SecurityContext> signingKeys`, `InternalTokenIssuer internalTokenIssuer`(Clock은 `Clock.systemUTC()`)
  - 설정 `bbs.auth.signing-key-location` (Spring `Resource`, PKCS#8 PEM 개인키). `kid` = 공개키 JWK thumbprint
  - `ProblemResponses.write(HttpServletResponse, AuthErrorCode, String instancePath)` — board의 `ProblemDetails`와 같은 필드, `instance`는 board와 같은 바이트 단위 인코딩
  - `enum AuthErrorCode { INVALID_REQUEST(400,"요청 값이 올바르지 않습니다."), UNAUTHENTICATED(401,"인증이 필요합니다."), ACCESS_DENIED(403,"권한이 없습니다.") }` — 메시지는 PRJ-SRS §5.1과 같다

`build.gradle.kts`: `id("bbs.spring-boot-conventions")`; 의존성 starter-web, starter-actuator, starter-security, starter-oauth2-client, starter-data-redis, starter-session-data-redis, `project(":libs:internal-token")`, jspecify; developmentOnly devtools, docker-compose; 테스트 spring-boot-testcontainers, starter-webmvc-test, starter-security-test, testcontainers-junit-jupiter, testcontainers-redis. `tasks.test`에 board와 같은 `bbs.compose-file` 시스템 속성.

`application.yml`: `spring.application.name: auth`, `server.port: 8082`, `server.forward-headers-strategy: framework`, `spring.profiles.default: local`, 가상 스레드, `spring.docker.compose.file: ../../deploy/compose.yaml`(`start_only`), `spring.data.redis.host/port`, `spring.session.timeout: 30m`, `server.shutdown: graceful`, 액추에이터 노출 `health,info,metrics`, `logging.pattern.correlation`(board와 같음). `application-local.yml`: board의 Keycloak 등록 설정을 옮기되 `user-name-attribute: sub`. `application-test.yml`: compose 끔, board의 테스트 등록 설정과 같은 고정 엔드포인트(`user-name-attribute: sub`).

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```java
// SigningKeyConfigTest — ApplicationContextRunner로 SigningKeyConfig만 올린다
@Test void 키_설정이_없으면_local이_아닌_프로필에서는_시작하지_않는다() // profiles "prod" → startupFailure, 메시지에 "bbs.auth.signing-key-location"
@Test void local_프로필은_키_설정이_없으면_임시_키로_시작한다()       // profiles "local" → RSAKey 빈 존재, 비공개 키 포함
@Test void 설정한_PEM_키를_읽고_kid는_공개키_thumbprint다()           // @TempDir에 PKCS#8 PEM 작성 → kid == thumbprint, 두 번 로드해도 같은 kid
// JwksControllerTest (IntegrationTestBase)
@Test void 공개키만_인증_없이_제공한다()  // GET /.well-known/jwks.json → 200, keys[0].kid 존재, "d"·"p"·"q" 필드 없음
// ActuatorAccessTest (IntegrationTestBase) — TC-MEM-049
@Test void 상태_확인과_정보는_인증_없이_볼_수_있다()
@Test void 지표는_인증_없이_볼_수_없다()          // 401, application/problem+json, code UNAUTHENTICATED
@Test void 지표는_일반_회원이_볼_수_없다()        // oidcLogin().authorities(ROLE_USER) → 403 ProblemDetail ACCESS_DENIED
@Test void 지표는_관리자가_볼_수_있다()           // ROLE_ADMIN → 200
```

- [ ] **Step 2: 실패 확인** — `./gradlew :services:auth:test` → 컴파일 실패.

- [ ] **Step 3: 구현** — `SigningKeyConfig`: 위치가 있으면 PEM을 읽어 `RSAPrivateCrtKey`에서 공개키를 만들고, 없으면 활성 프로필에 `local`이 있을 때만 `RSAKeyGenerator(2048)`로 만들고 WARN 로그를 남긴다. 둘 다 아니면 `IllegalStateException`. `SecurityConfig`의 이 작업 범위 규칙: health·info·JWKS 공개, 나머지 액추에이터 `ADMIN`, 진입점·거부 처리기는 `ProblemResponses`. `oauth2Login`은 다음 작업에서 붙인다.

- [ ] **Step 4: 통과 확인** — `./gradlew :services:auth:check` → 통과. `grep -nE "checkstyle|errorprone|spotless|jacoco" services/auth/build.gradle.kts` → 출력 없음 (TC-COM-025 근거).

- [ ] **Step 5: Commit** — `feat(auth): auth 서비스와 내부 토큰 서명 키를 추가한다`

---

### Task 3: auth 로그인·로그아웃과 사용자 변환

요구사항: MEM-FR-001, 003, 004, COM-NFR-008(역할), TC-MEM-009, 018, 046, Review Focus 1

**Files:**
- Create: `login/BbsOidcUserService.java`, `login/LoginUsers.java`
- Modify: `config/SecurityConfig.java` (`oauth2Login`, 로그아웃 204, CSRF 쿠키 저장소)
- Test: `login/LoginUsersTest.java`, `login/BbsOidcUserServiceTest.java`, `config/LogoutTest.java`

**Interfaces:**
- Consumes: Task 1 `InternalUser`, `InternalTokens.KNOWN_ROLES`
- Produces:
  - `BbsOidcUserService extends OidcUserService` — 반환하는 `DefaultOidcUser`의 권한은 기존 OIDC 권한 + `ROLE_USER`/`ROLE_ADMIN`(realm 역할 중 `KNOWN_ROLES`에 있는 것만), 이름 속성 `"sub"`
  - `static InternalUser LoginUsers.toInternalUser(OidcUser user)` — 닉네임: `preferred_username`이 없거나 공백뿐이면 `sub`; 이메일: 없거나 공백뿐이면 `sub + "@unknown.local"`; 역할: 권한 중 `ROLE_` + `KNOWN_ROLES`만 접두사를 떼서

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```java
// LoginUsersTest — TC-MEM-018
@Test void 사용자_이름이나_이메일이_공백뿐이면_subject로_대신한다()
@Test void 사용자_이름이나_이메일이_없으면_subject로_대신한다()
@Test void 역할_권한만_역할로_옮긴다()   // 권한 OIDC_USER, SCOPE_openid, ROLE_USER, ROLE_ADMIN → roles {USER, ADMIN}
// BbsOidcUserServiceTest — TC-MEM-046, Review Focus 1 (OidcUserService 상위 호출은 board의 기존 테스트처럼 가짜 사용자 정보로 대체)
@Test void realm_역할_중_bbs가_정의한_역할만_권한이_된다() // realm_access.roles [USER, ADMIN, offline_access, default-roles-bbs] → ROLE_USER, ROLE_ADMIN만
@Test void preferred_username이_없어도_로그인할_수_있다()   // 예외 없음, getName() == sub
// LogoutTest — TC-MEM-009
@Test void 로그아웃은_토큰과_함께_POST하면_204다()   // oidcLogin + csrf() → 204
@Test void 토큰_없는_로그아웃은_403_ProblemDetail이다()
```

- [ ] **Step 2: 실패 확인** — `./gradlew :services:auth:test --tests '*login*' --tests '*LogoutTest'` → 컴파일 실패.

- [ ] **Step 3: 구현** — board의 `BbsOidcUserService` 역할 매핑 로직을 옮기며 `KNOWN_ROLES` 필터를 더한다. `SecurityConfig`: `oauth2Login(userInfoEndpoint(oidcUserService))`, `logout(204)`, `csrf(CookieCsrfTokenRepository.withHttpOnlyFalse(), 지연 로딩을 끈 CsrfTokenRequestAttributeHandler)` — board의 현재 설정과 같다.

- [ ] **Step 4: 통과 확인** — `./gradlew :services:auth:check` → 통과.

- [ ] **Step 5: Commit** — `feat(auth): Keycloak 로그인과 로그아웃을 auth로 옮긴다`

---

### Task 4: ForwardAuth 판정과 내부 토큰 발급

요구사항: COM-NFR-002, 003, 007, 008, 009, MEM-FR-001, TC-MEM-010, 011, 040, 041, 045, 048, 050, 052, Review Focus 2~4

**Files:**
- Create: `forward/ForwardedRequest.java`, `forward/ForwardedRequestMatcher.java`, `forward/ForwardedRequestFilter.java`, `forward/ForwardAuthController.java`
- Modify: `config/SecurityConfig.java`
- Test: `forward/ForwardedRequestTest.java`, `forward/ForwardAuthTest.java`

**Interfaces:**
- Consumes: Task 2 `InternalTokenIssuer`, `ProblemResponses`; Task 3 `LoginUsers.toInternalUser`
- Produces:
  - `String ForwardAuthController.PATH = "/forward-auth"` — 모든 HTTP 메서드를 받는다 (Traefik 설정에 따라 원래 메서드로 올 수 있다)
  - `record ForwardedRequest(HttpMethod method, String path)`; `static ForwardedRequest from(HttpServletRequest) throws AmbiguousForwardedRequestException` — `X-Forwarded-Method`·`X-Forwarded-Uri`가 없거나, 경로(쿼리 제외)가 모호하면 예외. 쿼리 문자열은 검사하지 않는다
  - `static ForwardedRequestMatcher ForwardedRequestMatcher.forwarded(@Nullable HttpMethod method, String pattern)` — 요청 경로가 `PATH`이고 원래 요청이 `PathPattern`과 메서드에 맞으면 일치
  - 응답: 통과 시 `200`, 로그인 상태면 `Authorization: Bearer <내부 토큰>`

규칙 순서(PRJ-SDS §7.3): 모호한 경로 `400`(필터) → `forwarded(GET, "/api/posts/**")`, `forwarded(GET, "/api/comments/**")` 공개 → `forwarded(null, "/api/**")` 인증 → `PATH` 나머지 거부(`denyAll`). CSRF 보호 대상: 요청이 `PATH`이면 원래 메서드가 GET·HEAD·TRACE·OPTIONS가 아닐 때, 그 밖에는 Spring 기본 판정. 거부 응답의 `instance`는 `PATH`일 때 원래 경로(쿼리 제외)다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```java
// ForwardedRequestTest — TC-MEM-048, Review Focus 3·4
@ParameterizedTest @ValueSource(strings = {"/api/posts/%2e%2e/members/me", "/api/posts%2F1", "/api/posts;x=1",
  "/api/posts/..%5C", "/api/posts/../members/me", "/api/posts/./1", "/api/posts/%252e%252e", "/api/posts\\1", "/api/posts/%00"})
void 모호한_경로는_거부한다(String uri)
@ParameterizedTest @ValueSource(strings = {"/api/posts?keyword=%ED%95%9C%EA%B8%80", "/api/posts?keyword=a/../b", "/api/posts/1/comments"})
void 쿼리와_정상_경로는_허용하고_쿼리를_뺀_경로를_돌려준다(String uri)
@Test void 전달_헤더가_없으면_거부한다()

// ForwardAuthTest (IntegrationTestBase, MockMvc). 헬퍼: forwarded(method, uri) = get(PATH).header("X-Forwarded-Method", method).header("X-Forwarded-Uri", uri)
@Test void 비로그인_공개_조회는_토큰_없이_통과한다()          // TC-MEM-050: 200, Authorization 헤더 없음
@Test void 로그인한_공개_조회는_내부_토큰을_붙여_통과한다()     // TC-MEM-050, 045: 200, Bearer; InternalTokenDecoders.create(앱의 JWKSource)로 검증 → sub·nickname·email·roles, exp-iat=60초
@Test void 비로그인_쓰기는_401_ProblemDetail이다()           // TC-MEM-040: forwarded(POST, "/api/posts/\"x\"") → 401, code UNAUTHENTICATED, instance가 원래 경로(인코딩)
@Test void 로그인했어도_CSRF_토큰_없는_쓰기는_403이다()        // TC-MEM-010, 041: 403 ProblemDetail ACCESS_DENIED, board와 같은 필드
@Test void CSRF_토큰과_함께_보낸_쓰기는_토큰을_붙여_통과한다() // 쿠키 XSRF-TOKEN=t, 헤더 X-XSRF-TOKEN=t → 200 + Bearer
@Test void 조회는_CSRF_쿠키를_발급한다()                     // TC-MEM-011: Set-Cookie XSRF-TOKEN
@Test void 세션이_없어진_브라우저의_공개_조회는_200이다()      // Review Focus 2: 존재하지 않는 SESSION 쿠키 → 200, 토큰 없음
@Test void api가_아닌_원래_경로는_거부한다()                  // TC-MEM-052: forwarded(GET, "/actuator/metrics") → 403
@Test void 모호한_원래_경로는_400이다()                       // TC-MEM-048: 400 INVALID_REQUEST ProblemDetail
@Test void 전달_헤더_없이_직접_호출하면_400이다()              // Review Focus 4
```

- [ ] **Step 2: 실패 확인** — `./gradlew :services:auth:test --tests '*forward*'` → 컴파일 실패.

- [ ] **Step 3: 구현** — 모호성 판정은 경로 부분에 대해: 원문에 `%2F`·`%5C`·`%25`·`%00`·`;`·`\`가 있으면 거부, 퍼센트 디코딩 후 `/`로 나눈 조각 중 `.`·`..`가 있거나 제어 문자가 있으면 거부. 디코딩 결과가 원래 경로와 다른 형태로 해석될 여지를 없애는 것이 목적이다. `ForwardedRequestFilter`는 `PATH` 요청에만 동작하고 `CsrfFilter`보다 앞에 둔다. `ForwardAuthController`는 인증 주체가 `OidcUser`일 때만 `LoginUsers.toInternalUser` → `issuer.issue`.

- [ ] **Step 4: 통과 확인** — `./gradlew :services:auth:check` → 통과.

- [ ] **Step 5: Commit** — `feat(auth): ForwardAuth 판정과 내부 토큰 발급을 추가한다`

---

### Task 5: board를 Resource Server로 바꾸고 인증 코드를 걷어 낸다

요구사항: COM-NFR-001, 002, 007, 020, 021, COM-IF-008, MEM-FR-002, 003, 005, 006, MEM-NFR-001, TC-MEM-002, 003, 008, 012~017, 019~022, 042~044, TC-COM-018, Review Focus 5

**Files:**
- Modify: `services/board/build.gradle.kts` (제거: `oauth2.client`, `session.data.redis`; 추가: `oauth2.resource.server`, `project(":libs:internal-token")`)
- Modify: `common/config/SecurityConfig.java` (전면 교체), `application.yml`(세션 제거, `bbs.internal-token.jwk-set-uri: http://localhost:8082/.well-known/jwks.json`), `application-local.yml`(OAuth2 Client 설정 삭제 → 파일이 비면 삭제), `src/test/resources/application-test.yml`(OAuth2 Client 설정 삭제)
- Create: `common/config/InternalTokenConfig.java`, `member/adapter/in/security/InternalTokenAuthenticationConverter.java`
- Modify: `member/adapter/in/web/CurrentMemberArgumentResolver.java`, `member/application/service/MemberService.java`(`getIdBySubject` 삭제 — 호출처가 없어진다)
- Delete: `member/adapter/in/security/BbsOidcUserService.java`, `common/config/SpaForwardingConfig.java` 및 테스트 `BbsOidcUserServiceTest`, `SpaForwardingTest`, `SecurityCsrfTest`, `CsrfCookieIssuanceTest`
- Create (test): `support/TestInternalTokens.java`, `support/TestInternalTokenKeys.java`(`@TestConfiguration`, `@Primary JWKSource`), `common/config/InternalTokenValidationTest.java`, `member/adapter/in/security/InternalTokenAuthenticationConverterTest.java`
- Modify (test): `IntegrationTestBase`(`@Import(TestInternalTokenKeys.class)`), `PostControllerTest`, `CommentControllerTest`, `MemberControllerTest`, `ActuatorAccessTest`, `UnauthenticatedResponseTest`, `MemberServiceTest`, `MemberProvisioningConcurrencyTest`(표시 이름만 "첫 요청"으로)

**Interfaces:**
- Consumes: Task 1 `InternalTokenDecoders.create`, `InternalTokenIssuer`, `InternalUser.from`
- Produces:
  - 빈 `JWKSource<SecurityContext> internalTokenKeys` (`JWKSourceBuilder.create(new URL(jwkSetUri)).build()`), `JwtDecoder jwtDecoder`
  - `InternalTokenAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken>` — 권한 `ROLE_` + `InternalUser.from(jwt).roles()`
  - 테스트 헬퍼 `TestInternalTokens.bearer(String subject, String... roles) : RequestPostProcessor` (닉네임 = subject, 이메일 = subject + "@example.com"), `TestInternalTokens.issue(InternalUser)`, `TestInternalTokens.KEY`

`SecurityConfig` 규칙(PRJ-SDS §7.3 board 표): health·info 공개 → 나머지 액추에이터 `ADMIN` → Swagger·`/v3/api-docs/**` 공개 → 나머지 `permitAll`. `sessionManagement(STATELESS)`, `csrf` 끔, `oauth2ResourceServer(jwt(converter))`, 리소스 서버와 전체의 진입점은 `401 UNAUTHENTICATED` ProblemDetail, 거부 처리기는 `403 ACCESS_DENIED` ProblemDetail(둘 다 기존 `ProblemDetails`와 메시지 변환기). `SecurityConfig`는 변환기를 `Converter<Jwt, ? extends AbstractAuthenticationToken>` 타입으로 주입받는다(`common` → `member` 의존 금지).

`CurrentMemberArgumentResolver`: 주체가 `Jwt`면 `InternalUser.from(jwt)`로 `memberService.provision(subject, nickname, email)`.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```java
// InternalTokenValidationTest (IntegrationTestBase) — TC-MEM-042~044, 019
@Test void 다른_키로_서명한_토큰은_401이다()      // 새 RSAKey로 서명 → GET /api/members/me → 401 ProblemDetail UNAUTHENTICATED
@Test void 만료된_토큰은_401이다()
@Test void 발급자나_대상이_다른_토큰은_401이다()
@Test void 공개_조회에_잘못된_토큰이_오면_401이다()  // GET /api/posts + 위조 토큰 → 401 (TC-COM-019의 대조군)
// InternalTokenAuthenticationConverterTest — TC-MEM-008, Review Focus 5
@Test void 역할은_ROLE_접두사_권한이_된다()       // roles [USER, ADMIN] → ROLE_USER, ROLE_ADMIN
@Test void 역할_클레임이_없으면_권한이_없다()
// MemberControllerTest — TC-MEM-022 (기존 테스트는 bearer로 교체)
@Test void 처음_보는_사용자는_첫_요청에서_회원이_생긴다() // DB에 없는 subject의 토큰 → GET /api/members/me 200, 닉네임·이메일이 토큰 값, member 1행
// ActuatorAccessTest — TC-MEM-014~017: oidcLogin() → bearer(subject, "USER"/"ADMIN"), 403은 ProblemDetail 확인 추가
```

기존 컨트롤러 테스트의 `로그인(subject, roles...)`은 `TestInternalTokens.bearer(subject, roles...)`로 바꾸고 역할 인자는 `"ROLE_ADMIN"` → `"ADMIN"`. `.with(csrf())`는 모두 지운다. 회원을 미리 만드는 `@BeforeEach`는 그대로 둔다.

- [ ] **Step 2: 실패 확인** — `./gradlew :services:board:test` → 새 테스트 컴파일 실패.

- [ ] **Step 3: 구현** — 위 Files와 Interfaces대로. `HexagonalArchitectureTest`, `FeatureBoundaryTest`가 라이브러리 패키지(`com.board.bbs.token`)를 기능으로 오인하면 규칙이 아니라 가져오기 범위에서 jar를 빼는 방식으로 고치고 Ruling으로 남긴다.

- [ ] **Step 4: 통과 확인** — `./gradlew :services:board:check` → 통과. `grep -rniE "keycloak|oauth2-client|oauth2\.client|session" services/board/build.gradle.kts services/board/src/main` → 출력 없음 (TC-COM-018).

- [ ] **Step 5: Commit** — `refactor(board): 내부 토큰으로 사용자를 식별하고 인증 코드를 auth로 넘긴다`

---

### Task 6: 진입점(Traefik)과 개발 실행 환경, E2E

요구사항: COM-NFR-006, 007, 021, COM-IF-007, 008, COM-CON-003, 004, MEM-FR-001, 004, TC-MEM-001, 024, 039, TC-COM-019~022, 024

**Files:**
- Modify: `deploy/compose.yaml` (서비스 `traefik` 추가), `deploy/.env.example` (`ENTRY_BIND_ADDRESS`), `deploy/keycloak/bbs-realm.json`
- Create: `deploy/traefik/dynamic/routes.yml`
- Modify: `services/web/angular.json` (`proxyConfig` 삭제), `services/web/playwright.config.ts`; Delete: `services/web/proxy.conf.json`
- Create: `services/web/e2e/gateway.spec.ts`
- Modify: `README.md`, `CLAUDE.md` (실행 방법)

**Interfaces:**
- Consumes: auth `/forward-auth`(8082), board(8080), web(5173)

compose `traefik`: 이미지 `traefik:v3.6`(실행 시 `docker pull`로 존재를 확인하고, 없으면 가장 최근 3.x 태그를 쓰고 Ruling으로 남긴다), `command`: `--entrypoints.web.address=:8000`, `--entrypoints.ping.address=:8090`, `--ping.entrypoint=ping`, `--providers.file.directory=/etc/traefik/dynamic`, `--providers.file.watch=true`; `ports: "${ENTRY_BIND_ADDRESS:-${BIND_ADDRESS:-127.0.0.1}}:8000:8000"`; `extra_hosts: ["host.docker.internal:host-gateway"]`; `volumes: ./traefik/dynamic:/etc/traefik/dynamic:ro`; `healthcheck: ["CMD", "traefik", "healthcheck", "--ping", "--entrypoints.ping.address=:8090", "--ping.entrypoint=ping"]`.

`routes.yml`:
- 라우터 `auth`: ``PathPrefix(`/oauth2`) || PathPrefix(`/login`) || Path(`/logout`)`` → 서비스 `auth`(`http://host.docker.internal:8082`)
- 라우터 `api`: ``PathPrefix(`/api`)``, 미들웨어 `[strip-authorization, forward-auth]` → 서비스 `board`(`http://host.docker.internal:8080`)
- 라우터 `web`: ``PathPrefix(`/`)``, `priority: 1` → 서비스 `web`(`http://host.docker.internal:5173`)
- 미들웨어 `strip-authorization`: `headers.customRequestHeaders.Authorization: ""`
- 미들웨어 `forward-auth`: `forwardAuth.address: http://host.docker.internal:8082/forward-auth`, `authResponseHeaders: [Authorization]`, `addAuthCookiesToResponse: [XSRF-TOKEN]`

realm `bbs-app` 클라이언트: `redirectUris`·`webOrigins`·`post.logout.redirect.uris`를 `http://__BBS_HOST__:8000` 기준 하나로 바꾼다.

`playwright.config.ts`: `baseURL`을 `http://${host}:8000`으로, `webServer.url`은 화면 개발 서버(`:5173`) 그대로.

- [ ] **Step 1: E2E 시나리오를 쓴다** (`gateway.spec.ts`, 비로그인 상태 `test.use({ storageState: { cookies: [], origins: [] } })`)

```ts
test("브라우저가 보낸 Authorization 헤더는 board에 닿지 않는다", ...)  // TC-COM-019: request.get("/api/posts", {headers:{Authorization:"Bearer forged"}}) → 200
test("비로그인 쓰기는 진입점에서 401 ProblemDetail이다", ...)        // TC-MEM-024: request.post("/api/posts", 본문) → 401, code UNAUTHENTICATED
test("액추에이터와 공개키는 진입점으로 열리지 않는다", ...)          // TC-COM-020: /actuator/health, /actuator/metrics, /.well-known/jwks.json → content-type text/html
test("화면 경로로 직접 들어오면 화면을 돌려준다", ...)               // TC-COM-021: page.goto("/posts/1") → 화면의 banner가 보임
test("없는 API 경로는 404 ProblemDetail이다", ...)                  // TC-COM-022: request.get("/api/does-not-exist") → 404, application/problem+json
```

- [ ] **Step 2: 실패 확인** — compose를 띄우기 전 구성으로 `pnpm e2e` → `gateway.spec` 실패(진입점 없음).

- [ ] **Step 3: 구성 변경** — 위 파일들. README의 실행 순서는 `docker compose ... up -d` → `./gradlew :services:auth:bootRun` → `./gradlew :services:board:bootRun` → `cd services/web && pnpm dev` → 브라우저 `http://localhost:8000`. "다른 기기에서 접속할 때"는 `ENTRY_BIND_ADDRESS`와 `KEYCLOAK_BIND_ADDRESS`를 함께 연다. CLAUDE.md 검증 명령에 auth를 추가한다(`./gradlew check`가 모든 모듈을 포함함을 명시).

- [ ] **Step 4: 실행 환경 확인**
  - `.env` 없이 `docker compose -f deploy/compose.yaml up -d --wait` → 모든 서비스 healthy, `docker compose ps`의 진입점 포트 `127.0.0.1:8000` (TC-COM-024)
  - auth·board·web을 띄우고 `curl -si localhost:8000/api/posts`의 응답에 `Set-Cookie: XSRF-TOKEN`이 있음 (`addAuthCookiesToResponse` 동작 확인. 없으면 Traefik 버전을 확인하고 원인을 Ruling으로 남긴 뒤 해결)
  - `cd services/web && pnpm e2e` → 기존 4개 + `gateway.spec` 5개 통과 (TC-MEM-001, 024, 039, TC-COM-019~022)

- [ ] **Step 5: Commit** — 진입점·compose·realm·화면 설정·문서를 한 커밋으로: `feat(deploy): Traefik 진입점으로 auth와 board를 연결한다`

---

### Task 7: CI

요구사항: COM-NFR-033, 036, TC-COM-016, 017, 023

**Files:**
- Create: `.github/workflows/auth.yml` (board.yml과 같은 형태, `./gradlew :services:auth:check :spotlessCheck`, 실패 시 `services/auth/build/reports/` 업로드)
- Modify: `.github/workflows/board.yml` (`paths`에 `libs/**`), `.github/workflows/e2e.yml`
- Modify: `docs/project/sds.md` §10.2는 Task 1~6에서 이미 맞으면 그대로. `libs/**`를 실행 조건 설명에 추가

`auth.yml`의 `paths`: `services/auth/**`, `libs/**`, `build-logic/**`, `gradle/**`, `config/**`, `settings.gradle.kts`, `build.gradle.kts`, `gradlew`, `deploy/compose.yaml`, 자기 파일.

`e2e.yml`: `paths`에 `services/auth/**`, `libs/**` 추가. board 시작 단계 앞에 auth 시작 단계(`nohup ./gradlew :services:auth:bootRun > auth.log`, `/actuator/health` 대기)를 추가하고, 실패 시 업로드 목록에 `auth.log`. 같은 Gradle 데몬 경쟁을 피하려고 auth가 healthy가 된 뒤 board를 시작한다.

- [ ] **Step 1: 구성 검토** — `paths` 목록이 위와 같은지 확인한다 (TC-COM-017, 023의 구성 검토 근거).
- [ ] **Step 2: Commit** — `ci: auth 워크플로를 추가하고 E2E에 auth를 띄운다`
- [ ] **Step 3: PR의 CI 결과로 판정** — push 후 `auth`, `board`, `web`, `e2e` 실행 기록 (TC-COM-016, 023. 사용자 확인 후 push).

---

### Task 8: 문서 마무리와 QA 판정

요구사항: CLAUDE.md 4·7, 문서 체계 §3

**Files:**
- Modify: `docs/project/diagrams/context.drawio.svg` (진입점, auth, board, web, Keycloak은 auth와만 연결)
- Modify: `docs/project/sds.md` §7.1 셋째 항목 — 구현대로 "역할 걸러내기는 로그인할 때 세션의 권한에 반영하고, 닉네임·이메일 대체값은 내부 토큰을 만들 때 세션의 사용자 정보에 적용한다"로 고친다. `docs/features/member/sds.md` §2.1, §3.1 4단계도 같은 내용으로
- Modify: `docs/features/member/qa-checklist.md`, `docs/project/qa-checklist.md` — 실제 실행 결과로 판정. 테스트 이름은 실제 이름으로 채운다
- Modify: `docs/project/srs.md`, `docs/features/member/srs.md` — 검증 열에 남은 오래된 테스트 이름이 없는지 확인

- [ ] **Step 1: 구성도** — `drawio --export --format svg --theme light --embed-diagram`으로 내보내고, `--format png`로 렌더링을 확인한다. 라벨에 `html=1`을 쓰지 않는다.
- [ ] **Step 2: 전체 검증 실행** — 루트에서 `./gradlew test --rerun check`, `cd services/web && pnpm verify`, board 실행 중 `pnpm gen:api` 후 `pnpm typecheck`(생성 파일 변화 없음 기대), `pnpm e2e`. 출력을 작업 공간에 저장하고 판정의 근거로 쓴다.
- [ ] **Step 3: QA 판정** — MEM-QA 1.5.0 → 1.6.0, PRJ-QC 1.5.0 → 1.6.0의 판정과 수행 정보를 채운다. 실행하지 않은 항목은 N/T. CI로 판정하는 TC-COM-016, 023은 PR의 실행 기록이 나온 뒤 채운다.
- [ ] **Step 4: Commit** — `docs: auth 분리의 QA 판정과 구성도를 반영한다`

ADR-0016은 PR이 병합된 뒤 마무리 커밋에서 Accepted로 바꾼다(단계 1과 같은 절차).
