import { createFileRoute, type SearchSchemaInput, useNavigate } from "@tanstack/react-router";
import { Pagination } from "@/features/post/components/Pagination";
import { PostList } from "@/features/post/components/PostList";
import { SearchForm } from "@/features/post/components/SearchForm";
import { usePostList } from "@/features/post/queries";

type PostListSearchParams = {
  page: number;
  size: number;
  keyword?: string;
};

export const Route = createFileRoute("/")({
  validateSearch: (search: Record<string, unknown> & SearchSchemaInput): PostListSearchParams => {
    const page = Number(search.page);
    const size = Number(search.size);
    const keyword = typeof search.keyword === "string" ? search.keyword.trim() : "";

    return {
      page: Number.isInteger(page) && page >= 0 ? page : 0,
      size: Number.isInteger(size) && size > 0 && size <= 100 ? size : 20,
      ...(keyword ? { keyword } : {}),
    };
  },
  component: PostListPage,
});

function PostListPage() {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: Route.fullPath });
  const { data } = usePostList(search);

  return (
    <div className="space-y-4">
      <SearchForm
        defaultKeyword={search.keyword ?? ""}
        onSearch={(keyword) =>
          navigate({ search: { ...search, page: 0, ...(keyword ? { keyword } : {}) } })
        }
      />
      <PostList search={search} />
      <Pagination
        page={search.page}
        totalPages={data?.totalPages ?? 0}
        onChange={(page) => navigate({ search: { ...search, page } })}
      />
    </div>
  );
}
