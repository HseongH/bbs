import { provideHttpClient } from "@angular/common/http";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import type { components } from "@/core/api/schema";
import { server } from "@test/setup";
import { CommentSectionComponent } from "./comment-section";

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

async function 화면을_그린다() {
  return render(CommentSectionComponent, {
    inputs: { postId: 1 },
    providers: [provideHttpClient()],
  });
}

describe("CommentSectionComponent", () => {
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

    await 화면을_그린다();

    expect(await screen.findByText("원댓글")).toBeInTheDocument();
    expect(screen.getByText("답글")).toBeInTheDocument();
  });

  it("댓글을 작성하면 본문을 전송한다", async () => {
    let 받은본문: unknown = null;
    server.use(
      http.get("/api/posts/:postId/comments", () => HttpResponse.json(댓글페이지([]))),
      http.post("/api/posts/:postId/comments", async ({ request }) => {
        받은본문 = await request.json();
        return new HttpResponse(null, { status: 201 });
      }),
    );

    await 화면을_그린다();

    await userEvent.type(await screen.findByLabelText("댓글"), "새 댓글");
    await userEvent.click(screen.getByRole("button", { name: "등록" }));

    await vi.waitFor(() => expect(받은본문).toEqual({ body: "새 댓글" }));
  });

  it("댓글을 등록하면 입력란을 비운다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () => HttpResponse.json(댓글페이지([]))),
      http.post("/api/posts/:postId/comments", () => new HttpResponse(null, { status: 201 })),
    );

    await 화면을_그린다();

    const 입력란 = await screen.findByLabelText("댓글");
    await userEvent.type(입력란, "새 댓글");
    await userEvent.click(screen.getByRole("button", { name: "등록" }));

    await vi.waitFor(() => expect(입력란).toHaveValue(""));
  });

  it("대댓글에는 답글 버튼을 보여주지 않는다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(
          댓글페이지([댓글({ id: 2, body: "답글", depth: 1, parentCommentId: 1 })]),
        ),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByText("답글")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "답글 달기" })).not.toBeInTheDocument();
  });

  it("작성자 본인에게만 수정과 삭제를 보여준다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(댓글페이지([댓글({ id: 1, authorId: 999, body: "남의 댓글" })])),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByText("남의 댓글")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "삭제" })).not.toBeInTheDocument();
  });
});
