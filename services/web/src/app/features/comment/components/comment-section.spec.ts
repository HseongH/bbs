import { provideHttpClient } from "@angular/common/http";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import type { components } from "@/core/api/schema";
import { server } from "@test/setup";
import { CommentSectionComponent } from "./comment-section";

type CommentResponse = components["schemas"]["CommentResponse"];
type CommentThreadResponse = components["schemas"]["CommentThreadResponse"];
type CommentPage = components["schemas"]["PageResponseCommentThreadResponse"];

function 댓글(overrides: Partial<CommentResponse> = {}): CommentResponse {
  return {
    id: 1,
    postId: 1,
    authorId: 1,
    body: "댓글입니다",
    deleted: false,
    depth: 0,
    createdAt: "2026-09-21T00:00:00Z",
    ...overrides,
  };
}

function 묶음(root: CommentResponse, replies: CommentResponse[] = []): CommentThreadResponse {
  return { root, replies };
}

function 댓글페이지(threads: CommentThreadResponse[]): CommentPage {
  return {
    content: threads,
    page: 0,
    size: 20,
    totalElements: threads.length,
    totalPages: 1,
    last: true,
  };
}

/** 삭제된 댓글은 서버가 본문과 작성자를 보내지 않는다. */
function 삭제된댓글(id: number): CommentResponse {
  return { id, postId: 1, deleted: true, depth: 0, createdAt: "2026-09-21T00:00:00Z" };
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
            묶음(댓글({ id: 1, body: "원댓글" }), [
              댓글({ id: 2, body: "답글", depth: 1, parentCommentId: 1 }),
            ]),
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
          댓글페이지([
            묶음(댓글({ id: 1, authorId: 999, body: "원댓글" }), [
              댓글({ id: 2, authorId: 999, body: "답글", depth: 1, parentCommentId: 1 }),
            ]),
          ]),
        ),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByText("답글")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "답글 달기" })).toHaveLength(1);
  });

  it("작성자 본인에게만 수정과 삭제를 보여준다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(댓글페이지([묶음(댓글({ id: 1, authorId: 999, body: "남의 댓글" }))])),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByText("남의 댓글")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "삭제" })).not.toBeInTheDocument();
  });

  it("대댓글은 자기 원댓글 바로 아래에 보여준다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(
          댓글페이지([
            묶음(댓글({ id: 1, body: "원댓글1" }), [
              댓글({ id: 3, body: "답글1", depth: 1, parentCommentId: 1 }),
            ]),
            묶음(댓글({ id: 2, body: "원댓글2" })),
          ]),
        ),
      ),
    );

    await 화면을_그린다();

    await screen.findByText("원댓글1");
    const 본문들 = screen
      .getAllByText(/^(원댓글1|답글1|원댓글2)$/)
      .map((element) => element.textContent);
    expect(본문들).toEqual(["원댓글1", "답글1", "원댓글2"]);
  });

  it("삭제된 원댓글은 자리만 표시하고 버튼을 보여주지 않는다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(
          댓글페이지([
            묶음(삭제된댓글(1), [
              댓글({ id: 2, authorId: 999, body: "답글", depth: 1, parentCommentId: 1 }),
            ]),
          ]),
        ),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByText("삭제된 댓글입니다")).toBeInTheDocument();
    expect(screen.getByText("답글")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "답글 달기" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "수정" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "삭제" })).not.toBeInTheDocument();
  });

  it("댓글 수는 화면에 보이는 댓글만 센다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(
          댓글페이지([
            묶음(삭제된댓글(1), [
              댓글({ id: 2, body: "답글1", depth: 1, parentCommentId: 1 }),
              댓글({ id: 4, body: "답글2", depth: 1, parentCommentId: 1 }),
            ]),
            묶음(댓글({ id: 3, body: "원댓글" })),
          ]),
        ),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByRole("heading", { name: "댓글 3" })).toBeInTheDocument();
  });

  it("댓글을 불러오지 못하면 안내를 보여준다", async () => {
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json({ status: 500, code: "INTERNAL_ERROR" }, { status: 500 }),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByText("댓글을 불러오지 못했습니다.")).toBeInTheDocument();
  });

  it("댓글을 수정하면 기존 본문을 채우고 고친 본문을 전송한다", async () => {
    let 받은본문: unknown = null;
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(댓글페이지([묶음(댓글({ id: 7, body: "원래 본문" }))])),
      ),
      http.patch("/api/comments/:id", async ({ request }) => {
        받은본문 = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );

    await 화면을_그린다();

    await userEvent.click(await screen.findByRole("button", { name: "수정" }));
    const 입력란 = screen.getByLabelText("댓글 수정");
    expect(입력란).toHaveValue("원래 본문");
    await userEvent.clear(입력란);
    await userEvent.type(입력란, "고친 본문");
    await userEvent.click(within(입력란.closest("form")!).getByRole("button", { name: "수정" }));

    await vi.waitFor(() => expect(받은본문).toEqual({ body: "고친 본문" }));
  });

  it("답글을 등록하면 원댓글 식별자와 함께 전송한다", async () => {
    let 받은본문: unknown = null;
    server.use(
      http.get("/api/posts/:postId/comments", () =>
        HttpResponse.json(댓글페이지([묶음(댓글({ id: 7 }))])),
      ),
      http.post("/api/posts/:postId/comments", async ({ request }) => {
        받은본문 = await request.json();
        return new HttpResponse(null, { status: 201 });
      }),
    );

    await 화면을_그린다();

    await userEvent.click(await screen.findByRole("button", { name: "답글 달기" }));
    const 답글란 = screen.getByLabelText("답글");
    await userEvent.type(답글란, "답글입니다");
    await userEvent.click(within(답글란.closest("form")!).getByRole("button", { name: "등록" }));

    await vi.waitFor(() => expect(받은본문).toEqual({ body: "답글입니다", parentCommentId: 7 }));
  });
});
