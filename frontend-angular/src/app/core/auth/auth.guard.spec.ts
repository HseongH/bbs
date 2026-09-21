import { provideHttpClient, withInterceptors } from "@angular/common/http";

import { TestBed } from "@angular/core/testing";
import { http, HttpResponse } from "msw";
import { server } from "@test/setup";
import { authGuard } from "./auth.guard";
import { authInterceptor, REDIRECT_KEY } from "./auth.interceptor";

describe("authGuard", () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.stubGlobal("location", {
      origin: "http://localhost:5173",
      href: "http://localhost:5173/posts/new",
      pathname: "/posts/new",
      search: "",
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("회원 조회가 끝날 때까지 기다린 뒤 통과시킨다", async () => {
    server.use(
      http.get("/api/members/me", async () => {
        await new Promise((resolve) => setTimeout(resolve, 80));
        return HttpResponse.json({ id: 1, nickname: "테스터", email: "t@example.com" });
      }),
    );
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor]))],
    });

    const allowed = await TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    expect(allowed).toBe(true);
    expect(sessionStorage.getItem(REDIRECT_KEY)).toBeNull();
  });

  it("미인증이면 막고 로그인으로 보낸다", async () => {
    server.use(
      http.get("/api/members/me", () =>
        HttpResponse.json({ status: 401, code: "UNAUTHENTICATED" }, { status: 401 }),
      ),
    );
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor]))],
    });

    const allowed = await TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    expect(allowed).toBe(false);
    expect(sessionStorage.getItem(REDIRECT_KEY)).toBe("/posts/new");
  });
});
