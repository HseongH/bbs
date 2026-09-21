import { createFileRoute } from "@tanstack/react-router";
import { CommentSection } from "@/features/comment/components/CommentSection";
import { PostDetail } from "@/features/post/components/PostDetail";

export const Route = createFileRoute("/posts/$postId")({
  component: PostDetailPage,
});

function PostDetailPage() {
  const { postId } = Route.useParams();
  const id = Number(postId);

  return (
    <div className="space-y-6">
      <PostDetail postId={id} />
      <CommentSection postId={id} />
    </div>
  );
}
