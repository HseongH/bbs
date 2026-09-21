# 게시판 프론트엔드 Angular 전환 설계 문서

- 작성일: 2026-09-21
- 상태: 승인됨
- 선행 문서: [프론트엔드 설계 문서](2026-09-21-bbs-frontend-design.md)

## 1. 목적

기존 React 프론트엔드를 Angular로 다시 구현한다. 기능은 동일하게 유지하고, 백엔드는 건드리지 않는다.

전환 동기는 구조다. Angular의 의존성 주입, 데코레이터 기반 메타데이터, 서비스 계층 분리는 이 저장소의 Spring 백엔드와 발상이 닮아 있다. 한 저장소 안에서 앞뒤가 같은 사고방식으로 읽히는 것이 이 전환의 목표다.

### 유지되어야 할 것

- 기능 동등성: 목록·검색·페이징, 상세, 작성·수정·삭제, 댓글·대댓글, 좋아요, 로그인·로그아웃
- 계약 강제: OpenAPI 스키마에서 생성한 타입이 컴파일을 막아선다
- URL 상태: 검색어와 페이지가 URL에 남아 새로고침·뒤로가기·공유가 동작한다
- 품질 게이트: 린트, 타입 검사, 테스트가 한 명령으로 돌고 실패하면 멈춘다

### 제외

- 백엔드 변경
- 새 기능 추가
- 디자인 개편

## 2. 계층 대응

Angular 구조를 Spring 백엔드와 같은 방식으로 나눈다.

| Spring | Angular | 책임 |
|---|---|---|
| `@RestController` | 컴포넌트 | 화면과 사용자 입력 |
| `@Service` | `*Store` 서비스 | 조회·변경 조율, 재조회 범위 소유 |
| `@Repository` | `*ApiService` | HTTP 호출과 타입 경계 |
| `@ControllerAdvice` | `HttpInterceptor` | 401 처리 |
| DI 컨테이너 | Angular DI | 생성자 주입 |

React 버전에서 `features/*/queries.ts`가 갖고 있던 "무효화 범위 소유"를 `*Store` 서비스가 맡는다.

## 3. 기술 스택

확인일(2026-09-21) 기준. 구현 시 실제 설치로 검증한다.

| 영역 | 선택 | 버전 |
|---|---|---|
| 프레임워크 | Angular | 22.1.7 |
| CLI | Angular CLI | 22.1.8 |
| 변경 감지 | zoneless (`provideZonelessChangeDetection`) | `@publicApi 20.2` |
| 서버 상태 | `httpResource` + signal | `@publicApi 22.0` |
| 폼 | Signal Forms (`form`, `schema`) | `@publicApi 22.0` |
| UI | Tailwind CSS + Angular CDK | 4.3.x / 22.1.7 |
| API 타입 | openapi-typescript 생성물 재사용 | 7.13.0 |
| 린트·포맷 | angular-eslint + Prettier | 22.5.0 / 3.9.x |
| 단위·컴포넌트 테스트 | Angular CLI `unit-test` 빌더(Vitest) + Testing Library | 19.5.x |
| API 목 | MSW | 2.15.x |
| E2E | Playwright | 1.63.x |

Signal Forms는 22에 정식 등재되었으나 공개된 지 오래되지 않았다. 서버 검증 오류를 필드에 연결하는 과정에서 막히면 Reactive Forms로 전환하고, 그 판단은 폼을 처음 구현하는 태스크에서 확정한다.

## 4. 서버 상태

### 4.1 조회

`httpResource`가 담당한다. 파라미터를 signal로 넘기면 값이 바뀔 때 자동으로 다시 불러온다.

```ts
readonly posts = httpResource<PostPage>(() => ({
  url: "/api/posts",
  params: { page: this.page(), size: this.size(), ...this.keywordParam() },
}));
```

### 4.2 변경과 재조회

변경은 `HttpClient`로 보내고, 성공하면 관련 resource의 `reload()`를 호출한다. TanStack Query의 캐시와 자동 무효화가 없으므로 재조회 범위를 명시적으로 설계한다.

| 동작 | 재조회 대상 |
|---|---|
| 게시글 작성 | 목록 |
| 게시글 수정 | 해당 상세, 목록 |
| 게시글 삭제 | 목록 (상세는 라우트 이동) |
| 댓글 작성·수정·삭제 | 해당 게시글의 댓글 |
| 좋아요 | 해당 상세 |

**재조회 호출은 `*Store` 서비스 안에서만 한다.** 컴포넌트가 직접 `reload()`를 부르면 범위가 흩어진다.

## 5. 인증과 보안

### 5.1 CSRF

Angular `HttpClient`가 XSRF를 기본 지원하므로 직접 만들지 않는다.

```ts
provideHttpClient(withXsrfConfiguration({
  cookieName: "XSRF-TOKEN",
  headerName: "X-XSRF-TOKEN",
}))
```

### 5.2 401 처리

`HttpInterceptor` 한 곳에서 다룬다. 현재 경로를 저장하고 `/oauth2/authorization/keycloak`으로 이동한다.

로그인 여부를 확인하는 요청은 401이 정상 응답이므로 인터셉터를 건너뛰어야 한다. `HttpContextToken`으로 표현한다.

```ts
export const SKIP_LOGIN_REDIRECT = new HttpContextToken(() => false);
```

React 버전에서는 이 예외 때문에 `useCurrentMember`만 공용 클라이언트를 우회해 `fetch`를 직접 썼다. Angular에서는 같은 클라이언트를 쓰면서 의도를 타입으로 드러낸다.

### 5.3 로그아웃

`POST /logout`을 보내고 204를 받으면 첫 화면으로 이동한다. CSRF 토큰은 `HttpClient`가 자동으로 싣는다.

## 5.4 개발 서버 포트

Angular 개발 서버는 **5173을 쓴다.** Angular CLI 기본값은 4200이지만, 5173은 이미 Keycloak realm의 허용 리다이렉트 URI와 README에 등록되어 있다. 포트를 바꾸면 realm을 다시 만들고 문서를 고쳐야 하며, 그 작업은 이 전환과 아무 관련이 없다.

전환 기간에는 두 개발 서버를 동시에 띄우지 않는다. 한 번에 하나만 5173을 점유한다.

## 6. 디렉터리 구조

전환 기간에는 Angular를 `frontend-angular/`에서 개발한다. 마지막 단계에서 `frontend/`를 지우고 `frontend-angular/`를 그 자리로 옮긴다. 이렇게 하면 두 구현을 나란히 두고 비교하다가, 끝난 뒤에는 경로가 원래대로 돌아온다.

```
frontend/src/app/
├── core/
│   ├── api/
│   │   ├── schema.d.ts          기존 생성물을 그대로 옮긴다
│   │   ├── api.service.ts       경로·응답 타입을 강제하는 HttpClient 래퍼
│   │   └── problem.ts           기존 파일을 그대로 옮긴다
│   ├── auth/
│   │   ├── auth.interceptor.ts  401 처리
│   │   └── current-member.store.ts
│   └── config/
│       └── app.config.ts        provider 등록
├── features/
│   ├── post/
│   │   ├── post-api.service.ts  HTTP 호출
│   │   ├── post.store.ts        resource 소유와 재조회 범위
│   │   ├── pages/               라우트에 연결되는 컴포넌트
│   │   └── components/          목록, 상세, 폼, 좋아요
│   ├── comment/                 (동일 구조)
│   └── member/                  (동일 구조)
├── shared/ui/                   Tailwind + CDK 기반 button, input, textarea
├── app.routes.ts
└── app.component.ts             헤더와 라우터 아웃렛
```

`schema.d.ts`와 `problem.ts`는 프레임워크와 무관하므로 그대로 재사용한다.

## 7. 라우팅과 URL 상태

`withComponentInputBinding()`을 켜서 경로 파라미터와 쿼리 파라미터를 컴포넌트 입력으로 받는다.

| 경로 | 화면 | 인증 |
|---|---|---|
| `/` | 목록 (page, size, keyword) | 불필요 |
| `/posts/new` | 작성 | 필요 (`canActivate` 가드) |
| `/posts/:postId` | 상세, 댓글, 좋아요 | 불필요 |
| `/posts/:postId/edit` | 수정 | 필요 |

React의 `validateSearch`가 하던 정규화는 입력의 `transform`으로 처리한다. 잘못된 값이 들어와도 기본값으로 바뀌므로 화면이 깨지지 않는다.

## 8. 변경 감지

`provideZonelessChangeDetection()`으로 zone.js를 제거한다. 상태를 모두 signal로 다루므로 zone이 필요 없고 번들도 줄어든다.

## 9. 에러 처리

`problem.ts`를 그대로 쓰므로 판별 방식이 바뀌지 않는다. `code`로 분기한다.

| code | 처리 |
|---|---|
| `UNAUTHENTICATED` | 인터셉터가 로그인으로 이동 |
| `ACCESS_DENIED` | 권한 없음 안내 |
| `INVALID_REQUEST` | `errors`를 폼 필드에 표시 |
| `POST_NOT_FOUND`, `COMMENT_NOT_FOUND` | 없음 안내 |
| `ALREADY_LIKED`, `NOT_LIKED` | 좋아요 상태 되돌림 |

## 10. 테스트 전략

| 층 | 도구 | 대상 |
|---|---|---|
| 순수 로직 | Vitest | `problem.ts`, 스토어의 재조회 규칙 |
| 컴포넌트 | Vitest + Testing Library | 렌더링, 권한별 UI, 폼 검증 |
| HTTP | MSW | 기존 핸들러를 그대로 재사용 |
| E2E | Playwright | 기존 시나리오 4개를 이관 |

**E2E가 이 전환의 안전망이다.** 기존 시나리오는 역할과 라벨로 요소를 찾으므로 구현 프레임워크와 무관하다. React에서 통과하던 시나리오가 Angular에서도 통과하면 기능 동등성이 증명된다.

MSW 핸들러도 생성된 타입을 쓰므로 목이 계약과 어긋나면 컴파일이 깨진다.

## 11. 품질 게이트

`pnpm verify`가 아래를 순서대로 실행한다.

1. ESLint (angular-eslint, 템플릿 포함)
2. `tsc --noEmit`
3. Vitest

TypeScript는 `strict`와 `noUncheckedIndexedAccess`를 켠다. pre-commit 훅은 경로만 맞추면 그대로 동작한다.

Biome 대신 angular-eslint를 쓰는 이유는 Angular 템플릿 때문이다. Biome은 Angular HTML을 파싱하지 못해 접근성과 바인딩 규칙을 검사할 수 없다.

## 12. 전환 순서

각 단계가 끝날 때마다 빌드와 테스트가 통과하는 상태를 유지한다.

1. `chore: Angular 프로젝트 및 품질 도구 구성`
2. `feat: API 계층과 인증 인터셉터`
3. `feat: 레이아웃과 로그인 상태 표시`
4. `feat: 게시글 목록 화면`
5. `feat: 게시글 상세 화면`
6. `feat: 게시글 작성 및 수정 폼`
7. `feat: 댓글 및 대댓글`
8. `feat: 좋아요`
9. `test: E2E 시나리오 이관`
10. `chore: React 프론트엔드 제거` — `frontend/`를 지우고 `frontend-angular/`를 그 자리로 옮긴다
11. `docs: 문서 갱신`

React 제거를 마지막에 두어, 그 전까지 두 구현을 비교하며 빠진 동작을 확인할 수 있게 한다.

## 13. 완료 기준

- `cd frontend && pnpm verify`가 통과한다
- `pnpm e2e`가 통과한다. 시나리오는 React 버전과 동일하다
- `./gradlew build`가 여전히 통과한다 (백엔드 무변경 확인)
- 브라우저에서 로그인, 글 작성, 검색, 댓글, 대댓글, 좋아요, 로그아웃이 모두 동작한다
- 저장소에 React 코드가 남아 있지 않고, Angular가 `frontend/`에 자리한다
- Keycloak realm과 README를 고치지 않았다 (개발 서버가 같은 포트를 쓴다)
