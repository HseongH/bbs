import { useOptimistic, useState, useTransition } from "react";
import { toProblem } from "@/api/problem";
import { Button } from "@/components/ui/button";
import { useLikePost } from "../mutations";

export function LikeButton({ postId, likeCount }: { postId: number; likeCount: number }) {
  const likePost = useLikePost(postId);
  const [message, setMessage] = useState<string | null>(null);
  const [, startTransition] = useTransition();
  const [보이는수, 낙관적으로증가] = useOptimistic(likeCount, (current: number) => current + 1);

  return (
    <span className="inline-flex items-center gap-2">
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={() => {
          setMessage(null);
          startTransition(async () => {
            // 트랜지션이 끝나면 낙관적 값은 자동으로 실제 값으로 되돌아간다.
            낙관적으로증가(null);
            try {
              await likePost.mutateAsync();
            } catch (error) {
              setMessage(toProblem(error)?.detail ?? "좋아요에 실패했습니다.");
            }
          });
        }}
      >
        좋아요 {보이는수}
      </Button>
      {message ? <span className="text-sm text-red-600">{message}</span> : null}
    </span>
  );
}
