# 게시판 프론트엔드 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 기존 게시판 REST API 위에 React 화면을 올리고, 백엔드 계약이 바뀌면 프론트엔드 컴파일이 깨지도록 만든다.

**Architecture:** 같은 저장소의 `frontend/`에 Vite + React SPA를 둔다. 개발 중에는 Vite 프록시로 백엔드와 동일 오리진을 만들어 세션 쿠키를 그대로 쓴다. OpenAPI 스키마에서 타입을 생성해 API 계층이 계약을 강제하고, 서버 상태는 TanStack Query가, 검색·페이지 상태는 URL이 소유한다.

**Tech Stack:** Node 24.18, pnpm 11.25, React 19.3, Vite 8.3, TanStack Router 1.170, TanStack Query 5.103, openapi-typescript 7.13, openapi-fetch 0.17, Tailwind CSS 4.3, shadcn/ui, Biome 2.5, TypeScript 7.0, Vitest 5.0, Testing Library 16.3, MSW 2.15, Playwright 1.63

설계 문서: `docs/superpowers/specs/2026-09-21-bbs-frontend-design.md`

## 진행 상황 (2026-09-21 완료)

| 태스크 | 커밋 |
|---|---|
| 1. OpenAPI 문서에서 내부 파라미터 제거 | `4650ac0` |
| 2. SPA 딥링크 포워딩 | `d5c4e9d` |
| 3. 프론트엔드 프로젝트 및 품질 도구 구성 | `d0d24bd` |
| 4. API 타입 생성과 클라이언트 계층 | `4188602` |
| 5. 레이아웃과 로그인 상태 표시 | `e57e030` |
| 6. 게시글 목록 화면 | `653e96a` |
| 7. 게시글 상세 화면 | `306a841` |
| 8. 게시글 작성 및 수정 폼 | `f54fe64` |
| 9. 댓글 및 대댓글 | `17eb902` |
| 10. 좋아요 | `3d25c8f` |
| (추가) Keycloak 리다이렉트 URI 등록 | `f9ce589` |
| 11. E2E 시나리오 | `d4b6e4f` |
| 12. 문서화 | 완료 |

**전체 완료.** 계획 대비 달라진 점은 아래와 같다.

- TypeScript는 7이 아니라 6.0.3을 쓴다. `openapi-typescript`가 TS 7의 컴파일러 API와 호환되지 않는다
- `anyRequest().authenticated()`가 화면 리소스까지 덮고 있어 보안 규칙을 함께 조정했다
- springdoc이 JSpecify를 모르는 탓에 생성 타입의 모든 필드가 선택 값이어서, nullability를 required로 옮기는 커스터마이저를 추가했다
- Vite 프록시는 Host를 유지하므로 Keycloak realm에 개발 서버 주소를 등록해야 한다

---

## Global Constraints

- 프론트엔드는 저장소 안 `frontend/`에 둔다. 별도 저장소를 만들지 않는다
- **백엔드에 CORS 설정을 추가하지 않는다.** 개발 중 동일 오리진은 Vite 프록시가 만든다
- `frontend/src/api/schema.d.ts`는 생성물이다. 커밋하되 직접 수정하지 않으며, Biome 검사에서 제외한다
- 타입 생성은 `pnpm gen:api`로 수동 실행한다. 빌드가 실행 중인 백엔드에 의존하게 하지 않는다
- TypeScript는 `strict`와 `noUncheckedIndexedAccess`를 켠다
- 클라이언트 전역 상태 라이브러리(Redux, Zustand 등)를 도입하지 않는다
- 검색어·페이지·정렬은 URL 검색 파라미터에 둔다. 컴포넌트 상태로 복제하지 않는다
- `queryKey`는 `features/*/queries.ts`에서만 만든다. 컴포넌트가 직접 조립하지 않는다
- MSW 핸들러도 생성된 타입을 사용한다
- 입력·텍스트영역과 주요 동작 버튼은 `components/ui`의 shadcn 컴포넌트(`Input`, `Textarea`, `Button`)를 쓴다. 목록 안의 작은 텍스트형 조작(답글 달기, 수정, 삭제)은 본문 흐름에 묻히는 편이 낫므로 일반 `button` 요소로 둔다
- 주석은 로직을 설명할 때만 쓴다. 설정 파일과 기록용 주석은 남기지 않는다
- 커밋 메시지는 AngularJS 컨벤션(`type: subject`)이며 본문은 한국어로 쓴다
- 프론트엔드 변경은 `pnpm verify`가, 백엔드 변경은 `./gradlew build`가 통과한 상태로 커밋한다
- 계획 본문의 버전은 2026-09-21 기준이다. 설치 후 실제 해결 버전을 확인하고, 호환 문제가 있으면 그 자리에서 교정한 뒤 진행한다

---

## File Structure

### 백엔드 수정

| 파일 | 책임 |
|---|---|
| `src/main/java/com/board/bbs/common/config/OpenApiConfig.java` | 내부 인자를 문서에서 제외 |
| `src/main/java/com/board/bbs/post/adapter/in/web/PostController.java` | `Pageable`에 `@ParameterObject` |
| `src/main/java/com/board/bbs/comment/adapter/in/web/CommentController.java` | `Pageable`에 `@ParameterObject` |
| `src/main/java/com/board/bbs/common/config/SpaForwardingConfig.java` | SPA 딥링크를 `index.html`로 포워딩 |
| `src/test/java/com/board/bbs/common/config/OpenApiDocumentTest.java` | 문서에 내부 인자가 없음을 검증 |
| `src/test/java/com/board/bbs/common/config/SpaForwardingTest.java` | API 404가 보존됨을 검증 |

### 프론트엔드

| 파일 | 책임 |
|---|---|
| `frontend/package.json` | 스크립트, 의존성, Node·pnpm 고정 |
| `frontend/vite.config.ts` | 프록시, 라우터 플러그인, Tailwind, 테스트 설정 |
| `frontend/tsconfig.json` | strict, noUncheckedIndexedAccess |
| `frontend/biome.json` | 린트·포맷 규칙, 생성물 제외 |
| `frontend/src/api/schema.d.ts` | OpenAPI 생성 타입 |
| `frontend/src/api/client.ts` | openapi-fetch 인스턴스, 401 미들웨어 |
| `frontend/src/api/problem.ts` | ProblemDetail 파싱과 판별 |
| `frontend/src/features/member/queries.ts` | 현재 회원 조회 |
| `frontend/src/features/post/queries.ts` | 게시글 queryKey와 조회 훅 |
| `frontend/src/features/post/mutations.ts` | 게시글 변경 훅과 무효화 |
| `frontend/src/features/comment/queries.ts` | 댓글 queryKey와 조회 훅 |
| `frontend/src/features/comment/mutations.ts` | 댓글 변경 훅과 무효화 |
| `frontend/src/routes/*` | 라우트 정의 |
| `frontend/src/components/ui/` | shadcn Button, Input, Textarea |
| `frontend/src/lib/utils.ts` | Tailwind 클래스 병합 (`cn`) |
| `frontend/src/components/layout/Header.tsx` | 로그인 상태 표시 |
| `frontend/src/test/setup.ts` | Testing Library, MSW 서버 기동 |
| `frontend/src/test/handlers.ts` | MSW 기본 핸들러 |
| `frontend/e2e/*.spec.ts` | Playwright 시나리오 |

---
## Task 1: OpenAPI 문서에서 내부 파라미터 제거

`@CurrentMember`로 주입되는 인자가 필수 쿼리 파라미터로 문서화되어, 잘못된 사용법을 안내하고 생성될 타입을 오염시킨다.

**Files:**
- Modify: `src/main/java/com/board/bbs/common/config/OpenApiConfig.java`
- Modify: `src/main/java/com/board/bbs/post/adapter/in/web/PostController.java`
- Modify: `src/main/java/com/board/bbs/comment/adapter/in/web/CommentController.java`
- Test: `src/test/java/com/board/bbs/common/config/OpenApiDocumentTest.java`

**Interfaces:**
- Consumes: `com.board.bbs.common.security.CurrentMember` 애너테이션
- Produces: `/v3/api-docs`에 `requester`, `author`, `memberId`, `viewer` 파라미터가 없고, `Pageable`이 `page`·`size`·`sort`로 평탄화된 문서

- [ ] **Step 1: 현재 상태를 확인한다**

```bash
curl -s http://localhost:8080/v3/api-docs | python3 -c "
import json,sys
d=json.load(sys.stdin)
for path, ops in sorted(d['paths'].items()):
    for method, op in ops.items():
        names = [p['name'] for p in op.get('parameters', [])]
        print(f'{method.upper():6} {path:32} {names}')
"
```

기대: `requester`, `author`, `memberId`, `viewer`, `pageable`이 보인다. 백엔드가 떠 있지 않으면 `docker compose up -d && ./gradlew bootRun`으로 먼저 띄운다.

- [ ] **Step 2: 실패하는 테스트를 쓴다**

`src/test/java/com/board/bbs/common/config/OpenApiDocumentTest.java`

```java
package com.board.bbs.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.board.bbs.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@AutoConfigureMockMvc
class OpenApiDocumentTest extends IntegrationTestBase {

  private static final List<String> 내부_인자 = List.of("requester", "author", "memberId", "viewer");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private List<String> 모든_파라미터_이름() throws Exception {
    String body =
        mockMvc
            .perform(MockMvcRequestBuilders.get("/v3/api-docs"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode paths = objectMapper.readTree(body).get("paths");
    List<String> names = new ArrayList<>();
    paths.forEach(
        path ->
            path.forEach(
                operation -> {
                  JsonNode parameters = operation.get("parameters");
                  if (parameters != null) {
                    parameters.forEach(parameter -> names.add(parameter.get("name").asText()));
                  }
                }));
    return names;
  }

  @Test
  void 서버가_채우는_인자는_문서에_노출되지_않는다() throws Exception {
    assertThat(모든_파라미터_이름()).doesNotContainAnyElementsOf(내부_인자);
  }

  @Test
  void 페이지_정보는_개별_파라미터로_평탄화된다() throws Exception {
    List<String> names = 모든_파라미터_이름();

    assertThat(names).doesNotContain("pageable");
    assertThat(names).contains("page", "size");
  }
}
```

- [ ] **Step 3: 테스트가 실패하는 것을 확인한다**

```bash
./gradlew test --tests '*OpenApiDocumentTest*'
```

기대: 두 테스트 모두 FAIL. `requester` 등이 검출되고 `pageable`이 남아 있다.

- [ ] **Step 4: 내부 인자를 문서에서 제외한다**

`OpenApiConfig.java`의 클래스 본문 맨 위에 정적 초기화 블록을 추가하고 import를 넣는다.

```java
import com.board.bbs.common.security.CurrentMember;
import org.springdoc.core.utils.SpringDocUtils;
```

```java
public class OpenApiConfig {

  static {
    SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentMember.class);
  }
```

컨트롤러마다 `@Parameter(hidden = true)`를 붙이는 대신 이 방식을 쓴다. 새 엔드포인트에서 빠뜨릴 여지가 없다.

- [ ] **Step 5: Pageable을 평탄화한다**

`PostController.java`와 `CommentController.java` 양쪽에서 `Pageable` 파라미터 앞에 `@ParameterObject`를 붙이고 import를 추가한다.

```java
import org.springdoc.core.annotations.ParameterObject;
```

`PostController.search`:

```java
      @RequestParam(required = false) @Nullable Long authorId,
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
```

`CommentController.list`:

```java
      @PathVariable Long postId, @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
```

- [ ] **Step 6: 테스트가 통과하는 것을 확인한다**

```bash
./gradlew spotlessApply test --tests '*OpenApiDocumentTest*'
```

기대: PASS

- [ ] **Step 7: 전체 빌드를 확인하고 커밋한다**

```bash
./gradlew build
git add src
git commit -m "fix: OpenAPI 문서에서 서버가 채우는 인자 제거

@CurrentMember로 주입되는 인자가 필수 쿼리 파라미터로 문서화되어
있었다. Swagger UI가 잘못된 사용법을 안내했고, 작성자를 쿼리로
지정할 수 있다는 오해를 주었다.

springdoc이 해당 애너테이션을 무시하도록 설정해 새 엔드포인트에서
빠뜨릴 여지를 없앤다. Pageable은 @ParameterObject로 평탄화하여
문서가 실제 호출 방식과 일치하게 한다."
```

---

## Task 2: SPA 딥링크 포워딩

`/posts/3` 같은 클라이언트 라우트로 직접 접근하면 404가 반환된다. 정적 리소스가 존재할 때 이를 `index.html`로 포워딩한다.

**Files:**
- Create: `src/main/java/com/board/bbs/common/config/SpaForwardingConfig.java`
- Test: `src/test/java/com/board/bbs/common/config/SpaForwardingTest.java`

**Interfaces:**
- Consumes: 없음
- Produces: 정적 리소스에 `index.html`이 있으면 API가 아닌 경로가 그것으로 응답한다. API 경로의 404는 그대로 유지된다

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`src/test/java/com/board/bbs/common/config/SpaForwardingTest.java`

```java
package com.board.bbs.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.bbs.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SpaForwardingTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Test
  void 존재하지_않는_API_경로는_여전히_404를_반환한다() throws Exception {
    mockMvc
        .perform(get("/api/does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void 액추에이터_경로는_포워딩되지_않는다() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void 정적_리소스가_없으면_클라이언트_경로도_404다() throws Exception {
    mockMvc.perform(get("/posts/1")).andExpect(status().isNotFound());
  }
}
```

세 번째 테스트는 현재 동작을 고정한다. 프론트엔드 빌드 산출물이 없는 상태에서는 404가 정상이며, `index.html`이 생기면 그때 200이 된다. 이 계획에서는 산출물을 저장소에 넣지 않으므로 404가 유지된다.

- [ ] **Step 2: 테스트를 실행한다**

```bash
./gradlew test --tests '*SpaForwardingTest*'
```

기대: 세 테스트 모두 PASS. 아직 구현이 없어도 현재 동작이 그러하다. 이 테스트는 다음 단계에서 **깨뜨리지 않는 것**을 보장하는 안전망이다.

- [ ] **Step 3: 포워딩 설정을 만든다**

`src/main/java/com/board/bbs/common/config/SpaForwardingConfig.java`

```java
package com.board.bbs.common.config;

import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/** 클라이언트 라우팅을 쓰는 화면이 새로고침이나 직접 접근에도 동작하도록 index.html로 넘긴다. */
@Configuration
public class SpaForwardingConfig implements WebMvcConfigurer {

  private static final List<String> 서버가_처리하는_접두사 =
      List.of("api/", "actuator/", "swagger-ui", "v3/api-docs", "oauth2/", "login", "logout");

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/static/")
        .resourceChain(true)
        .addResolver(new SpaResourceResolver());
  }

  private static final class SpaResourceResolver extends PathResourceResolver {

    @Override
    protected @Nullable Resource getResource(String resourcePath, Resource location)
        throws IOException {

      Resource requested = location.createRelative(resourcePath);
      if (requested.exists() && requested.isReadable()) {
        return requested;
      }
      if (서버가_처리하는_접두사.stream().anyMatch(resourcePath::startsWith)) {
        return null;
      }
      Resource index = location.createRelative("index.html");
      return index.exists() && index.isReadable() ? index : null;
    }
  }
}
```

파일이 실제로 있으면 그대로 주고, 서버가 처리하는 경로는 건드리지 않으며, 나머지는 `index.html`이 있을 때만 그것을 준다. `index.html`이 없으면 기존처럼 404가 난다.

- [ ] **Step 4: 기존 동작이 깨지지 않았는지 확인한다**

```bash
./gradlew test --tests '*SpaForwardingTest*' --tests '*PostControllerTest*' --tests '*CommentControllerTest*'
```

기대: 모두 PASS. 특히 API 404가 JSON ProblemDetail로 유지되어야 한다.

- [ ] **Step 5: 전체 빌드를 확인하고 커밋한다**

```bash
./gradlew spotlessApply build
git add src
git commit -m "feat: SPA 딥링크 포워딩 추가

클라이언트 라우팅 경로로 직접 접근하거나 새로고침해도 화면이
뜨도록 정적 리소스의 index.html로 넘긴다.

API와 액추에이터, 인증 경로는 서버가 처리하므로 포워딩 대상에서
제외한다. 존재하지 않는 API 경로가 화면을 반환하면 클라이언트가
오류를 감지할 수 없다."
```

---
## Task 3: 프론트엔드 프로젝트 및 품질 도구 구성

이후 모든 태스크가 올라설 기반을 만든다. 이 태스크가 끝나면 `pnpm verify`가 동작한다.

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/tsconfig.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/biome.json`
- Create: `frontend/index.html`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/styles.css`
- Create: `frontend/src/test/setup.ts`
- Create: `frontend/src/lib/cn.ts`
- Create: `frontend/.gitignore`
- Modify: `.gitignore` (저장소 루트)
- Modify: `hooks/pre-commit`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `pnpm verify` — Biome 검사 → `tsc --noEmit` → Vitest를 순서대로 실행
  - `pnpm dev` — Vite 개발 서버(5173), `/api`·`/oauth2`·`/login`·`/logout`을 8080으로 프록시
  - `pnpm gen:api` — `/v3/api-docs`에서 `src/api/schema.d.ts` 생성
  - `cn(...classes)` — Tailwind 클래스 병합 유틸리티 (`@/lib/utils`)
  - `Button`, `Input`, `Textarea` — `@/components/ui/*`의 폼 기본 컴포넌트

- [ ] **Step 1: 디렉터리와 package.json을 만든다**

```bash
mkdir -p frontend/src/{api,features,routes,components/{ui,layout},lib,test} frontend/e2e
```

`frontend/package.json`

```json
{
  "name": "bbs-frontend",
  "private": true,
  "type": "module",
  "engines": {
    "node": ">=24.18.0"
  },
  "packageManager": "pnpm@11.25.0",
  "scripts": {
    "dev": "vite",
    "build": "tsc --noEmit && vite build",
    "preview": "vite preview",
    "gen:api": "openapi-typescript http://localhost:8080/v3/api-docs -o src/api/schema.d.ts",
    "lint": "biome check .",
    "lint:fix": "biome check --write .",
    "typecheck": "tsc --noEmit",
    "test": "vitest run",
    "test:watch": "vitest",
    "e2e": "playwright test",
    "verify": "pnpm lint && pnpm typecheck && pnpm test"
  },
  "dependencies": {
    "@tanstack/react-query": "^5.103.1",
    "@tanstack/react-router": "^1.170.38",
    "openapi-fetch": "^0.17.0",
    "react": "^19.3.0",
    "react-dom": "^19.3.0"
  },
  "devDependencies": {
    "@biomejs/biome": "^2.5.14",
    "@playwright/test": "^1.63.0",
    "@tailwindcss/vite": "^4.3.3",
    "@tanstack/react-query-devtools": "^5.103.1",
    "@tanstack/router-plugin": "^1.168.40",
    "@testing-library/jest-dom": "^7.0.1",
    "@testing-library/react": "^16.3.3",
    "@testing-library/user-event": "^14.6.7",
    "@types/react": "^19.3.0",
    "@types/react-dom": "^19.3.0",
    "@vitejs/plugin-react": "^6.1.1",
    "jsdom": "^30.1.0",
    "msw": "^2.15.0",
    "openapi-typescript": "^7.13.0",
    "tailwindcss": "^4.3.3",
    "typescript": "^7.0.2",
    "vite": "^8.3.0",
    "vitest": "^5.0.1"
  }
}
```

- [ ] **Step 2: 의존성을 설치하고 실제 해결 버전을 확인한다**

```bash
cd frontend && pnpm install
pnpm list --depth=0
```

기대: 오류 없이 설치 완료. TypeScript가 7.x로 설치되지 않거나 다른 패키지와 충돌하면 `typescript`를 `^6` 계열로 내리고 다시 설치한 뒤 그대로 진행한다. 이 결정은 여기서 확정한다.

- [ ] **Step 3: TypeScript 설정을 만든다**

`frontend/tsconfig.json`

```json
{
  "compilerOptions": {
    "target": "ES2023",
    "lib": ["ES2023", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "verbatimModuleSyntax": true,
    "isolatedModules": true,
    "skipLibCheck": true,
    "noEmit": true,
    "types": ["vitest/globals", "@testing-library/jest-dom"],
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src", "e2e", "vite.config.ts"]
}
```

- [ ] **Step 4: Vite 설정을 만든다**

`frontend/vite.config.ts`

```ts
import { fileURLToPath, URL } from "node:url";
import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const backend = "http://localhost:8080";

export default defineConfig({
  plugins: [tanstackRouter({ target: "react", autoCodeSplitting: true }), react(), tailwindcss()],
  resolve: {
    alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
  },
  server: {
    port: 5173,
    proxy: {
      "/api": { target: backend, changeOrigin: false },
      "/oauth2": { target: backend, changeOrigin: false },
      "/login": { target: backend, changeOrigin: false },
      "/logout": { target: backend, changeOrigin: false },
      "/v3/api-docs": { target: backend, changeOrigin: false },
    },
  },
  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    exclude: ["node_modules", "dist", "e2e"],
  },
});
```

`changeOrigin: false`가 중요하다. Host 헤더를 바꾸면 Keycloak 리다이렉트 URI가 어긋난다.

- [ ] **Step 5: Biome 설정을 만든다**

`frontend/biome.json`

```json
{
  "$schema": "https://biomejs.dev/schemas/2.5.14/schema.json",
  "files": {
    "includes": ["**", "!src/api/schema.d.ts", "!dist/**", "!node_modules/**"]
  },
  "formatter": {
    "enabled": true,
    "indentStyle": "space",
    "indentWidth": 2,
    "lineWidth": 100
  },
  "linter": {
    "enabled": true,
    "rules": {
      "recommended": true,
      "correctness": {
        "useExhaustiveDependencies": "error"
      },
      "suspicious": {
        "noExplicitAny": "error"
      }
    }
  },
  "assist": {
    "actions": {
      "source": {
        "organizeImports": "on"
      }
    }
  }
}
```

- [ ] **Step 6: 진입점과 스타일을 만든다**

`frontend/index.html`

```html
<!doctype html>
<html lang="ko">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>게시판</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

`frontend/src/styles.css`

```css
@import "tailwindcss";
```

`frontend/src/main.tsx` — 라우터는 Task 5에서 붙인다. 지금은 기동만 확인한다.

```tsx
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const container = document.getElementById("root");
if (!container) {
  throw new Error("root 엘리먼트를 찾을 수 없습니다.");
}

createRoot(container).render(
  <StrictMode>
    <p className="p-4">준비 중</p>
  </StrictMode>,
);
```

- [ ] **Step 6-1: shadcn/ui를 초기화하고 기본 컴포넌트를 받는다**

```bash
cd frontend && pnpm dlx shadcn@latest init
```

프롬프트에서 스타일은 기본값, 기본 색상은 Slate, CSS 파일은 `src/styles.css`, 경로 별칭은 `@/*`를 선택한다. `components.json`과 `src/lib/utils.ts`가 생성된다.

```bash
pnpm dlx shadcn@latest add button input textarea
```

`src/components/ui/button.tsx`, `input.tsx`, `textarea.tsx`가 생성된다. 이 파일들은 복사본이므로 저장소에 커밋한다.

생성된 `src/lib/utils.ts`의 `cn`이 `clsx`와 `tailwind-merge`를 쓰므로, 앞서 만든 `src/lib/cn.ts`를 지우고 이후 코드는 `@/lib/utils`의 `cn`을 쓴다.

```bash
rm src/lib/cn.ts src/lib/cn.test.ts
```

CLI가 Tailwind v4를 인식하지 못하거나 초기화에 실패하면, shadcn 없이 진행하고 `Button`·`Input`·`Textarea`를 `src/components/ui/`에 직접 작성한다. 각각 `className`을 받아 기본 스타일과 병합하는 얇은 래퍼면 충분하다. 이 판단은 여기서 확정한다.

- [ ] **Step 7: 테스트 설정과 유틸리티를 만든다**

`frontend/src/test/setup.ts`

```ts
import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

afterEach(() => {
  cleanup();
});
```

`src/lib/utils.ts`의 `cn`은 Step 6-1에서 shadcn CLI가 생성했다. 별도로 만들지 않는다.

- [ ] **Step 8: 설정이 동작하는지 확인할 테스트를 쓴다**

`frontend/src/lib/utils.test.ts`

```ts
import { describe, expect, it } from "vitest";
import { cn } from "./utils";

describe("cn", () => {
  it("여러 클래스를 잇는다", () => {
    expect(cn("px-2", "py-1")).toBe("px-2 py-1");
  });

  it("거짓 값을 걸러낸다", () => {
    expect(cn("px-2", false, null, undefined)).toBe("px-2");
  });

  it("뒤에 온 Tailwind 클래스가 앞의 같은 속성을 덮는다", () => {
    expect(cn("px-2", "px-4")).toBe("px-4");
  });
});
```

세 번째 테스트가 `tailwind-merge`가 실제로 동작하는지 확인한다. 직접 작성한 래퍼로 대체했다면 이 테스트는 제거한다.

- [ ] **Step 9: 무시 파일을 만든다**

`frontend/.gitignore`

```
node_modules
dist
coverage
playwright-report
test-results
.env
```

저장소 루트 `.gitignore` 끝에 추가한다.

```
### Frontend ###
frontend/node_modules
frontend/dist
```

- [ ] **Step 10: 검증 명령이 통과하는지 확인한다**

```bash
cd frontend && pnpm lint:fix && pnpm verify
```

기대: Biome 통과, 타입 검사 통과, `cn` 테스트 2건 통과.

- [ ] **Step 11: 개발 서버가 뜨는지 확인한다**

```bash
cd frontend && pnpm dev &
sleep 5
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:5173
curl -s -o /dev/null -w "프록시 HTTP %{http_code}\n" http://localhost:5173/api/posts
```

기대: 첫 번째 200, 두 번째 200(백엔드가 떠 있을 때). 확인 후 개발 서버를 종료한다.

- [ ] **Step 12: pre-commit 훅에 프론트엔드 검사를 추가한다**

`hooks/pre-commit`을 아래로 교체한다.

```sh
#!/bin/sh

./gradlew spotlessCheck checkstyleMain -q

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ Code quality check failed."
    echo "Run './gradlew spotlessApply' to format the code."
    exit 1
fi

if git diff --cached --name-only | grep -q '^frontend/'; then
    (cd frontend && pnpm lint && pnpm typecheck)

    if [ $? -ne 0 ]; then
        echo ""
        echo "❌ Frontend check failed."
        echo "Run 'cd frontend && pnpm lint:fix' to format the code."
        exit 1
    fi
fi

exit 0
```

```bash
./gradlew installGitHooks
```

- [ ] **Step 13: 커밋한다**

```bash
git add frontend .gitignore hooks
git commit -m "chore: 프론트엔드 프로젝트 및 품질 도구 구성

Vite와 React 기반 프로젝트를 저장소 안에 두고, 개발 중에는 프록시로
백엔드와 동일 오리진을 만든다. 세션 쿠키 인증이므로 CORS를 추가하는
대신 이 방식을 쓴다.

pnpm verify가 린트, 타입 검사, 테스트를 순서대로 실행한다. 타입은
strict에 더해 noUncheckedIndexedAccess를 켜 배열 인덱싱이 undefined를
반환할 수 있음을 드러낸다.

pre-commit 훅은 frontend 아래 변경이 있을 때만 프론트엔드 검사를
실행한다."
```

---

## Task 4: API 타입 생성과 클라이언트 계층

백엔드 계약을 타입으로 가져오고, 모든 호출이 지나갈 단일 통로를 만든다.

**Files:**
- Create: `frontend/src/api/schema.d.ts` (생성물)
- Create: `frontend/src/api/problem.ts`
- Create: `frontend/src/api/client.ts`
- Create: `frontend/src/api/problem.test.ts`
- Create: `frontend/src/test/handlers.ts`

**Interfaces:**
- Consumes: Task 1의 정리된 OpenAPI 문서
- Produces:
  - `client` — openapi-fetch 인스턴스. `client.GET("/api/posts", { params: { query: {...} } })` 형태로 호출한다
  - `ProblemDetail` 타입 — `{ type, title, status, detail, instance, code, errors? }`
  - `toProblem(error: unknown): ProblemDetail | null`
  - `isProblemCode(error: unknown, code: string): boolean`
  - `fieldErrors(error: unknown): Record<string, string>`
  - `LOGIN_URL` — `"/oauth2/authorization/keycloak"`
  - `REDIRECT_KEY` — 로그인 전 경로를 담는 `sessionStorage` 키

- [ ] **Step 1: 백엔드를 띄우고 타입을 생성한다**

```bash
docker compose up -d
./gradlew bootRun &
sleep 40
cd frontend && pnpm gen:api
head -30 src/api/schema.d.ts
```

기대: `src/api/schema.d.ts`가 생성되고 `/api/posts` 등의 경로 타입이 보인다. `requester`, `author`, `memberId`, `viewer`가 없어야 한다.

```bash
grep -cE "requester|memberId|viewer" src/api/schema.d.ts
```

기대: `0`

- [ ] **Step 2: ProblemDetail 파싱 테스트를 쓴다**

`frontend/src/api/problem.test.ts`

```ts
import { describe, expect, it } from "vitest";
import { fieldErrors, isProblemCode, toProblem } from "./problem";

const 권한오류 = {
  type: "urn:bbs:error:access_denied",
  title: "Forbidden",
  status: 403,
  detail: "권한이 없습니다.",
  instance: "/api/posts/2",
  code: "ACCESS_DENIED",
};

const 검증오류 = {
  type: "urn:bbs:error:invalid_request",
  title: "Bad Request",
  status: 400,
  detail: "요청 값이 올바르지 않습니다.",
  instance: "/api/posts",
  code: "INVALID_REQUEST",
  errors: { title: "제목은 필수입니다." },
};

describe("toProblem", () => {
  it("ProblemDetail 형태를 인식한다", () => {
    expect(toProblem(권한오류)?.code).toBe("ACCESS_DENIED");
  });

  it("형태가 다르면 null을 반환한다", () => {
    expect(toProblem({ message: "그냥 오류" })).toBeNull();
    expect(toProblem(null)).toBeNull();
    expect(toProblem("문자열")).toBeNull();
  });
});

describe("isProblemCode", () => {
  it("코드가 일치하면 참이다", () => {
    expect(isProblemCode(권한오류, "ACCESS_DENIED")).toBe(true);
    expect(isProblemCode(권한오류, "POST_NOT_FOUND")).toBe(false);
  });
});

describe("fieldErrors", () => {
  it("검증 오류의 필드별 메시지를 꺼낸다", () => {
    expect(fieldErrors(검증오류)).toEqual({ title: "제목은 필수입니다." });
  });

  it("검증 오류가 아니면 빈 객체다", () => {
    expect(fieldErrors(권한오류)).toEqual({});
  });
});
```

- [ ] **Step 3: 테스트가 실패하는 것을 확인한다**

```bash
cd frontend && pnpm test
```

기대: `./problem` 모듈을 찾을 수 없어 실패

- [ ] **Step 4: ProblemDetail 파싱을 구현한다**

`frontend/src/api/problem.ts`

```ts
export type ProblemDetail = {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  code: string;
  errors?: Record<string, string>;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

export function toProblem(error: unknown): ProblemDetail | null {
  if (!isRecord(error)) {
    return null;
  }
  if (typeof error.code !== "string" || typeof error.status !== "number") {
    return null;
  }
  return error as unknown as ProblemDetail;
}

export function isProblemCode(error: unknown, code: string): boolean {
  return toProblem(error)?.code === code;
}

export function fieldErrors(error: unknown): Record<string, string> {
  const problem = toProblem(error);
  if (!problem?.errors) {
    return {};
  }
  return problem.errors;
}
```

- [ ] **Step 5: 테스트가 통과하는 것을 확인한다**

```bash
cd frontend && pnpm test
```

기대: PASS

- [ ] **Step 6: API 클라이언트를 만든다**

`frontend/src/api/client.ts`

```ts
import createClient, { type Middleware } from "openapi-fetch";
import type { paths } from "./schema";

export const LOGIN_URL = "/oauth2/authorization/keycloak";
export const REDIRECT_KEY = "bbs:redirectAfterLogin";

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

/** 401은 개별 호출부가 아니라 여기서만 다룬다. */
const authMiddleware: Middleware = {
  async onResponse({ response }) {
    if (response.status === 401) {
      startLogin();
    }
    return response;
  },
};

export const client = createClient<paths>({ baseUrl: "" });
client.use(authMiddleware);
```

`baseUrl`이 빈 문자열인 것은 의도적이다. 동일 오리진이므로 `/api/...` 상대 경로로 나간다.

- [ ] **Step 7: MSW 기본 핸들러를 만든다**

`frontend/src/test/handlers.ts` — 생성된 타입을 사용해 목이 계약과 어긋나면 컴파일이 깨지게 한다.

```ts
import { http, HttpResponse } from "msw";
import type { components } from "@/api/schema";

type MemberResponse = components["schemas"]["MemberResponse"];
type PostSummaryResponse = components["schemas"]["PostSummaryResponse"];
type PageResponsePostSummaryResponse =
  components["schemas"]["PageResponsePostSummaryResponse"];

export const 로그인회원: MemberResponse = {
  id: 1,
  nickname: "테스터",
  email: "tester@example.com",
};

export function 게시글요약(overrides: Partial<PostSummaryResponse> = {}): PostSummaryResponse {
  return {
    id: 1,
    title: "첫 글",
    authorId: 1,
    authorNickname: "테스터",
    viewCount: 0,
    likeCount: 0,
    createdAt: "2026-09-21T00:00:00Z",
    ...overrides,
  };
}

export function 게시글페이지(
  content: PostSummaryResponse[],
): PageResponsePostSummaryResponse {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: 1,
    last: true,
  };
}

export const handlers = [
  http.get("/api/members/me", () => HttpResponse.json(로그인회원)),
  http.get("/api/posts", () => HttpResponse.json(게시글페이지([게시글요약()]))),
];
```

생성된 스키마의 필드 이름이 다르면 여기서 컴파일 오류가 난다. 오류 메시지를 보고 실제 이름에 맞춘다.

- [ ] **Step 8: MSW 서버를 테스트 설정에 연결한다**

`frontend/src/test/setup.ts`를 아래로 교체한다.

```ts
import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll } from "vitest";
import { handlers } from "./handlers";

export const server = setupServer(...handlers);

beforeAll(() => {
  server.listen({ onUnhandledRequest: "error" });
});

afterEach(() => {
  cleanup();
  server.resetHandlers();
});

afterAll(() => {
  server.close();
});
```

`onUnhandledRequest: "error"`로 두어 목이 없는 요청이 조용히 통과하지 않게 한다.

- [ ] **Step 9: 클라이언트가 실제로 동작하는지 테스트한다**

`frontend/src/api/client.test.ts`

```ts
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { server } from "@/test/setup";
import { client, REDIRECT_KEY } from "./client";

describe("client", () => {
  it("목록을 타입에 맞게 가져온다", async () => {
    const { data, error } = await client.GET("/api/posts", {
      params: { query: { page: 0, size: 20 } },
    });

    expect(error).toBeUndefined();
    expect(data?.content?.[0]?.title).toBe("첫 글");
  });

  it("401을 받으면 로그인으로 이동하고 현재 경로를 기억한다", async () => {
    server.use(
      http.get("/api/members/me", () =>
        HttpResponse.json({ status: 401, code: "UNAUTHENTICATED" }, { status: 401 }),
      ),
    );
    const assign = vi.fn();
    vi.stubGlobal("location", { href: "", pathname: "/posts/1", search: "" });
    void assign;

    await client.GET("/api/members/me");

    expect(sessionStorage.getItem(REDIRECT_KEY)).toBe("/posts/1");
    vi.unstubAllGlobals();
  });
});
```

`window.location.href` 대입은 jsdom에서 실제 이동을 일으키지 않으므로 `vi.stubGlobal`로 대체해 기록만 확인한다.

- [ ] **Step 10: 테스트가 통과하는 것을 확인한다**

```bash
cd frontend && pnpm verify
```

기대: 모두 PASS. 타입 오류가 나면 `schema.d.ts`의 실제 타입 이름에 맞춰 핸들러를 고친다.

- [ ] **Step 11: 커밋한다**

```bash
cd .. && git add frontend
git commit -m "feat: API 타입 생성과 클라이언트 계층 추가

OpenAPI 스키마에서 타입을 생성해 백엔드 계약이 바뀌면 컴파일이
깨지게 한다. 생성물은 커밋하여 백엔드 변경이 diff로 드러나게 하고,
생성은 수동 실행하여 빌드가 실행 중인 서버에 의존하지 않게 한다.

401 처리는 fetch 미들웨어 한 곳에만 둔다. 개별 호출부가 인증
만료를 신경 쓰지 않아도 된다.

MSW 핸들러도 같은 생성 타입을 사용하므로, 목이 실제 계약과
어긋나면 테스트가 통과하기 전에 컴파일이 실패한다."
```

---
## Task 5: 레이아웃과 로그인 상태 표시

라우터와 쿼리 클라이언트를 세우고, 로그인 여부를 화면에 드러낸다. 이후 모든 화면이 이 위에 올라간다.

**Files:**
- Create: `frontend/src/features/member/queries.ts`
- Create: `frontend/src/components/layout/Header.tsx`
- Create: `frontend/src/components/layout/Header.test.tsx`
- Create: `frontend/src/routes/__root.tsx`
- Create: `frontend/src/routes/index.tsx`
- Create: `frontend/src/test/render.tsx`
- Modify: `frontend/src/main.tsx`

**Interfaces:**
- Consumes: `client`, `takeRedirectPath`, `startLogin` (Task 4)
- Produces:
  - `memberKeys.me()` → `["member", "me"]`
  - `useCurrentMember()` → `UseQueryResult<MemberResponse | null>` — 미인증이면 `null`, 401로 리다이렉트하지 않는다
  - `renderWithProviders(ui)` — QueryClient와 라우터 컨텍스트를 붙여 렌더링하는 테스트 유틸리티
  - `<Header />` — 로그인 시 닉네임과 로그아웃, 비로그인 시 로그인 버튼

- [ ] **Step 1: 현재 회원 조회 훅 테스트를 쓴다**

`frontend/src/features/member/queries.test.tsx`

```tsx
import { renderHook, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { queryWrapper } from "@/test/render";
import { server } from "@/test/setup";
import { useCurrentMember } from "./queries";

describe("useCurrentMember", () => {
  it("로그인 상태면 회원 정보를 반환한다", async () => {
    const { result } = renderHook(() => useCurrentMember(), { wrapper: queryWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.nickname).toBe("테스터");
  });

  it("미인증이면 null을 반환하고 로그인으로 이동하지 않는다", async () => {
    server.use(
      http.get("/api/members/me", () =>
        HttpResponse.json({ status: 401, code: "UNAUTHENTICATED" }, { status: 401 }),
      ),
    );

    const { result } = renderHook(() => useCurrentMember(), { wrapper: queryWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBeNull();
  });
});
```

두 번째 테스트가 이 훅의 핵심이다. 로그인 여부를 **확인하는** 요청이 401을 받았다고 로그인으로 튕기면 비로그인 사용자가 목록조차 볼 수 없다.

- [ ] **Step 2: 테스트 유틸리티를 만든다**

`frontend/src/test/render.tsx`

```tsx
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { ReactElement, ReactNode } from "react";

function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: 0 },
      mutations: { retry: false },
    },
  });
}

export function queryWrapper(): ({ children }: { children: ReactNode }) => ReactElement {
  const queryClient = createTestQueryClient();
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

export function renderWithProviders(ui: ReactElement) {
  return render(ui, { wrapper: queryWrapper() });
}
```

테스트에서 재시도를 끄는 것이 중요하다. 켜두면 실패 경로 테스트가 느려지고 타이밍에 흔들린다.

- [ ] **Step 3: 테스트가 실패하는 것을 확인한다**

```bash
cd frontend && pnpm test
```

기대: `./queries` 모듈을 찾을 수 없어 실패

- [ ] **Step 4: 회원 조회 훅을 구현한다**

`frontend/src/features/member/queries.ts`

```ts
import { useQuery } from "@tanstack/react-query";
import { client } from "@/api/client";
import type { components } from "@/api/schema";

export type Member = components["schemas"]["MemberResponse"];

export const memberKeys = {
  all: ["member"] as const,
  me: () => [...memberKeys.all, "me"] as const,
};

/**
 * 로그인 여부 확인은 401을 정상 응답으로 취급한다.
 *
 * 이 요청까지 로그인으로 튕기면 비로그인 사용자가 아무 화면도 볼 수 없다.
 */
export function useCurrentMember() {
  return useQuery({
    queryKey: memberKeys.me(),
    queryFn: async (): Promise<Member | null> => {
      const response = await fetch("/api/members/me", { headers: { Accept: "application/json" } });
      if (response.status === 401) {
        return null;
      }
      if (!response.ok) {
        throw new Error(`회원 정보를 가져오지 못했습니다. (${response.status})`);
      }
      return (await response.json()) as Member;
    },
    staleTime: 5 * 60 * 1000,
  });
}
```

이 훅만 `client`를 쓰지 않고 `fetch`를 직접 쓴다. `client`의 401 미들웨어를 피해야 하기 때문이며, 이유를 주석으로 남긴다.

- [ ] **Step 5: 테스트가 통과하는 것을 확인한다**

```bash
cd frontend && pnpm test
```

기대: PASS

- [ ] **Step 6: 헤더 테스트를 쓴다**

`frontend/src/components/layout/Header.test.tsx`

```tsx
import { screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/setup";
import { Header } from "./Header";

describe("Header", () => {
  it("로그인 상태면 닉네임과 로그아웃을 보여준다", async () => {
    renderWithProviders(<Header />);

    await waitFor(() => expect(screen.getByText("테스터")).toBeInTheDocument());
    expect(screen.getByRole("link", { name: "로그아웃" })).toBeInTheDocument();
  });

  it("비로그인이면 로그인 버튼을 보여준다", async () => {
    server.use(
      http.get("/api/members/me", () =>
        HttpResponse.json({ status: 401, code: "UNAUTHENTICATED" }, { status: 401 }),
      ),
    );

    renderWithProviders(<Header />);

    await waitFor(() =>
      expect(screen.getByRole("link", { name: "로그인" })).toBeInTheDocument(),
    );
  });
});
```

- [ ] **Step 7: 헤더를 구현한다**

`frontend/src/components/layout/Header.tsx`

```tsx
import { Link } from "@tanstack/react-router";
import { LOGIN_URL } from "@/api/client";
import { useCurrentMember } from "@/features/member/queries";

export function Header() {
  const { data: member, isPending } = useCurrentMember();

  return (
    <header className="border-b border-slate-200">
      <div className="mx-auto flex max-w-4xl items-center justify-between px-4 py-3">
        <Link to="/" className="text-lg font-semibold">
          게시판
        </Link>
        <nav className="flex items-center gap-3 text-sm">
          {isPending ? null : member ? (
            <>
              <span className="text-slate-700">{member.nickname}</span>
              <a href="/logout" className="text-slate-500 hover:text-slate-900">
                로그아웃
              </a>
            </>
          ) : (
            <a href={LOGIN_URL} className="text-slate-500 hover:text-slate-900">
              로그인
            </a>
          )}
        </nav>
      </div>
    </header>
  );
}
```

로그인과 로그아웃은 `Link`가 아니라 `<a>`를 쓴다. 클라이언트 라우팅이 아니라 서버로 실제 이동해야 한다.

- [ ] **Step 8: 루트 라우트를 만든다**

`frontend/src/routes/__root.tsx`

```tsx
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createRootRoute, Outlet } from "@tanstack/react-router";
import { Header } from "@/components/layout/Header";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30 * 1000, retry: 1, refetchOnWindowFocus: false },
  },
});

export const Route = createRootRoute({
  component: RootLayout,
  notFoundComponent: () => (
    <main className="mx-auto max-w-4xl px-4 py-16 text-center text-slate-600">
      요청하신 페이지를 찾을 수 없습니다.
    </main>
  ),
});

function RootLayout() {
  return (
    <QueryClientProvider client={queryClient}>
      <Header />
      <main className="mx-auto max-w-4xl px-4 py-6">
        <Outlet />
      </main>
    </QueryClientProvider>
  );
}
```

`frontend/src/routes/index.tsx` — 목록 화면은 Task 6에서 채운다.

```tsx
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  component: () => <p>목록 준비 중</p>,
});
```

- [ ] **Step 9: 진입점을 라우터로 교체한다**

`frontend/src/main.tsx`

```tsx
import { createRouter, RouterProvider } from "@tanstack/react-router";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { takeRedirectPath } from "./api/client";
import { routeTree } from "./routeTree.gen";
import "./styles.css";

const router = createRouter({ routeTree });

declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}

const container = document.getElementById("root");
if (!container) {
  throw new Error("root 엘리먼트를 찾을 수 없습니다.");
}

const redirectPath = takeRedirectPath();
if (redirectPath && redirectPath !== window.location.pathname + window.location.search) {
  window.history.replaceState(null, "", redirectPath);
}

createRoot(container).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);
```

`routeTree.gen.ts`는 라우터 플러그인이 생성한다. `.gitignore`에 넣지 않고 커밋한다.

- [ ] **Step 10: 검증하고 화면을 확인한다**

```bash
cd frontend && pnpm lint:fix && pnpm verify
pnpm dev &
sleep 5
curl -s http://localhost:5173 | grep -c "root"
```

기대: `pnpm verify` 통과. 브라우저에서 `http://localhost:5173`을 열어 헤더가 보이고, 로그인 버튼을 눌렀을 때 Keycloak으로 이동한 뒤 닉네임이 표시되는지 확인한다. 확인 후 개발 서버를 종료한다.

- [ ] **Step 11: 커밋한다**

```bash
cd .. && git add frontend
git commit -m "feat: 레이아웃과 로그인 상태 표시 추가

라우터와 쿼리 클라이언트를 세우고 헤더에 로그인 상태를 드러낸다.

로그인 여부를 확인하는 요청은 401을 정상 응답으로 취급한다. 이
요청까지 로그인으로 보내면 비로그인 사용자가 목록조차 볼 수 없다.

로그인과 로그아웃은 클라이언트 라우팅이 아니라 서버로 실제
이동해야 하므로 앵커를 쓴다."
```

---

## Task 6: 게시글 목록 화면

검색과 페이징을 URL에 두어 새로고침·뒤로가기·공유가 모두 동작하게 한다.

**Files:**
- Create: `frontend/src/features/post/queries.ts`
- Create: `frontend/src/features/post/components/PostList.tsx`
- Create: `frontend/src/features/post/components/SearchForm.tsx`
- Create: `frontend/src/features/post/components/Pagination.tsx`
- Create: `frontend/src/features/post/components/PostList.test.tsx`
- Create: `frontend/src/routes/posts.$postId.tsx` (껍데기)
- Modify: `frontend/src/routes/index.tsx`

**Interfaces:**
- Consumes: `client` (Task 4), `renderWithProviders` (Task 5)
- Produces:
  - `postKeys.all` → `["post"]`
  - `postKeys.lists()` → `["post", "list"]`
  - `postKeys.list(params)` → `["post", "list", params]`
  - `postKeys.detail(id)` → `["post", "detail", id]`
  - `PostListSearch` 타입 — `{ page: number; size: number; keyword?: string; authorId?: number }`
  - `usePostList(search: PostListSearch)` → `UseQueryResult<PageResponsePostSummaryResponse>`

- [ ] **Step 1: 목록 화면 테스트를 쓴다**

`frontend/src/features/post/components/PostList.test.tsx`

```tsx
import { screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { 게시글요약, 게시글페이지 } from "@/test/handlers";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/setup";
import { PostList } from "./PostList";

describe("PostList", () => {
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

    renderWithProviders(<PostList search={{ page: 0, size: 20 }} />);

    await waitFor(() => expect(screen.getByText("첫 글")).toBeInTheDocument());
    expect(screen.getByText("둘째 글")).toBeInTheDocument();
    expect(screen.getAllByText("작성자")).toHaveLength(2);
  });

  it("결과가 없으면 안내를 보여준다", async () => {
    server.use(http.get("/api/posts", () => HttpResponse.json(게시글페이지([]))));

    renderWithProviders(<PostList search={{ page: 0, size: 20 }} />);

    await waitFor(() => expect(screen.getByText(/게시글이 없습니다/)).toBeInTheDocument());
  });

  it("검색어를 쿼리로 전달한다", async () => {
    let 받은키워드: string | null = null;
    server.use(
      http.get("/api/posts", ({ request }) => {
        받은키워드 = new URL(request.url).searchParams.get("keyword");
        return HttpResponse.json(게시글페이지([게시글요약({ title: "스프링 입문" })]));
      }),
    );

    renderWithProviders(<PostList search={{ page: 0, size: 20, keyword: "스프링" }} />);

    await waitFor(() => expect(받은키워드).toBe("스프링"));
  });
});
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
cd frontend && pnpm test
```

기대: `./PostList` 모듈을 찾을 수 없어 실패

- [ ] **Step 3: 조회 훅을 만든다**

`frontend/src/features/post/queries.ts`

```ts
import { useQuery } from "@tanstack/react-query";
import { client } from "@/api/client";
import type { components } from "@/api/schema";

export type PostSummary = components["schemas"]["PostSummaryResponse"];
export type PostDetail = components["schemas"]["PostResponse"];
export type PostPage = components["schemas"]["PageResponsePostSummaryResponse"];

export type PostListSearch = {
  page: number;
  size: number;
  keyword?: string;
  authorId?: number;
};

export const postKeys = {
  all: ["post"] as const,
  lists: () => [...postKeys.all, "list"] as const,
  list: (search: PostListSearch) => [...postKeys.lists(), search] as const,
  details: () => [...postKeys.all, "detail"] as const,
  detail: (id: number) => [...postKeys.details(), id] as const,
};

export function usePostList(search: PostListSearch) {
  return useQuery({
    queryKey: postKeys.list(search),
    queryFn: async (): Promise<PostPage> => {
      const { data, error } = await client.GET("/api/posts", { params: { query: search } });
      if (error) {
        throw error;
      }
      return data;
    },
  });
}

export function usePostDetail(id: number) {
  return useQuery({
    queryKey: postKeys.detail(id),
    queryFn: async (): Promise<PostDetail> => {
      const { data, error } = await client.GET("/api/posts/{id}", {
        params: { path: { id } },
      });
      if (error) {
        throw error;
      }
      return data;
    },
  });
}
```

- [ ] **Step 4: 목록 컴포넌트를 만든다**

`frontend/src/features/post/components/PostList.tsx`

```tsx
import { Link } from "@tanstack/react-router";
import { type PostListSearch, usePostList } from "../queries";

export function PostList({ search }: { search: PostListSearch }) {
  const { data, isPending, isError } = usePostList(search);

  if (isPending) {
    return <p className="py-8 text-center text-slate-500">불러오는 중입니다.</p>;
  }
  if (isError) {
    return <p className="py-8 text-center text-red-600">목록을 불러오지 못했습니다.</p>;
  }
  if (data.content.length === 0) {
    return <p className="py-8 text-center text-slate-500">게시글이 없습니다.</p>;
  }

  return (
    <ul className="divide-y divide-slate-200">
      {data.content.map((post) => (
        <li key={post.id} className="py-3">
          <Link
            to="/posts/$postId"
            params={{ postId: String(post.id) }}
            className="font-medium hover:underline"
          >
            {post.title}
          </Link>
          <div className="mt-1 flex gap-3 text-sm text-slate-500">
            <span>{post.authorNickname}</span>
            <span>조회 {post.viewCount}</span>
            <span>좋아요 {post.likeCount}</span>
          </div>
        </li>
      ))}
    </ul>
  );
}
```

- [ ] **Step 4-1: 상세 라우트를 빈 껍데기로 먼저 만든다**

`PostList`가 `/posts/$postId`로 링크하므로 라우트가 없으면 타입이 맞지 않는다. Task 7에서 내용을 채운다.

`frontend/src/routes/posts.$postId.tsx`

```tsx
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/posts/$postId")({
  component: () => <p>상세 준비 중</p>,
});
```

- [ ] **Step 5: 테스트가 통과하는 것을 확인한다**

```bash
cd frontend && pnpm test
```

기대: PASS

- [ ] **Step 6: 검색 폼과 페이지네이션을 만든다**

`frontend/src/features/post/components/SearchForm.tsx`

```tsx
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export function SearchForm({
  defaultKeyword,
  onSearch,
}: {
  defaultKeyword: string;
  onSearch: (keyword: string) => void;
}) {
  return (
    <form
      className="flex gap-2"
      onSubmit={(event) => {
        event.preventDefault();
        const formData = new FormData(event.currentTarget);
        onSearch(String(formData.get("keyword") ?? ""));
      }}
    >
      <Input
        name="keyword"
        defaultValue={defaultKeyword}
        placeholder="제목이나 본문으로 검색"
        aria-label="검색어"
        className="flex-1"
      />
      <Button type="submit">검색</Button>
    </form>
  );
}
```

`frontend/src/features/post/components/Pagination.tsx`

```tsx
import { Button } from "@/components/ui/button";

export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <nav className="flex justify-center gap-1 py-4" aria-label="페이지">
      {Array.from({ length: totalPages }, (_, index) => index).map((index) => (
        <Button
          key={index}
          type="button"
          size="sm"
          variant={index === page ? "default" : "ghost"}
          aria-current={index === page ? "page" : undefined}
          onClick={() => onChange(index)}
        >
          {index + 1}
        </Button>
      ))}
    </nav>
  );
}
```

- [ ] **Step 7: 목록 라우트를 완성한다**

`frontend/src/routes/index.tsx`

```tsx
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Pagination } from "@/features/post/components/Pagination";
import { PostList } from "@/features/post/components/PostList";
import { SearchForm } from "@/features/post/components/SearchForm";
import { usePostList } from "@/features/post/queries";

type PostListSearchParams = {
  page: number;
  size: number;
  keyword?: string;
};

export const Route = createFileRoute("/")({
  validateSearch: (search: Record<string, unknown>): PostListSearchParams => {
    const page = Number(search.page);
    const size = Number(search.size);
    const keyword = typeof search.keyword === "string" ? search.keyword.trim() : "";

    return {
      page: Number.isInteger(page) && page >= 0 ? page : 0,
      size: Number.isInteger(size) && size > 0 && size <= 100 ? size : 20,
      ...(keyword ? { keyword } : {}),
    };
  },
  component: PostListPage,
});

function PostListPage() {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const { data } = usePostList(search);

  return (
    <div className="space-y-4">
      <SearchForm
        defaultKeyword={search.keyword ?? ""}
        onSearch={(keyword) =>
          navigate({ search: { ...search, page: 0, ...(keyword ? { keyword } : {}) } })
        }
      />
      <PostList search={search} />
      <Pagination
        page={search.page}
        totalPages={data?.totalPages ?? 0}
        onChange={(page) => navigate({ search: { ...search, page } })}
      />
    </div>
  );
}
```

`validateSearch`가 잘못된 값을 기본값으로 정규화하므로 `?page=abc`로 들어와도 화면이 깨지지 않는다.

- [ ] **Step 8: 검증하고 커밋한다**

```bash
cd frontend && pnpm lint:fix && pnpm verify
```

브라우저에서 검색어를 넣고 URL이 바뀌는지, 새로고침해도 유지되는지, 뒤로가기가 이전 검색으로 돌아가는지 확인한다.

```bash
cd .. && git add frontend
git commit -m "feat: 게시글 목록 화면 추가

검색어와 페이지를 URL 검색 파라미터에 두어 새로고침, 뒤로가기,
링크 공유가 모두 동작하게 한다. 잘못된 값이 들어와도 기본값으로
정규화되므로 화면이 깨지지 않는다.

queryKey는 features/post/queries.ts가 소유한다. 컴포넌트가 키를
직접 조립하면 무효화 범위가 새어 나간다."
```

---
## Task 7: 게시글 상세 화면

상세 조회와 권한별 버튼 노출을 만든다. 댓글과 좋아요는 이후 태스크에서 이 화면에 붙는다.

**Files:**
- Modify: `frontend/src/routes/posts.$postId.tsx` (Task 6의 껍데기를 채운다)
- Create: `frontend/src/routes/posts.$postId.edit.tsx`
- Create: `frontend/src/features/post/components/PostDetail.tsx`
- Create: `frontend/src/features/post/components/PostDetail.test.tsx`
- Create: `frontend/src/features/post/mutations.ts`

**Interfaces:**
- Consumes: `usePostDetail`, `postKeys` (Task 6), `useCurrentMember` (Task 5), `isProblemCode` (Task 4)
- Produces:
  - `useDeletePost()` → `UseMutationResult<void, unknown, number>` — 성공 시 목록 무효화
  - `canEdit(member, post)` → `boolean` — 작성자 본인만 true
  - `<PostDetail postId={number} />`

- [ ] **Step 1: 상세 화면 테스트를 쓴다**

`frontend/src/features/post/components/PostDetail.test.tsx`

```tsx
import { screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import type { components } from "@/api/schema";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/setup";
import { PostDetail } from "./PostDetail";

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

describe("PostDetail", () => {
  it("제목과 본문을 보여준다", async () => {
    server.use(http.get("/api/posts/1", () => HttpResponse.json(게시글())));

    renderWithProviders(<PostDetail postId={1} />);

    await waitFor(() => expect(screen.getByRole("heading", { name: "제목" })).toBeInTheDocument());
    expect(screen.getByText("본문입니다.")).toBeInTheDocument();
  });

  it("작성자 본인에게만 수정과 삭제를 보여준다", async () => {
    server.use(http.get("/api/posts/1", () => HttpResponse.json(게시글({ authorId: 1 }))));

    renderWithProviders(<PostDetail postId={1} />);

    await waitFor(() => expect(screen.getByRole("link", { name: "수정" })).toBeInTheDocument());
    expect(screen.getByRole("button", { name: "삭제" })).toBeInTheDocument();
  });

  it("다른 사람 글에는 수정과 삭제를 보여주지 않는다", async () => {
    server.use(http.get("/api/posts/1", () => HttpResponse.json(게시글({ authorId: 999 }))));

    renderWithProviders(<PostDetail postId={1} />);

    await waitFor(() => expect(screen.getByRole("heading", { name: "제목" })).toBeInTheDocument());
    expect(screen.queryByRole("link", { name: "수정" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "삭제" })).not.toBeInTheDocument();
  });

  it("없는 글이면 안내를 보여준다", async () => {
    server.use(
      http.get("/api/posts/1", () =>
        HttpResponse.json(
          { status: 404, code: "POST_NOT_FOUND", detail: "게시글을 찾을 수 없습니다." },
          { status: 404 },
        ),
      ),
    );

    renderWithProviders(<PostDetail postId={1} />);

    await waitFor(() =>
      expect(screen.getByText(/게시글을 찾을 수 없습니다/)).toBeInTheDocument(),
    );
  });
});
```

기본 MSW 핸들러의 `/api/members/me`가 `id: 1`을 반환하므로, `authorId: 1`이면 본인이고 `999`면 남의 글이다.

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
cd frontend && pnpm test
```

기대: `./PostDetail` 모듈을 찾을 수 없어 실패

- [ ] **Step 3: 변경 훅을 만든다**

`frontend/src/features/post/mutations.ts`

```ts
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { client } from "@/api/client";
import { postKeys } from "./queries";

export function useCreatePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: { title: string; content: string }): Promise<string> => {
      const { response, error } = await client.POST("/api/posts", { body: input });
      if (error) {
        throw error;
      }
      return response.headers.get("Location") ?? "/";
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
}

export function useUpdatePost(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: { title: string; content: string }): Promise<void> => {
      const { error } = await client.PATCH("/api/posts/{id}", {
        params: { path: { id } },
        body: input,
      });
      if (error) {
        throw error;
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.detail(id) });
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
}

export function useDeletePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number): Promise<void> => {
      const { error } = await client.DELETE("/api/posts/{id}", { params: { path: { id } } });
      if (error) {
        throw error;
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
}
```

- [ ] **Step 4: 상세 컴포넌트를 만든다**

`frontend/src/features/post/components/PostDetail.tsx`

```tsx
import { Link, useNavigate } from "@tanstack/react-router";
import { isProblemCode, toProblem } from "@/api/problem";
import { Button } from "@/components/ui/button";
import { useCurrentMember } from "@/features/member/queries";
import { useDeletePost } from "../mutations";
import { usePostDetail } from "../queries";

export function PostDetail({ postId }: { postId: number }) {
  const { data: post, isPending, error } = usePostDetail(postId);
  const { data: member } = useCurrentMember();
  const navigate = useNavigate();
  const deletePost = useDeletePost();

  if (isPending) {
    return <p className="py-8 text-center text-slate-500">불러오는 중입니다.</p>;
  }
  if (error) {
    const detail = isProblemCode(error, "POST_NOT_FOUND")
      ? "게시글을 찾을 수 없습니다."
      : (toProblem(error)?.detail ?? "게시글을 불러오지 못했습니다.");
    return <p className="py-8 text-center text-slate-600">{detail}</p>;
  }

  const 본인글 = member != null && member.id === post.authorId;

  return (
    <article className="space-y-4">
      <header className="space-y-2 border-b border-slate-200 pb-3">
        <h1 className="text-2xl font-semibold">{post.title}</h1>
        <div className="flex gap-3 text-sm text-slate-500">
          <span>조회 {post.viewCount}</span>
          <span>좋아요 {post.likeCount}</span>
        </div>
      </header>

      <p className="whitespace-pre-wrap leading-relaxed">{post.content}</p>

      {본인글 ? (
        <div className="flex gap-2">
          <Link
            to="/posts/$postId/edit"
            params={{ postId: String(post.id) }}
            className="rounded border border-slate-300 px-3 py-1 text-sm"
          >
            수정
          </Link>
          <Button
            type="button"
            variant="destructive"
            size="sm"
            disabled={deletePost.isPending}
            onClick={() => {
              if (!window.confirm("게시글을 삭제할까요?")) {
                return;
              }
              deletePost.mutate(post.id, { onSuccess: () => void navigate({ to: "/" }) });
            }}
          >
            삭제
          </Button>
        </div>
      ) : null}
    </article>
  );
}
```

- [ ] **Step 5: 라우트를 채우고 수정 라우트를 껍데기로 만든다**

`PostDetail`이 `/posts/$postId/edit`으로 링크하므로 해당 라우트가 없으면 타입이 맞지 않는다. Task 8에서 내용을 채운다.

`frontend/src/routes/posts.$postId.edit.tsx`

```tsx
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/posts/$postId/edit")({
  component: () => <p>수정 준비 중</p>,
});
```

Task 6에서 껍데기로 만든 상세 라우트를 채운다.

`frontend/src/routes/posts.$postId.tsx`

```tsx
import { createFileRoute } from "@tanstack/react-router";
import { PostDetail } from "@/features/post/components/PostDetail";

export const Route = createFileRoute("/posts/$postId")({
  component: PostDetailPage,
});

function PostDetailPage() {
  const { postId } = Route.useParams();
  return <PostDetail postId={Number(postId)} />;
}
```

- [ ] **Step 6: 테스트가 통과하는 것을 확인한다**

```bash
cd frontend && pnpm lint:fix && pnpm verify
```

기대: PASS

- [ ] **Step 7: 커밋한다**

```bash
cd .. && git add frontend
git commit -m "feat: 게시글 상세 화면 추가

작성자 본인에게만 수정과 삭제를 노출한다. 서버가 권한을 최종
판단하므로 이 노출은 편의이며 보안 경계가 아니다.

에러는 ProblemDetail의 code로 분기해 없는 글과 그 밖의 실패를
구분해 안내한다."
```

---

## Task 8: 게시글 작성 및 수정 폼

React 19의 Actions로 제출 상태와 서버 검증 오류를 한 곳에서 다룬다.

**Files:**
- Create: `frontend/src/features/post/components/PostForm.tsx`
- Create: `frontend/src/features/post/components/PostForm.test.tsx`
- Create: `frontend/src/routes/posts.new.tsx`
- Modify: `frontend/src/routes/posts.$postId.edit.tsx` (Task 7의 껍데기를 채운다)

**Interfaces:**
- Consumes: `useCreatePost`, `useUpdatePost` (Task 7), `fieldErrors` (Task 4), `useCurrentMember` (Task 5)
- Produces:
  - `<PostForm mode="create" />`
  - `<PostForm mode="edit" postId={number} initial={{ title, content }} />`
  - 서버가 준 `errors`를 필드 아래에 표시한다

- [ ] **Step 1: 폼 테스트를 쓴다**

`frontend/src/features/post/components/PostForm.test.tsx`

```tsx
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/setup";
import { PostForm } from "./PostForm";

describe("PostForm", () => {
  it("입력한 값을 본문으로 전송한다", async () => {
    let 받은본문: unknown = null;
    server.use(
      http.post("/api/posts", async ({ request }) => {
        받은본문 = await request.json();
        return new HttpResponse(null, { status: 201, headers: { Location: "/api/posts/7" } });
      }),
    );

    renderWithProviders(<PostForm mode="create" />);

    await userEvent.type(screen.getByLabelText("제목"), "새 글");
    await userEvent.type(screen.getByLabelText("본문"), "내용입니다");
    await userEvent.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() => expect(받은본문).toEqual({ title: "새 글", content: "내용입니다" }));
  });

  it("서버 검증 오류를 필드 아래에 보여준다", async () => {
    server.use(
      http.post("/api/posts", () =>
        HttpResponse.json(
          {
            status: 400,
            code: "INVALID_REQUEST",
            detail: "요청 값이 올바르지 않습니다.",
            errors: { title: "제목은 필수입니다." },
          },
          { status: 400 },
        ),
      ),
    );

    renderWithProviders(<PostForm mode="create" />);

    await userEvent.type(screen.getByLabelText("본문"), "내용");
    await userEvent.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() => expect(screen.getByText("제목은 필수입니다.")).toBeInTheDocument());
  });

  it("수정 모드에서는 기존 값이 채워진다", () => {
    renderWithProviders(
      <PostForm mode="edit" postId={1} initial={{ title: "기존 제목", content: "기존 본문" }} />,
    );

    expect(screen.getByLabelText("제목")).toHaveValue("기존 제목");
    expect(screen.getByLabelText("본문")).toHaveValue("기존 본문");
  });
});
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
cd frontend && pnpm test
```

기대: `./PostForm` 모듈을 찾을 수 없어 실패

- [ ] **Step 3: 폼을 구현한다**

`frontend/src/features/post/components/PostForm.tsx`

```tsx
import { useNavigate } from "@tanstack/react-router";
import { useActionState } from "react";
import { fieldErrors, toProblem } from "@/api/problem";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useCreatePost, useUpdatePost } from "../mutations";

type FormState = {
  fields: Record<string, string>;
  message: string | null;
};

const 초기상태: FormState = { fields: {}, message: null };

type PostFormProps =
  | { mode: "create"; postId?: undefined; initial?: undefined }
  | { mode: "edit"; postId: number; initial: { title: string; content: string } };

export function PostForm({ mode, postId, initial }: PostFormProps) {
  const navigate = useNavigate();
  const createPost = useCreatePost();
  const updatePost = useUpdatePost(postId ?? 0);

  const [state, submit, isPending] = useActionState(
    async (_previous: FormState, formData: FormData): Promise<FormState> => {
      const input = {
        title: String(formData.get("title") ?? ""),
        content: String(formData.get("content") ?? ""),
      };

      try {
        if (mode === "create") {
          const location = await createPost.mutateAsync(input);
          const id = location.replace(/.*\//, "");
          await navigate({ to: "/posts/$postId", params: { postId: id } });
        } else {
          await updatePost.mutateAsync(input);
          await navigate({ to: "/posts/$postId", params: { postId: String(postId) } });
        }
        return 초기상태;
      } catch (error) {
        return {
          fields: fieldErrors(error),
          message: toProblem(error)?.detail ?? "저장하지 못했습니다.",
        };
      }
    },
    초기상태,
  );

  return (
    <form action={submit} className="space-y-4">
      <div className="space-y-1">
        <label htmlFor="title" className="block text-sm font-medium">
          제목
        </label>
        <Input id="title" name="title" defaultValue={initial?.title ?? ""} />
        {state.fields.title ? (
          <p className="text-sm text-red-600">{state.fields.title}</p>
        ) : null}
      </div>

      <div className="space-y-1">
        <label htmlFor="content" className="block text-sm font-medium">
          본문
        </label>
        <Textarea id="content" name="content" rows={12} defaultValue={initial?.content ?? ""} />
        {state.fields.content ? (
          <p className="text-sm text-red-600">{state.fields.content}</p>
        ) : null}
      </div>

      {state.message && Object.keys(state.fields).length === 0 ? (
        <p className="text-sm text-red-600">{state.message}</p>
      ) : null}

      <Button type="submit" disabled={isPending}>
        저장
      </Button>
    </form>
  );
}
```

- [ ] **Step 4: 라우트를 만든다**

`frontend/src/routes/posts.new.tsx`

```tsx
import { createFileRoute } from "@tanstack/react-router";
import { PostForm } from "@/features/post/components/PostForm";

export const Route = createFileRoute("/posts/new")({
  component: () => <PostForm mode="create" />,
});
```

`frontend/src/routes/posts.$postId.edit.tsx`

```tsx
import { createFileRoute } from "@tanstack/react-router";
import { PostForm } from "@/features/post/components/PostForm";
import { usePostDetail } from "@/features/post/queries";

export const Route = createFileRoute("/posts/$postId/edit")({
  component: PostEditPage,
});

function PostEditPage() {
  const { postId } = Route.useParams();
  const { data: post, isPending } = usePostDetail(Number(postId));

  if (isPending) {
    return <p className="py-8 text-center text-slate-500">불러오는 중입니다.</p>;
  }

  return (
    <PostForm
      mode="edit"
      postId={Number(postId)}
      initial={{ title: post.title, content: post.content }}
    />
  );
}
```

- [ ] **Step 5: 목록 화면에 작성 버튼을 붙인다**

`frontend/src/routes/index.tsx`의 `SearchForm` 위에 추가한다.

```tsx
      <div className="flex justify-end">
        <Link to="/posts/new" className="rounded bg-slate-900 px-4 py-2 text-sm text-white">
          글쓰기
        </Link>
      </div>
```

`import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";`로 import를 수정한다.

- [ ] **Step 6: 테스트가 통과하는 것을 확인한다**

```bash
cd frontend && pnpm lint:fix && pnpm verify
```

기대: PASS

- [ ] **Step 7: 커밋한다**

```bash
cd .. && git add frontend
git commit -m "feat: 게시글 작성 및 수정 폼 추가

React 19의 Actions로 제출 중 상태와 서버 오류를 한 곳에서 다룬다.
백엔드가 주는 errors 필드를 그대로 필드별 메시지로 연결하므로
검증 규칙을 프론트엔드에 복제하지 않는다."
```

---
## Task 9: 댓글 및 대댓글

상세 화면에 댓글 목록과 작성·수정·삭제를 붙인다. 대댓글은 한 단계까지만 허용한다.

**Files:**
- Create: `frontend/src/features/comment/queries.ts`
- Create: `frontend/src/features/comment/mutations.ts`
- Create: `frontend/src/features/comment/components/CommentSection.tsx`
- Create: `frontend/src/features/comment/components/CommentItem.tsx`
- Create: `frontend/src/features/comment/components/CommentForm.tsx`
- Create: `frontend/src/features/comment/components/CommentSection.test.tsx`
- Modify: `frontend/src/routes/posts.$postId.tsx`

**Interfaces:**
- Consumes: `client` (Task 4), `useCurrentMember` (Task 5), `fieldErrors` (Task 4)
- Produces:
  - `commentKeys.list(postId)` → `["comment", "list", postId]`
  - `useCommentList(postId)` → `UseQueryResult<PageResponseCommentResponse>`
  - `useWriteComment(postId)` → `mutate({ body, parentCommentId? })`
  - `useUpdateComment(postId)` → `mutate({ id, body })`
  - `useDeleteComment(postId)` → `mutate(id)`
  - 세 변경 훅 모두 성공 시 `commentKeys.list(postId)`를 무효화한다

- [ ] **Step 1: 댓글 화면 테스트를 쓴다**

`frontend/src/features/comment/components/CommentSection.test.tsx`

```tsx
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import type { components } from "@/api/schema";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/setup";
import { CommentSection } from "./CommentSection";

type CommentResponse = components["schemas"]["CommentResponse"];
type CommentPage = components["schemas"]["PageResponseCommentResponse"];

function 댓글(overrides: Partial<CommentResponse> = {}): CommentResponse {
  return {
    id: 1,
    postId: 1,
    authorId: 1,
    body: "댓글입니다",
    parentCommentId: null,
    depth: 0,
    createdAt: "2026-09-21T00:00:00Z",
    ...overrides,
  };
}

function 댓글페이지(content: CommentResponse[]): CommentPage {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1, last: true };
}

describe("CommentSection", () => {
  it("댓글과 대댓글을 함께 보여준다", async () => {
    server.use(
      http.get("/api/posts/1/comments", () =>
        HttpResponse.json(
          댓글페이지([
            댓글({ id: 1, body: "원댓글", depth: 0 }),
            댓글({ id: 2, body: "답글", depth: 1, parentCommentId: 1 }),
          ]),
        ),
      ),
    );

    renderWithProviders(<CommentSection postId={1} />);

    await waitFor(() => expect(screen.getByText("원댓글")).toBeInTheDocument());
    expect(screen.getByText("답글")).toBeInTheDocument();
  });

  it("댓글을 작성하면 본문을 전송한다", async () => {
    let 받은본문: unknown = null;
    server.use(
      http.get("/api/posts/1/comments", () => HttpResponse.json(댓글페이지([]))),
      http.post("/api/posts/1/comments", async ({ request }) => {
        받은본문 = await request.json();
        return new HttpResponse(null, { status: 201, headers: { Location: "/api/comments/5" } });
      }),
    );

    renderWithProviders(<CommentSection postId={1} />);

    await userEvent.type(await screen.findByLabelText("댓글"), "새 댓글");
    await userEvent.click(screen.getByRole("button", { name: "등록" }));

    await waitFor(() => expect(받은본문).toEqual({ body: "새 댓글" }));
  });

  it("대댓글에는 답글 버튼을 보여주지 않는다", async () => {
    server.use(
      http.get("/api/posts/1/comments", () =>
        HttpResponse.json(댓글페이지([댓글({ id: 2, body: "답글", depth: 1, parentCommentId: 1 })])),
      ),
    );

    renderWithProviders(<CommentSection postId={1} />);

    await waitFor(() => expect(screen.getByText("답글")).toBeInTheDocument());
    expect(screen.queryByRole("button", { name: "답글 달기" })).not.toBeInTheDocument();
  });

  it("작성자 본인에게만 수정과 삭제를 보여준다", async () => {
    server.use(
      http.get("/api/posts/1/comments", () =>
        HttpResponse.json(댓글페이지([댓글({ id: 1, authorId: 999, body: "남의 댓글" })])),
      ),
    );

    renderWithProviders(<CommentSection postId={1} />);

    await waitFor(() => expect(screen.getByText("남의 댓글")).toBeInTheDocument());
    expect(screen.queryByRole("button", { name: "삭제" })).not.toBeInTheDocument();
  });
});
```

세 번째 테스트가 백엔드의 깊이 제한을 UI에 반영했는지 확인한다. 서버가 400을 주기 전에 버튼 자체가 없어야 한다.

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
cd frontend && pnpm test
```

기대: `./CommentSection` 모듈을 찾을 수 없어 실패

- [ ] **Step 3: 조회 훅을 만든다**

`frontend/src/features/comment/queries.ts`

```ts
import { useQuery } from "@tanstack/react-query";
import { client } from "@/api/client";
import type { components } from "@/api/schema";

export type Comment = components["schemas"]["CommentResponse"];
export type CommentPage = components["schemas"]["PageResponseCommentResponse"];

export const commentKeys = {
  all: ["comment"] as const,
  lists: () => [...commentKeys.all, "list"] as const,
  list: (postId: number) => [...commentKeys.lists(), postId] as const,
};

export function useCommentList(postId: number) {
  return useQuery({
    queryKey: commentKeys.list(postId),
    queryFn: async (): Promise<CommentPage> => {
      const { data, error } = await client.GET("/api/posts/{postId}/comments", {
        params: { path: { postId }, query: { page: 0, size: 100 } },
      });
      if (error) {
        throw error;
      }
      return data;
    },
  });
}
```

댓글은 한 번에 100건까지 가져온다. 게시판 규모에서 댓글 페이지네이션은 UI만 복잡하게 만든다.

- [ ] **Step 4: 변경 훅을 만든다**

`frontend/src/features/comment/mutations.ts`

```ts
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { client } from "@/api/client";
import { commentKeys } from "./queries";

export function useWriteComment(postId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: { body: string; parentCommentId?: number }): Promise<void> => {
      const { error } = await client.POST("/api/posts/{postId}/comments", {
        params: { path: { postId } },
        body: input,
      });
      if (error) {
        throw error;
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: commentKeys.list(postId) });
    },
  });
}

export function useUpdateComment(postId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: { id: number; body: string }): Promise<void> => {
      const { error } = await client.PATCH("/api/comments/{id}", {
        params: { path: { id: input.id } },
        body: { body: input.body },
      });
      if (error) {
        throw error;
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: commentKeys.list(postId) });
    },
  });
}

export function useDeleteComment(postId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number): Promise<void> => {
      const { error } = await client.DELETE("/api/comments/{id}", { params: { path: { id } } });
      if (error) {
        throw error;
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: commentKeys.list(postId) });
    },
  });
}
```

- [ ] **Step 5: 댓글 폼을 만든다**

`frontend/src/features/comment/components/CommentForm.tsx`

```tsx
import { useActionState } from "react";
import { fieldErrors, toProblem } from "@/api/problem";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";

type FormState = { error: string | null };

export function CommentForm({
  label,
  submitLabel,
  defaultValue,
  onSubmit,
}: {
  label: string;
  submitLabel: string;
  defaultValue?: string;
  onSubmit: (body: string) => Promise<void>;
}) {
  const [state, submit, isPending] = useActionState(
    async (_previous: FormState, formData: FormData): Promise<FormState> => {
      try {
        await onSubmit(String(formData.get("body") ?? ""));
        return { error: null };
      } catch (error) {
        const fields = fieldErrors(error);
        return { error: fields.body ?? toProblem(error)?.detail ?? "등록하지 못했습니다." };
      }
    },
    { error: null },
  );

  const id = `comment-${label}`;

  return (
    <form action={submit} className="space-y-1">
      <label htmlFor={id} className="sr-only">
        {label}
      </label>
      <Textarea id={id} name="body" rows={3} defaultValue={defaultValue} aria-label={label} />
      {state.error ? <p className="text-sm text-red-600">{state.error}</p> : null}
      <Button type="submit" size="sm" disabled={isPending}>
        {submitLabel}
      </Button>
    </form>
  );
}
```

- [ ] **Step 6: 댓글 항목과 목록을 만든다**

`frontend/src/features/comment/components/CommentItem.tsx`

```tsx
import { useState } from "react";
import { useCurrentMember } from "@/features/member/queries";
import { useDeleteComment, useUpdateComment, useWriteComment } from "../mutations";
import type { Comment } from "../queries";
import { CommentForm } from "./CommentForm";

export function CommentItem({ comment, postId }: { comment: Comment; postId: number }) {
  const { data: member } = useCurrentMember();
  const [모드, set모드] = useState<"보기" | "수정" | "답글">("보기");

  const updateComment = useUpdateComment(postId);
  const deleteComment = useDeleteComment(postId);
  const writeComment = useWriteComment(postId);

  const 본인댓글 = member != null && member.id === comment.authorId;
  const 답글가능 = member != null && comment.depth === 0;

  return (
    <li className={comment.depth > 0 ? "border-l-2 border-slate-200 py-2 pl-6" : "py-2"}>
      {모드 === "수정" ? (
        <CommentForm
          label="댓글 수정"
          submitLabel="수정"
          defaultValue={comment.body}
          onSubmit={async (body) => {
            await updateComment.mutateAsync({ id: comment.id, body });
            set모드("보기");
          }}
        />
      ) : (
        <p className="whitespace-pre-wrap text-sm">{comment.body}</p>
      )}

      <div className="mt-1 flex gap-2 text-xs text-slate-500">
        {답글가능 ? (
          <button type="button" onClick={() => set모드(모드 === "답글" ? "보기" : "답글")}>
            답글 달기
          </button>
        ) : null}
        {본인댓글 ? (
          <>
            <button type="button" onClick={() => set모드(모드 === "수정" ? "보기" : "수정")}>
              수정
            </button>
            <button
              type="button"
              className="text-red-600"
              onClick={() => {
                if (window.confirm("댓글을 삭제할까요?")) {
                  deleteComment.mutate(comment.id);
                }
              }}
            >
              삭제
            </button>
          </>
        ) : null}
      </div>

      {모드 === "답글" ? (
        <div className="mt-2 pl-6">
          <CommentForm
            label="답글"
            submitLabel="등록"
            onSubmit={async (body) => {
              await writeComment.mutateAsync({ body, parentCommentId: comment.id });
              set모드("보기");
            }}
          />
        </div>
      ) : null}
    </li>
  );
}
```

`frontend/src/features/comment/components/CommentSection.tsx`

```tsx
import { useCurrentMember } from "@/features/member/queries";
import { useWriteComment } from "../mutations";
import { useCommentList } from "../queries";
import { CommentForm } from "./CommentForm";
import { CommentItem } from "./CommentItem";

export function CommentSection({ postId }: { postId: number }) {
  const { data, isPending, isError } = useCommentList(postId);
  const { data: member } = useCurrentMember();
  const writeComment = useWriteComment(postId);

  return (
    <section className="space-y-3 border-t border-slate-200 pt-4">
      <h2 className="text-lg font-medium">댓글 {data?.totalElements ?? 0}</h2>

      {member ? (
        <CommentForm
          label="댓글"
          submitLabel="등록"
          onSubmit={async (body) => {
            await writeComment.mutateAsync({ body });
          }}
        />
      ) : (
        <p className="text-sm text-slate-500">댓글을 쓰려면 로그인이 필요합니다.</p>
      )}

      {isPending ? <p className="text-sm text-slate-500">불러오는 중입니다.</p> : null}
      {isError ? <p className="text-sm text-red-600">댓글을 불러오지 못했습니다.</p> : null}

      {data ? (
        <ul className="divide-y divide-slate-100">
          {data.content.map((comment) => (
            <CommentItem key={comment.id} comment={comment} postId={postId} />
          ))}
        </ul>
      ) : null}
    </section>
  );
}
```

- [ ] **Step 7: 상세 라우트에 붙인다**

`frontend/src/routes/posts.$postId.tsx`

```tsx
import { createFileRoute } from "@tanstack/react-router";
import { CommentSection } from "@/features/comment/components/CommentSection";
import { PostDetail } from "@/features/post/components/PostDetail";

export const Route = createFileRoute("/posts/$postId")({
  component: PostDetailPage,
});

function PostDetailPage() {
  const { postId } = Route.useParams();
  const id = Number(postId);

  return (
    <div className="space-y-6">
      <PostDetail postId={id} />
      <CommentSection postId={id} />
    </div>
  );
}
```

- [ ] **Step 8: 테스트가 통과하는 것을 확인한다**

```bash
cd frontend && pnpm lint:fix && pnpm verify
```

기대: PASS

- [ ] **Step 9: 커밋한다**

```bash
cd .. && git add frontend
git commit -m "feat: 댓글 및 대댓글 추가

대댓글에는 답글 버튼을 노출하지 않아 서버가 400으로 거부하기 전에
막는다. 깊이 제한의 최종 판단은 여전히 도메인이 한다.

댓글은 한 번에 100건을 가져온다. 게시판 규모에서 댓글 페이지네이션은
UI만 복잡하게 만든다."
```

---

## Task 10: 좋아요

낙관적 업데이트로 즉시 반응하고, 실패하면 되돌린다.

**Files:**
- Create: `frontend/src/features/post/components/LikeButton.tsx`
- Create: `frontend/src/features/post/components/LikeButton.test.tsx`
- Modify: `frontend/src/features/post/mutations.ts`
- Modify: `frontend/src/features/post/components/PostDetail.tsx`

**Interfaces:**
- Consumes: `postKeys` (Task 6), `isProblemCode` (Task 4), `useCurrentMember` (Task 5)
- Produces:
  - `useLikePost(postId)` → `mutate()` — 성공·실패 모두 상세를 무효화한다
  - `useUnlikePost(postId)` → `mutate()`
  - `<LikeButton postId={number} likeCount={number} />`

- [ ] **Step 1: 좋아요 테스트를 쓴다**

`frontend/src/features/post/components/LikeButton.test.tsx`

```tsx
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/setup";
import { LikeButton } from "./LikeButton";

describe("LikeButton", () => {
  it("누르면 즉시 숫자가 올라간다", async () => {
    server.use(
      http.post("/api/posts/1/likes", async () => {
        await new Promise((resolve) => setTimeout(resolve, 50));
        return new HttpResponse(null, { status: 204 });
      }),
    );

    renderWithProviders(<LikeButton postId={1} likeCount={2} />);

    await userEvent.click(screen.getByRole("button", { name: /좋아요/ }));

    await waitFor(() => expect(screen.getByRole("button")).toHaveTextContent("3"));
  });

  it("이미 좋아요한 경우 안내하고 숫자를 되돌린다", async () => {
    server.use(
      http.post("/api/posts/1/likes", () =>
        HttpResponse.json(
          { status: 409, code: "ALREADY_LIKED", detail: "이미 좋아요한 게시글입니다." },
          { status: 409 },
        ),
      ),
    );

    renderWithProviders(<LikeButton postId={1} likeCount={2} />);

    await userEvent.click(screen.getByRole("button", { name: /좋아요/ }));

    await waitFor(() =>
      expect(screen.getByText("이미 좋아요한 게시글입니다.")).toBeInTheDocument(),
    );
    expect(screen.getByRole("button")).toHaveTextContent("2");
  });
});
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
cd frontend && pnpm test
```

기대: `./LikeButton` 모듈을 찾을 수 없어 실패

- [ ] **Step 3: 좋아요 훅을 추가한다**

`frontend/src/features/post/mutations.ts` 끝에 추가한다.

```ts
export function useLikePost(postId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (): Promise<void> => {
      const { error } = await client.POST("/api/posts/{id}/likes", {
        params: { path: { id: postId } },
      });
      if (error) {
        throw error;
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.detail(postId) });
    },
  });
}

export function useUnlikePost(postId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (): Promise<void> => {
      const { error } = await client.DELETE("/api/posts/{id}/likes", {
        params: { path: { id: postId } },
      });
      if (error) {
        throw error;
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.detail(postId) });
    },
  });
}
```

`onSuccess`가 아니라 `onSettled`를 쓴다. 실패했을 때도 서버의 실제 값으로 되돌려야 한다.

- [ ] **Step 4: 좋아요 버튼을 만든다**

`frontend/src/features/post/components/LikeButton.tsx`

```tsx
import { useOptimistic, useState, useTransition } from "react";
import { toProblem } from "@/api/problem";
import { Button } from "@/components/ui/button";
import { useLikePost } from "../mutations";

export function LikeButton({ postId, likeCount }: { postId: number; likeCount: number }) {
  const likePost = useLikePost(postId);
  const [message, setMessage] = useState<string | null>(null);
  const [, startTransition] = useTransition();
  const [보이는수, 낙관적으로증가] = useOptimistic(likeCount, (current: number) => current + 1);

  return (
    <div className="space-y-1">
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={() => {
          setMessage(null);
          startTransition(async () => {
            낙관적으로증가(null);
            try {
              await likePost.mutateAsync();
            } catch (error) {
              setMessage(toProblem(error)?.detail ?? "좋아요에 실패했습니다.");
            }
          });
        }}
      >
        좋아요 {보이는수}
      </Button>
      {message ? <p className="text-sm text-red-600">{message}</p> : null}
    </div>
  );
}
```

`useOptimistic`은 트랜지션이 끝나면 자동으로 원래 값으로 돌아간다. 실패 시 되돌리는 코드를 따로 쓰지 않아도 되는 이유다.

- [ ] **Step 5: 상세 화면에 붙인다**

`frontend/src/features/post/components/PostDetail.tsx`의 헤더에서 좋아요 표시를 버튼으로 바꾼다.

```tsx
        <div className="flex items-center gap-3 text-sm text-slate-500">
          <span>조회 {post.viewCount}</span>
          {member ? <LikeButton postId={post.id} likeCount={post.likeCount} /> : <span>좋아요 {post.likeCount}</span>}
        </div>
```

`import { LikeButton } from "./LikeButton";`를 추가한다.

- [ ] **Step 6: 테스트가 통과하는 것을 확인한다**

```bash
cd frontend && pnpm lint:fix && pnpm verify
```

기대: PASS

- [ ] **Step 7: 커밋한다**

```bash
cd .. && git add frontend
git commit -m "feat: 좋아요 추가

useOptimistic으로 클릭 즉시 숫자를 반영하고, 트랜지션이 끝나면
서버 값으로 정렬된다. 실패 시에도 서버 값으로 맞춰야 하므로
무효화는 onSuccess가 아니라 onSettled에서 한다.

백엔드가 중복과 미존재를 409로 구분해 주므로 안내 문구를 그대로
쓴다."
```

---
## Task 11: E2E 시나리오

실제 브라우저로 로그인부터 좋아요까지 한 번 통과시킨다. 목이 아닌 진짜 백엔드를 쓴다.

**Files:**
- Create: `frontend/playwright.config.ts`
- Create: `frontend/e2e/auth.setup.ts`
- Create: `frontend/e2e/board.spec.ts`
- Modify: `frontend/.gitignore`

**Interfaces:**
- Consumes: 실행 중인 백엔드(8080)와 Docker 컨테이너, `tester` / `tester` 계정
- Produces: `pnpm e2e` — 로그인 상태를 한 번 만들어 재사용하는 E2E 실행

- [ ] **Step 1: Playwright 브라우저를 설치한다**

```bash
cd frontend && pnpm exec playwright install chromium
```

- [ ] **Step 2: 설정을 만든다**

`frontend/playwright.config.ts`

```ts
import { defineConfig, devices } from "@playwright/test";

const 저장된로그인 = "e2e/.auth/user.json";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  reporter: "list",
  use: {
    baseURL: "http://localhost:5173",
    trace: "on-first-retry",
  },
  projects: [
    { name: "setup", testMatch: /auth\.setup\.ts/ },
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"], storageState: 저장된로그인 },
      dependencies: ["setup"],
    },
  ],
  webServer: {
    command: "pnpm dev",
    url: "http://localhost:5173",
    reuseExistingServer: true,
    timeout: 60_000,
  },
});
```

`workers: 1`인 이유는 모든 시나리오가 같은 데이터베이스를 공유하기 때문이다. 병렬로 돌리면 서로의 데이터를 밟는다.

- [ ] **Step 3: 로그인 상태를 한 번 만든다**

`frontend/e2e/auth.setup.ts`

```ts
import { expect, test as setup } from "@playwright/test";

const 저장경로 = "e2e/.auth/user.json";

setup("tester로 로그인한다", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("link", { name: "로그인" }).click();

  await page.getByLabel(/username|사용자/i).fill("tester");
  await page.getByLabel(/password|비밀번호/i).fill("tester");
  await page.getByRole("button", { name: /sign in|로그인/i }).click();

  await expect(page.getByText("tester")).toBeVisible();
  await page.context().storageState({ path: 저장경로 });
});
```

Keycloak 로그인 화면의 라벨은 언어 설정에 따라 다르므로 정규식으로 양쪽을 받는다.

- [ ] **Step 4: 주요 흐름을 시나리오로 쓴다**

`frontend/e2e/board.spec.ts`

```ts
import { expect, test } from "@playwright/test";

test("글을 쓰고 댓글과 좋아요를 남긴다", async ({ page }) => {
  const 제목 = `E2E 테스트 글 ${Date.now()}`;

  await page.goto("/");
  await page.getByRole("link", { name: "글쓰기" }).click();

  await page.getByLabel("제목").fill(제목);
  await page.getByLabel("본문").fill("E2E로 작성한 본문입니다.");
  await page.getByRole("button", { name: "저장" }).click();

  await expect(page.getByRole("heading", { name: 제목 })).toBeVisible();

  await page.getByLabel("댓글").fill("E2E 댓글");
  await page.getByRole("button", { name: "등록" }).click();
  await expect(page.getByText("E2E 댓글")).toBeVisible();

  await page.getByRole("button", { name: /좋아요/ }).click();
  await expect(page.getByRole("button", { name: /좋아요 1/ })).toBeVisible();
});

test("검색 결과가 URL에 남는다", async ({ page }) => {
  const 제목 = `검색대상 ${Date.now()}`;

  await page.goto("/posts/new");
  await page.getByLabel("제목").fill(제목);
  await page.getByLabel("본문").fill("검색을 위한 본문");
  await page.getByRole("button", { name: "저장" }).click();
  await expect(page.getByRole("heading", { name: 제목 })).toBeVisible();

  await page.goto("/");
  await page.getByLabel("검색어").fill(제목);
  await page.getByRole("button", { name: "검색" }).click();

  await expect(page).toHaveURL(/keyword=/);
  await expect(page.getByRole("link", { name: 제목 })).toBeVisible();

  await page.reload();
  await expect(page.getByRole("link", { name: 제목 })).toBeVisible();
});
```

- [ ] **Step 5: 무시 파일에 인증 상태를 추가한다**

`frontend/.gitignore`에 추가한다.

```
e2e/.auth
```

- [ ] **Step 6: 실행한다**

백엔드와 컨테이너가 떠 있어야 한다.

```bash
cd .. && docker compose up -d && ./gradlew bootRun &
sleep 40
cd frontend && pnpm e2e
```

기대: 3개 시나리오(setup 1건, 본문 2건) 모두 PASS.

로그인 화면의 라벨이 맞지 않아 실패하면, `pnpm exec playwright codegen http://localhost:8080/oauth2/authorization/keycloak`으로 실제 셀렉터를 확인해 교정한다.

- [ ] **Step 7: 커밋한다**

```bash
cd .. && git add frontend
git commit -m "test: E2E 시나리오 추가

목이 아닌 실제 백엔드와 Keycloak으로 로그인부터 좋아요까지
확인한다. 로그인은 한 번만 수행하고 저장된 상태를 재사용한다.

모든 시나리오가 같은 데이터베이스를 공유하므로 단일 워커로
실행한다. E2E는 인프라가 필요하므로 pnpm verify에는 포함하지
않는다."
```

---

## Task 12: 문서화

**Files:**
- Modify: `README.md`
- Create: `frontend/README.md`

**Interfaces:**
- Consumes: Task 1~11의 결과
- Produces: 없음

- [ ] **Step 1: 루트 README에 프론트엔드 절을 추가한다**

`README.md`의 "실행" 절 뒤에 아래 내용을 넣는다.

```markdown
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
```

"빌드와 검증" 절의 표 아래에 추가한다.

```markdown
프론트엔드는 `cd frontend && pnpm verify`가 Biome, 타입 검사, 테스트를 순서대로 실행한다. E2E는 백엔드와 컨테이너가 필요하므로 `pnpm e2e`로 따로 실행한다.
```

"구조" 절에 추가한다.

```markdown
프론트엔드는 `frontend/`에 있으며 백엔드와 같은 기능별 분리를 따른다.

```
frontend/src/
├── api/          생성된 타입, 클라이언트, ProblemDetail 파싱
├── features/     post, comment, member — 각각 queries·mutations·components
├── routes/       파일 기반 라우트
└── components/   레이아웃과 공용 UI
```
```

- [ ] **Step 2: 프론트엔드 README를 만든다**

`frontend/README.md`

```markdown
# 게시판 프론트엔드

Vite + React 19 기반 SPA. 백엔드 API는 저장소 루트의 Spring 애플리케이션이다.

## 명령

| 명령 | 용도 |
|---|---|
| `pnpm dev` | 개발 서버 (5173). 백엔드로 프록시한다 |
| `pnpm verify` | 린트 → 타입 검사 → 테스트 |
| `pnpm gen:api` | 실행 중인 백엔드에서 API 타입 재생성 |
| `pnpm e2e` | Playwright E2E (백엔드와 컨테이너 필요) |
| `pnpm build` | 타입 검사 후 프로덕션 빌드 |

## 원칙

- `src/api/schema.d.ts`는 생성물이다. 직접 수정하지 않는다
- `queryKey`는 `features/*/queries.ts`가 소유한다. 컴포넌트가 조립하지 않는다
- 검색어와 페이지는 URL에 둔다. 컴포넌트 상태로 복제하지 않는다
- 401 처리는 `src/api/client.ts`의 미들웨어 한 곳에만 있다
- MSW 핸들러도 생성된 타입을 쓴다. 목이 계약과 어긋나면 컴파일이 깨진다
```

- [ ] **Step 3: 최종 검증을 수행한다**

```bash
./gradlew clean build
cd frontend && pnpm verify
cd .. && git status --short
```

기대: 백엔드 빌드 통과, 프론트엔드 검증 통과, 커밋되지 않은 소스 변경 없음.

- [ ] **Step 4: 커밋한다**

```bash
git add README.md frontend/README.md
git commit -m "docs: 프론트엔드 실행 방법과 원칙 문서화

루트 README에 개발 서버 실행과 타입 재생성 방법을 더하고,
프론트엔드 디렉터리에 유지해야 할 원칙을 남긴다."
```

---

## 완료 기준

- `./gradlew build`가 통과한다 (백엔드 수정 포함)
- `cd frontend && pnpm verify`가 통과한다
- `pnpm e2e`가 통과한다 (백엔드와 컨테이너가 뜬 상태에서)
- `/v3/api-docs`에 `requester`, `author`, `memberId`, `viewer` 파라미터가 없다
- 브라우저에서 로그인, 글 작성, 검색, 댓글, 대댓글, 좋아요가 모두 동작한다
- 검색어와 페이지가 URL에 남아 새로고침·뒤로가기·공유가 동작한다
- 비로그인 상태에서 목록과 상세를 볼 수 있고, 쓰기 동작만 로그인으로 유도된다
