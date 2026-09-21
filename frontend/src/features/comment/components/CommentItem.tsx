import { useState } from "react";
import { useCurrentMember } from "@/features/member/queries";
import { useDeleteComment, useUpdateComment, useWriteComment } from "../mutations";
import type { Comment } from "../queries";
import { CommentForm } from "./CommentForm";

type 표시모드 = "보기" | "수정" | "답글";

export function CommentItem({ comment, postId }: { comment: Comment; postId: number }) {
  const { data: member } = useCurrentMember();
  const [모드, set모드] = useState<표시모드>("보기");

  const updateComment = useUpdateComment(postId);
  const deleteComment = useDeleteComment(postId);
  const writeComment = useWriteComment(postId);

  const 본인댓글 = member != null && member.id === comment.authorId;
  // 답글의 답글은 서버가 400으로 거부한다. 버튼 자체를 내보내지 않아 그 전에 막는다.
  const 답글가능 = member != null && comment.depth === 0;

  return (
    <li className={comment.depth > 0 ? "border-l-2 border-slate-200 py-2 pl-6" : "py-2"}>
      {모드 === "수정" ? (
        <CommentForm
          label="댓글 수정"
          submitLabel="수정"
          defaultValue={comment.body}
          onSubmit={async (body) => {
            await updateComment.mutateAsync({ id: comment.id, body });
            set모드("보기");
          }}
        />
      ) : (
        <p className="whitespace-pre-wrap text-sm">{comment.body}</p>
      )}

      <div className="mt-1 flex gap-2 text-xs text-slate-500">
        {답글가능 ? (
          <button type="button" onClick={() => set모드(모드 === "답글" ? "보기" : "답글")}>
            답글 달기
          </button>
        ) : null}
        {본인댓글 ? (
          <>
            <button type="button" onClick={() => set모드(모드 === "수정" ? "보기" : "수정")}>
              수정
            </button>
            <button
              type="button"
              className="text-red-600"
              onClick={() => {
                if (window.confirm("댓글을 삭제할까요?")) {
                  deleteComment.mutate(comment.id);
                }
              }}
            >
              삭제
            </button>
          </>
        ) : null}
      </div>

      {모드 === "답글" ? (
        <div className="mt-2 pl-6">
          <CommentForm
            label="답글"
            submitLabel="등록"
            onSubmit={async (body) => {
              await writeComment.mutateAsync({ body, parentCommentId: comment.id });
              set모드("보기");
            }}
          />
        </div>
      ) : null}
    </li>
  );
}
