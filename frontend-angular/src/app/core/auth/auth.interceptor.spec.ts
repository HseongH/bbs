import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
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
    http.get("/api/posts").subscribe({ error: () => undefined });
    controller.expectOne("/api/posts").flush(null, { status: 401, statusText: "Unauthorized" });

    expect(sessionStorage.getItem(REDIRECT_KEY)).toBe("/posts/1");
    expect(window.location.href).toBe("/oauth2/authorization/keycloak");
  });

  it("토큰이 붙은 요청은 401이어도 로그인으로 보내지 않는다", () => {
    const context = new HttpContext().set(SKIP_LOGIN_REDIRECT, true);

    http.get("/api/members/me", { context }).subscribe({ error: () => undefined });
    controller
      .expectOne("/api/members/me")
      .flush(null, { status: 401, statusText: "Unauthorized" });

    expect(sessionStorage.getItem(REDIRECT_KEY)).toBeNull();
  });

  it("401이 아닌 오류는 건드리지 않는다", () => {
    http.get("/api/posts/1").subscribe({ error: () => undefined });
    controller.expectOne("/api/posts/1").flush(null, { status: 404, statusText: "Not Found" });

    expect(sessionStorage.getItem(REDIRECT_KEY)).toBeNull();
  });
});
