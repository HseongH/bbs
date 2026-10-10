import { provideHttpClient } from "@angular/common/http";
import { provideRouter } from "@angular/router";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import type { components } from "@/core/api/schema";
import { server } from "@test/setup";
import { PostEditPage } from "./post-edit-page";

type PostResponse = components["schemas"]["PostResponse"];

const 게시글: PostResponse = {
  id: 1,
  title: "제목",
  content: "본문입니다.",
  authorId: 1,
  viewCount: 0,
  likeCount: 0,
  createdAt: "2026-09-21T00:00:00Z",
};

async function 화면을_그린다() {
  return render(PostEditPage, {
    inputs: { postId: "1" },
    providers: [provideHttpClient(), provideRouter([])],
  });
}

describe("PostEditPage", () => {
  it("게시글을 불러오지 못하면 안내를 보여준다", async () => {
    server.use(
      http.get("/api/posts/:postId", () =>
        HttpResponse.json({ status: 404, code: "POST_NOT_FOUND" }, { status: 404 }),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByText(/불러오지 못했습니다/)).toBeInTheDocument();
  });

  it("저장이 검증 오류로 실패하면 입력란 아래에 서버 메시지를 보여준다", async () => {
    server.use(
      http.get("/api/posts/:postId", () => HttpResponse.json(게시글)),
      http.patch("/api/posts/:postId", () =>
        HttpResponse.json(
          {
            status: 400,
            code: "INVALID_REQUEST",
            detail: "요청 값이 올바르지 않습니다.",
            errors: { title: "제목은 필수입니다." },
          },
          { status: 400 },
        ),
      ),
    );
    await 화면을_그린다();

    await userEvent.click(await screen.findByRole("button", { name: "저장" }));

    expect(await screen.findByText("제목은 필수입니다.")).toBeInTheDocument();
  });
});
