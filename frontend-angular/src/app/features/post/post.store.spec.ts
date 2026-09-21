import { provideHttpClient } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { http, HttpResponse } from "msw";
import { 게시글요약, 게시글페이지 } from "@test/handlers";
import { server } from "@test/setup";
import { PostStore } from "./post.store";

function 스토어를_만든다(): PostStore {
  TestBed.configureTestingModule({ providers: [provideHttpClient()] });
  return TestBed.inject(PostStore);
}

describe("PostStore", () => {
  it("검색 조건이 바뀌면 다시 불러온다", async () => {
    const 받은키워드: (string | null)[] = [];
    server.use(
      http.get("/api/posts", ({ request }) => {
        받은키워드.push(new URL(request.url).searchParams.get("keyword"));
        return HttpResponse.json(게시글페이지([게시글요약()]));
      }),
    );

    const store = 스토어를_만든다();
    store.setSearch({ page: 0, size: 20 });
    await vi.waitFor(() => expect(store.list.hasValue()).toBe(true));

    store.setSearch({ page: 0, size: 20, keyword: "스프링" });
    await vi.waitFor(() => expect(받은키워드).toContain("스프링"));
  });

  it("공백뿐인 키워드는 조건에서 빠진다", async () => {
    let 받은키워드: string | null = "미확인";
    server.use(
      http.get("/api/posts", ({ request }) => {
        받은키워드 = new URL(request.url).searchParams.get("keyword");
        return HttpResponse.json(게시글페이지([]));
      }),
    );

    const store = 스토어를_만든다();
    store.setSearch({ page: 0, size: 20, keyword: "   " });

    await vi.waitFor(() => expect(받은키워드).toBeNull());
  });
});
