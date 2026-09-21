# 게시판 (bbs)

Java 25 · Spring Boot 4.1.1 위에서 헥사고날 아키텍처로 구현한 REST 게시판 API와, React 19로 만든 화면.

기능을 채우는 것보다 **설계 의도가 코드와 빌드로 강제되는지**에 무게를 둔 참고 구현이다.

## 이 저장소에서 볼 수 있는 것

- **구조가 테스트로 강제된다.** 의존 방향, 트랜잭션 경계, 엔티티와 컨트롤러의 위치를 ArchUnit이 검증한다. 문서는 낡지만 테스트는 낡지 않는다.
- **규칙이 도메인 안에 있다.** 제목 길이, 답글 깊이, 수정·삭제 권한 같은 규칙은 모두 도메인 객체 안에 있다. 서비스는 조율만 하므로 규칙을 우회하는 경로가 없다.
- **null 계약이 컴파일러에게 검사된다.** 모든 패키지가 JSpecify `@NullMarked`이고 NullAway가 위반 시 컴파일을 실패시킨다.
- **API 계약이 프론트엔드 컴파일로 강제된다.** OpenAPI 스키마에서 타입을 생성하므로 백엔드가 바뀌면 프론트엔드 타입 검사가 깨진다. 테스트의 API 목도 같은 타입을 쓴다.
- **동시성이 데이터베이스 수준에서 처리된다.** 카운터는 원자적 `UPDATE`, 중복 좋아요는 유니크 제약과 `ON CONFLICT`, 조회수 중복은 Redis `SETNX`로 판정한다. 가상 스레드로 동시 요청을 보내 검증한다.

## 요구 환경

- JDK 25 (Temurin 25.0.4 LTS 기준, `.sdkmanrc` 참고)
- Node.js 24 이상, pnpm 11 (화면을 함께 띄울 때)
- Docker

## 실행

```bash
docker compose up -d
./gradlew bootRun
```

`compose.yaml`이 PostgreSQL · Redis · Keycloak을 띄우고, Keycloak realm은 `docker/keycloak/bbs-realm.json`에서 자동으로 구성된다. 별도 수작업 없이 바로 로그인을 시험할 수 있다.

| 주소 | 용도 |
|---|---|
| http://localhost:8080/swagger-ui.html | API 문서 |
| http://localhost:8080/oauth2/authorization/keycloak | 로그인 시작 |
| http://localhost:8081 | Keycloak 관리 콘솔 (`admin` / `admin`) |

테스트 계정은 `tester` / `tester` (일반), `admin-user` / `admin` (관리자)이다.

### 프론트엔드

```bash
cd frontend
pnpm install
pnpm dev
```

`http://localhost:5173`에서 화면을 연다. 개발 서버가 `/api`, `/oauth2`, `/login`, `/logout`을 백엔드로 프록시하므로 브라우저 입장에서는 동일 오리진이며 세션 쿠키가 그대로 동작한다.

백엔드 API가 바뀌면 타입을 다시 생성한다.

```bash
cd frontend && pnpm gen:api
```

### 다른 기기에서 접속할 때

인증은 브라우저가 실제로 닿을 수 있는 주소로만 동작한다. `localhost`가 아닌 주소로 접속한다면 서버와 Keycloak에 같은 주소를 알려 줘야 한다.

```bash
export BBS_HOST=<서버 주소>

docker compose up -d
./gradlew bootRun
cd frontend && pnpm dev --host
```

`BBS_HOST`는 Keycloak의 공개 주소와 허용 리다이렉트 URI, 그리고 백엔드가 참조하는 issuer를 한꺼번에 결정한다. 지정하지 않으면 `localhost`로 동작한다.

한 번에 하나의 주소만 쓸 수 있다. `BBS_HOST`를 바꾸면 Keycloak을 다시 만들어야 realm의 리다이렉트 URI가 갱신된다.

```bash
docker compose rm -sf keycloak && docker compose up -d keycloak
```

## 빌드와 검증

```bash
./gradlew build
```

이 한 줄이 아래를 모두 수행하며, 하나라도 실패하면 빌드가 실패한다.

| 검사 | 도구 |
|---|---|
| 포맷 | Spotless (google-java-format) |
| 정적 분석 | Checkstyle (Google Style) |
| null 안정성 | NullAway (JSpecify 모드) |
| 테스트 | JUnit 5 · Testcontainers |
| 아키텍처 규칙 | ArchUnit |
| 커버리지 | JaCoCo (전체 80%, 도메인·애플리케이션 90%) |

통합 테스트는 Testcontainers로 실제 PostgreSQL과 Redis를 띄우므로 Docker가 필요하다.

프론트엔드는 `cd frontend && pnpm verify`가 Biome, 타입 검사, 테스트를 순서대로 실행한다. E2E는 백엔드와 컨테이너가 필요하므로 `pnpm e2e`로 따로 실행한다.

`installGitHooks` 태스크가 `build` 시 자동으로 실행되어, 커밋 전에 포맷과 정적 분석을 검사하는 훅을 설치한다. `frontend/` 아래 변경이 있으면 프론트엔드 검사도 함께 실행한다.

## 구조

```
com.board.bbs
├── common/            공유 커널 (설정, 에러 규약, 보안 지원)
├── member/            회원
├── post/              게시글
└── comment/           댓글
```

기능별 패키지 안이 다시 헥사고날 계층으로 나뉜다.

```
post/
├── domain/                 순수 Java. 프레임워크 의존 없음
├── application/
│   ├── port/in/            유스케이스 인터페이스
│   ├── port/out/           영속성·부가기능 포트
│   └── service/            트랜잭션 경계
└── adapter/
    ├── in/web/             컨트롤러, 요청·응답 record
    ├── out/persistence/    JPA 엔티티, 매퍼, 어댑터
    └── out/redis/          Redis 어댑터
```

### 의존 규칙

`domain ← application ← adapter` 한 방향으로만 흐른다. `HexagonalArchitectureTest`가 아래를 강제한다.

- 도메인은 `org.springframework`, `jakarta`, `com.querydsl`을 참조할 수 없다
- 애플리케이션은 어댑터를 참조할 수 없다 (포트 인터페이스로만 통신)
- 인바운드 어댑터는 아웃바운드 어댑터를 참조할 수 없다
- `@Transactional`은 `application.service`에만 존재한다
- JPA 엔티티는 `adapter.out.persistence`에만, 컨트롤러는 `adapter.in.web`에만 존재한다
- 모든 패키지에 `@NullMarked` 선언이 있어야 한다

프론트엔드는 `frontend/`에 있으며 백엔드와 같은 기능별 분리를 따른다.

```
frontend/src/
├── api/          생성된 타입, 클라이언트, ProblemDetail 파싱
├── features/     post, comment, member — 각각 queries·mutations·components
├── routes/       파일 기반 라우트
└── components/   레이아웃과 공용 UI
```

## API

| 메서드 | 경로 | 인증 |
|---|---|---|
| `POST` | `/api/posts` | 필요 |
| `GET` | `/api/posts` | 불필요 |
| `GET` | `/api/posts/{id}` | 불필요 |
| `PATCH` | `/api/posts/{id}` | 작성자 |
| `DELETE` | `/api/posts/{id}` | 작성자 또는 관리자 |
| `POST` `DELETE` | `/api/posts/{id}/likes` | 필요 |
| `POST` | `/api/posts/{postId}/comments` | 필요 |
| `GET` | `/api/posts/{postId}/comments` | 불필요 |
| `PATCH` `DELETE` | `/api/comments/{id}` | 작성자 (삭제는 관리자도) |
| `GET` | `/api/members/me` | 필요 |

모든 에러 응답은 RFC 9457 ProblemDetail 형식이며 `code` 확장 필드를 포함한다.

```json
{
  "type": "urn:bbs:error:access_denied",
  "title": "Forbidden",
  "status": 403,
  "detail": "권한이 없습니다.",
  "instance": "/api/posts/2",
  "code": "ACCESS_DENIED"
}
```

## 주요 설계 결정

### QueryDSL은 openfeign 포크를 쓴다

`com.querydsl:querydsl-jpa`의 최신 버전(5.1.0)은 Hibernate 6을 대상으로 빌드되어 있다. Spring Boot 4.1은 Hibernate 7을 쓰므로 그대로는 동작하지 않는다. Hibernate 7을 대상으로 유지보수되는 `io.github.openfeign.querydsl` 7.0을 사용한다.

### 도메인과 JPA 엔티티를 분리한다

매퍼 코드가 늘어나는 대가를 치르지만, 도메인 테스트가 스프링 컨텍스트 없이 밀리초 단위로 돌고 영속성 기술을 바꿔도 도메인이 영향을 받지 않는다.

### 권한 검사는 도메인에 둔다

`post.updateBy(requester, ...)`가 작성자 여부를 판단한다. 서비스로 옮기면 서비스를 거치지 않는 경로가 생길 때 규칙이 새어 나간다. 관리자는 삭제만 가능하고 수정은 불가능한데, 이 구분도 도메인 안에 있다.

### 카운터는 원자적 UPDATE로 증감한다

엔티티를 읽어 `+1` 하고 저장하면 동시 요청에서 값을 잃는다. `UPDATE post SET view_count = view_count + 1`로 데이터베이스가 직접 더하게 한다.

### 중복은 데이터베이스가 판정한다

좋아요 중복은 `(post_id, member_id)` 유니크 제약과 `INSERT ... ON CONFLICT DO NOTHING`으로 처리한다. 사전 조회 후 저장하는 방식은 두 요청이 동시에 통과할 수 있고, 제약 위반 예외가 발생하면 진행 중인 트랜잭션 전체를 쓸 수 없게 된다.

### 목록과 상세는 다른 응답이다

목록은 본문을 담지 않는 별도 프로젝션으로 조회하고, 작성자 닉네임은 조인으로 함께 읽어 N+1이 생길 여지를 없앤다. 정렬에 식별자를 포함시켜 작성 시각이 같은 행들의 순서를 고정하므로 페이지 경계에서 행이 겹치거나 빠지지 않는다.

### 스키마는 Flyway가 유일한 출처다

`ddl-auto: validate`로 두어 엔티티가 DDL을 만들지 않는다. 도메인 규칙은 데이터베이스 제약으로도 한 번 더 막는다 (댓글 깊이 `CHECK`, 좋아요 유니크).

### 세션은 Redis에 저장한다

애플리케이션 인스턴스를 무상태로 유지해 수평 확장이 가능하다. 인증은 Keycloak OIDC이며, 최초 로그인 시 `sub`를 기준으로 로컬 회원을 자동 생성한다.

## 문서

- [백엔드 설계 문서](docs/superpowers/specs/2026-09-18-bbs-design.md) · [구현 계획](docs/superpowers/plans/2026-09-18-bbs.md)
- [프론트엔드 설계 문서](docs/superpowers/specs/2026-09-21-bbs-frontend-design.md) · [구현 계획](docs/superpowers/plans/2026-09-21-bbs-frontend.md)
- [프론트엔드 안내](frontend/README.md)
