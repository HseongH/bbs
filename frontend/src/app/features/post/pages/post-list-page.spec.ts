import { provideHttpClient } from "@angular/common/http";
import { provideRouter } from "@angular/router";
import { render, screen } from "@testing-library/angular";
import { http, HttpResponse } from "msw";
import { 게시글요약, 게시글페이지 } from "@test/handlers";
import { server } from "@test/setup";
import { PostListPage } from "./post-list-page";

async function 화면을_그린다() {
  return render(PostListPage, { providers: [provideHttpClient(), provideRouter([])] });
}

describe("PostListPage", () => {
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

    await 화면을_그린다();

    expect(await screen.findByText("첫 글")).toBeInTheDocument();
    expect(screen.getByText("둘째 글")).toBeInTheDocument();
  });

  it("결과가 없으면 안내를 보여준다", async () => {
    server.use(http.get("/api/posts", () => HttpResponse.json(게시글페이지([]))));

    await 화면을_그린다();

    expect(await screen.findByText(/게시글이 없습니다/)).toBeInTheDocument();
  });
});
