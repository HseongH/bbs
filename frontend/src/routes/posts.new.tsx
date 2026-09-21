import { createFileRoute } from "@tanstack/react-router";
import { PostForm } from "@/features/post/components/PostForm";

export const Route = createFileRoute("/posts/new")({
  component: () => <PostForm mode="create" />,
});
