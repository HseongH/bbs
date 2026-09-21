# Angular 전환 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 기존 React 프론트엔드를 Angular로 다시 구현하고, 기능 동등성을 같은 E2E 시나리오로 증명한다.

**Architecture:** 조회는 `httpResource`와 signal이, 변경은 `HttpClient`가 맡는다. 재조회 범위는 기능별 스토어 서비스가 소유하며 이는 Spring의 서비스 계층과 같은 자리다. OpenAPI 생성 타입과 ProblemDetail 파싱은 프레임워크와 무관하므로 그대로 옮긴다.

**Tech Stack:** Angular 22.1.7 (zoneless), `httpResource`, Signal Forms, Tailwind CSS 4.3 + Angular CDK, angular-eslint 22.5 + Prettier, Angular CLI `unit-test` 빌더(Vitest) + Testing Library 19.5, MSW 2.15, Playwright 1.63

설계 문서: `docs/superpowers/specs/2026-09-21-angular-migration-design.md`

## Global Constraints

- 전환 기간에는 `frontend-angular/`에서 개발하고, 마지막 태스크에서 `frontend/`를 지운 뒤 그 자리로 옮긴다
- **개발 서버는 5173을 쓴다.** Angular CLI 기본값 4200을 쓰면 Keycloak realm의 허용 리다이렉트 URI와 어긋나 로그인이 깨진다
- 백엔드를 수정하지 않는다. `./gradlew build`가 계속 통과해야 한다
- Keycloak realm(`docker/keycloak/bbs-realm.json`)과 루트 README의 접속 주소를 바꾸지 않는다
- `schema.d.ts`는 `frontend/src/api/schema.d.ts`를 그대로 옮긴 생성물이다. 직접 수정하지 않으며 린트 대상에서 제외한다
- 재조회(`reload()`)는 `*.store.ts`에서만 호출한다. 컴포넌트가 직접 부르지 않는다
- 경로 별칭은 `@/*`(앱 소스)와 `@test/*`(테스트 지원)를 쓴다. 상대 경로로 디렉터리를 거슬러 올라가지 않는다
- 검색어와 페이지는 URL 쿼리 파라미터에 둔다. 컴포넌트 상태로 복제하지 않는다
- 401 처리는 `auth.interceptor.ts` 한 곳에만 둔다
- CSRF는 `withXsrfConfiguration`으로 처리한다. 직접 헤더를 붙이지 않는다
- `provideZonelessChangeDetection()`을 쓴다. zone.js를 의존성에 넣지 않는다
- TypeScript는 6.x를 쓴다. 7은 `openapi-typescript`와 호환되지 않는다
- 주석은 로직을 설명할 때만 쓴다. 설정 파일과 기록용 주석은 남기지 않는다
- 커밋 메시지는 AngularJS 컨벤션(`type: subject`)이며 본문은 한국어로 쓴다
- 각 태스크는 `pnpm verify`가 통과한 상태로 커밋한다

## 확인된 API 사실 (2026-09-21)

계획의 코드는 아래 시그니처를 전제로 한다. 설치 후 다르면 그 자리에서 교정한다.

```ts
// @angular/common/http — @publicApi 22.0
httpResource<T>(request: (ctx) => HttpResourceRequest | undefined, options?): HttpResourceRef<T | undefined>
// HttpResourceRequest: { url: string; method?: string; body?: unknown; params?: ...; headers?: ...; context?: HttpContext }
```

리소스가 제공하는 시그널과 메서드:

| 멤버 | 타입 |
|---|---|
| `value()` | `Signal<T>` |
| `isLoading()` | `Signal<boolean>` |
| `error()` | `Signal<Error \| undefined>` |
| `status()` | `Signal<'idle' \| 'error' \| 'loading' \| 'reloading' \| 'resolved' \| 'local'>` |
| `hasValue()` | `boolean` |
| `reload()` | `boolean` |

`defaultValue`를 주지 않으면 반환 타입이 `HttpResourceRef<T | undefined>`이므로 `value()`가 `undefined`일 수 있다.

---

## File Structure

| 파일 | 책임 |
|---|---|
| `frontend-angular/package.json` | 스크립트, 의존성, Node·pnpm 고정 |
| `frontend-angular/angular.json` | 빌드·개발서버(5173)·단위테스트 설정 |
| `frontend-angular/eslint.config.js` | angular-eslint 규칙 (TS + 템플릿) |
| `frontend-angular/src/app/core/api/schema.d.ts` | OpenAPI 생성 타입 (이관) |
| `frontend-angular/src/app/core/api/problem.ts` | ProblemDetail 파싱 (이관) |
| `frontend-angular/src/app/core/api/api.service.ts` | 경로·응답 타입을 강제하는 HttpClient 래퍼 |
| `frontend-angular/src/app/core/auth/auth.interceptor.ts` | 401 처리, `SKIP_LOGIN_REDIRECT` |
| `frontend-angular/src/app/core/auth/current-member.store.ts` | 현재 회원 조회, 로그아웃 |
| `frontend-angular/src/app/core/auth/auth.guard.ts` | 인증 필요 라우트 보호 |
| `frontend-angular/src/app/app.config.ts` | provider 등록 |
| `frontend-angular/src/app/app.routes.ts` | 라우트 정의 |
| `frontend-angular/src/app/app.ts` | 헤더와 라우터 아웃렛 |
| `frontend-angular/src/app/features/post/post-api.service.ts` | 게시글 HTTP 호출 |
| `frontend-angular/src/app/features/post/post.store.ts` | 게시글 리소스와 재조회 범위 |
| `frontend-angular/src/app/features/post/pages/*` | 라우트 컴포넌트 |
| `frontend-angular/src/app/features/post/components/*` | 목록, 상세, 폼, 좋아요, 검색, 페이지네이션 |
| `frontend-angular/src/app/features/comment/*` | 댓글 (동일 구조) |
| `frontend-angular/src/app/shared/ui/button.ts` | 공용 버튼. 입력과 텍스트영역은 감쌀 동작이 없어 표준 요소에 유틸리티 클래스를 쓴다 |
| `frontend-angular/src/test/handlers.ts` | MSW 핸들러 (이관) |
| `frontend-angular/src/test/setup.ts` | MSW 서버, Testing Library 정리 |
| `frontend-angular/e2e/*` | Playwright 시나리오 (이관) |

---
## Task 1: Angular 프로젝트 및 품질 도구 구성

이후 모든 태스크가 올라설 기반을 만든다. 이 태스크가 끝나면 `pnpm verify`와 `pnpm dev`가 동작한다.

**Files:**
- Create: `frontend-angular/` 전체 (CLI 생성)
- Modify: `frontend-angular/package.json`
- Modify: `frontend-angular/angular.json`
- Modify: `frontend-angular/tsconfig.json`
- Create: `frontend-angular/eslint.config.js`
- Create: `frontend-angular/.prettierrc.json`
- Create: `frontend-angular/src/styles.css`
- Create: `frontend-angular/proxy.conf.json`
- Modify: `.gitignore` (저장소 루트)

**Interfaces:**
- Consumes: 없음 (최초 태스크)
- Produces:
  - `pnpm dev` — 5173에서 개발 서버, `/api`·`/oauth2`·`/login`·`/logout`을 8080으로 프록시
  - `pnpm verify` — ESLint → `tsc --noEmit` → 단위 테스트
  - `pnpm test` — Vitest 단위 테스트
  - Tailwind 유틸리티 클래스 사용 가능

- [ ] **Step 1: Angular 프로젝트를 생성한다**

```bash
cd '/home/hseongh/문서/dev/bbs'
pnpm dlx @angular/cli@22.1.8 new frontend-angular \
  --style=css --ssr=false --zoneless --package-manager=pnpm \
  --skip-git --skip-install --defaults
```

기대: `frontend-angular/`가 생성된다. `--zoneless`가 지원되지 않는다는 오류가 나면 옵션 없이 생성한 뒤 Step 5에서 `provideZonelessChangeDetection()`을 직접 넣고 `zone.js` 의존성을 제거한다.

- [ ] **Step 2: 의존성을 설치하고 실제 버전을 확인한다**

```bash
cd frontend-angular
pnpm add -D tailwindcss @tailwindcss/postcss postcss angular-eslint eslint typescript-eslint prettier
pnpm add @angular/cdk
pnpm add -D @testing-library/angular @testing-library/dom @testing-library/user-event msw @playwright/test jsdom
pnpm install
pnpm list --depth=0
```

기대: 설치 성공. TypeScript가 7.x로 들어오면 `pnpm add -D typescript@^6.0.3`으로 내린다. `openapi-typescript`가 TS 7의 컴파일러 API와 호환되지 않기 때문이며, 같은 저장소의 React 버전도 6을 쓴다.

- [ ] **Step 3: 개발 서버 포트와 프록시를 설정한다**

`frontend-angular/proxy.conf.json`

```json
{
  "/api": { "target": "http://localhost:8080", "secure": false },
  "/oauth2": { "target": "http://localhost:8080", "secure": false },
  "/login": { "target": "http://localhost:8080", "secure": false },
  "/logout": { "target": "http://localhost:8080", "secure": false },
  "/v3/api-docs": { "target": "http://localhost:8080", "secure": false }
}
```

`angular.json`의 `projects.frontend-angular.architect.serve.options`에 아래를 넣는다. `changeOrigin`을 켜지 않는 것이 중요하다. Host 헤더를 바꾸면 Keycloak 리다이렉트 URI가 어긋난다.

```json
"port": 5173,
"host": "0.0.0.0",
"proxyConfig": "proxy.conf.json"
```

- [ ] **Step 4: Tailwind를 설정한다**

`frontend-angular/.postcssrc.json`

```json
{
  "plugins": {
    "@tailwindcss/postcss": {}
  }
}
```

`frontend-angular/src/styles.css`

```css
@import "tailwindcss";
```

- [ ] **Step 5: zoneless와 HTTP provider를 등록한다**

`frontend-angular/src/app/app.config.ts`

```ts
import {
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
  type ApplicationConfig,
} from "@angular/core";
import { provideHttpClient, withXsrfConfiguration } from "@angular/common/http";
import { provideRouter, withComponentInputBinding } from "@angular/router";
import { routes } from "./app.routes";

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(
      withXsrfConfiguration({ cookieName: "XSRF-TOKEN", headerName: "X-XSRF-TOKEN" }),
    ),
  ],
};
```

`zone.js`가 `package.json`과 `angular.json`의 `polyfills`에 남아 있으면 제거한다.

- [ ] **Step 6: TypeScript를 엄격하게 설정한다**

`frontend-angular/tsconfig.json`의 `compilerOptions`에 아래를 추가하거나 확인한다.

```json
"strict": true,
"noUncheckedIndexedAccess": true,
"noUnusedLocals": true,
"noUnusedParameters": true,
"paths": {
  "@/*": ["./src/app/*"],
  "@test/*": ["./src/test/*"]
}
```

- [ ] **Step 7: ESLint와 Prettier를 설정한다**

```bash
cd frontend-angular && pnpm dlx ng add angular-eslint --skip-confirmation
```

생성된 `eslint.config.js`의 무시 목록에 생성물을 추가한다.

```js
{ ignores: ["dist", "node_modules", "src/app/core/api/schema.d.ts"] }
```

`frontend-angular/.prettierrc.json`

```json
{
  "printWidth": 100,
  "singleQuote": false,
  "overrides": [{ "files": "*.html", "parser": "angular" }]
}
```

- [ ] **Step 8: 스크립트를 정리한다**

`frontend-angular/package.json`의 `scripts`를 아래로 교체한다.

```json
{
  "dev": "ng serve",
  "build": "ng build",
  "test": "ng test",
  "lint": "ng lint && prettier --check .",
  "lint:fix": "ng lint --fix && prettier --write .",
  "typecheck": "tsc --noEmit -p tsconfig.app.json",
  "e2e": "playwright test",
  "gen:api": "openapi-typescript http://localhost:8080/v3/api-docs -o src/app/core/api/schema.d.ts",
  "verify": "pnpm lint && pnpm typecheck && pnpm test"
}
```

`openapi-typescript`도 개발 의존성에 추가한다.

```bash
pnpm add -D openapi-typescript@^7.13.0
```

- [ ] **Step 9: 단위 테스트 러너를 Vitest로 바꾼다**

`angular.json`의 `architect.test`를 아래로 교체한다.

```json
"test": {
  "builder": "@angular/build:unit-test",
  "options": {
    "tsConfig": "tsconfig.spec.json",
    "runner": "vitest",
    "setupFiles": ["src/test/setup.ts"]
  }
}
```

기대: `pnpm test`가 Vitest로 실행된다. 빌더 옵션 이름이 다르면 `pnpm dlx ng test --help`로 실제 옵션을 확인해 맞춘다.

- [ ] **Step 10: 동작을 확인할 테스트를 쓴다**

`frontend-angular/src/test/setup.ts` — MSW는 Task 2에서 붙인다. 지금은 비워 둔다.

```ts
export {};
```

`frontend-angular/src/app/app.spec.ts`가 CLI 생성물로 존재한다. 내용을 아래로 교체한다.

```ts
import { TestBed } from "@angular/core/testing";
import { describe, expect, it } from "vitest";
import { App } from "./app";

describe("App", () => {
  it("생성된다", async () => {
    await TestBed.configureTestingModule({ imports: [App] }).compileComponents();
    const fixture = TestBed.createComponent(App);

    expect(fixture.componentInstance).toBeTruthy();
  });
});
```

CLI가 만든 컴포넌트 클래스 이름이 `App`이 아니면 실제 이름에 맞춘다.

- [ ] **Step 11: 검증 명령이 통과하는지 확인한다**

```bash
cd frontend-angular && pnpm lint:fix && pnpm verify
```

기대: 린트, 타입 검사, 테스트 1건 통과.

- [ ] **Step 12: 개발 서버와 프록시를 확인한다**

백엔드와 컨테이너가 떠 있어야 한다. 다른 개발 서버가 5173을 쓰고 있으면 먼저 종료한다.

```bash
cd frontend-angular && pnpm dev &
sleep 20
curl -s -o /dev/null -w "화면 HTTP %{http_code}\n" http://localhost:5173/
curl -s -o /dev/null -w "프록시 HTTP %{http_code}\n" http://localhost:5173/api/posts
```

기대: 둘 다 200. 확인 후 개발 서버를 종료한다.

- [ ] **Step 13: 무시 파일과 커밋**

저장소 루트 `.gitignore`에 추가한다.

```
frontend-angular/node_modules
frontend-angular/dist
frontend-angular/.angular
```

```bash
cd '/home/hseongh/문서/dev/bbs'
git add frontend-angular .gitignore
git commit -m "chore: Angular 프로젝트 및 품질 도구 구성

zoneless 변경 감지와 signal 기반 구조로 프로젝트를 세운다.
개발 서버는 5173을 쓴다. Keycloak realm에 등록된 주소와 같아야
로그인이 동작하므로 CLI 기본값 대신 기존 포트를 유지한다.

CSRF는 HttpClient의 기본 기능으로 처리한다. React 버전에서 직접
만든 미들웨어가 필요 없다.

린트는 Biome 대신 angular-eslint를 쓴다. Biome은 Angular 템플릿을
파싱하지 못해 접근성과 바인딩 규칙을 검사할 수 없다."
```

---

## Task 2: API 계층과 인증 인터셉터

모든 HTTP 호출이 지나갈 통로와 401 처리를 만든다.

**Files:**
- Create: `frontend-angular/src/app/core/api/schema.d.ts` (이관)
- Create: `frontend-angular/src/app/core/api/problem.ts` (이관)
- Create: `frontend-angular/src/app/core/api/problem.spec.ts` (이관)
- Create: `frontend-angular/src/app/core/auth/auth.interceptor.ts`
- Create: `frontend-angular/src/app/core/auth/auth.interceptor.spec.ts`
- Create: `frontend-angular/src/test/handlers.ts` (이관)
- Modify: `frontend-angular/src/test/setup.ts`
- Modify: `frontend-angular/src/app/app.config.ts`

**Interfaces:**
- Consumes: Task 1의 `provideHttpClient`
- Produces:
  - `ProblemDetail` 타입과 `toProblem`, `isProblemCode`, `fieldErrors` (React 버전과 동일한 시그니처)
  - `LOGIN_URL` — `"/oauth2/authorization/keycloak"`
  - `REDIRECT_KEY` — `"bbs:redirectAfterLogin"`
  - `SKIP_LOGIN_REDIRECT` — `HttpContextToken<boolean>`. 이 토큰을 단 요청은 401이어도 로그인으로 보내지 않는다
  - `takeRedirectPath(): string | null`
  - `authInterceptor` — `HttpInterceptorFn`

- [ ] **Step 1: 프레임워크와 무관한 파일을 옮긴다**

```bash
cd '/home/hseongh/문서/dev/bbs'
mkdir -p frontend-angular/src/app/core/api frontend-angular/src/app/core/auth frontend-angular/src/test
cp frontend/src/api/schema.d.ts frontend-angular/src/app/core/api/schema.d.ts
cp frontend/src/api/problem.ts  frontend-angular/src/app/core/api/problem.ts
cp frontend/src/api/problem.test.ts frontend-angular/src/app/core/api/problem.spec.ts
cp frontend/src/test/handlers.ts frontend-angular/src/test/handlers.ts
```

`handlers.ts`의 import 경로를 `@/core/api/schema`로 바꾼다. `problem.spec.ts`는 `vitest`에서 가져오는 형태 그대로 둔다.

- [ ] **Step 2: 옮긴 파일이 통과하는지 확인한다**

```bash
cd frontend-angular && pnpm test
```

기대: `problem.spec.ts`의 테스트 6건 통과. 경로 오류가 나면 import를 교정한다.

- [ ] **Step 3: 인터셉터 테스트를 쓴다**

`frontend-angular/src/app/core/auth/auth.interceptor.spec.ts`

```ts
import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { authInterceptor, REDIRECT_KEY, SKIP_LOGIN_REDIRECT } from "./auth.interceptor";

describe("authInterceptor", () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);

    vi.stubGlobal("location", {
      origin: "http://localhost:5173",
      href: "http://localhost:5173/posts/1",
      pathname: "/posts/1",
      search: "",
    });
    sessionStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("401을 받으면 현재 경로를 기억하고 로그인으로 보낸다", () => {
    http.get("/api/posts").subscribe({ error: () => {} });
    controller.expectOne("/api/posts").flush(null, { status: 401, statusText: "Unauthorized" });

    expect(sessionStorage.getItem(REDIRECT_KEY)).toBe("/posts/1");
    expect(window.location.href).toBe("/oauth2/authorization/keycloak");
  });

  it("토큰이 붙은 요청은 401이어도 로그인으로 보내지 않는다", () => {
    const context = new HttpContext().set(SKIP_LOGIN_REDIRECT, true);

    http.get("/api/members/me", { context }).subscribe({ error: () => {} });
    controller
      .expectOne("/api/members/me")
      .flush(null, { status: 401, statusText: "Unauthorized" });

    expect(sessionStorage.getItem(REDIRECT_KEY)).toBeNull();
  });

  it("401이 아닌 오류는 건드리지 않는다", () => {
    http.get("/api/posts/1").subscribe({ error: () => {} });
    controller.expectOne("/api/posts/1").flush(null, { status: 404, statusText: "Not Found" });

    expect(sessionStorage.getItem(REDIRECT_KEY)).toBeNull();
  });
});
```

두 번째 테스트가 이 인터셉터의 핵심이다. 로그인 여부를 확인하는 요청까지 로그인으로 보내면 비로그인 사용자가 아무 화면도 볼 수 없다.

- [ ] **Step 4: 테스트가 실패하는 것을 확인한다**

```bash
cd frontend-angular && pnpm test
```

기대: `./auth.interceptor` 모듈을 찾을 수 없어 실패

- [ ] **Step 5: 인터셉터를 구현한다**

`frontend-angular/src/app/core/auth/auth.interceptor.ts`

```ts
import { HttpContextToken, type HttpInterceptorFn } from "@angular/common/http";
import { catchError, throwError } from "rxjs";

export const LOGIN_URL = "/oauth2/authorization/keycloak";
export const REDIRECT_KEY = "bbs:redirectAfterLogin";

/** 로그인 여부를 확인하는 요청처럼 401이 정상 응답인 경우에 단다. */
export const SKIP_LOGIN_REDIRECT = new HttpContextToken(() => false);

export function startLogin(): void {
  try {
    sessionStorage.setItem(REDIRECT_KEY, window.location.pathname + window.location.search);
  } catch {
    // 시크릿 모드 등 저장이 막힌 환경에서는 복귀 없이 로그인만 진행한다.
  }
  window.location.href = LOGIN_URL;
}

/** 로그인 후 돌아갈 경로를 한 번만 꺼낸다. */
export function takeRedirectPath(): string | null {
  try {
    const path = sessionStorage.getItem(REDIRECT_KEY);
    sessionStorage.removeItem(REDIRECT_KEY);
    return path;
  } catch {
    return null;
  }
}

export const authInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError((error: unknown) => {
      const 무시 = request.context.get(SKIP_LOGIN_REDIRECT);
      if (!무시 && error instanceof Object && "status" in error && error.status === 401) {
        startLogin();
      }
      return throwError(() => error);
    }),
  );
```

- [ ] **Step 6: 테스트가 통과하는 것을 확인한다**

```bash
cd frontend-angular && pnpm test
```

기대: PASS

- [ ] **Step 7: 인터셉터와 MSW를 등록한다**

`app.config.ts`의 `provideHttpClient`를 아래로 바꾼다.

```ts
    provideHttpClient(
      withInterceptors([authInterceptor]),
      withXsrfConfiguration({ cookieName: "XSRF-TOKEN", headerName: "X-XSRF-TOKEN" }),
    ),
```

`frontend-angular/src/test/setup.ts`

```ts
import "@testing-library/jest-dom/vitest";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll } from "vitest";
import { handlers } from "./handlers";

export const server = setupServer(...handlers);

beforeAll(() => {
  server.listen({ onUnhandledRequest: "error" });
});

afterEach(() => {
  server.resetHandlers();
});

afterAll(() => {
  server.close();
});
```

`@testing-library/jest-dom`을 설치한다.

```bash
cd frontend-angular && pnpm add -D @testing-library/jest-dom
```

- [ ] **Step 8: 검증하고 커밋한다**

```bash
cd frontend-angular && pnpm lint:fix && pnpm verify
cd .. && git add frontend-angular
git commit -m "feat: API 계층과 인증 인터셉터 추가

OpenAPI 생성 타입과 ProblemDetail 파싱은 프레임워크와 무관하므로
React 구현에서 그대로 옮긴다.

401 처리는 인터셉터 한 곳에 둔다. 로그인 여부를 확인하는 요청은
401이 정상 응답이므로 HttpContextToken으로 예외를 표시한다.
React에서는 이 예외 때문에 공용 클라이언트를 우회해야 했다."
```

---
## Task 3: 레이아웃과 로그인 상태 표시

라우팅 뼈대를 세우고 헤더에 로그인 상태를 드러낸다. 이후 모든 화면이 이 위에 올라간다.

**Files:**
- Create: `frontend-angular/src/app/core/auth/current-member.store.ts`
- Create: `frontend-angular/src/app/core/auth/current-member.store.spec.ts`
- Create: `frontend-angular/src/app/core/auth/auth.guard.ts`
- Create: `frontend-angular/src/app/shared/ui/button.ts`
- Modify: `frontend-angular/src/app/app.ts`
- Modify: `frontend-angular/src/app/app.routes.ts`
- Modify: `frontend-angular/src/main.ts`

**Interfaces:**
- Consumes: `SKIP_LOGIN_REDIRECT`, `LOGIN_URL`, `takeRedirectPath` (Task 2)
- Produces:
  - `CurrentMemberStore` — `member: Signal<Member | null>`, `isLoading: Signal<boolean>`, `reload(): void`, `logout(): Promise<void>`
  - `Member` 타입 — `components["schemas"]["MemberResponse"]`
  - `authGuard` — `CanActivateFn`. 미인증이면 로그인으로 보낸다
  - `<app-button>` — `variant: "default" | "outline" | "destructive" | "ghost"`, `size: "default" | "sm"`

- [ ] **Step 1: 현재 회원 스토어 테스트를 쓴다**

`frontend-angular/src/app/core/auth/current-member.store.spec.ts`

```ts
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { server } from "@test/setup";
import { authInterceptor } from "./auth.interceptor";
import { CurrentMemberStore } from "./current-member.store";

async function 스토어를_만든다(): Promise<CurrentMemberStore> {
  TestBed.configureTestingModule({
    providers: [provideHttpClient(withInterceptors([authInterceptor]))],
  });
  const store = TestBed.inject(CurrentMemberStore);
  await TestBed.inject(TestBed).whenStable?.();
  return store;
}

describe("CurrentMemberStore", () => {
  it("로그인 상태면 회원 정보를 노출한다", async () => {
    const store = await 스토어를_만든다();

    await vi.waitFor(() => expect(store.member()?.nickname).toBe("테스터"));
  });

  it("미인증이면 null이고 로그인으로 이동하지 않는다", async () => {
    server.use(
      http.get("/api/members/me", () =>
        HttpResponse.json({ status: 401, code: "UNAUTHENTICATED" }, { status: 401 }),
      ),
    );

    const store = await 스토어를_만든다();

    await vi.waitFor(() => expect(store.member()).toBeNull());
  });
});
```

`vi`와 `TestBed.whenStable` 사용법이 실제와 다르면 실행 결과에 맞춰 교정한다. 중요한 것은 두 번째 단언이다.

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
cd frontend-angular && pnpm test
```

기대: `./current-member.store` 모듈을 찾을 수 없어 실패

- [ ] **Step 3: 스토어를 구현한다**

`frontend-angular/src/app/core/auth/current-member.store.ts`

```ts
import { HttpClient, HttpContext, httpResource } from "@angular/common/http";
import { computed, inject, Injectable } from "@angular/core";
import { firstValueFrom } from "rxjs";
import type { components } from "@/core/api/schema";
import { SKIP_LOGIN_REDIRECT } from "./auth.interceptor";

export type Member = components["schemas"]["MemberResponse"];

@Injectable({ providedIn: "root" })
export class CurrentMemberStore {
  private readonly http = inject(HttpClient);

  /** 401은 로그인하지 않았다는 정상 응답이므로 인터셉터를 건너뛴다. */
  private readonly resource = httpResource<Member>(() => ({
    url: "/api/members/me",
    context: new HttpContext().set(SKIP_LOGIN_REDIRECT, true),
  }));

  readonly member = computed<Member | null>(() =>
    this.resource.error() ? null : (this.resource.value() ?? null),
  );
  readonly isLoading = this.resource.isLoading;

  reload(): void {
    this.resource.reload();
  }

  async logout(): Promise<void> {
    await firstValueFrom(this.http.post("/logout", null, { responseType: "text" }));
    window.location.href = "/";
  }
}
```

- [ ] **Step 4: 테스트가 통과하는 것을 확인한다**

```bash
cd frontend-angular && pnpm test
```

기대: PASS

- [ ] **Step 5: 공용 버튼 컴포넌트를 만든다**

`frontend-angular/src/app/shared/ui/button.ts`

```ts
import { ChangeDetectionStrategy, Component, computed, input } from "@angular/core";

type Variant = "default" | "outline" | "destructive" | "ghost";
type Size = "default" | "sm";

const VARIANT: Record<Variant, string> = {
  default: "bg-slate-900 text-white hover:bg-slate-800",
  outline: "border border-slate-300 hover:bg-slate-50",
  destructive: "border border-red-300 text-red-600 hover:bg-red-50",
  ghost: "text-slate-600 hover:bg-slate-100",
};

const SIZE: Record<Size, string> = {
  default: "px-4 py-2 text-sm",
  sm: "px-3 py-1 text-sm",
};

@Component({
  selector: "app-button",
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button [type]="type()" [disabled]="disabled()" [class]="classes()">
      <ng-content />
    </button>
  `,
})
export class ButtonComponent {
  readonly type = input<"button" | "submit">("button");
  readonly variant = input<Variant>("default");
  readonly size = input<Size>("default");
  readonly disabled = input(false);

  protected readonly classes = computed(
    () =>
      `rounded disabled:opacity-50 ${VARIANT[this.variant()]} ${SIZE[this.size()]}`,
  );
}
```

- [ ] **Step 6: 루트 컴포넌트에 헤더를 넣는다**

`frontend-angular/src/app/app.ts`

```ts
import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { RouterLink, RouterOutlet } from "@angular/router";
import { LOGIN_URL } from "@/core/auth/auth.interceptor";
import { CurrentMemberStore } from "@/core/auth/current-member.store";

@Component({
  selector: "app-root",
  imports: [RouterLink, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="border-b border-slate-200">
      <div class="mx-auto flex max-w-4xl items-center justify-between px-4 py-3">
        <a routerLink="/" class="text-lg font-semibold">게시판</a>
        <nav class="flex items-center gap-3 text-sm">
          @if (!store.isLoading()) {
            @if (store.member(); as member) {
              <span class="text-slate-700">{{ member.nickname }}</span>
              <button type="button" class="text-slate-500 hover:text-slate-900" (click)="logout()">
                로그아웃
              </button>
            } @else {
              <a [href]="loginUrl" class="text-slate-500 hover:text-slate-900">로그인</a>
            }
          }
        </nav>
      </div>
    </header>
    <main class="mx-auto max-w-4xl px-4 py-6">
      <router-outlet />
    </main>
  `,
})
export class App {
  protected readonly store = inject(CurrentMemberStore);
  protected readonly loginUrl = LOGIN_URL;

  protected logout(): void {
    void this.store.logout();
  }
}
```

로그인은 앵커, 로그아웃은 버튼이다. 로그인은 서버로 실제 이동해야 하고 로그아웃은 POST 요청이다.

- [ ] **Step 7: 라우트와 가드를 만든다**

`frontend-angular/src/app/core/auth/auth.guard.ts`

```ts
import { inject } from "@angular/core";
import type { CanActivateFn } from "@angular/router";
import { startLogin } from "./auth.interceptor";
import { CurrentMemberStore } from "./current-member.store";

export const authGuard: CanActivateFn = () => {
  const store = inject(CurrentMemberStore);
  if (store.member()) {
    return true;
  }
  startLogin();
  return false;
};
```

`frontend-angular/src/app/app.routes.ts` — 화면은 이후 태스크에서 채운다.

```ts
import type { Routes } from "@angular/router";

export const routes: Routes = [
  { path: "", pathMatch: "full", loadComponent: () => import("@/features/post/pages/post-list-page").then((m) => m.PostListPage) },
];
```

`frontend-angular/src/app/features/post/pages/post-list-page.ts` — 자리만 만든다.

```ts
import { ChangeDetectionStrategy, Component } from "@angular/core";

@Component({
  selector: "app-post-list-page",
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<p>목록 준비 중</p>`,
})
export class PostListPage {}
```

- [ ] **Step 8: 로그인 후 복귀를 처리한다**

`frontend-angular/src/main.ts`의 `bootstrapApplication` 호출 앞에 추가한다.

```ts
import { takeRedirectPath } from "@/core/auth/auth.interceptor";

const redirectPath = takeRedirectPath();
if (redirectPath && redirectPath !== window.location.pathname + window.location.search) {
  window.history.replaceState(null, "", redirectPath);
}
```

- [ ] **Step 9: 검증하고 화면을 확인한다**

```bash
cd frontend-angular && pnpm lint:fix && pnpm verify
pnpm dev &
sleep 20
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:5173/
```

브라우저에서 `http://localhost:5173`을 열어 헤더가 보이는지, 로그인 버튼을 눌렀을 때 Keycloak으로 가고 돌아와 닉네임이 뜨는지 확인한다. 확인 후 개발 서버를 종료한다.

- [ ] **Step 10: 커밋한다**

```bash
cd .. && git add frontend-angular
git commit -m "feat: 레이아웃과 로그인 상태 표시 추가

현재 회원 조회는 401을 정상 응답으로 취급한다. 인터셉터를 건너뛰는
컨텍스트 토큰을 달아 같은 HttpClient를 그대로 쓴다.

로그인은 서버로 이동해야 하므로 앵커를, 로그아웃은 POST 요청이므로
버튼을 쓴다."
```

---

## Task 4: 게시글 목록 화면

검색과 페이징을 URL에 두고, 스토어가 리소스와 재조회 범위를 소유한다.

**Files:**
- Create: `frontend-angular/src/app/features/post/post-api.service.ts`
- Create: `frontend-angular/src/app/features/post/post.store.ts`
- Create: `frontend-angular/src/app/features/post/post.store.spec.ts`
- Create: `frontend-angular/src/app/features/post/components/post-list.ts`
- Create: `frontend-angular/src/app/features/post/components/search-form.ts`
- Create: `frontend-angular/src/app/features/post/components/pagination.ts`
- Modify: `frontend-angular/src/app/features/post/pages/post-list-page.ts`
- Create: `frontend-angular/src/app/features/post/pages/post-list-page.spec.ts`

**Interfaces:**
- Consumes: `schema.d.ts` 타입 (Task 2)
- Produces:
  - `PostSummary`, `PostDetail`, `PostPage` 타입
  - `PostListSearch` — `{ page: number; size: number; keyword?: string }`
  - `PostStore.setSearch(search: PostListSearch): void`
  - `PostStore.list` — `HttpResourceRef<PostPage | undefined>`
  - `PostApiService.search(search)` / `.get(id)` — `Observable<...>`

- [ ] **Step 1: 스토어 테스트를 쓴다**

`frontend-angular/src/app/features/post/post.store.spec.ts`

```ts
import { provideHttpClient } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { 게시글요약, 게시글페이지 } from "@test/handlers";
import { server } from "@test/setup";
import { PostStore } from "./post.store";

function 스토어를_만든다(): PostStore {
  TestBed.configureTestingModule({ providers: [provideHttpClient()] });
  return TestBed.inject(PostStore);
}

describe("PostStore", () => {
  it("검색 조건이 바뀌면 다시 불러온다", async () => {
    const 받은키워드: Array<string | null> = [];
    server.use(
      http.get("/api/posts", ({ request }) => {
        받은키워드.push(new URL(request.url).searchParams.get("keyword"));
        return HttpResponse.json(게시글페이지([게시글요약()]));
      }),
    );

    const store = 스토어를_만든다();
    store.setSearch({ page: 0, size: 20 });
    await vi.waitFor(() => expect(store.list.hasValue()).toBe(true));

    store.setSearch({ page: 0, size: 20, keyword: "스프링" });
    await vi.waitFor(() => expect(받은키워드).toContain("스프링"));
  });

  it("공백뿐인 키워드는 조건에서 빠진다", async () => {
    let 받은키워드: string | null = "미확인";
    server.use(
      http.get("/api/posts", ({ request }) => {
        받은키워드 = new URL(request.url).searchParams.get("keyword");
        return HttpResponse.json(게시글페이지([]));
      }),
    );

    const store = 스토어를_만든다();
    store.setSearch({ page: 0, size: 20, keyword: "   " });

    await vi.waitFor(() => expect(받은키워드).toBeNull());
  });
});
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
cd frontend-angular && pnpm test
```

기대: `./post.store` 모듈을 찾을 수 없어 실패

- [ ] **Step 3: API 서비스를 만든다**

`frontend-angular/src/app/features/post/post-api.service.ts`

```ts
import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import type { Observable } from "rxjs";
import type { components } from "@/core/api/schema";

export type PostSummary = components["schemas"]["PostSummaryResponse"];
export type PostDetail = components["schemas"]["PostResponse"];
export type PostPage = components["schemas"]["PageResponsePostSummaryResponse"];

@Injectable({ providedIn: "root" })
export class PostApiService {
  private readonly http = inject(HttpClient);

  create(input: { title: string; content: string }): Observable<void> {
    return this.http.post<void>("/api/posts", input);
  }

  update(id: number, input: { title: string; content: string }): Observable<void> {
    return this.http.patch<void>(`/api/posts/${id}`, input);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`/api/posts/${id}`);
  }

  like(id: number): Observable<void> {
    return this.http.post<void>(`/api/posts/${id}/likes`, null);
  }

  unlike(id: number): Observable<void> {
    return this.http.delete<void>(`/api/posts/${id}/likes`);
  }
}
```

- [ ] **Step 4: 스토어를 만든다**

`frontend-angular/src/app/features/post/post.store.ts`

```ts
import { httpResource } from "@angular/common/http";
import { inject, Injectable, signal } from "@angular/core";
import { firstValueFrom } from "rxjs";
import { type PostDetail, type PostPage, PostApiService } from "./post-api.service";

export type PostListSearch = {
  page: number;
  size: number;
  keyword?: string;
};

@Injectable({ providedIn: "root" })
export class PostStore {
  private readonly api = inject(PostApiService);

  private readonly search = signal<PostListSearch>({ page: 0, size: 20 });
  private readonly selectedId = signal<number | null>(null);

  readonly list = httpResource<PostPage>(() => {
    const { page, size, keyword } = this.search();
    const 정리된키워드 = keyword?.trim();
    return {
      url: "/api/posts",
      params: { page, size, ...(정리된키워드 ? { keyword: 정리된키워드 } : {}) },
    };
  });

  readonly detail = httpResource<PostDetail>(() => {
    const id = this.selectedId();
    return id === null ? undefined : { url: `/api/posts/${id}` };
  });

  setSearch(search: PostListSearch): void {
    this.search.set(search);
  }

  select(id: number | null): void {
    this.selectedId.set(id);
  }

  async create(input: { title: string; content: string }): Promise<void> {
    await firstValueFrom(this.api.create(input));
    this.list.reload();
  }

  async update(id: number, input: { title: string; content: string }): Promise<void> {
    await firstValueFrom(this.api.update(id, input));
    this.detail.reload();
    this.list.reload();
  }

  async remove(id: number): Promise<void> {
    await firstValueFrom(this.api.remove(id));
    this.list.reload();
  }
}
```

재조회 호출이 이 파일에만 있다. 컴포넌트가 직접 `reload()`를 부르면 범위가 흩어진다.

- [ ] **Step 5: 테스트가 통과하는 것을 확인한다**

```bash
cd frontend-angular && pnpm test
```

기대: PASS

- [ ] **Step 6: 목록 컴포넌트를 만든다**

`frontend-angular/src/app/features/post/components/post-list.ts`

```ts
import { ChangeDetectionStrategy, Component, input } from "@angular/core";
import { RouterLink } from "@angular/router";
import type { PostPage } from "../post-api.service";

@Component({
  selector: "app-post-list",
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (isLoading()) {
      <p class="py-8 text-center text-slate-500">불러오는 중입니다.</p>
    } @else if (hasError()) {
      <p class="py-8 text-center text-red-600">목록을 불러오지 못했습니다.</p>
    } @else if ((page()?.content ?? []).length === 0) {
      <p class="py-8 text-center text-slate-500">게시글이 없습니다.</p>
    } @else {
      <ul class="divide-y divide-slate-200">
        @for (post of page()!.content; track post.id) {
          <li class="py-3">
            <a [routerLink]="['/posts', post.id]" class="font-medium hover:underline">
              {{ post.title }}
            </a>
            <div class="mt-1 flex gap-3 text-sm text-slate-500">
              <span>{{ post.authorNickname }}</span>
              <span>조회 {{ post.viewCount }}</span>
              <span>좋아요 {{ post.likeCount }}</span>
            </div>
          </li>
        }
      </ul>
    }
  `,
})
export class PostListComponent {
  readonly page = input<PostPage | undefined>();
  readonly isLoading = input(false);
  readonly hasError = input(false);
}
```

- [ ] **Step 7: 검색 폼과 페이지네이션을 만든다**

`frontend-angular/src/app/features/post/components/search-form.ts`

```ts
import { ChangeDetectionStrategy, Component, input, output } from "@angular/core";
import { ButtonComponent } from "@/shared/ui/button";

@Component({
  selector: "app-search-form",
  imports: [ButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="flex gap-2" (submit)="submit($event)">
      <input
        name="keyword"
        [value]="keyword()"
        placeholder="제목이나 본문으로 검색"
        aria-label="검색어"
        class="flex-1 rounded border border-slate-300 px-3 py-2"
      />
      <app-button type="submit">검색</app-button>
    </form>
  `,
})
export class SearchFormComponent {
  readonly keyword = input("");
  readonly search = output<string>();

  protected submit(event: Event): void {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const data = new FormData(form);
    this.search.emit(String(data.get("keyword") ?? ""));
  }
}
```

`frontend-angular/src/app/features/post/components/pagination.ts`

```ts
import { ChangeDetectionStrategy, Component, computed, input, output } from "@angular/core";
import { ButtonComponent } from "@/shared/ui/button";

@Component({
  selector: "app-pagination",
  imports: [ButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (totalPages() > 1) {
      <nav class="flex justify-center gap-1 py-4" aria-label="페이지">
        @for (index of pages(); track index) {
          <app-button
            size="sm"
            [variant]="index === page() ? 'default' : 'ghost'"
            (click)="change.emit(index)"
          >
            {{ index + 1 }}
          </app-button>
        }
      </nav>
    }
  `,
})
export class PaginationComponent {
  readonly page = input(0);
  readonly totalPages = input(0);
  readonly change = output<number>();

  protected readonly pages = computed(() =>
    Array.from({ length: this.totalPages() }, (_, index) => index),
  );
}
```

- [ ] **Step 8: 목록 화면 테스트를 쓴다**

`frontend-angular/src/app/features/post/pages/post-list-page.spec.ts`

```ts
import { provideHttpClient } from "@angular/common/http";
import { provideRouter } from "@angular/router";
import { render, screen } from "@testing-library/angular";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { 게시글요약, 게시글페이지 } from "@test/handlers";
import { server } from "@test/setup";
import { PostListPage } from "./post-list-page";

describe("PostListPage", () => {
  it("게시글 제목과 작성자를 보여준다", async () => {
    server.use(
      http.get("/api/posts", () =>
        HttpResponse.json(
          게시글페이지([
            게시글요약({ id: 1, title: "첫 글", authorNickname: "작성자" }),
            게시글요약({ id: 2, title: "둘째 글", authorNickname: "작성자" }),
          ]),
        ),
      ),
    );

    await render(PostListPage, { providers: [provideHttpClient(), provideRouter([])] });

    expect(await screen.findByText("첫 글")).toBeInTheDocument();
    expect(screen.getByText("둘째 글")).toBeInTheDocument();
  });

  it("결과가 없으면 안내를 보여준다", async () => {
    server.use(http.get("/api/posts", () => HttpResponse.json(게시글페이지([]))));

    await render(PostListPage, { providers: [provideHttpClient(), provideRouter([])] });

    expect(await screen.findByText(/게시글이 없습니다/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 9: 목록 화면을 완성한다**

`frontend-angular/src/app/features/post/pages/post-list-page.ts`

```ts
import { ChangeDetectionStrategy, Component, effect, inject, input } from "@angular/core";
import { Router, RouterLink } from "@angular/router";
import { PaginationComponent } from "../components/pagination";
import { PostListComponent } from "../components/post-list";
import { SearchFormComponent } from "../components/search-form";
import { PostStore } from "../post.store";

function 정수로(value: string | undefined, 기본값: number, 최댓값: number): number {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 && parsed <= 최댓값 ? parsed : 기본값;
}

@Component({
  selector: "app-post-list-page",
  imports: [PostListComponent, SearchFormComponent, PaginationComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-4">
      <div class="flex justify-end">
        <a routerLink="/posts/new" class="rounded bg-slate-900 px-4 py-2 text-sm text-white">
          글쓰기
        </a>
      </div>
      <app-search-form [keyword]="keyword() ?? ''" (search)="onSearch($event)" />
      <app-post-list
        [page]="store.list.value()"
        [isLoading]="store.list.isLoading()"
        [hasError]="store.list.error() !== undefined"
      />
      <app-pagination
        [page]="정수로(page(), 0, 10000)"
        [totalPages]="store.list.value()?.totalPages ?? 0"
        (change)="onPage($event)"
      />
    </div>
  `,
})
export class PostListPage {
  protected readonly store = inject(PostStore);
  private readonly router = inject(Router);

  readonly page = input<string>();
  readonly size = input<string>();
  readonly keyword = input<string>();

  protected readonly 정수로 = 정수로;

  constructor() {
    // URL이 상태의 출처다. 값이 바뀌면 스토어에 반영하고, 스토어가 다시 불러온다.
    effect(() => {
      const keyword = this.keyword()?.trim();
      this.store.setSearch({
        page: 정수로(this.page(), 0, 10000),
        size: 정수로(this.size(), 20, 100) || 20,
        ...(keyword ? { keyword } : {}),
      });
    });
  }

  protected onSearch(keyword: string): void {
    void this.router.navigate([], {
      queryParams: { page: 0, keyword: keyword.trim() || null },
      queryParamsHandling: "merge",
    });
  }

  protected onPage(page: number): void {
    void this.router.navigate([], { queryParams: { page }, queryParamsHandling: "merge" });
  }
}
```

- [ ] **Step 10: 검증하고 커밋한다**

```bash
cd frontend-angular && pnpm lint:fix && pnpm verify
```

브라우저에서 검색어를 넣고 URL이 바뀌는지, 새로고침해도 유지되는지, 뒤로가기가 이전 검색으로 돌아가는지 확인한다.

```bash
cd .. && git add frontend-angular
git commit -m "feat: 게시글 목록 화면 추가

검색어와 페이지를 URL 쿼리 파라미터에 두고, 컴포넌트 입력으로
받아 스토어에 반영한다. URL이 상태의 출처이므로 새로고침과
뒤로가기가 그대로 동작한다.

재조회 호출은 스토어에만 둔다. 컴포넌트가 직접 부르면 무효화
범위가 흩어진다."
```

---
## Task 5: 게시글 상세 화면

**Files:**
- Create: `frontend-angular/src/app/features/post/components/post-detail.ts`
- Create: `frontend-angular/src/app/features/post/pages/post-detail-page.ts`
- Create: `frontend-angular/src/app/features/post/pages/post-detail-page.spec.ts`
- Modify: `frontend-angular/src/app/app.routes.ts`

**Interfaces:**
- Consumes: `PostStore.detail`, `PostStore.select(id)`, `PostStore.remove(id)` (Task 4), `CurrentMemberStore.member` (Task 3)
- Produces: `/posts/:postId` 라우트. 작성자 본인에게만 수정·삭제를 노출한다

- [ ] **Step 1: 상세 화면 테스트를 쓴다**

`frontend-angular/src/app/features/post/pages/post-detail-page.spec.ts`

```ts
import { provideHttpClient } from "@angular/common/http";
import { provideRouter } from "@angular/router";
import { render, screen } from "@testing-library/angular";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import type { components } from "@/core/api/schema";
import { server } from "@test/setup";
import { PostDetailPage } from "./post-detail-page";

type PostResponse = components["schemas"]["PostResponse"];

function 게시글(overrides: Partial<PostResponse> = {}): PostResponse {
  return {
    id: 1,
    title: "제목",
    content: "본문입니다.",
    authorId: 1,
    viewCount: 3,
    likeCount: 2,
    createdAt: "2026-09-21T00:00:00Z",
    ...overrides,
  };
}

async function 화면을_그린다(postId = "1") {
  return render(PostDetailPage, {
    inputs: { postId },
    providers: [provideHttpClient(), provideRouter([])],
  });
}

describe("PostDetailPage", () => {
  it("제목과 본문을 보여준다", async () => {
    server.use(http.get("/api/posts/:id", () => HttpResponse.json(게시글())));

    await 화면을_그린다();

    expect(await screen.findByRole("heading", { name: "제목" })).toBeInTheDocument();
    expect(screen.getByText("본문입니다.")).toBeInTheDocument();
  });

  it("작성자 본인에게만 수정과 삭제를 보여준다", async () => {
    server.use(http.get("/api/posts/:id", () => HttpResponse.json(게시글({ authorId: 1 }))));

    await 화면을_그린다();

    expect(await screen.findByRole("link", { name: "수정" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "삭제" })).toBeInTheDocument();
  });

  it("다른 사람 글에는 수정과 삭제를 보여주지 않는다", async () => {
    server.use(http.get("/api/posts/:id", () => HttpResponse.json(게시글({ authorId: 999 }))));

    await 화면을_그린다();

    expect(await screen.findByRole("heading", { name: "제목" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "수정" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "삭제" })).not.toBeInTheDocument();
  });

  it("없는 글이면 안내를 보여준다", async () => {
    server.use(
      http.get("/api/posts/:id", () =>
        HttpResponse.json({ status: 404, code: "POST_NOT_FOUND" }, { status: 404 }),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByText(/게시글을 찾을 수 없습니다/)).toBeInTheDocument();
  });
});
```

기본 MSW 핸들러의 `/api/members/me`가 `id: 1`을 반환하므로 `authorId: 1`이면 본인이고 `999`면 남의 글이다.

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
cd frontend-angular && pnpm test
```

기대: `./post-detail-page` 모듈을 찾을 수 없어 실패

- [ ] **Step 3: 상세 컴포넌트를 만든다**

`frontend-angular/src/app/features/post/components/post-detail.ts`

```ts
import { ChangeDetectionStrategy, Component, input, output } from "@angular/core";
import { RouterLink } from "@angular/router";
import { ButtonComponent } from "@/shared/ui/button";
import type { PostDetail } from "../post-api.service";

@Component({
  selector: "app-post-detail",
  imports: [RouterLink, ButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (post(); as value) {
      <article class="space-y-4">
        <header class="space-y-2 border-b border-slate-200 pb-3">
          <h1 class="text-2xl font-semibold">{{ value.title }}</h1>
          <div class="flex items-center gap-3 text-sm text-slate-500">
            <span>조회 {{ value.viewCount }}</span>
            <ng-content select="[like]" />
          </div>
        </header>

        <p class="leading-relaxed whitespace-pre-wrap">{{ value.content }}</p>

        @if (canEdit()) {
          <div class="flex gap-2">
            <a
              [routerLink]="['/posts', value.id, 'edit']"
              class="rounded border border-slate-300 px-3 py-1 text-sm"
            >
              수정
            </a>
            <app-button variant="destructive" size="sm" (click)="remove.emit()">삭제</app-button>
          </div>
        }
      </article>
    }
  `,
})
export class PostDetailComponent {
  readonly post = input<PostDetail | undefined>();
  readonly canEdit = input(false);
  readonly remove = output<void>();
}
```

- [ ] **Step 4: 상세 라우트 컴포넌트를 만든다**

`frontend-angular/src/app/features/post/pages/post-detail-page.ts`

```ts
import { ChangeDetectionStrategy, Component, computed, effect, inject, input } from "@angular/core";
import { Router } from "@angular/router";
import { isProblemCode, toProblem } from "@/core/api/problem";
import { CurrentMemberStore } from "@/core/auth/current-member.store";
import { PostDetailComponent } from "../components/post-detail";
import { PostStore } from "../post.store";

@Component({
  selector: "app-post-detail-page",
  imports: [PostDetailComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (store.detail.isLoading()) {
      <p class="py-8 text-center text-slate-500">불러오는 중입니다.</p>
    } @else if (errorMessage(); as message) {
      <p class="py-8 text-center text-slate-600">{{ message }}</p>
    } @else {
      <app-post-detail
        [post]="store.detail.value()"
        [canEdit]="canEdit()"
        (remove)="remove()"
      />
    }
  `,
})
export class PostDetailPage {
  protected readonly store = inject(PostStore);
  private readonly memberStore = inject(CurrentMemberStore);
  private readonly router = inject(Router);

  readonly postId = input.required<string>();

  protected readonly errorMessage = computed(() => {
    const error = this.store.detail.error();
    if (!error) {
      return null;
    }
    if (isProblemCode(error, "POST_NOT_FOUND")) {
      return "게시글을 찾을 수 없습니다.";
    }
    return toProblem(error)?.detail ?? "게시글을 불러오지 못했습니다.";
  });

  protected readonly canEdit = computed(() => {
    const member = this.memberStore.member();
    const post = this.store.detail.value();
    return member != null && post != null && member.id === post.authorId;
  });

  constructor() {
    effect(() => {
      this.store.select(Number(this.postId()));
    });
  }

  protected remove(): void {
    if (!window.confirm("게시글을 삭제할까요?")) {
      return;
    }
    void this.store.remove(Number(this.postId())).then(() => this.router.navigate(["/"]));
  }
}
```

`HttpErrorResponse`의 본문이 `error` 속성에 담기므로 `isProblemCode`가 인식하지 못할 수 있다. 첫 실행에서 "없는 글" 테스트가 실패하면 `toProblem(error)` 대신 `toProblem((error as { error?: unknown }).error)`로 감싸고, 그 변환을 `problem.ts`에 `fromHttpError(error: unknown)`로 추가해 한 곳에서 처리한다.

- [ ] **Step 5: 라우트를 등록한다**

`frontend-angular/src/app/app.routes.ts`에 추가한다.

```ts
  {
    path: "posts/:postId",
    loadComponent: () => import("@/features/post/pages/post-detail-page").then((m) => m.PostDetailPage),
  },
```

- [ ] **Step 6: 검증하고 커밋한다**

```bash
cd frontend-angular && pnpm lint:fix && pnpm verify
cd .. && git add frontend-angular
git commit -m "feat: 게시글 상세 화면 추가

작성자 본인에게만 수정과 삭제를 노출한다. 서버가 권한을 최종
판단하므로 이 노출은 편의이며 보안 경계가 아니다.

에러는 ProblemDetail의 code로 분기해 없는 글과 그 밖의 실패를
구분해 안내한다."
```

---

## Task 6: 게시글 작성 및 수정 폼

Signal Forms로 구현한다. 막히면 Reactive Forms로 내리고 그 판단을 이 태스크에서 확정한다.

**Files:**
- Create: `frontend-angular/src/app/features/post/components/post-form.ts`
- Create: `frontend-angular/src/app/features/post/components/post-form.spec.ts`
- Create: `frontend-angular/src/app/features/post/pages/post-new-page.ts`
- Create: `frontend-angular/src/app/features/post/pages/post-edit-page.ts`
- Modify: `frontend-angular/src/app/app.routes.ts`

**Interfaces:**
- Consumes: `PostStore.create`, `PostStore.update` (Task 4), `fieldErrors` (Task 2), `authGuard` (Task 3)
- Produces:
  - `<app-post-form>` — `initial: { title: string; content: string }`, `submitting: boolean`, `fieldErrors: Record<string, string>`, `message: string | null`, `(save)` 출력
  - `/posts/new`, `/posts/:postId/edit` 라우트 (인증 필요)

- [ ] **Step 1: 폼 방식을 결정한다**

Signal Forms의 실제 사용법을 짧은 예제로 확인한다.

```bash
cd frontend-angular
cat > /tmp/signal-forms-probe.ts <<'EOF'
import { form, schema } from "@angular/forms/signals";
EOF
pnpm exec tsc --noEmit /tmp/signal-forms-probe.ts 2>&1 | head -5
```

기대: 오류 없음. `@angular/forms/signals` 경로가 존재하지 않거나 `form`·`schema`를 찾을 수 없으면 **Reactive Forms로 진행하고 이 결정을 커밋 메시지에 남긴다.** 아래 Step 3에 두 방식을 모두 적어 두었다.

- [ ] **Step 2: 폼 테스트를 쓴다**

`frontend-angular/src/app/features/post/components/post-form.spec.ts`

```ts
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { PostFormComponent } from "./post-form";

describe("PostFormComponent", () => {
  it("입력한 값을 전달한다", async () => {
    const save = vi.fn();

    await render(PostFormComponent, {
      inputs: { initial: { title: "", content: "" }, submitting: false, fieldErrors: {} },
      on: { save },
    });

    await userEvent.type(screen.getByLabelText("제목"), "새 글");
    await userEvent.type(screen.getByLabelText("본문"), "내용입니다");
    await userEvent.click(screen.getByRole("button", { name: "저장" }));

    expect(save).toHaveBeenCalledWith({ title: "새 글", content: "내용입니다" });
  });

  it("서버 검증 오류를 필드 아래에 보여준다", async () => {
    await render(PostFormComponent, {
      inputs: {
        initial: { title: "", content: "" },
        submitting: false,
        fieldErrors: { title: "제목은 필수입니다." },
      },
    });

    expect(screen.getByText("제목은 필수입니다.")).toBeInTheDocument();
  });

  it("수정 모드에서는 기존 값이 채워진다", async () => {
    await render(PostFormComponent, {
      inputs: {
        initial: { title: "기존 제목", content: "기존 본문" },
        submitting: false,
        fieldErrors: {},
      },
    });

    expect(screen.getByLabelText("제목")).toHaveValue("기존 제목");
    expect(screen.getByLabelText("본문")).toHaveValue("기존 본문");
  });
});
```

- [ ] **Step 3: 폼 컴포넌트를 만든다**

`frontend-angular/src/app/features/post/components/post-form.ts` — Step 1에서 Signal Forms가 쓸 수 있다고 확인된 경우, `form()`으로 모델을 만들고 템플릿에서 `[control]`로 연결한다. 확인되지 않았다면 아래 Reactive Forms 구현을 쓴다.

```ts
import { ChangeDetectionStrategy, Component, effect, inject, input, output } from "@angular/core";
import { FormBuilder, ReactiveFormsModule } from "@angular/forms";
import { ButtonComponent } from "@/shared/ui/button";

@Component({
  selector: "app-post-form",
  imports: [ReactiveFormsModule, ButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="form" class="space-y-4" (ngSubmit)="submit()">
      <div class="space-y-1">
        <label for="title" class="block text-sm font-medium">제목</label>
        <input
          id="title"
          formControlName="title"
          class="w-full rounded border border-slate-300 px-3 py-2"
        />
        @if (fieldErrors()["title"]; as message) {
          <p class="text-sm text-red-600">{{ message }}</p>
        }
      </div>

      <div class="space-y-1">
        <label for="content" class="block text-sm font-medium">본문</label>
        <textarea
          id="content"
          formControlName="content"
          rows="12"
          class="w-full rounded border border-slate-300 px-3 py-2"
        ></textarea>
        @if (fieldErrors()["content"]; as message) {
          <p class="text-sm text-red-600">{{ message }}</p>
        }
      </div>

      @if (message()) {
        <p class="text-sm text-red-600">{{ message() }}</p>
      }

      <app-button type="submit" [disabled]="submitting()">저장</app-button>
    </form>
  `,
})
export class PostFormComponent {
  readonly initial = input.required<{ title: string; content: string }>();
  readonly submitting = input(false);
  readonly fieldErrors = input<Record<string, string>>({});
  readonly message = input<string | null>(null);
  readonly save = output<{ title: string; content: string }>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    title: "",
    content: "",
  });

  constructor() {
    effect(() => {
      this.form.setValue(this.initial());
    });
  }

  protected submit(): void {
    this.save.emit(this.form.getRawValue());
  }
}
```

- [ ] **Step 4: 테스트가 통과하는 것을 확인한다**

```bash
cd frontend-angular && pnpm test
```

기대: PASS

- [ ] **Step 5: 작성·수정 라우트를 만든다**

`frontend-angular/src/app/features/post/pages/post-new-page.ts`

```ts
import { ChangeDetectionStrategy, Component, inject, signal } from "@angular/core";
import { Router } from "@angular/router";
import { fieldErrors, toProblem } from "@/core/api/problem";
import { PostFormComponent } from "../components/post-form";
import { PostStore } from "../post.store";

@Component({
  selector: "app-post-new-page",
  imports: [PostFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-post-form
      [initial]="{ title: '', content: '' }"
      [submitting]="submitting()"
      [fieldErrors]="errors()"
      [message]="message()"
      (save)="save($event)"
    />
  `,
})
export class PostNewPage {
  private readonly store = inject(PostStore);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly errors = signal<Record<string, string>>({});
  protected readonly message = signal<string | null>(null);

  protected async save(input: { title: string; content: string }): Promise<void> {
    this.submitting.set(true);
    this.errors.set({});
    this.message.set(null);
    try {
      await this.store.create(input);
      await this.router.navigate(["/"]);
    } catch (error) {
      this.errors.set(fieldErrors(error));
      this.message.set(toProblem(error)?.detail ?? "저장하지 못했습니다.");
    } finally {
      this.submitting.set(false);
    }
  }
}
```

`frontend-angular/src/app/features/post/pages/post-edit-page.ts` — 같은 구조이며 `initial`을 `store.detail.value()`에서 채우고 `store.update(id, input)`를 호출한 뒤 상세로 이동한다.

```ts
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from "@angular/core";
import { Router } from "@angular/router";
import { fieldErrors, toProblem } from "@/core/api/problem";
import { PostFormComponent } from "../components/post-form";
import { PostStore } from "../post.store";

@Component({
  selector: "app-post-edit-page",
  imports: [PostFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (store.detail.value(); as post) {
      <app-post-form
        [initial]="{ title: post.title, content: post.content }"
        [submitting]="submitting()"
        [fieldErrors]="errors()"
        [message]="message()"
        (save)="save($event)"
      />
    } @else {
      <p class="py-8 text-center text-slate-500">불러오는 중입니다.</p>
    }
  `,
})
export class PostEditPage {
  protected readonly store = inject(PostStore);
  private readonly router = inject(Router);

  readonly postId = input.required<string>();

  protected readonly submitting = signal(false);
  protected readonly errors = signal<Record<string, string>>({});
  protected readonly message = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.store.select(Number(this.postId()));
    });
  }

  protected async save(input: { title: string; content: string }): Promise<void> {
    this.submitting.set(true);
    this.errors.set({});
    this.message.set(null);
    try {
      await this.store.update(Number(this.postId()), input);
      await this.router.navigate(["/posts", this.postId()]);
    } catch (error) {
      this.errors.set(fieldErrors(error));
      this.message.set(toProblem(error)?.detail ?? "저장하지 못했습니다.");
    } finally {
      this.submitting.set(false);
    }
  }
}
```

`app.routes.ts`에 추가한다. 순서가 중요하다. `posts/new`가 `posts/:postId`보다 앞에 와야 한다.

```ts
  {
    path: "posts/new",
    canActivate: [authGuard],
    loadComponent: () => import("@/features/post/pages/post-new-page").then((m) => m.PostNewPage),
  },
  {
    path: "posts/:postId/edit",
    canActivate: [authGuard],
    loadComponent: () => import("@/features/post/pages/post-edit-page").then((m) => m.PostEditPage),
  },
```

- [ ] **Step 6: 검증하고 커밋한다**

```bash
cd frontend-angular && pnpm lint:fix && pnpm verify
cd .. && git add frontend-angular
git commit -m "feat: 게시글 작성 및 수정 폼 추가

백엔드가 주는 errors 필드를 그대로 필드별 메시지로 연결하므로
검증 규칙을 프론트엔드에 복제하지 않는다.

라우트 순서에 주의한다. posts/new가 posts/:postId보다 뒤에 오면
new가 식별자로 해석된다."
```

Signal Forms 대신 Reactive Forms를 선택했다면 그 이유를 커밋 메시지에 한 줄 덧붙인다.

---
## Task 7: 댓글 및 대댓글

**Files:**
- Create: `frontend-angular/src/app/features/comment/comment-api.service.ts`
- Create: `frontend-angular/src/app/features/comment/comment.store.ts`
- Create: `frontend-angular/src/app/features/comment/components/comment-form.ts`
- Create: `frontend-angular/src/app/features/comment/components/comment-item.ts`
- Create: `frontend-angular/src/app/features/comment/components/comment-section.ts`
- Create: `frontend-angular/src/app/features/comment/components/comment-section.spec.ts`
- Modify: `frontend-angular/src/app/features/post/pages/post-detail-page.ts`

**Interfaces:**
- Consumes: `CurrentMemberStore.member` (Task 3), `fieldErrors` (Task 2)
- Produces:
  - `Comment`, `CommentPage` 타입
  - `CommentStore.select(postId: number | null)`, `.list` (`HttpResourceRef<CommentPage | undefined>`)
  - `CommentStore.write({ body, parentCommentId? })`, `.update(id, body)`, `.remove(id)` — 모두 성공 후 `list.reload()`
  - `<app-comment-section [postId]="number">`

- [ ] **Step 1: 댓글 화면 테스트를 쓴다**

`frontend-angular/src/app/features/comment/components/comment-section.spec.ts`

```ts
import { provideHttpClient } from "@angular/common/http";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import type { components } from "@/core/api/schema";
import { server } from "@test/setup";
import { CommentSectionComponent } from "./comment-section";

type CommentResponse = components["schemas"]["CommentResponse"];
type CommentPage = components["schemas"]["PageResponseCommentResponse"];

function 댓글(overrides: Partial<CommentResponse> = {}): CommentResponse {
  return {
    id: 1,
    postId: 1,
    authorId: 1,
    body: "댓글입니다",
    depth: 0,
    createdAt: "2026-09-21T00:00:00Z",
    ...overrides,
  };
}

function 댓글페이지(content: CommentResponse[]): CommentPage {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1, last: true };
}

async function 화면을_그린다() {
  return render(CommentSectionComponent, {
    inputs: { postId: 1 },
    providers: [provideHttpClient()],
  });
}

describe("CommentSectionComponent", () => {
  it("댓글과 대댓글을 함께 보여준다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(
          댓글페이지([
            댓글({ id: 1, body: "원댓글", depth: 0 }),
            댓글({ id: 2, body: "답글", depth: 1, parentCommentId: 1 }),
          ]),
        ),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByText("원댓글")).toBeInTheDocument();
    expect(screen.getByText("답글")).toBeInTheDocument();
  });

  it("댓글을 작성하면 본문을 전송한다", async () => {
    let 받은본문: unknown = null;
    server.use(
      http.get("/api/posts/:postId/comments", () => HttpResponse.json(댓글페이지([]))),
      http.post("/api/posts/:postId/comments", async ({ request }) => {
        받은본문 = await request.json();
        return new HttpResponse(null, { status: 201 });
      }),
    );

    await 화면을_그린다();

    await userEvent.type(await screen.findByLabelText("댓글"), "새 댓글");
    await userEvent.click(screen.getByRole("button", { name: "등록" }));

    await vi.waitFor(() => expect(받은본문).toEqual({ body: "새 댓글" }));
  });

  it("대댓글에는 답글 버튼을 보여주지 않는다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(댓글페이지([댓글({ id: 2, body: "답글", depth: 1, parentCommentId: 1 })])),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByText("답글")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "답글 달기" })).not.toBeInTheDocument();
  });

  it("작성자 본인에게만 수정과 삭제를 보여준다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(댓글페이지([댓글({ id: 1, authorId: 999, body: "남의 댓글" })])),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByText("남의 댓글")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "삭제" })).not.toBeInTheDocument();
  });
});
```

세 번째 테스트가 백엔드의 깊이 제한을 UI에 반영했는지 확인한다. 서버가 400을 주기 전에 버튼 자체가 없어야 한다.

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
cd frontend-angular && pnpm test
```

기대: `./comment-section` 모듈을 찾을 수 없어 실패

- [ ] **Step 3: API 서비스와 스토어를 만든다**

`frontend-angular/src/app/features/comment/comment-api.service.ts`

```ts
import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import type { Observable } from "rxjs";
import type { components } from "@/core/api/schema";

export type Comment = components["schemas"]["CommentResponse"];
export type CommentPage = components["schemas"]["PageResponseCommentResponse"];

@Injectable({ providedIn: "root" })
export class CommentApiService {
  private readonly http = inject(HttpClient);

  write(postId: number, input: { body: string; parentCommentId?: number }): Observable<void> {
    return this.http.post<void>(`/api/posts/${postId}/comments`, input);
  }

  update(id: number, body: string): Observable<void> {
    return this.http.patch<void>(`/api/comments/${id}`, { body });
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`/api/comments/${id}`);
  }
}
```

`frontend-angular/src/app/features/comment/comment.store.ts`

```ts
import { httpResource } from "@angular/common/http";
import { inject, Injectable, signal } from "@angular/core";
import { firstValueFrom } from "rxjs";
import { CommentApiService, type CommentPage } from "./comment-api.service";

@Injectable({ providedIn: "root" })
export class CommentStore {
  private readonly api = inject(CommentApiService);
  private readonly postId = signal<number | null>(null);

  /** 게시판 규모에서 댓글 페이지네이션은 UI만 복잡하게 만든다. 한 번에 가져온다. */
  readonly list = httpResource<CommentPage>(() => {
    const id = this.postId();
    return id === null
      ? undefined
      : { url: `/api/posts/${id}/comments`, params: { page: 0, size: 100 } };
  });

  select(postId: number | null): void {
    this.postId.set(postId);
  }

  async write(input: { body: string; parentCommentId?: number }): Promise<void> {
    const id = this.postId();
    if (id === null) {
      throw new Error("게시글이 선택되지 않았습니다.");
    }
    await firstValueFrom(this.api.write(id, input));
    this.list.reload();
  }

  async update(id: number, body: string): Promise<void> {
    await firstValueFrom(this.api.update(id, body));
    this.list.reload();
  }

  async remove(id: number): Promise<void> {
    await firstValueFrom(this.api.remove(id));
    this.list.reload();
  }
}
```

- [ ] **Step 4: 댓글 폼과 항목을 만든다**

`frontend-angular/src/app/features/comment/components/comment-form.ts`

```ts
import { ChangeDetectionStrategy, Component, input, output, signal } from "@angular/core";
import { ButtonComponent } from "@/shared/ui/button";

@Component({
  selector: "app-comment-form",
  imports: [ButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="space-y-1" (submit)="submit($event)">
      <textarea
        name="body"
        rows="3"
        [value]="initial()"
        [attr.aria-label]="label()"
        class="w-full rounded border border-slate-300 px-3 py-2 text-sm"
      ></textarea>
      @if (error(); as message) {
        <p class="text-sm text-red-600">{{ message }}</p>
      }
      <app-button type="submit" size="sm" [disabled]="submitting()">{{ submitLabel() }}</app-button>
    </form>
  `,
})
export class CommentFormComponent {
  readonly label = input.required<string>();
  readonly submitLabel = input.required<string>();
  readonly initial = input("");
  readonly error = input<string | null>(null);
  readonly save = output<string>();

  protected readonly submitting = signal(false);

  protected submit(event: Event): void {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const data = new FormData(form);
    this.save.emit(String(data.get("body") ?? ""));
  }
}
```

`frontend-angular/src/app/features/comment/components/comment-item.ts` — 표시 모드를 `signal<"보기" | "수정" | "답글">`로 두고, 본인 댓글이면 수정·삭제를, 깊이가 0이고 로그인 상태이면 답글 버튼을 노출한다. 답글의 답글은 서버가 400으로 거부하므로 버튼 자체를 내보내지 않아 그 전에 막는다. 수정·삭제·답글은 모두 `CommentStore`의 메서드를 부른다.

```ts
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from "@angular/core";
import { CurrentMemberStore } from "@/core/auth/current-member.store";
import type { Comment } from "../comment-api.service";
import { CommentStore } from "../comment.store";
import { CommentFormComponent } from "./comment-form";

type 표시모드 = "보기" | "수정" | "답글";

@Component({
  selector: "app-comment-item",
  imports: [CommentFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <li [class]="comment().depth > 0 ? 'border-l-2 border-slate-200 py-2 pl-6' : 'py-2'">
      @if (모드() === "수정") {
        <app-comment-form
          label="댓글 수정"
          submitLabel="수정"
          [initial]="comment().body"
          (save)="수정한다($event)"
        />
      } @else {
        <p class="text-sm whitespace-pre-wrap">{{ comment().body }}</p>
      }

      <div class="mt-1 flex gap-2 text-xs text-slate-500">
        @if (답글가능()) {
          <button type="button" (click)="전환('답글')">답글 달기</button>
        }
        @if (본인댓글()) {
          <button type="button" (click)="전환('수정')">수정</button>
          <button type="button" class="text-red-600" (click)="삭제한다()">삭제</button>
        }
      </div>

      @if (모드() === "답글") {
        <div class="mt-2 pl-6">
          <app-comment-form label="답글" submitLabel="등록" (save)="답글단다($event)" />
        </div>
      }
    </li>
  `,
})
export class CommentItemComponent {
  readonly comment = input.required<Comment>();

  private readonly store = inject(CommentStore);
  private readonly memberStore = inject(CurrentMemberStore);

  protected readonly 모드 = signal<표시모드>("보기");

  protected readonly 본인댓글 = computed(
    () => this.memberStore.member()?.id === this.comment().authorId,
  );
  protected readonly 답글가능 = computed(
    () => this.memberStore.member() != null && this.comment().depth === 0,
  );

  protected 전환(대상: 표시모드): void {
    this.모드.update((현재) => (현재 === 대상 ? "보기" : 대상));
  }

  protected 수정한다(body: string): void {
    void this.store.update(this.comment().id, body).then(() => this.모드.set("보기"));
  }

  protected 답글단다(body: string): void {
    void this.store
      .write({ body, parentCommentId: this.comment().id })
      .then(() => this.모드.set("보기"));
  }

  protected 삭제한다(): void {
    if (window.confirm("댓글을 삭제할까요?")) {
      void this.store.remove(this.comment().id);
    }
  }
}
```

- [ ] **Step 5: 댓글 영역을 만든다**

`frontend-angular/src/app/features/comment/components/comment-section.ts`

```ts
import { ChangeDetectionStrategy, Component, effect, inject, input } from "@angular/core";
import { CurrentMemberStore } from "@/core/auth/current-member.store";
import { CommentStore } from "../comment.store";
import { CommentFormComponent } from "./comment-form";
import { CommentItemComponent } from "./comment-item";

@Component({
  selector: "app-comment-section",
  imports: [CommentFormComponent, CommentItemComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="space-y-3 border-t border-slate-200 pt-4">
      <h2 class="text-lg font-medium">댓글 {{ store.list.value()?.totalElements ?? 0 }}</h2>

      @if (memberStore.member()) {
        <app-comment-form label="댓글" submitLabel="등록" (save)="작성한다($event)" />
      } @else {
        <p class="text-sm text-slate-500">댓글을 쓰려면 로그인이 필요합니다.</p>
      }

      @if (store.list.isLoading()) {
        <p class="text-sm text-slate-500">불러오는 중입니다.</p>
      } @else if (store.list.error()) {
        <p class="text-sm text-red-600">댓글을 불러오지 못했습니다.</p>
      }

      <ul class="divide-y divide-slate-100">
        @for (comment of store.list.value()?.content ?? []; track comment.id) {
          <app-comment-item [comment]="comment" />
        }
      </ul>
    </section>
  `,
})
export class CommentSectionComponent {
  readonly postId = input.required<number>();

  protected readonly store = inject(CommentStore);
  protected readonly memberStore = inject(CurrentMemberStore);

  constructor() {
    effect(() => {
      this.store.select(this.postId());
    });
  }

  protected 작성한다(body: string): void {
    void this.store.write({ body });
  }
}
```

- [ ] **Step 6: 상세 화면에 붙인다**

`post-detail-page.ts`의 `imports`에 `CommentSectionComponent`를 추가하고 템플릿 끝에 넣는다.

```html
      <app-comment-section [postId]="+postId()" />
```

- [ ] **Step 7: 검증하고 커밋한다**

```bash
cd frontend-angular && pnpm lint:fix && pnpm verify
cd .. && git add frontend-angular
git commit -m "feat: 댓글 및 대댓글 추가

대댓글에는 답글 버튼을 노출하지 않아 서버가 400으로 거부하기 전에
막는다. 깊이 제한의 최종 판단은 여전히 도메인이 한다.

재조회는 댓글 스토어가 소유한다. 작성, 수정, 삭제 모두 같은
목록을 다시 불러온다."
```

---

## Task 8: 좋아요

**Files:**
- Create: `frontend-angular/src/app/features/post/components/like-button.ts`
- Create: `frontend-angular/src/app/features/post/components/like-button.spec.ts`
- Modify: `frontend-angular/src/app/features/post/post.store.ts`
- Modify: `frontend-angular/src/app/features/post/pages/post-detail-page.ts`

**Interfaces:**
- Consumes: `PostApiService.like/unlike` (Task 4), `toProblem` (Task 2)
- Produces:
  - `PostStore.like(id)` / `.unlike(id)` — 성공·실패 모두 `detail.reload()`
  - `<app-like-button [postId]="number" [likeCount]="number">`

- [ ] **Step 1: 좋아요 테스트를 쓴다**

`frontend-angular/src/app/features/post/components/like-button.spec.ts`

```ts
import { provideHttpClient } from "@angular/common/http";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { server } from "@test/setup";
import { LikeButtonComponent } from "./like-button";

describe("LikeButtonComponent", () => {
  it("누르면 숫자가 올라간다", async () => {
    server.use(http.post("/api/posts/:id/likes", () => new HttpResponse(null, { status: 204 })));

    await render(LikeButtonComponent, {
      inputs: { postId: 1, likeCount: 2 },
      providers: [provideHttpClient()],
    });

    await userEvent.click(screen.getByRole("button", { name: /좋아요/ }));

    await vi.waitFor(() =>
      expect(screen.getByRole("button", { name: /좋아요 3/ })).toBeInTheDocument(),
    );
  });

  it("이미 좋아요한 경우 안내하고 숫자를 되돌린다", async () => {
    server.use(
      http.post("/api/posts/:id/likes", () =>
        HttpResponse.json(
          { status: 409, code: "ALREADY_LIKED", detail: "이미 좋아요한 게시글입니다." },
          { status: 409 },
        ),
      ),
    );

    await render(LikeButtonComponent, {
      inputs: { postId: 1, likeCount: 2 },
      providers: [provideHttpClient()],
    });

    await userEvent.click(screen.getByRole("button", { name: /좋아요/ }));

    expect(await screen.findByText("이미 좋아요한 게시글입니다.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /좋아요 2/ })).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
cd frontend-angular && pnpm test
```

기대: `./like-button` 모듈을 찾을 수 없어 실패

- [ ] **Step 3: 스토어에 좋아요를 추가한다**

`post.store.ts` 끝에 추가한다. 실패해도 서버 값으로 맞춰야 하므로 `finally`에서 다시 불러온다.

```ts
  async like(id: number): Promise<void> {
    try {
      await firstValueFrom(this.api.like(id));
    } finally {
      this.detail.reload();
    }
  }

  async unlike(id: number): Promise<void> {
    try {
      await firstValueFrom(this.api.unlike(id));
    } finally {
      this.detail.reload();
    }
  }
```

- [ ] **Step 4: 좋아요 버튼을 만든다**

`frontend-angular/src/app/features/post/components/like-button.ts`

```ts
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from "@angular/core";
import { toProblem } from "@/core/api/problem";
import { ButtonComponent } from "@/shared/ui/button";
import { PostStore } from "../post.store";

@Component({
  selector: "app-like-button",
  imports: [ButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="inline-flex items-center gap-2">
      <app-button variant="outline" size="sm" (click)="누른다()">
        좋아요 {{ 보이는수() }}
      </app-button>
      @if (message(); as text) {
        <span class="text-sm text-red-600">{{ text }}</span>
      }
    </span>
  `,
})
export class LikeButtonComponent {
  readonly postId = input.required<number>();
  readonly likeCount = input.required<number>();

  private readonly store = inject(PostStore);
  private readonly 낙관적증가 = signal(0);

  protected readonly message = signal<string | null>(null);
  protected readonly 보이는수 = computed(() => this.likeCount() + this.낙관적증가());

  protected 누른다(): void {
    this.message.set(null);
    // 서버 응답 전에 먼저 보여 주고, 응답이 오면 상세가 다시 불려와 실제 값으로 맞춰진다.
    this.낙관적증가.set(1);
    void this.store
      .like(this.postId())
      .catch((error: unknown) => {
        this.message.set(toProblem(error)?.detail ?? "좋아요에 실패했습니다.");
      })
      .finally(() => this.낙관적증가.set(0));
  }
}
```

- [ ] **Step 5: 상세 화면에 붙인다**

`post-detail-page.ts`의 템플릿에서 `<app-post-detail>` 안에 `like` 슬롯으로 넣는다.

```html
      <app-post-detail [post]="store.detail.value()" [canEdit]="canEdit()" (remove)="remove()">
        @if (memberStore.member()) {
          <app-like-button
            like
            [postId]="+postId()"
            [likeCount]="store.detail.value()?.likeCount ?? 0"
          />
        } @else {
          <span like>좋아요 {{ store.detail.value()?.likeCount ?? 0 }}</span>
        }
      </app-post-detail>
```

`memberStore`를 `protected`로 노출하고 `imports`에 `LikeButtonComponent`를 추가한다.

- [ ] **Step 6: 검증하고 커밋한다**

```bash
cd frontend-angular && pnpm lint:fix && pnpm verify
cd .. && git add frontend-angular
git commit -m "feat: 좋아요 추가

클릭 즉시 숫자를 올려 보여 주고, 상세를 다시 불러와 서버 값으로
맞춘다. 실패해도 맞춰야 하므로 재조회는 finally에서 한다.

백엔드가 중복과 미존재를 409로 구분해 주므로 안내 문구를 그대로
쓴다."
```

---
## Task 9: E2E 시나리오 이관

기능 동등성을 증명하는 단계다. React 버전과 **같은 시나리오**가 통과해야 한다.

**Files:**
- Create: `frontend-angular/playwright.config.ts`
- Create: `frontend-angular/e2e/auth.setup.ts` (이관)
- Create: `frontend-angular/e2e/board.spec.ts` (이관)
- Create: `frontend-angular/e2e/logout.spec.ts` (이관)
- Modify: `frontend-angular/.gitignore`

**Interfaces:**
- Consumes: 실행 중인 백엔드와 컨테이너, `tester`/`tester` 계정
- Produces: `pnpm e2e` — React 버전과 동일한 4개 시나리오

- [ ] **Step 1: 시나리오를 그대로 복사한다**

```bash
cd '/home/hseongh/문서/dev/bbs'
mkdir -p frontend-angular/e2e
cp frontend/e2e/auth.setup.ts frontend-angular/e2e/auth.setup.ts
cp frontend/e2e/board.spec.ts frontend-angular/e2e/board.spec.ts
cp frontend/e2e/logout.spec.ts frontend-angular/e2e/logout.spec.ts
cp frontend/playwright.config.ts frontend-angular/playwright.config.ts
printf 'e2e/.auth\nplaywright-report\ntest-results\n' >> frontend-angular/.gitignore
```

시나리오는 역할과 라벨로 요소를 찾으므로 구현 프레임워크와 무관하다. 그대로 통과하는 것이 목표이며, 통과하지 않으면 **테스트가 아니라 화면을 고친다.**

- [ ] **Step 2: 브라우저를 설치한다**

```bash
cd frontend-angular && pnpm add -D @playwright/test && pnpm exec playwright install chromium
```

- [ ] **Step 3: 실행한다**

백엔드와 컨테이너가 떠 있어야 하고, 다른 개발 서버가 5173을 쓰고 있으면 종료한다.

```bash
cd '/home/hseongh/문서/dev/bbs'
docker compose up -d
BBS_HOST=localhost ./gradlew bootRun &
sleep 60
cd frontend-angular && pnpm e2e
```

기대: 4개 시나리오 통과 (setup 1건, board 2건, logout 1건).

- [ ] **Step 4: 실패를 화면 문제로 다룬다**

셀렉터가 맞지 않아 실패하면 원인을 두 가지로 나눈다.

- **접근성 이름이 다르다** — Angular 템플릿에서 `aria-label`이나 라벨 연결이 빠진 것이다. 화면을 고친다
- **동작이 없다** — 기능이 누락된 것이다. 해당 태스크로 돌아가 채운다

시나리오 파일 자체는 고치지 않는다. 고쳐야만 통과한다면 그건 기능이 달라졌다는 신호다.

- [ ] **Step 5: 커밋한다**

```bash
cd '/home/hseongh/문서/dev/bbs' && git add frontend-angular
git commit -m "test: E2E 시나리오 이관

React 버전과 같은 시나리오를 그대로 옮긴다. 역할과 라벨로 요소를
찾으므로 구현 프레임워크와 무관하며, 동일 시나리오 통과가 기능
동등성을 증명한다."
```

---

## Task 10: React 프론트엔드 제거

**Files:**
- Delete: `frontend/` 전체
- Move: `frontend-angular/` → `frontend/`
- Modify: `.gitignore` (저장소 루트)
- Modify: `hooks/pre-commit`

**Interfaces:**
- Consumes: Task 1~9의 결과
- Produces: `frontend/`에 Angular 구현만 남는다

- [ ] **Step 1: Angular 쪽이 통과하는지 먼저 확인한다**

```bash
cd '/home/hseongh/문서/dev/bbs/frontend-angular' && pnpm verify && pnpm e2e
```

기대: 모두 통과. 실패하면 제거하지 않고 원인을 먼저 해결한다.

- [ ] **Step 2: React를 지우고 Angular를 그 자리로 옮긴다**

```bash
cd '/home/hseongh/문서/dev/bbs'
git rm -r --quiet frontend
mv frontend-angular frontend
```

`node_modules`는 git 대상이 아니므로 `mv`로 함께 이동한다. 이동 후 경로가 바뀌었으므로 의존성을 다시 설치한다.

```bash
cd frontend && pnpm install
```

- [ ] **Step 3: 무시 파일을 정리한다**

저장소 루트 `.gitignore`에서 `frontend-angular/*` 항목을 지우고 `frontend/*` 항목만 남긴다.

```
### Frontend ###
frontend/node_modules
frontend/dist
frontend/.angular
```

- [ ] **Step 4: pre-commit 훅을 확인한다**

`hooks/pre-commit`은 `frontend/` 경로를 검사하므로 그대로 동작한다. 실행되는 명령이 `pnpm lint`와 `pnpm typecheck`인지 확인한다.

```bash
grep -A4 "frontend/" hooks/pre-commit
./gradlew installGitHooks
```

- [ ] **Step 5: 전체가 통과하는지 확인한다**

```bash
cd '/home/hseongh/문서/dev/bbs'
./gradlew build
cd frontend && pnpm verify && pnpm e2e
```

기대: 백엔드 빌드 통과, 프론트 검증 통과, E2E 4건 통과.

- [ ] **Step 6: 커밋한다**

```bash
cd '/home/hseongh/문서/dev/bbs' && git add -A
git commit -m "chore: React 프론트엔드 제거

Angular 구현이 같은 E2E 시나리오를 통과하는 것을 확인한 뒤
frontend를 Angular 구현으로 교체한다.

두 구현을 함께 두면 백엔드가 바뀔 때마다 양쪽을 고쳐야 한다.
React 구현은 git 이력에 남아 있어 언제든 비교할 수 있다."
```

---

## Task 11: 문서 갱신

**Files:**
- Modify: `README.md`
- Modify: `frontend/README.md`
- Modify: `docs/superpowers/plans/2026-09-21-angular-migration.md`

**Interfaces:**
- Consumes: Task 1~10의 결과
- Produces: 없음

- [ ] **Step 1: 루트 README의 프론트엔드 설명을 고친다**

"이 저장소에서 볼 수 있는 것"의 항목을 Angular 기준으로 바꾼다.

```markdown
- **API 계약이 프론트엔드 컴파일로 강제된다.** OpenAPI 스키마에서 타입을 생성하므로 백엔드가 바뀌면 프론트엔드 타입 검사가 깨진다. 테스트의 API 목도 같은 타입을 쓴다.
- **앞뒤가 같은 방식으로 읽힌다.** 백엔드는 헥사고날, 프론트엔드는 Angular의 의존성 주입과 서비스 계층으로 같은 종류의 경계를 만든다.
```

"### 프론트엔드" 절의 명령을 확인한다. `pnpm install`과 `pnpm dev`는 그대로이며 포트도 5173으로 같다.

"구조" 절의 프론트엔드 트리를 바꾼다.

```
frontend/src/app/
├── core/         API 타입·클라이언트, 인증 인터셉터
├── features/     post, comment, member — api.service·store·components
├── shared/ui/    공용 컴포넌트
└── app.routes.ts
```

"빌드와 검증" 절에서 Biome을 ESLint로 고친다.

```markdown
프론트엔드는 `cd frontend && pnpm verify`가 ESLint, 타입 검사, 테스트를 순서대로 실행한다.
```

"문서" 절에 전환 문서를 더한다.

```markdown
- [Angular 전환 설계 문서](docs/superpowers/specs/2026-09-21-angular-migration-design.md) · [구현 계획](docs/superpowers/plans/2026-09-21-angular-migration.md)
```

- [ ] **Step 2: 프론트엔드 README를 다시 쓴다**

`frontend/README.md`

```markdown
# 게시판 프론트엔드

Angular 22 기반 SPA. 백엔드 API는 저장소 루트의 Spring 애플리케이션이다.

## 명령

| 명령 | 용도 |
|---|---|
| `pnpm dev` | 개발 서버 (5173). 백엔드로 프록시한다 |
| `pnpm verify` | 린트 → 타입 검사 → 테스트 |
| `pnpm gen:api` | 실행 중인 백엔드에서 API 타입 재생성 |
| `pnpm e2e` | Playwright E2E (백엔드와 컨테이너 필요). `BBS_HOST`를 따른다 |
| `pnpm build` | 프로덕션 빌드 |

## 원칙

- `src/app/core/api/schema.d.ts`는 생성물이다. 직접 수정하지 않는다
- 재조회(`reload()`)는 `*.store.ts`에서만 호출한다. 컴포넌트가 직접 부르면 무효화 범위가 흩어진다
- 검색어와 페이지는 URL에 둔다. 컴포넌트 상태로 복제하지 않는다
- 401 처리는 `core/auth/auth.interceptor.ts` 한 곳에만 있다. 401이 정상 응답인 요청은 `SKIP_LOGIN_REDIRECT` 토큰을 단다
- CSRF는 `withXsrfConfiguration`이 처리한다. 헤더를 직접 붙이지 않는다
- MSW 핸들러도 생성된 타입을 쓴다. 목이 계약과 어긋나면 컴파일이 깨진다

## 알아둘 점

개발 서버는 5173을 쓴다. Angular CLI 기본값은 4200이지만 Keycloak realm에 등록된 주소가 5173이라 바꾸면 로그인이 깨진다.

TypeScript는 6.x를 쓴다. 7은 컴파일러 API를 다시 구현해 `openapi-typescript`가 동작하지 않는다.
```

- [ ] **Step 3: 계획 문서에 결과를 기록한다**

이 계획 문서 맨 앞에 완료 표와, 계획 대비 달라진 결정을 적는다. 특히 Task 6에서 Signal Forms와 Reactive Forms 중 무엇을 택했는지와 그 이유를 남긴다.

- [ ] **Step 4: 최종 검증을 수행한다**

```bash
cd '/home/hseongh/문서/dev/bbs'
./gradlew clean build
cd frontend && pnpm verify && pnpm e2e
cd .. && git status --short
```

기대: 모두 통과, 커밋되지 않은 변경 없음.

- [ ] **Step 5: 커밋한다**

```bash
git add README.md frontend/README.md docs
git commit -m "docs: Angular 전환 결과 문서화

실행 방법과 유지해야 할 원칙을 Angular 기준으로 고치고, 계획
대비 달라진 결정을 기록한다."
```

---

## 완료 기준

- `cd frontend && pnpm verify`가 통과한다
- `pnpm e2e`가 통과하며, 시나리오는 React 버전과 동일하다
- `./gradlew build`가 통과한다 (백엔드 무변경 확인)
- 브라우저에서 로그인, 글 작성, 검색, 댓글, 대댓글, 좋아요, 로그아웃이 모두 동작한다
- 저장소에 React 코드가 남아 있지 않고 Angular가 `frontend/`에 자리한다
- `docker/keycloak/bbs-realm.json`과 루트 README의 접속 주소를 바꾸지 않았다
