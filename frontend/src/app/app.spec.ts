import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideRouter, Router } from "@angular/router";
import { TestBed } from "@angular/core/testing";
import { render, screen } from "@testing-library/angular";
import { http, HttpResponse } from "msw";
import { authInterceptor } from "@/core/auth/auth.interceptor";
import { 로그인회원 } from "@test/handlers";
import { server } from "@test/setup";
import { App } from "./app";
import { routes } from "./app.routes";

async function 화면을_그린다() {
  return render(App, {
    providers: [provideHttpClient(withInterceptors([authInterceptor])), provideRouter([])],
  });
}

describe("App", () => {
  it("없는 경로는 안내를 보여준다", async () => {
    await render(App, {
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideRouter(routes)],
    });

    await TestBed.inject(Router).navigateByUrl("/없는경로");

    expect(await screen.findByText(/찾을 수 없습니다/)).toBeInTheDocument();
  });

  it("로그인 상태면 닉네임과 로그아웃을 보여준다", async () => {
    await 화면을_그린다();

    expect(await screen.findByText("테스터")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "로그아웃" })).toBeInTheDocument();
  });

  it("회원 조회가 끝나기 전에는 로그인도 닉네임도 보여주지 않는다", async () => {
    server.use(
      http.get("/api/members/me", async () => {
        await new Promise((resolve) => setTimeout(resolve, 100));
        return HttpResponse.json(로그인회원);
      }),
    );

    await 화면을_그린다();

    expect(screen.queryByRole("link", { name: "로그인" })).not.toBeInTheDocument();
    expect(await screen.findByText("테스터")).toBeInTheDocument();
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
