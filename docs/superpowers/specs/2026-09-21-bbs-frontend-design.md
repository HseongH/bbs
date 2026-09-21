# 게시판 프론트엔드 설계 문서

- 작성일: 2026-09-21
- 상태: 승인됨
- 선행 문서: [게시판 설계 문서](2026-09-18-bbs-design.md)

## 1. 목적과 범위

기존 게시판 REST API 위에 React 화면을 올린다. 백엔드와 같은 기준을 적용한다 — **계약과 규칙이 빌드로 강제되어야 하고, 어긋나면 컴파일이 깨져야 한다.**

### 포함

- 게시글 목록 (검색, 오프셋 페이징)
- 게시글 상세 (댓글·대댓글, 좋아요)
- 게시글 작성·수정 폼
- 로그인 상태 표시와 권한별 UI
- 미인증 요청에 대한 자동 로그인 이동

### 제외 (YAGNI)

- 관리자 백오피스 화면
- 다크 모드, 국제화
- 무한 스크롤 (백엔드가 오프셋 페이징이므로 페이지 번호 UI를 쓴다)
- 서버 사이드 렌더링

## 2. 기술 스택

확인일(2026-09-21) 기준 최신 버전이다. 구현 시 실제 설치로 검증한다.

| 영역 | 선택 | 버전 |
|---|---|---|
| 런타임 | Node.js | 24.18.0 |
| 패키지 관리자 | pnpm | 11.25.0 |
| UI | React | 19.3.0 |
| 번들러 | Vite | 8.3.0 |
| 라우팅 | TanStack Router | 1.170.x |
| 서버 상태 | TanStack Query | 5.103.x |
| API 타입 | openapi-typescript | 7.13.0 |
| API 호출 | openapi-fetch | 0.17.0 |
| 스타일 | Tailwind CSS | 4.3.x |
| 컴포넌트 | shadcn/ui | CLI로 복사 |
| 린트·포맷 | Biome | 2.5.x |
| 타입 검사 | TypeScript | 7.0.x |
| 단위·컴포넌트 테스트 | Vitest + Testing Library | 5.0.x |
| API 목 | MSW | 2.15.x |
| E2E | Playwright | 1.63.x |

Node와 pnpm 버전은 `package.json`의 `engines`와 `packageManager` 필드로 고정한다. 백엔드가 `.sdkmanrc`로 JDK와 Gradle을 고정하는 것과 같은 이유다.

TypeScript 7은 네이티브 포팅 버전이다. 도구 호환성 문제가 드러나면 6.x 계열로 내린다. 이 판단은 구현 첫 단계에서 실제 빌드로 확정한다.

## 3. 배치와 개발 흐름

### 3.1 같은 저장소, 동일 오리진

백엔드는 Keycloak OIDC 세션 쿠키로 인증하며 CORS 설정이 없다. 프론트엔드를 다른 오리진에 두면 CORS와 SameSite 쿠키 문제를 동시에 떠안는다. 개발 중에는 Vite 프록시로 동일 오리진을 만든다.

```
/api/*     → http://localhost:8080
/oauth2/*  → http://localhost:8080
/login/*   → http://localhost:8080
/logout    → http://localhost:8080
```

브라우저에는 모든 요청이 `localhost:5173`으로 보이므로 세션 쿠키가 그대로 실린다. **백엔드에 CORS를 추가하지 않는다.**

### 3.2 배포

`pnpm build` 산출물을 Spring의 정적 리소스로 서빙한다. 프론트엔드 빌드를 Gradle 빌드에 묶지 않는다. 묶으면 백엔드만 고치는 경우에도 Node 툴체인이 필요해진다.

## 4. 선행 백엔드 수정

프론트엔드 작업의 전제 조건이므로 같은 작업에 포함한다.

### 4.1 OpenAPI에서 내부 파라미터 제거

`@CurrentMember`로 주입되는 인자가 필수 쿼리 파라미터로 문서화되고 있다.

```
DELETE /api/posts/{id}   requester (query, required)
POST   /api/posts        author    (query, required)
GET    /api/members/me   memberId  (query, required)
GET    /api/posts/{id}   viewer    (query, optional)
```

springdoc이 커스텀 인자 리졸버를 모르기 때문이다. Swagger UI가 잘못된 사용법을 안내하고, "작성자를 쿼리로 지정할 수 있다"는 오해를 준다. 스키마에서 타입을 생성하면 이 가짜 파라미터가 그대로 딸려 온다.

`SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentMember.class)`를 설정에 한 줄 추가해 해결한다. 컨트롤러마다 `@Parameter(hidden = true)`를 붙이는 방식보다 낫다 — 새 엔드포인트에서 빠뜨릴 여지가 없다.

### 4.2 Pageable 평탄화

`GET /api/posts`와 `GET /api/posts/{postId}/comments`의 `Pageable`이 객체 하나로 문서화된다. `@ParameterObject`를 붙여 `page`, `size`, `sort`로 평탄화한다.

### 4.3 SPA 딥링크 포워딩

`/posts/3` 같은 클라이언트 라우트로 직접 접근하면 현재 404가 반환된다. `/api`, `/actuator`, `/swagger-ui`, `/v3/api-docs`, `/oauth2`, `/login`, `/logout`을 제외한 경로를 `index.html`로 포워딩한다.

정적 리소스가 없는 개발 환경에서는 이 포워딩이 동작하지 않아도 무방하다. Vite 개발 서버가 라우팅을 담당하기 때문이다.

## 5. 디렉터리 구조

백엔드의 기능별 분리를 따른다.

```
frontend/
├── src/
│   ├── api/
│   │   ├── schema.d.ts       생성물. 커밋하되 직접 수정하지 않는다
│   │   ├── client.ts         openapi-fetch 인스턴스, 401 미들웨어
│   │   └── problem.ts        ProblemDetail 파싱과 판별
│   ├── features/
│   │   ├── post/
│   │   │   ├── queries.ts    queryKey 정의와 조회 훅
│   │   │   ├── mutations.ts  변경 훅과 무효화 범위
│   │   │   └── components/
│   │   ├── comment/          (동일 구조)
│   │   └── member/           (동일 구조)
│   ├── routes/               TanStack Router 파일 기반 라우트
│   ├── components/
│   │   ├── ui/               shadcn/ui 복사본
│   │   └── layout/           헤더, 레이아웃
│   ├── lib/                  유틸리티
│   └── test/                 테스트 설정, MSW 핸들러
├── e2e/                      Playwright 시나리오
├── biome.json
├── tsconfig.json
├── vite.config.ts
└── package.json
```

**`features/*/queries.ts`가 그 기능의 `queryKey`와 무효화 범위를 소유한다.** 컴포넌트가 키를 직접 조립하면 무효화가 새어 나간다.

## 6. API 계층

### 6.1 타입 생성

`openapi-typescript`로 `/v3/api-docs`에서 `src/api/schema.d.ts`를 생성한다.

- 생성은 `pnpm gen:api`로 **수동 실행**하며, 실행 중인 백엔드의 `http://localhost:8080/v3/api-docs`를 읽어 결과물을 커밋한다
- 빌드가 실행 중인 서버에 의존하면 CI에서 깨진다
- 생성물이 커밋되어 있으므로 백엔드 변경이 diff로 드러난다

### 6.2 호출

`openapi-fetch`로 호출한다. 경로, 쿼리, 요청 본문, 응답 타입이 모두 스키마에서 추론되므로 백엔드 계약이 바뀌면 `tsc`가 깨진다.

```ts
const { data, error } = await client.GET("/api/posts", {
  params: { query: { keyword, page, size } },
});
```

### 6.3 401 처리

fetch 미들웨어가 401 응답을 가로챈다.

1. 현재 경로를 `sessionStorage`에 저장한다
2. `window.location.href = "/oauth2/authorization/keycloak"`로 이동한다
3. 로그인 후 앱 진입 시 저장된 경로로 복귀한다

이 처리는 미들웨어 한 곳에만 있다. 개별 호출부가 401을 다루지 않는다.

## 7. 라우트와 상태

### 7.1 라우트

| 경로 | 화면 | 인증 |
|---|---|---|
| `/` | 목록 (검색, 페이징) | 불필요 |
| `/posts/new` | 작성 | 필요 |
| `/posts/$postId` | 상세, 댓글, 좋아요 | 불필요 (쓰기는 필요) |
| `/posts/$postId/edit` | 수정 | 작성자 |

인증이 필요한 라우트는 `beforeLoad`에서 회원 정보를 확인하고, 없으면 로그인으로 보낸다.

### 7.2 상태 구분

| 상태 | 위치 | 이유 |
|---|---|---|
| 검색어, 페이지 번호, 정렬 | URL 검색 파라미터 | 새로고침·뒤로가기·공유가 동작해야 한다 |
| 서버 데이터 | TanStack Query | 캐싱과 무효화를 한 곳에서 관리한다 |
| 폼 입력값 | 컴포넌트 지역 상태 | 제출 전까지 다른 곳이 알 필요가 없다 |

**클라이언트 전역 상태 라이브러리를 두지 않는다.** 위 세 가지를 빼면 전역으로 관리할 것이 남지 않는다.

URL 검색 파라미터는 TanStack Router의 `validateSearch`로 검증한다. 잘못된 값이 들어와도 기본값으로 정규화되므로 화면이 깨지지 않는다.

### 7.3 무효화 전략

| 동작 | 무효화 대상 |
|---|---|
| 게시글 작성 | 목록 |
| 게시글 수정 | 해당 상세, 목록 |
| 게시글 삭제 | 목록 (상세는 라우트 이동) |
| 댓글 작성·수정·삭제 | 해당 게시글의 댓글 목록 |
| 좋아요 | 해당 상세 (낙관적 업데이트 후 확정) |

## 8. React 19 활용

- **폼은 Actions와 `useActionState`** — 제출 중 상태, 성공, 서버 오류를 한 곳에서 다룬다. 백엔드가 주는 `errors` 필드를 필드별 메시지로 연결한다.
- **좋아요는 `useOptimistic`** — 클릭 즉시 반영하고 실패하면 되돌린다. 백엔드가 409(`ALREADY_LIKED`, `NOT_LIKED`)를 명확히 주므로 되돌릴 근거가 있다.
- **`ref`를 일반 prop으로 받는다** — `forwardRef`를 쓰지 않는다.

## 9. 에러 처리

모든 에러 응답이 RFC 9457 ProblemDetail 한 형식이므로 파싱도 한 곳이다. `code` 확장 필드로 분기한다.

| code | 처리 |
|---|---|
| `UNAUTHENTICATED` | 로그인으로 이동 (미들웨어) |
| `ACCESS_DENIED` | 권한 없음 안내 |
| `INVALID_REQUEST` | `errors`를 폼 필드에 표시 |
| `POST_NOT_FOUND`, `COMMENT_NOT_FOUND` | 404 화면 |
| `ALREADY_LIKED`, `NOT_LIKED` | 낙관적 업데이트 되돌림 |
| 그 외 | 에러 경계에서 재시도 UI |

## 10. 테스트 전략

| 층 | 도구 | 대상 |
|---|---|---|
| 컴포넌트·훅 | Vitest + Testing Library | 렌더링, 상호작용, 폼 검증, 권한별 UI |
| API 연동 | MSW | 네트워크 계층에서 가로채 실제 호출 경로 검증 |
| E2E | Playwright | 로그인 → 작성 → 댓글 → 좋아요 |

**MSW 핸들러도 생성된 타입을 사용한다.** 목이 실제 계약과 어긋나면 컴파일이 깨지므로, 목만 통과하고 실제로는 동작하지 않는 상황을 막는다.

Playwright는 Keycloak 로그인을 한 번 수행해 `storageState`로 저장하고 나머지 시나리오가 재사용한다. E2E는 백엔드와 Docker 컨테이너가 떠 있어야 하므로 기본 검증 명령에는 포함하지 않는다.

## 11. 품질 게이트

`pnpm verify` 하나가 아래를 순서대로 실행한다. 백엔드의 `./gradlew build`와 같은 역할이다.

1. Biome — 린트와 포맷 검사
2. `tsc --noEmit` — 타입 검사
3. Vitest — 단위·컴포넌트 테스트

TypeScript는 `strict`에 더해 `noUncheckedIndexedAccess`를 켠다. 배열 인덱싱이 `undefined`를 반환할 수 있음을 타입이 말하게 하는 설정이며, 백엔드의 NullAway와 같은 역할을 한다.

기존 `hooks/pre-commit`에 프론트엔드 검사를 덧붙인다. `frontend/` 아래 변경이 있을 때만 실행한다.

## 12. 구현 순서

커밋 메시지는 AngularJS 컨벤션을 따른다.

1. `fix: OpenAPI 문서에서 내부 파라미터 제거`
2. `feat: SPA 딥링크 포워딩 추가`
3. `chore: 프론트엔드 프로젝트 및 품질 도구 구성`
4. `feat: API 타입 생성과 클라이언트 계층`
5. `feat: 레이아웃과 로그인 상태 표시`
6. `feat: 게시글 목록 화면`
7. `feat: 게시글 상세 화면`
8. `feat: 게시글 작성 및 수정 폼`
9. `feat: 댓글 및 대댓글`
10. `feat: 좋아요`
11. `test: E2E 시나리오 추가`
12. `docs: README에 프론트엔드 실행 방법 추가`

## 13. 완료 기준

- `pnpm verify`가 통과한다
- `./gradlew build`가 여전히 통과한다 (백엔드 수정 포함)
- 백엔드와 프론트엔드를 띄운 상태에서 로그인, 글 작성, 검색, 댓글, 좋아요가 모두 동작한다
- `/v3/api-docs`에 `requester`, `author`, `memberId`, `viewer` 파라미터가 없다
- 브라우저에서 `/posts/1`로 직접 접근해도 화면이 뜬다
