import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideRouter } from "@angular/router";
import { render, screen } from "@testing-library/angular";
import { http, HttpResponse } from "msw";
import { authInterceptor } from "@/core/auth/auth.interceptor";
import { server } from "@test/setup";
import { App } from "./app";

async function 화면을_그린다() {
  return render(App, {
    providers: [provideHttpClient(withInterceptors([authInterceptor])), provideRouter([])],
  });
}

describe("App", () => {
  it("로그인 상태면 닉네임과 로그아웃을 보여준다", async () => {
    await 화면을_그린다();

    expect(await screen.findByText("테스터")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "로그아웃" })).toBeInTheDocument();
  });

  it("비로그인이면 로그인 버튼을 보여준다", async () => {
    server.use(
      http.get("/api/members/me", () =>
        HttpResponse.json({ status: 401, code: "UNAUTHENTICATED" }, { status: 401 }),
      ),
    );

    await 화면을_그린다();

    expect(await screen.findByRole("link", { name: "로그인" })).toBeInTheDocument();
  });
});
