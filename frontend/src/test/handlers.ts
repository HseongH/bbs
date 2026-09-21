import { HttpResponse, http } from "msw";
import type { components } from "@/api/schema";

type MemberResponse = components["schemas"]["MemberResponse"];
type PostSummaryResponse = components["schemas"]["PostSummaryResponse"];
type PageResponsePostSummaryResponse = components["schemas"]["PageResponsePostSummaryResponse"];

export const 로그인회원: MemberResponse = {
  id: 1,
  nickname: "테스터",
  email: "tester@example.com",
};

export function 게시글요약(overrides: Partial<PostSummaryResponse> = {}): PostSummaryResponse {
  return {
    id: 1,
    title: "첫 글",
    authorId: 1,
    authorNickname: "테스터",
    viewCount: 0,
    likeCount: 0,
    createdAt: "2026-09-21T00:00:00Z",
    ...overrides,
  };
}

export function 게시글페이지(content: PostSummaryResponse[]): PageResponsePostSummaryResponse {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: 1,
    last: true,
  };
}

export const handlers = [
  http.get("/api/members/me", () => HttpResponse.json(로그인회원)),
  http.get("/api/posts", () => HttpResponse.json(게시글페이지([게시글요약()]))),
];
