import { expect, test } from "@playwright/test";

// 로그아웃은 세션을 무효화하므로 공유 로그인 상태를 쓰지 않고 자체 세션으로 진행한다.
test.use({ storageState: { cookies: [], origins: [] } });

test("로그아웃하면 비로그인 상태로 돌아간다", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("link", { name: "로그인" }).click();
  await page.locator("#username").fill("tester");
  await page.locator("#password").fill("tester");
  await page.locator("#kc-login").click();

  await expect(page.getByRole("banner").getByText("tester")).toBeVisible();

  await page.getByRole("button", { name: "로그아웃" }).click();

  await expect(page.getByRole("banner").getByRole("link", { name: "로그인" })).toBeVisible();
  await expect(page.getByRole("banner").getByText("tester")).toBeHidden();

  // 비로그인 상태에서도 목록은 볼 수 있어야 한다.
  await expect(page.getByRole("link", { name: "글쓰기" })).toBeVisible();
});
