import { expect, test } from "@playwright/test";

test("글을 쓰고 댓글과 좋아요를 남긴다", async ({ page }) => {
  const 제목 = `E2E 테스트 글 ${Date.now()}`;

  await page.goto("/");
  await page.getByRole("link", { name: "글쓰기" }).click();

  await page.getByLabel("제목").fill(제목);
  await page.getByLabel("본문").fill("E2E로 작성한 본문입니다.");
  await page.getByRole("button", { name: "저장" }).click();

  await expect(page.getByRole("heading", { name: 제목 })).toBeVisible();

  await page.getByLabel("댓글").fill("E2E 댓글");
  await page.getByRole("button", { name: "등록" }).click();
  await expect(page.getByText("E2E 댓글")).toBeVisible();

  await page.getByRole("button", { name: /좋아요/ }).click();
  await expect(page.getByRole("button", { name: /좋아요 1/ })).toBeVisible();
});

test("검색 결과가 URL에 남는다", async ({ page }) => {
  const 제목 = `검색대상 ${Date.now()}`;

  await page.goto("/posts/new");
  await page.getByLabel("제목").fill(제목);
  await page.getByLabel("본문").fill("검색을 위한 본문");
  await page.getByRole("button", { name: "저장" }).click();
  await expect(page.getByRole("heading", { name: 제목 })).toBeVisible();

  await page.goto("/");
  await page.getByLabel("검색어").fill(제목);
  await page.getByRole("button", { name: "검색" }).click();

  await expect(page).toHaveURL(/keyword=/);
  await expect(page.getByRole("link", { name: 제목 })).toBeVisible();

  await page.reload();
  await expect(page.getByRole("link", { name: 제목 })).toBeVisible();
});
