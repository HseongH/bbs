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
