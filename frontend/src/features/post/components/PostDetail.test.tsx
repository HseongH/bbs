import { screen, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";
import type { components } from "@/api/schema";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/setup";
import { PostDetail } from "./PostDetail";

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

describe("PostDetail", () => {
  it("제목과 본문을 보여준다", async () => {
    server.use(http.get("/api/posts/:id", () => HttpResponse.json(게시글())));

    renderWithProviders(<PostDetail postId={1} />);

    await waitFor(() => expect(screen.getByRole("heading", { name: "제목" })).toBeInTheDocument());
    expect(screen.getByText("본문입니다.")).toBeInTheDocument();
  });

  it("작성자 본인에게만 수정과 삭제를 보여준다", async () => {
    server.use(http.get("/api/posts/:id", () => HttpResponse.json(게시글({ authorId: 1 }))));

    renderWithProviders(<PostDetail postId={1} />);

    await waitFor(() => expect(screen.getByRole("link", { name: "수정" })).toBeInTheDocument());
    expect(screen.getByRole("button", { name: "삭제" })).toBeInTheDocument();
  });

  it("다른 사람 글에는 수정과 삭제를 보여주지 않는다", async () => {
    server.use(http.get("/api/posts/:id", () => HttpResponse.json(게시글({ authorId: 999 }))));

    renderWithProviders(<PostDetail postId={1} />);

    await waitFor(() => expect(screen.getByRole("heading", { name: "제목" })).toBeInTheDocument());
    expect(screen.queryByRole("link", { name: "수정" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "삭제" })).not.toBeInTheDocument();
  });

  it("없는 글이면 안내를 보여준다", async () => {
    server.use(
      http.get("/api/posts/:id", () =>
        HttpResponse.json(
          { status: 404, code: "POST_NOT_FOUND", detail: "게시글을 찾을 수 없습니다." },
          { status: 404 },
        ),
      ),
    );

    renderWithProviders(<PostDetail postId={1} />);

    await waitFor(() => expect(screen.getByText(/게시글을 찾을 수 없습니다/)).toBeInTheDocument());
  });
});
