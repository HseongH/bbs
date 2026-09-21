import { Link, useNavigate } from "@tanstack/react-router";
import { isProblemCode, toProblem } from "@/api/problem";
import { Button } from "@/components/ui/button";
import { useCurrentMember } from "@/features/member/queries";
import { useDeletePost } from "../mutations";
import { usePostDetail } from "../queries";

export function PostDetail({ postId }: { postId: number }) {
  const { data: post, isPending, error } = usePostDetail(postId);
  const { data: member } = useCurrentMember();
  const navigate = useNavigate();
  const deletePost = useDeletePost();

  if (isPending) {
    return <p className="py-8 text-center text-slate-500">불러오는 중입니다.</p>;
  }
  if (error) {
    const detail = isProblemCode(error, "POST_NOT_FOUND")
      ? "게시글을 찾을 수 없습니다."
      : (toProblem(error)?.detail ?? "게시글을 불러오지 못했습니다.");
    return <p className="py-8 text-center text-slate-600">{detail}</p>;
  }

  const 본인글 = member != null && member.id === post.authorId;

  return (
    <article className="space-y-4">
      <header className="space-y-2 border-b border-slate-200 pb-3">
        <h1 className="text-2xl font-semibold">{post.title}</h1>
        <div className="flex items-center gap-3 text-sm text-slate-500">
          <span>조회 {post.viewCount}</span>
          <span>좋아요 {post.likeCount}</span>
        </div>
      </header>

      <p className="whitespace-pre-wrap leading-relaxed">{post.content}</p>

      {본인글 ? (
        <div className="flex gap-2">
          <Link
            to="/posts/$postId/edit"
            params={{ postId: String(post.id) }}
            className="rounded border border-slate-300 px-3 py-1 text-sm"
          >
            수정
          </Link>
          <Button
            type="button"
            variant="destructive"
            size="sm"
            disabled={deletePost.isPending}
            onClick={() => {
              if (!window.confirm("게시글을 삭제할까요?")) {
                return;
              }
              deletePost.mutate(post.id, { onSuccess: () => void navigate({ to: "/" }) });
            }}
          >
            삭제
          </Button>
        </div>
      ) : null}
    </article>
  );
}
