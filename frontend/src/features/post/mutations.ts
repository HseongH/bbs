import { useMutation, useQueryClient } from "@tanstack/react-query";
import { client } from "@/api/client";
import { postKeys } from "./queries";

export function useCreatePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: { title: string; content: string }): Promise<string> => {
      const { response, error } = await client.POST("/api/posts", { body: input });
      if (error) {
        throw error;
      }
      return response.headers.get("Location") ?? "/";
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
}

export function useUpdatePost(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: { title: string; content: string }): Promise<void> => {
      const { error } = await client.PATCH("/api/posts/{id}", {
        params: { path: { id } },
        body: input,
      });
      if (error) {
        throw error;
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.detail(id) });
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
}

export function useDeletePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number): Promise<void> => {
      const { error } = await client.DELETE("/api/posts/{id}", { params: { path: { id } } });
      if (error) {
        throw error;
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
}

/** 실패했을 때도 서버의 실제 값으로 되돌려야 하므로 onSuccess가 아니라 onSettled에서 무효화한다. */
export function useLikePost(postId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (): Promise<void> => {
      const { error } = await client.POST("/api/posts/{id}/likes", {
        params: { path: { id: postId } },
      });
      if (error) {
        throw error;
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.detail(postId) });
    },
  });
}

export function useUnlikePost(postId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (): Promise<void> => {
      const { error } = await client.DELETE("/api/posts/{id}/likes", {
        params: { path: { id: postId } },
      });
      if (error) {
        throw error;
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.detail(postId) });
    },
  });
}
