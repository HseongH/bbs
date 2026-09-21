import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/posts/$postId/edit")({
  component: () => <p>수정 준비 중</p>,
});
