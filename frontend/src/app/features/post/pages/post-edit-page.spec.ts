import { provideHttpClient } from "@angular/common/http";
import { provideRouter } from "@angular/router";
import { render, screen } from "@testing-library/angular";
import { http, HttpResponse } from "msw";
import { server } from "@test/setup";
import { PostEditPage } from "./post-edit-page";

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
});
