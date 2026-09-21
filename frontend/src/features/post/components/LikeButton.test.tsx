import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/setup";
import { LikeButton } from "./LikeButton";

describe("LikeButton", () => {
  it("누르면 즉시 숫자가 올라간다", async () => {
    server.use(
      http.post("/api/posts/:id/likes", async () => {
        await new Promise((resolve) => setTimeout(resolve, 50));
        return new HttpResponse(null, { status: 204 });
      }),
    );

    renderWithProviders(<LikeButton postId={1} likeCount={2} />);

    await userEvent.click(await screen.findByRole("button", { name: /좋아요/ }));

    await waitFor(() => expect(screen.getByRole("button")).toHaveTextContent("3"));
  });

  it("이미 좋아요한 경우 안내하고 숫자를 되돌린다", async () => {
    server.use(
      http.post("/api/posts/:id/likes", () =>
        HttpResponse.json(
          { status: 409, code: "ALREADY_LIKED", detail: "이미 좋아요한 게시글입니다." },
          { status: 409 },
        ),
      ),
    );

    renderWithProviders(<LikeButton postId={1} likeCount={2} />);

    await userEvent.click(await screen.findByRole("button", { name: /좋아요/ }));

    await waitFor(() =>
      expect(screen.getByText("이미 좋아요한 게시글입니다.")).toBeInTheDocument(),
    );
    expect(screen.getByRole("button")).toHaveTextContent("2");
  });
});
