import type { Routes } from "@angular/router";
import { authGuard } from "@/core/auth/auth.guard";

export const routes: Routes = [
  {
    path: "",
    pathMatch: "full",
    title: "게시글 목록",
    loadComponent: () => import("@/features/post/pages/post-list-page").then((m) => m.PostListPage),
  },
  {
    path: "posts/new",
    title: "글쓰기",
    canActivate: [authGuard],
    loadComponent: () => import("@/features/post/pages/post-new-page").then((m) => m.PostNewPage),
  },
  {
    path: "posts/:postId/edit",
    title: "게시글 수정",
    canActivate: [authGuard],
    loadComponent: () => import("@/features/post/pages/post-edit-page").then((m) => m.PostEditPage),
  },
  {
    path: "posts/:postId",
    title: "게시글",
    loadComponent: () =>
      import("@/features/post/pages/post-detail-page").then((m) => m.PostDetailPage),
  },
  {
    path: "**",
    title: "페이지를 찾을 수 없음",
    loadComponent: () => import("@/shared/ui/not-found-page").then((m) => m.NotFoundPage),
  },
];
