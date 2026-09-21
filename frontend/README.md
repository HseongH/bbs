# 게시판 프론트엔드

Vite + React 19 기반 SPA. 백엔드 API는 저장소 루트의 Spring 애플리케이션이다.

## 명령

| 명령 | 용도 |
|---|---|
| `pnpm dev` | 개발 서버 (5173). 백엔드로 프록시한다. 외부 접속은 `pnpm dev --host` |
| `pnpm verify` | 린트 → 타입 검사 → 테스트 |
| `pnpm gen:api` | 실행 중인 백엔드에서 API 타입 재생성 |
| `pnpm e2e` | Playwright E2E (백엔드와 컨테이너 필요). `BBS_HOST`를 따른다 |
| `pnpm build` | 타입 검사 후 프로덕션 빌드 |

## 원칙

- `src/api/schema.d.ts`는 생성물이다. 직접 수정하지 않는다
- `queryKey`는 `features/*/queries.ts`가 소유한다. 컴포넌트가 조립하지 않는다
- 검색어와 페이지는 URL에 둔다. 컴포넌트 상태로 복제하지 않는다
- 401 처리는 `src/api/client.ts`의 미들웨어 한 곳에만 있다
- MSW 핸들러도 생성된 타입을 쓴다. 목이 계약과 어긋나면 컴파일이 깨진다

## 알아둘 점

TypeScript는 6.x를 쓴다. 7은 컴파일러 API를 다시 구현해 `openapi-typescript`가 동작하지 않는다.

테스트에서 컴포넌트를 렌더링할 때는 `findBy*`를 쓴다. 테스트용 메모리 라우터가 마운트되는 데 한 틱이 걸려 동기 조회는 빈 DOM을 본다.
