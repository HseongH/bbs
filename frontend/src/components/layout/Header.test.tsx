import { screen, waitFor } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "@/test/render";
import { server } from "@/test/setup";
import { Header } from "./Header";

describe("Header", () => {
  it("로그인 상태면 닉네임과 로그아웃을 보여준다", async () => {
    renderWithProviders(<Header />);

    await waitFor(() => expect(screen.getByText("테스터")).toBeInTheDocument());
    expect(screen.getByRole("link", { name: "로그아웃" })).toBeInTheDocument();
  });

  it("비로그인이면 로그인 버튼을 보여준다", async () => {
    server.use(
      http.get("/api/members/me", () =>
        HttpResponse.json({ status: 401, code: "UNAUTHENTICATED" }, { status: 401 }),
      ),
    );

    renderWithProviders(<Header />);

    await waitFor(() => expect(screen.getByRole("link", { name: "로그인" })).toBeInTheDocument());
  });
});
