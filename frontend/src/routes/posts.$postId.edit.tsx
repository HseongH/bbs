import { createFileRoute } from "@tanstack/react-router";
import { PostForm } from "@/features/post/components/PostForm";
import { usePostDetail } from "@/features/post/queries";

export const Route = createFileRoute("/posts/$postId/edit")({
  component: PostEditPage,
});

function PostEditPage() {
  const { postId } = Route.useParams();
  const { data: post, isPending, isError } = usePostDetail(Number(postId));

  if (isPending) {
    return <p className="py-8 text-center text-slate-500">불러오는 중입니다.</p>;
  }
  if (isError) {
    return <p className="py-8 text-center text-slate-600">게시글을 불러오지 못했습니다.</p>;
  }

  return (
    <PostForm
      mode="edit"
      postId={Number(postId)}
      initial={{ title: post.title, content: post.content }}
    />
  );
}
