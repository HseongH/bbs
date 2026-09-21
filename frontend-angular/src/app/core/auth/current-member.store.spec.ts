import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { http, HttpResponse } from "msw";
import { server } from "@test/setup";
import { authInterceptor } from "./auth.interceptor";
import { CurrentMemberStore } from "./current-member.store";

function 스토어를_만든다(): CurrentMemberStore {
  TestBed.configureTestingModule({
    providers: [provideHttpClient(withInterceptors([authInterceptor]))],
  });
  return TestBed.inject(CurrentMemberStore);
}

describe("CurrentMemberStore", () => {
  it("로그인 상태면 회원 정보를 노출한다", async () => {
    const store = 스토어를_만든다();

    await vi.waitFor(() => expect(store.member()?.nickname).toBe("테스터"));
  });

  it("미인증이면 null이고 로그인으로 이동하지 않는다", async () => {
    server.use(
      http.get("/api/members/me", () =>
        HttpResponse.json({ status: 401, code: "UNAUTHENTICATED" }, { status: 401 }),
      ),
    );
    sessionStorage.clear();

    const store = 스토어를_만든다();

    await vi.waitFor(() => expect(store.member()).toBeNull());
    expect(sessionStorage.getItem("bbs:redirectAfterLogin")).toBeNull();
  });
});
