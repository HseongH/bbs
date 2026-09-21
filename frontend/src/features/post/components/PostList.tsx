import { Link } from "@tanstack/react-router";
import { type PostListSearch, usePostList } from "../queries";

export function PostList({ search }: { search: PostListSearch }) {
  const { data, isPending, isError } = usePostList(search);

  if (isPending) {
    return <p className="py-8 text-center text-slate-500">불러오는 중입니다.</p>;
  }
  if (isError) {
    return <p className="py-8 text-center text-red-600">목록을 불러오지 못했습니다.</p>;
  }
  if (data.content.length === 0) {
    return <p className="py-8 text-center text-slate-500">게시글이 없습니다.</p>;
  }

  return (
    <ul className="divide-y divide-slate-200">
      {data.content.map((post) => (
        <li key={post.id} className="py-3">
          <Link
            to="/posts/$postId"
            params={{ postId: String(post.id) }}
            className="font-medium hover:underline"
          >
            {post.title}
          </Link>
          <div className="mt-1 flex gap-3 text-sm text-slate-500">
            <span>{post.authorNickname}</span>
            <span>조회 {post.viewCount}</span>
            <span>좋아요 {post.likeCount}</span>
          </div>
        </li>
      ))}
    </ul>
  );
}
