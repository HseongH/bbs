import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  createMemoryHistory,
  createRootRoute,
  createRoute,
  createRouter,
  RouterProvider,
} from "@tanstack/react-router";
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

/** 재시도를 끄지 않으면 실패 경로 테스트가 느려지고 타이밍에 흔들린다. */
export function queryWrapper(): ({ children }: { children: ReactNode }) => ReactElement {
  const queryClient = createTestQueryClient();
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

/**
 * 실제 라우트 트리 대신 같은 경로 모양을 가진 최소 트리를 쓴다.
 *
 * <p>Link가 경로를 해석할 수 있어야 하므로 화면에서 쓰는 경로를 모두 등록하되, 컴포넌트는 비워 둔다.
 */
function createTestRouteTree(ui: ReactElement) {
  const rootRoute = createRootRoute({ component: () => ui });
  const 빈화면 = () => null;

  return rootRoute.addChildren([
    createRoute({ getParentRoute: () => rootRoute, path: "/", component: 빈화면 }),
    createRoute({ getParentRoute: () => rootRoute, path: "/posts/new", component: 빈화면 }),
    createRoute({ getParentRoute: () => rootRoute, path: "/posts/$postId", component: 빈화면 }),
    createRoute({
      getParentRoute: () => rootRoute,
      path: "/posts/$postId/edit",
      component: 빈화면,
    }),
  ]);
}

export function renderWithProviders(ui: ReactElement, initialPath = "/") {
  const queryClient = createTestQueryClient();
  const router = createRouter({
    routeTree: createTestRouteTree(ui),
    history: createMemoryHistory({ initialEntries: [initialPath] }),
  });

  return render(
    <QueryClientProvider client={queryClient}>
      {/* biome-ignore lint/suspicious/noExplicitAny: 테스트 전용 라우터는 앱 라우터 타입과 다르다 */}
      <RouterProvider router={router as any} />
    </QueryClientProvider>,
  );
}
