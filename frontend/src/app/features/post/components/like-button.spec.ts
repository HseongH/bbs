import { provideHttpClient } from "@angular/common/http";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { server } from "@test/setup";
import { LikeButtonComponent } from "./like-button";

describe("LikeButtonComponent", () => {
  it("누르면 숫자가 올라간다", async () => {
    server.use(http.post("/api/posts/:id/likes", () => new HttpResponse(null, { status: 204 })));

    await render(LikeButtonComponent, {
      inputs: { postId: 1, likeCount: 2 },
      providers: [provideHttpClient()],
    });

    await userEvent.click(screen.getByRole("button", { name: /좋아요/ }));

    await vi.waitFor(() =>
      expect(screen.getByRole("button", { name: /좋아요 3/ })).toBeInTheDocument(),
    );
  });

  it("이미 좋아요한 경우 안내한다", async () => {
    server.use(
      http.post("/api/posts/:id/likes", () =>
        HttpResponse.json(
          { status: 409, code: "ALREADY_LIKED", detail: "이미 좋아요한 게시글입니다." },
          { status: 409 },
        ),
      ),
    );

    await render(LikeButtonComponent, {
      inputs: { postId: 1, likeCount: 2 },
      providers: [provideHttpClient()],
    });

    await userEvent.click(screen.getByRole("button", { name: /좋아요/ }));

    expect(await screen.findByText("이미 좋아요한 게시글입니다.")).toBeInTheDocument();
  });
});
