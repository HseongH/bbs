# 게시판 프론트엔드

Angular 22 기반 SPA. 백엔드 API는 저장소 루트의 Spring 애플리케이션이다.

## 명령

| 명령           | 용도                                                         |
| -------------- | ------------------------------------------------------------ |
| `pnpm dev`     | 개발 서버 (5173). 백엔드로 프록시한다                        |
| `pnpm verify`  | 린트 → 타입 검사 → 테스트                                    |
| `pnpm gen:api` | 실행 중인 백엔드에서 API 타입 재생성                         |
| `pnpm e2e`     | Playwright E2E (백엔드와 컨테이너 필요). `BBS_HOST`를 따른다 |
| `pnpm build`   | 프로덕션 빌드                                                |

## 구조

```
src/app/
├── core/
│   ├── api/      생성된 타입과 ProblemDetail 파싱
│   └── auth/     인증 인터셉터, 라우트 가드, 현재 회원 스토어
├── features/     post, comment — 각각 api.service·store·components
├── shared/ui/    공용 UI
└── app.routes.ts
```

기능마다 두 계층으로 나뉜다. `*-api.service.ts`는 HTTP 호출만 하고, `*.store.ts`가 `httpResource`를 소유하며 언제 다시 읽을지를 정한다. 컴포넌트는 스토어만 주입받는다.

## 원칙

- `src/app/core/api/schema.d.ts`는 생성물이다. 직접 수정하지 않는다
- 재조회(`reload()`)는 `*.store.ts`에서만 호출한다. 컴포넌트가 직접 부르면 무효화 범위가 흩어진다
- 검색어와 페이지는 URL에 둔다. 컴포넌트 상태로 복제하지 않는다
- 401 처리는 `core/auth/auth.interceptor.ts` 한 곳에만 있다. 401이 정상 응답인 요청은 `SKIP_LOGIN_REDIRECT` 토큰을 단다
- CSRF는 `withXsrfConfiguration`이 처리한다. 헤더를 직접 붙이지 않는다
- MSW 핸들러도 생성된 타입을 쓴다. 목이 계약과 어긋나면 컴파일이 깨진다
- 템플릿에서 참조하는 멤버 이름은 ASCII로 둔다. Angular 템플릿 파서가 한글 식별자를 읽지 못한다

## 알아둘 점

개발 서버는 5173을 쓴다. Angular CLI 기본값은 4200이지만 Keycloak realm에 등록된 주소가 5173이라 바꾸면 로그인이 깨진다.

TypeScript는 6.x를 쓴다. 7은 컴파일러 API를 다시 구현해 `openapi-typescript`가 동작하지 않는다.

단위 테스트는 `isolate: true`로 돈다. Angular CLI의 `unit-test` 빌더 기본값은 `false`인데, 그러면 파일 사이로 상태가 새어 실패가 엉뚱한 파일에서 나타난다.
