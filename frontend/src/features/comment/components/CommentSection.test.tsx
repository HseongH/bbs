import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";
import type { components } from "@/api/schema";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/setup";
import { CommentSection } from "./CommentSection";

type CommentResponse = components["schemas"]["CommentResponse"];
type CommentPage = components["schemas"]["PageResponseCommentResponse"];

function 댓글(overrides: Partial<CommentResponse> = {}): CommentResponse {
  return {
    id: 1,
    postId: 1,
    authorId: 1,
    body: "댓글입니다",
    depth: 0,
    createdAt: "2026-09-21T00:00:00Z",
    ...overrides,
  };
}

function 댓글페이지(content: CommentResponse[]): CommentPage {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1, last: true };
}

describe("CommentSection", () => {
  it("댓글과 대댓글을 함께 보여준다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(
          댓글페이지([
            댓글({ id: 1, body: "원댓글", depth: 0 }),
            댓글({ id: 2, body: "답글", depth: 1, parentCommentId: 1 }),
          ]),
        ),
      ),
    );

    renderWithProviders(<CommentSection postId={1} />);

    await waitFor(() => expect(screen.getByText("원댓글")).toBeInTheDocument());
    expect(screen.getByText("답글")).toBeInTheDocument();
  });

  it("댓글을 작성하면 본문을 전송한다", async () => {
    let 받은본문: unknown = null;
    server.use(
      http.get("/api/posts/:postId/comments", () => HttpResponse.json(댓글페이지([]))),
      http.post("/api/posts/:postId/comments", async ({ request }) => {
        받은본문 = await request.json();
        return new HttpResponse(null, { status: 201, headers: { Location: "/api/comments/5" } });
      }),
    );

    renderWithProviders(<CommentSection postId={1} />);

    await userEvent.type(await screen.findByLabelText("댓글"), "새 댓글");
    await userEvent.click(screen.getByRole("button", { name: "등록" }));

    await waitFor(() => expect(받은본문).toEqual({ body: "새 댓글" }));
  });

  it("대댓글에는 답글 버튼을 보여주지 않는다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(
          댓글페이지([댓글({ id: 2, body: "답글", depth: 1, parentCommentId: 1 })]),
        ),
      ),
    );

    renderWithProviders(<CommentSection postId={1} />);

    await waitFor(() => expect(screen.getByText("답글")).toBeInTheDocument());
    expect(screen.queryByRole("button", { name: "답글 달기" })).not.toBeInTheDocument();
  });

  it("작성자 본인에게만 수정과 삭제를 보여준다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(댓글페이지([댓글({ id: 1, authorId: 999, body: "남의 댓글" })])),
      ),
    );

    renderWithProviders(<CommentSection postId={1} />);

    await waitFor(() => expect(screen.getByText("남의 댓글")).toBeInTheDocument());
    expect(screen.queryByRole("button", { name: "삭제" })).not.toBeInTheDocument();
  });
});
