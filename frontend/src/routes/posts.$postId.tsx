import { createFileRoute } from "@tanstack/react-router";
import { PostDetail } from "@/features/post/components/PostDetail";

export const Route = createFileRoute("/posts/$postId")({
  component: PostDetailPage,
});

function PostDetailPage() {
  const { postId } = Route.useParams();
  return <PostDetail postId={Number(postId)} />;
}
