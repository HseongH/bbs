import { expect, test } from "@playwright/test";

// 진입점(Traefik)이 요청을 어디로 보내고 무엇을 막는지 확인한다 (ADR-0016).

test.describe("비로그인", () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test("브라우저가 보낸 Authorization 헤더는 board에 닿지 않는다", async ({ request }) => {
    // 헤더가 board에 닿았다면 board가 서명 검증에 실패해 401을 돌려준다.
    const response = await request.get("/api/posts", {
      headers: { Authorization: "Bearer forged" },
    });

    expect(response.status()).toBe(200);
  });

  test("비로그인 쓰기는 진입점에서 401 ProblemDetail이다", async ({ request }) => {
    // 실제 화면처럼 조회 응답에서 받은 CSRF 쿠키를 헤더로 되돌려 보낸다.
    await request.get("/api/posts");
    const csrf = (await request.storageState()).cookies.find((c) => c.name === "XSRF-TOKEN");
    expect(csrf).toBeDefined();

    const response = await request.post("/api/posts", {
      headers: { "X-XSRF-TOKEN": csrf?.value ?? "" },
      data: { title: "제목", content: "본문" },
    });

    expect(response.status()).toBe(401);
    expect(response.headers()["content-type"]).toContain("application/problem+json");
    expect((await response.json()).code).toBe("UNAUTHENTICATED");
  });

  test("액추에이터와 공개키는 진입점으로 열리지 않는다", async ({ request }) => {
    for (const path of ["/actuator/health", "/actuator/metrics", "/.well-known/jwks.json"]) {
      const body = await (await request.get(path)).text();

      expect(body, path).not.toContain('"kty"');
      expect(body, path).not.toContain('"status":"UP"');
      expect(body, path).not.toContain('"names"');
    }
  });

  test("화면 경로로 직접 들어오면 화면을 돌려준다", async ({ page }) => {
    await page.goto("/posts/1");

    await expect(page.getByRole("banner")).toBeVisible();
  });
});

test("없는 API 경로는 404 ProblemDetail이다", async ({ request }) => {
  // 로그인 상태여야 auth의 판정을 통과해 board까지 간다.
  const response = await request.get("/api/does-not-exist");

  expect(response.status()).toBe(404);
  expect(response.headers()["content-type"]).toContain("application/problem+json");
});
