import { provideHttpClient } from "@angular/common/http";
import { provideRouter } from "@angular/router";
import { render, screen } from "@testing-library/angular";
import { http, HttpResponse } from "msw";
import type { components } from "@/core/api/schema";
import { server } from "@test/setup";
import { PostDetailPage } from "./post-detail-page";

type PostResponse = components["schemas"]["PostResponse"];

function 게시글(overrides: Partial<PostResponse> = {}): PostResponse {
  return {
    id: 1,
    title: "제목",
    content: "본문입니다.",
    authorId: 1,
    viewCount: 3,
    likeCount: 2,
    createdAt: "2026-09-21T00:00:00Z",
    ...overrides,
  };
}

async function 화면을_그린다(postId = "1") {
  return render(PostDetailPage, {
    inputs: { postId },
    providers: [provideHttpClient(), provideRouter([])],
  });
}

describe("PostDetailPage", () => {
  it("제목과 본문을 보여준다", async () => {
    server.use(http.get("/api/posts/:id", () => HttpResponse.json(게시글())));

    await 화면을_그린다();

    expect(await screen.findByRole("heading", { name: "제목" })).toBeInTheDocument();
    expect(screen.getByText("본문입니다.")).toBeInTheDocument();
  });

  it("작성자 본인에게만 수정과 삭제를 보여준다", async () => {
    server.use(http.get("/api/posts/:id", () => HttpResponse.json(게시글({ authorId: 1 }))));

    await 화면을_그린다();

    expect(await screen.findByRole("link", { name: "수정" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "삭제" })).toBeInTheDocument();
  });

  it("다른 사람 글에는 수정과 삭제를 보여주지 않는다", async () => {
    server.use(http.get("/api/posts/:id", () => HttpResponse.json(게시글({ authorId: 999 }))));

    await 화면을_그린다();

    expect(await screen.findByRole("heading", { name: "제목" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "수정" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "삭제" })).not.toBeInTheDocument();
  });

  it("없는 글이면 안내를 보여준다", async () => {
    server.use(
      http.get("/api/posts/:id", () =>
        HttpResponse.json({ status: 404, code: "POST_NOT_FOUND" }, { status: 404 }),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByText(/게시글을 찾을 수 없습니다/)).toBeInTheDocument();
  });
});
