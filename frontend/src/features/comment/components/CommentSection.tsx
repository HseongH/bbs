import { useCurrentMember } from "@/features/member/queries";
import { useWriteComment } from "../mutations";
import { useCommentList } from "../queries";
import { CommentForm } from "./CommentForm";
import { CommentItem } from "./CommentItem";

export function CommentSection({ postId }: { postId: number }) {
  const { data, isPending, isError } = useCommentList(postId);
  const { data: member } = useCurrentMember();
  const writeComment = useWriteComment(postId);

  return (
    <section className="space-y-3 border-t border-slate-200 pt-4">
      <h2 className="text-lg font-medium">댓글 {data?.totalElements ?? 0}</h2>

      {member ? (
        <CommentForm
          label="댓글"
          submitLabel="등록"
          onSubmit={async (body) => {
            await writeComment.mutateAsync({ body });
          }}
        />
      ) : (
        <p className="text-sm text-slate-500">댓글을 쓰려면 로그인이 필요합니다.</p>
      )}

      {isPending ? <p className="text-sm text-slate-500">불러오는 중입니다.</p> : null}
      {isError ? <p className="text-sm text-red-600">댓글을 불러오지 못했습니다.</p> : null}

      {data ? (
        <ul className="divide-y divide-slate-100">
          {data.content.map((comment) => (
            <CommentItem key={comment.id} comment={comment} postId={postId} />
          ))}
        </ul>
      ) : null}
    </section>
  );
}
