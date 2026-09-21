import { useMutation, useQueryClient } from "@tanstack/react-query";
import { client } from "@/api/client";
import { commentKeys } from "./queries";

export function useWriteComment(postId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: { body: string; parentCommentId?: number }): Promise<void> => {
      const { error } = await client.POST("/api/posts/{postId}/comments", {
        params: { path: { postId } },
        body: input,
      });
      if (error) {
        throw error;
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: commentKeys.list(postId) });
    },
  });
}

export function useUpdateComment(postId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: { id: number; body: string }): Promise<void> => {
      const { error } = await client.PATCH("/api/comments/{id}", {
        params: { path: { id: input.id } },
        body: { body: input.body },
      });
      if (error) {
        throw error;
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: commentKeys.list(postId) });
    },
  });
}

export function useDeleteComment(postId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number): Promise<void> => {
      const { error } = await client.DELETE("/api/comments/{id}", { params: { path: { id } } });
      if (error) {
        throw error;
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: commentKeys.list(postId) });
    },
  });
}
