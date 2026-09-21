import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import { server } from "@/test/setup";
import { client, REDIRECT_KEY } from "./client";

afterEach(() => {
  vi.unstubAllGlobals();
  sessionStorage.clear();
});

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
    vi.stubGlobal("location", {
      origin: "http://localhost:3000",
      href: "http://localhost:3000/posts/1",
      pathname: "/posts/1",
      search: "",
    });

    await client.GET("/api/members/me");

    expect(sessionStorage.getItem(REDIRECT_KEY)).toBe("/posts/1");
    expect(window.location.href).toBe("/oauth2/authorization/keycloak");
  });

  it("오류 응답은 error로 전달된다", async () => {
    server.use(
      http.get("/api/posts/:id", () =>
        HttpResponse.json({ status: 404, code: "POST_NOT_FOUND" }, { status: 404 }),
      ),
    );

    const { error } = await client.GET("/api/posts/{id}", { params: { path: { id: 1 } } });

    expect(error).toMatchObject({ code: "POST_NOT_FOUND" });
  });
});
