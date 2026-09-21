import { useNavigate } from "@tanstack/react-router";
import { useActionState } from "react";
import { fieldErrors, toProblem } from "@/api/problem";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useCreatePost, useUpdatePost } from "../mutations";

type FormState = {
  fields: Record<string, string>;
  message: string | null;
};

const 초기상태: FormState = { fields: {}, message: null };

type PostFormProps =
  | { mode: "create"; postId?: undefined; initial?: undefined }
  | { mode: "edit"; postId: number; initial: { title: string; content: string } };

export function PostForm({ mode, postId, initial }: PostFormProps) {
  const navigate = useNavigate();
  const createPost = useCreatePost();
  const updatePost = useUpdatePost(postId ?? 0);

  const [state, submit, isPending] = useActionState(
    async (_previous: FormState, formData: FormData): Promise<FormState> => {
      const input = {
        title: String(formData.get("title") ?? ""),
        content: String(formData.get("content") ?? ""),
      };

      try {
        if (mode === "create") {
          const location = await createPost.mutateAsync(input);
          const id = location.replace(/.*\//, "");
          await navigate({ to: "/posts/$postId", params: { postId: id } });
        } else {
          await updatePost.mutateAsync(input);
          await navigate({ to: "/posts/$postId", params: { postId: String(postId) } });
        }
        return 초기상태;
      } catch (error) {
        return {
          fields: fieldErrors(error),
          message: toProblem(error)?.detail ?? "저장하지 못했습니다.",
        };
      }
    },
    초기상태,
  );

  return (
    <form action={submit} className="space-y-4">
      <div className="space-y-1">
        <label htmlFor="title" className="block text-sm font-medium">
          제목
        </label>
        <Input id="title" name="title" defaultValue={initial?.title ?? ""} />
        {state.fields.title ? <p className="text-sm text-red-600">{state.fields.title}</p> : null}
      </div>

      <div className="space-y-1">
        <label htmlFor="content" className="block text-sm font-medium">
          본문
        </label>
        <Textarea id="content" name="content" rows={12} defaultValue={initial?.content ?? ""} />
        {state.fields.content ? (
          <p className="text-sm text-red-600">{state.fields.content}</p>
        ) : null}
      </div>

      {state.message && Object.keys(state.fields).length === 0 ? (
        <p className="text-sm text-red-600">{state.message}</p>
      ) : null}

      <Button type="submit" disabled={isPending}>
        저장
      </Button>
    </form>
  );
}
