import { useQuery } from "@tanstack/react-query";
import { client } from "@/api/client";
import type { components } from "@/api/schema";

export type Comment = components["schemas"]["CommentResponse"];
export type CommentPage = components["schemas"]["PageResponseCommentResponse"];

export const commentKeys = {
  all: ["comment"] as const,
  lists: () => [...commentKeys.all, "list"] as const,
  list: (postId: number) => [...commentKeys.lists(), postId] as const,
};

/** 게시판 규모에서 댓글 페이지네이션은 UI만 복잡하게 만든다. 한 번에 가져온다. */
export function useCommentList(postId: number) {
  return useQuery({
    queryKey: commentKeys.list(postId),
    queryFn: async (): Promise<CommentPage> => {
      const { data, error } = await client.GET("/api/posts/{postId}/comments", {
        params: { path: { postId }, query: { page: 0, size: 100 } },
      });
      if (error) {
        throw error;
      }
      return data;
    },
  });
}
