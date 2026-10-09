import { expect, test as setup } from "@playwright/test";

const 저장경로 = "e2e/.auth/user.json";

setup("tester로 로그인한다", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("link", { name: "로그인" }).click();

  // Keycloak 로그인 화면은 비밀번호 표시 토글도 같은 라벨을 쓰므로 입력 요소를 id로 지목한다.
  await page.locator("#username").fill("tester");
  await page.locator("#password").fill("tester");
  await page.locator("#kc-login").click();

  // 목록의 작성자 이름과 구분해야 하므로 헤더 안에서 확인한다.
  await expect(page.getByRole("banner").getByText("tester")).toBeVisible();
  await page.context().storageState({ path: 저장경로 });
});
