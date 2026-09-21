import { useQuery } from "@tanstack/react-query";
import { client } from "@/api/client";
import type { components } from "@/api/schema";

export type PostSummary = components["schemas"]["PostSummaryResponse"];
export type PostDetail = components["schemas"]["PostResponse"];
export type PostPage = components["schemas"]["PageResponsePostSummaryResponse"];

export type PostListSearch = {
  page: number;
  size: number;
  keyword?: string;
  authorId?: number;
};

export const postKeys = {
  all: ["post"] as const,
  lists: () => [...postKeys.all, "list"] as const,
  list: (search: PostListSearch) => [...postKeys.lists(), search] as const,
  details: () => [...postKeys.all, "detail"] as const,
  detail: (id: number) => [...postKeys.details(), id] as const,
};

export function usePostList(search: PostListSearch) {
  return useQuery({
    queryKey: postKeys.list(search),
    queryFn: async (): Promise<PostPage> => {
      const { data, error } = await client.GET("/api/posts", { params: { query: search } });
      if (error) {
        throw error;
      }
      return data;
    },
  });
}

export function usePostDetail(id: number) {
  return useQuery({
    queryKey: postKeys.detail(id),
    queryFn: async (): Promise<PostDetail> => {
      const { data, error } = await client.GET("/api/posts/{id}", { params: { path: { id } } });
      if (error) {
        throw error;
      }
      return data;
    },
  });
}
