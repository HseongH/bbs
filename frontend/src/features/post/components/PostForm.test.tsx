import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/setup";
import { PostForm } from "./PostForm";

describe("PostForm", () => {
  it("입력한 값을 본문으로 전송한다", async () => {
    let 받은본문: unknown = null;
    server.use(
      http.post("/api/posts", async ({ request }) => {
        받은본문 = await request.json();
        return new HttpResponse(null, { status: 201, headers: { Location: "/api/posts/7" } });
      }),
    );

    renderWithProviders(<PostForm mode="create" />);

    await userEvent.type(await screen.findByLabelText("제목"), "새 글");
    await userEvent.type(await screen.findByLabelText("본문"), "내용입니다");
    await userEvent.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() => expect(받은본문).toEqual({ title: "새 글", content: "내용입니다" }));
  });

  it("서버 검증 오류를 필드 아래에 보여준다", async () => {
    server.use(
      http.post("/api/posts", () =>
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

    renderWithProviders(<PostForm mode="create" />);

    await userEvent.type(await screen.findByLabelText("본문"), "내용");
    await userEvent.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() => expect(screen.getByText("제목은 필수입니다.")).toBeInTheDocument());
  });

  it("수정 모드에서는 기존 값이 채워진다", async () => {
    renderWithProviders(
      <PostForm mode="edit" postId={1} initial={{ title: "기존 제목", content: "기존 본문" }} />,
    );

    expect(await screen.findByLabelText("제목")).toHaveValue("기존 제목");
    expect(await screen.findByLabelText("본문")).toHaveValue("기존 본문");
  });
});
