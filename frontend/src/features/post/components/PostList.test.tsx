import { screen, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";
import { 게시글요약, 게시글페이지 } from "@/test/handlers";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/setup";
import { PostList } from "./PostList";

describe("PostList", () => {
  it("게시글 제목과 작성자를 보여준다", async () => {
    server.use(
      http.get("/api/posts", () =>
        HttpResponse.json(
          게시글페이지([
            게시글요약({ id: 1, title: "첫 글", authorNickname: "작성자" }),
            게시글요약({ id: 2, title: "둘째 글", authorNickname: "작성자" }),
          ]),
        ),
      ),
    );

    renderWithProviders(<PostList search={{ page: 0, size: 20 }} />);

    await waitFor(() => expect(screen.getByText("첫 글")).toBeInTheDocument());
    expect(screen.getByText("둘째 글")).toBeInTheDocument();
    expect(screen.getAllByText("작성자")).toHaveLength(2);
  });

  it("결과가 없으면 안내를 보여준다", async () => {
    server.use(http.get("/api/posts", () => HttpResponse.json(게시글페이지([]))));

    renderWithProviders(<PostList search={{ page: 0, size: 20 }} />);

    await waitFor(() => expect(screen.getByText(/게시글이 없습니다/)).toBeInTheDocument());
  });

  it("검색어를 쿼리로 전달한다", async () => {
    let 받은키워드: string | null = null;
    server.use(
      http.get("/api/posts", ({ request }) => {
        받은키워드 = new URL(request.url).searchParams.get("keyword");
        return HttpResponse.json(게시글페이지([게시글요약({ title: "스프링 입문" })]));
      }),
    );

    renderWithProviders(<PostList search={{ page: 0, size: 20, keyword: "스프링" }} />);

    await waitFor(() => expect(받은키워드).toBe("스프링"));
  });
});
